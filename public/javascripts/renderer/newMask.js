
function getRandomColor() {
  var letters = '0123456789ABCDEF'.split('');
  var color = '#';
  for (var i = 0; i < 6; i++ ) {
    color += letters[Math.floor(Math.random() * 16)];
  }
  return color;
}

function maskError( jqXHR, textStatus, errorThrown ) {
  /*
  var $mask = $( '#maskNew' );
  var maskContext = $mask[0].getContext( '2d' );
  maskContext.font = '15px Arial';
  maskContext.fillText( 'Mask is not available', 10, 20 );
  */
  alert( 'Mask is not available');
}
function tracesError( jqXHR, textStatus, errorThrown ) {
  alert( 'Request was unsuccesfull: '+ textStatus+'\n\n'+errorThrown );
}

function drawMask( data, textStatus, jqXHR ) {
  if( !data[0] )
    return maskError( jqXHR, 'error' );

  data = data[0];

  var media = data.medialocator;
  var quality = data.quality;

  var $mask = $( '#maskNew' );
  var maskElement = $mask[0];
  var maskContext = maskElement.getContext( '2d' );

  var $newMask = $( '<canvas></canvas>' );
  $newMask.prop( 'id', 'maskNewCont' );
  $newMask.css( 'visibility', 'hidden' );
  $( document.body ).append( $newMask );

  var newMaskElement = $newMask[0];
  var newMaskContext = newMaskElement.getContext( '2d' );

  // Image
  var maskImageUrl = '/retrieveMaskImage?media='+media;
  var maskImage = new Image();

  // Save function context
  var _this = this;

  maskImage.onload = function maskImageLoaded() {
    newMaskElement.width = window.innerWidth*0.8/4;
    newMaskElement.height = window.innerWidth*0.8/4*this.height/this.width;
    maskElement.width = window.innerWidth*0.8/4;
    maskElement.height = window.innerWidth*0.8/4*this.height/this.width;


    newMaskContext.drawImage( maskImage, 0, 0, newMaskElement.width, newMaskElement.height );
    var imData = newMaskContext.getImageData( 0, 0, newMaskElement.width, newMaskElement.height );

    // Change the alpha of the valid points
    for(var i=0; i<imData.data.length; i+=4 ) {
      var r,g,b,a;

      r = imData.data[ i ];
      g = imData.data[ i+1 ];
      b = imData.data[ i+2 ];

      if( r===0 && g===0 && b===0 ) {
        imData.data[ i+3 ] = 170;
      } else {
        imData.data[ i+3 ] = 255;
      }
    }

    newMaskContext.putImageData( imData, 0, 0 );
    maskContext.globalCompositeOperation = 'copy';
    maskContext.drawImage( newMaskElement, 0, 0 );
    maskContext.globalCompositeOperation = 'darker';
    maskContext.drawImage( _this.taskImage, 0, 0, maskElement.width, maskElement.height );
    maskContext.globalCompositeOperation = 'source-over';
    maskContext.font = 'bold 15px Arial';
    maskContext.fillStyle = 'white';
    maskContext.fillText( 'Quality: '+quality, 10, 20 );
  };

  maskImage.src = maskImageUrl;
}

