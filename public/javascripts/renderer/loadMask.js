function loadMask(tag) {

var selectionimg = $("#ImgAttivattiva").val()
    ,mediaimg = $("#mediaLocator").val()
    ,canvas = document.getElementById("draws")
    ,ctx = canvas.getContext("2d")
    ,taskCanvas = document.getElementById("task")
    ,taskContext = taskCanvas.getContext("2d")
    ,taskImage=new Image();
	taskImage.src=mediaimg;

 
 var viewport = document.getElementById("viewport");
 $(viewport).show();


 $("#ImgPreview").val($("#ImgAttivattiva").val());
 var maskCanvas = document.getElementById("mask");
 var maskContext = maskCanvas.getContext("2d");
 maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);



	var taskCanvas = document.getElementById("task");
	var taskContext = taskCanvas.getContext("2d");
	var maskCanvas = document.getElementById("mask");
	var maskContext = maskCanvas.getContext("2d");
	maskCanvas.width=taskCanvas.width;
	maskCanvas.height=taskCanvas.height;

	maskContext.font="15px Arial";
	maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
	maskContext.fillText("Computing aggregated mask...",10,20);


	var maskImage = new Image();
	maskImage.src = "/retrieveMask?imageID="+selectionimg+"&tag="+tag;
	maskImage.onload = function() {

		if((maskImage.src.substring(maskImage.src.indexOf("=")+1,maskImage.src.indexOf("&"))==$("#ImgAttivattiva").val())&& ($("#ImgPreview").val() == $("#ImgAttivattiva").val())){
			maskContext.save();
			maskContext.beginPath();
			maskCanvas.width = window.innerWidth*0.8/4;
			maskCanvas.height = window.innerWidth*0.8/4*this.height/this.width;
			/*
			maskContext.drawImage(taskImage,0,0,maskCanvas.width,maskCanvas.height);
			maskContext.drawImage(maskImage,0,0,maskCanvas.width,maskCanvas.height);
			maskContext.restore();
			*/
			
			maskContext.drawImage(maskImage,0,0,maskCanvas.width,maskCanvas.height);
			maskContext.globalCompositeOperation = 'darker';
			maskContext.drawImage(taskImage,0,0,maskCanvas.width,maskCanvas.height);
			
			maskContext.globalCompositeOperation = 'source-over';
			maskContext.font="bold 15px Arial";
			maskContext.fillStyle = 'white';
			maskContext.fillText('Quality: unknown', 10,20);
	     };
	}


  
};
