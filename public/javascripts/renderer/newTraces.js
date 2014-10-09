function getRandomColor() {
	var letters = '0123456789ABCDEF'.split('');
	var color = '#';
	for ( var i = 0; i < 6; i++) {
		color += letters[Math.floor(Math.random() * 16)];
	}
	return color;
}

function tracesError(jqXHR, textStatus, errorThrown) {
	alert('Request was unsuccesfull: ' + textStatus + '\n\n' + errorThrown);
}

function drawTraces(data, textStatus, jqXHR) {
	var numAnnotations = data.length;

	var $tagTitle = $('#tagTitle');
	var $maskButtons = $("#maskButtons");

	var $drawCanvas = $('#draws');
	var $taskCanvas = $('#task');
	var taskCanvasElement = $taskCanvas[0];
	var taskContext = taskCanvasElement.getContext('2d');

	if (this.idAnno == null) {
		if (document.getElementById("actions") != null) {
			this.idAnno = document.getElementById("actions").value;
		} else {
			this.idAnno="all";
		}
	}

	// Set the canvas sizes
	$drawCanvas.width(taskCanvasElement.width);
	$drawCanvas.height(taskCanvasElement.height);

	var options = "<option>all</option>";
	for ( var i = 0; i < data.length; i++) {
		var id = data[i].id;
		if (this.idAnno == id) {
			options += "<option value=\"" + id + "\" selected>" + id
					+ "</option>";
		} else {
			options += "<option value=\"" + id + "\">" + id + "</option>";
		}

	}

	// Update the tag title
	
	//bottone fashionista:
//	"<a class='btn' onclick=\"loadMaskFashionista('"
//	+ this.idImage
//	+ "','"
//	+ this.tagName
//	+ "') \"><strong>Cloth parser (Fashionista)</strong></a>"
	
	$tagTitle.replaceWith('<div id="tagTitle" class="panelTitle"> Tag "'
			+ this.tagName + '" (' + numAnnotations + ' annotations)</div>');
	$maskButtons.replaceWith("<div class='span12' id='maskButtons'>"
			+ "<a class='btn' onclick=\"loadMask('"
			+ this.tagName
			+ "')\"><strong>View mask without spam detector</strong></a>"
			+ "<select onChange=\"onChange=newTraces('"
			+ this.idTag
			+ "', '"
			+ this.idImage
			+ "', '"
			+ this.tagName
			+ "', '"
			+ numAnnotations
			+ "', '"
			+ this.width
			+ "', '"
			+ this.height
			+ "', null)\";' class='btn' name='actions' id='actions'>"
			+ options + "</select></div>");
	
	
	var _this = this;

	function fY(y) {
		return Math.round(y * taskCanvasElement.height / _this.height);
	}
	function fX(x) {
		return Math.round(x * taskCanvasElement.width / _this.width);
	}

	function paintTrace(idAnno) {
		var result = data.shift();
		if (!result)
			return;

		if (idAnno != 'all' && result.id != idAnno) {
			paintTrace(idAnno);
			return;
		}

		var points = JSON.parse(result.points);
		var drawable = true;
		var firstOutside = false;

		taskContext.lineJoin = 'round';
		taskContext.beginPath();
		taskContext.strokeStyle = getRandomColor();

		var firstPoint = points[0];
		var oldPointX = fX(firstPoint.x);
		var oldPointY = fY(firstPoint.y);
		taskContext.moveTo(oldPointX, oldPointY);

		if (firstPoint.x > _this.width || firstPoint.y > _this.height) {
			// first pont outside of canvas
			firstOutside = true;
			drawable = false;
		}

		for ( var i = 0; i < points.length; i++) {
			var point = points[i];

			if (point.x > _this.width || point.y > _this.height)
				continue;

			if (point.end !== true) {

				var newPointX = fX(point.x);
				var newPointY = fY(point.y);

				if (!drawable) {
					drawable = true;
					taskContext.beginPath();
					taskContext.moveTo(newPointX, newPointY);
				}

				// if(newPointX-oldPointX > 40 || newPointX-oldPointX < -40 ||
				// newPointY-oldPointY >40 || newPointY-oldPointY < -40){
				// taskContext.stroke();
				// taskContext.beginPath();
				// taskContext.moveTo( newPointX, newPointY );
				// }

				taskContext.lineWidth = 3;
				taskContext.lineTo(newPointX, newPointY);

				oldPointX = newPointX;
				oldPointY = newPointY;

			} else {

				if (drawable) {
					taskContext.stroke();
					drawable = false;

				}

			}
		}

		// Last stroke, just in case
		if (drawable)

			taskContext.stroke();

		// Draw the next track in 100ms
		setTimeout(paintTrace(idAnno), 100);
	}

	// Start painting the traces
	paintTrace(this.idAnno);
}

function newTraces(idTag, idImage, tagName, numAnnotations, im_width,
		im_height, idAnno) {

	// Reset canvas
	var canvasIds = [ 'maskNew', 'mask', 'maskFashion', 'draws', 'task' ];
	$.each(canvasIds, function clearCanvas(id) {
		var $canvas = $('#' + id);
		if ($canvas.length > 0) {
			var canvas = $canvas[0];
			var ctx = canvas.getContext('2d');
			ctx.clearRect(0, 0, canvas.width, canvas.height);
		}
	});

	// Task canvas + context
	var $taskCanvas = $('#task');
	var taskContext = $taskCanvas[0].getContext('2d');

	// Image
	var imageUrl = $('#mediaLocator').val();
	var taskImage = new Image();
	taskImage.src = imageUrl;
	taskContext.drawImage(taskImage, 0, 0, $taskCanvas.width(), $taskCanvas
			.height());

	// function context (aka 'this')
	var context = {
		width : im_width,
		height : im_height,
		tagName : tagName,
		idTag : idTag,
		idImage : idImage,
		numAnnotations : numAnnotations,
		taskImage : taskImage,
		idAnno : idAnno
	};

	// Request the traces
	$.ajax({
		url : 'getTraces',
		dataType : 'json',
		headers : {
			'idImage' : idImage,
			'tagName' : tagName
		},
		context : context
	}).done(drawTraces).fail(tracesError);

}