function drawTraces( data, textStatus, jqXHR ) {
  var numAnnotations = data.length;

  var $tagTitle = $( '#tagTitle' );
  var $maskButtons = $("#maskButtons");

  var $drawCanvas = $( '#draws' );
  var $taskCanvas = $( '#task' );
  var taskCanvasElement = $taskCanvas[0];
  var taskContext = taskCanvasElement.getContext( '2d' );


  // Set the canvas sizes
  $drawCanvas.width( taskCanvasElement.width );
  $drawCanvas.height( taskCanvasElement.height );

  // Update the tag title
  $tagTitle.replaceWith( '<div id="tagTitle" class="panelTitle"> Tag "'+this.tagName+'" ('+numAnnotations+' annotations)</div>' );
  $maskButtons.replaceWith( "<div class='span12' id='maskButtons'>"+
              "<a class='btn' onclick=\"loadMask('"+this.tagName+"')\"><strong>View mask without spam detector</strong></a>"+
              "<a class='btn' onclick=\"loadMaskFashionista('"+this.idImage+"','"+this.tagName+"')\"><strong>Cloth parser (Fashionista)</strong></a>"+
              "</div>" );

  var _this = this;

  function fY( y ) {
    return Math.round( y*taskCanvasElement.height/_this.height );
  }
  function fX( x ) {
    return Math.round( x*taskCanvasElement.width/_this.width );
  }


  function paintTrace() {
    var result = data.shift();
    if( !result )
      return;

    var points = JSON.parse( result.points );
    var drawable = true;
    var firstOutside = false;
  

    taskContext.lineJoin = 'round';
    taskContext.beginPath();
    taskContext.strokeStyle = getRandomColor();


    var firstPoint = points[ 0 ];
    var oldPointX=fX( firstPoint.x );
    var oldPointY=fY( firstPoint.y );
    taskContext.moveTo(oldPointX , oldPointY );
    
    if( firstPoint.x>_this.width || firstPoint.y>_this.height){
    	//first pont outside of canvas
    	firstOutside=true;
    	drawable=false;
    }

    for (var i = 0; i < points.length; i++) {
      var point = points[i];

      if( point.x>_this.width || point.y>_this.height)
        continue;

      if( point.end!==true ) {

    	  var newPointX=fX( point.x );
          var newPointY=fY( point.y );
    	  
        if( !drawable ) {
          drawable = true;
          taskContext.beginPath();
          taskContext.moveTo( newPointX, newPointY );
        }
        
        
        
        if(newPointX-oldPointX > 3 || newPointX-oldPointX < 3 || newPointY-oldPointY >3 || newPointY-oldPointY < 3){
        	taskContext.stroke();
        	taskContext.beginPath();
            taskContext.moveTo( newPointX, newPointY );
        }
        

        taskContext.lineWidth = 3;
        taskContext.lineTo( newPointX, newPointY );
        

        oldPointX=newPointX;
        oldPointY=newPointY;

      } else {

        if( drawable ) {
          taskContext.stroke();
          drawable = false;
          
        }

      }
    }

    // Last stroke, just in case
    if( drawable )
    	
    		taskContext.stroke();

    // Draw the next track in 100ms
    setTimeout( paintTrace, 100 );
  }

  // Start painting the traces
  paintTrace();
}






function newMask(idTag, idImage, tagName, numAnnotations,im_width,im_height){

  // Reset canvas
	var canvasIds = [ 'maskNew', 'mask', 'maskFashion', 'draws', 'task' ];
  $.each( canvasIds, function clearCanvas( id ) {
    var $canvas = $( '#'+id );
    if( $canvas.length>0 ) {
      var canvas = $canvas[0];
      var ctx = canvas.getContext( '2d' );
      ctx.clearRect( 0, 0, canvas.width, canvas.height );
    }
  } );

  // Task canvas + context
  var $taskCanvas = $( '#task' );
  var taskContext = $taskCanvas[0].getContext( '2d' );

  // Image
  var imageUrl = $( '#mediaLocator' ).val();
  var taskImage = new Image();
  taskImage.src = imageUrl;
  taskContext.drawImage( taskImage, 0, 0, $taskCanvas.width(), $taskCanvas.height() );

  // function context (aka 'this')
  var context = {
    width: im_width,
    height: im_height,
    tagName: tagName,
    idImage: idImage,
    numAnnotations: numAnnotations,
    taskImage: taskImage
  };

  // Request the traces
  $.ajax( {
    url: 'getTraces',
    dataType: 'json',
    headers : {
      'idImage' : idImage,
      'tagName' : tagName
    },
    context: context
  } )
  .done( drawTraces )
  .fail( tracesError );


  // Request the masks
  $.ajax( {
    url: 'MaskAjax',
    dataType: 'json',
    headers : {
      'idImage' : idImage,
      'tag' : tagName
    },
    context: context
  } )
  .done( drawMask )
  .fail( maskError );
}
