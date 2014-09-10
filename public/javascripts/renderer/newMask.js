
/////////////////////////////////////////////////////////////////////////////////OLD
/*
var rendererGlobalVar = (function() {
var socket;


    return {
setSocket: function(val) {
socket=val;
},
getSocket: function() {
            return socket;
        }
    };
})();

var imageH, imageW;


function newMask(idTag, idImage, tagName, numAnnotations){

	//clear masks
	var maskCanvas = document.getElementById("maskNew");
	var maskContext = maskCanvas.getContext("2d");
	maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
	var maskCanvas2 = document.getElementById("mask");
	var maskContext2 = maskCanvas2.getContext("2d");
	maskContext2.clearRect(0,0,maskCanvas2.width,maskCanvas2.height);
	var maskCanvas3 = document.getElementById("maskFashion");
	var maskContext3 = maskCanvas3.getContext("2d");
	maskContext3.clearRect(0,0,maskCanvas3.width,maskCanvas3.height);
	var maskCanvas4 = document.getElementById("draws");
	var maskContext4 = maskCanvas4.getContext("2d");
	maskContext4.clearRect(0,0,maskCanvas4.width,maskCanvas4.height);
	   

	 var url= "http://54.228.220.100/";
	 var maskImage;
	 var canvas = document.getElementById("maskNew");
	 var ctx = canvas.getContext("2d");
	 var maskCanvas = document.getElementById("maskNew");
	 var maskContext = maskCanvas.getContext("2d");
	 var viewport = document.getElementById("viewport");
	
	 var tagTitle = $("#tagTitle");
	 tagTitle.replaceWith("<div id='tagTitle' class='panelTitle'> Tag '"+tagName+"' ("+numAnnotations+" annotations)</div>");
	 var maskButtons = $("#maskButtons");
	 maskButtons.replaceWith("<div class='span12' id='maskButtons'>"+
	 						"<a class='btn' onclick=\"loadMask('"+tagName+"')\"><i class='icon-picture'></i> <strong>View mask without spam detector</strong></a>"+
	 						"<a class='btn' onclick=\"loadMaskFashionista('"+idImage+"','"+tagName+"')\"><i class='icon-picture'></i> <strong>Cloth parser (Fashionista)</strong></a>"+
	 						"</div>");

	var selectionimg = $("#ImgAttivattiva").val()
    ,mediaimg = $("#mediaLocator").val()
    ,canvas = document.getElementById("draws")
    ,ctx = canvas.getContext("2d")
    ,taskCanvas = document.getElementById("task")
    ,taskContext = taskCanvas.getContext("2d")
    ,taskImage=new Image();
	taskImage.src=mediaimg;


 var graph1 = document.getElementById("chart_div1");
 var graph2 = document.getElementById("chart_div2");
 var viewport = document.getElementById("viewport");

 $(viewport).show();
 $(graph1).hide();
 $(graph2).hide();

 $("#ImgPreview").val($("#ImgAttivattiva").val());

taskImage.onload = function() {
             taskContext.save();
             taskContext.beginPath();
             imageW=this.width;
             imageH=this.height;
      			 canvas.width=window.innerWidth*0.8/4;
      			 canvas.height=window.innerWidth*0.8/4*this.height/this.width;
      			 taskCanvas.width = window.innerWidth*0.8/4;
      			 taskCanvas.height = window.innerWidth*0.8/4*this.height/this.width;
             taskContext.drawImage(taskImage,0,0,taskCanvas.width,taskCanvas.height);
             taskContext.restore();
             //Clear the mask canvas
             var maskCanvas = document.getElementById("mask");
             var maskContext = maskCanvas.getContext("2d");
             maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
         };


try {
var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
socket = new WS("ws://"+window.location.host+"/rendererStream?imageID="+selectionimg);

      socket.onmessage = onSocketMessage;

      socket.onopen = function(evt) {
		console.log( evt, this );
        connected = true;
        rendererGlobalVar.setSocket(socket);
		drawTracesMask(taskImage,idImage,idTag, tagName);
      };

      socket.onclose = function (evt) {
            connected = false;
      };

      socket.onerror = function(evt) {console.error("error", evt);};

    }
    catch (e) {
		console.error("Error in the connection");
		console.error(e);
    }
}

var drawTracesMask = function(taskImage,selectionimg,idTag,tag) {
	
var canvas = document.getElementById("draws");
var ctx = canvas.getContext("2d");
var taskCanvas = document.getElementById("task");
var taskContext = taskCanvas.getContext("2d");
var maskCanvas = document.getElementById("mask");
var maskContext = maskCanvas.getContext("2d");
maskCanvas.width=taskCanvas.width;
maskCanvas.height=taskCanvas.height;
var url= "http://54.228.220.100/";
var maskImage;
var canvas = document.getElementById("maskNew");
var ctx = canvas.getContext("2d");
var maskCanvas = document.getElementById("maskNew");
var maskContext = maskCanvas.getContext("2d");
var viewport = document.getElementById("viewport");
		

        var serverSocket = rendererGlobalVar.getSocket();
        serverSocket.send(JSON.stringify({"type":"tag","tag":tag}));
        var maskImage = null;
        


        $.jAjax({
            url: "MaskAjax",
            headers : {
                "idImage" : selectionimg,
                "idTag" : idTag
            },
            onComplete: function(xhr,status){
                if(xhr.readyState === 4){
                    if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

                   	 	var result = JSON.parse(xhr.responseText);
                   	
                        var media = result[0].medialocator;
                        var quality = result[0].quality;
                       
                        
                        var selectionimg = $("#ImgAttivattiva").val()
                        ,mediaimg = $("#mediaLocator").val()
                        ,canvas = document.getElementById("draws")
                        ,ctx = canvas.getContext("2d")
                        ,taskCanvas = document.getElementById("task")
                        ,taskContext = taskCanvas.getContext("2d")
                        ,taskImage=new Image();
                        taskImage.src=mediaimg;
                        
                        var canvasmasknew = document.createElement("canvas");
                        canvasmasknew.id = "maskNewCont";
                        canvasmasknew.style.visibility  = "hidden";
                        document.body.appendChild(canvasmasknew);

                        maskCanvasCont = document.getElementById("maskNewCont");
                        maskContextCont = maskCanvasCont.getContext("2d");

                        maskImage=new Image();
                        maskImage.src="/retrieveMaskImage?imageID="+selectionimg+"&tag="+idTag;
                        //maskImage.src=media;

                        maskImage.onload = function() {
                                                        
                       	 							maskCanvasCont.width = window.innerWidth*0.8/4;
                       	 							maskCanvasCont.height = window.innerWidth*0.8/4*this.height/this.width;
                       	 							maskCanvas.width = window.innerWidth*0.8/4;
                       	 							maskCanvas.height = window.innerWidth*0.8/4*this.height/this.width;
       												
                       	 							maskContextCont.drawImage(maskImage,0,0,maskCanvasCont.width,maskCanvasCont.height);
       												 var imdata = maskContextCont.getImageData(0, 0, maskCanvasCont.width, maskCanvasCont.height);
       												 var r,g,b,a;
       												 for (var p=0;p<imdata.data.length;p+=4) {

       												  r = imdata.data[p];
       												  g = imdata.data[p+1];
       												  b = imdata.data[p+2];
       												  
       												  if((r==0)&&(g==0)&&(b==0))
       												  {
       													  imdata.data[p+3] = 170;

       												  }
       												  else
       												  {
       													  imdata.data[p+3] = 255;

       												  }

       												 }
       												 
       												 maskContextCont.putImageData(imdata,0,0);
       												 maskContext.globalCompositeOperation = "copy";
       												 maskContext.drawImage(maskCanvasCont, 0, 0);
       												 maskContext.globalCompositeOperation = 'darker';
       												 maskContext.drawImage(taskImage,0,0,maskCanvas.width,maskCanvas.height);
       												 maskContext.globalCompositeOperation = 'source-over';
                                                        maskContext.font="bold 15px Arial";
                                                        maskContext.fillStyle = 'white';
                                                        maskContext.fillText('Quality: ' + quality, 10,20);

                                                      
                                                    };
                        
                    }
                    else{
                    	//alert("Request was unsuccesfull: "+ xhr.status);

                    	maskContext.font="15px Arial";
                    	maskContext.fillText("Mask is not available",10,20);
                    }
                        

                }
            }
        });
}


function send (o) {
    if (!connected) return;
    socket.send(JSON.stringify(o));
  }


var gameloop = (function(){
  var canvas = document.getElementById("draws");
  var ctx = canvas.getContext("2d");
  var taskCanvas = document.getElementById("task");
  var taskContext = taskCanvas.getContext("2d");
  var maskCanvas = document.getElementById("mask");
  var maskContext = maskCanvas.getContext("2d");
 

  onSocketMessage = function (e) {
    var m = JSON.parse(e.data);
    switch(m.type)
     {
     case "trace":
     if((m.imageId == $("#ImgAttivattiva").val())&& (m.imageId == $("#ImgPreview").val())){
    	 taskContext.lineJoin = "round";

    	 taskContext.beginPath();
                                        var points = m.points;
     var drawable=true;
     taskContext.moveTo(Math.round(points[0].x*maskCanvas.width/imageW), Math.round(points[0].y*maskCanvas.height/imageH));
     for(var i=0;i<points.length;i++)
     {
     var p=points[i];
     if(p.removed===false&&p.color!='end')
     {
     if(!drawable)
     {
     drawable=true;
     taskContext.beginPath();
     taskContext.moveTo(Math.round(p.x*maskCanvas.width/imageW),Math.round(p.y*maskCanvas.height/imageH));
     }
     taskContext.strokeStyle=m.color;
     taskContext.lineWidth = p.size;
     taskContext.lineTo(Math.round(p.x*maskCanvas.width/imageW), Math.round(p.y*maskCanvas.height/imageH));
     }
     else
     {
     if(drawable)
     {
    	 taskContext.stroke();
     drawable=false;
     }
     }

                                        };
                            }
     break;


     }


    };

    var w = taskCanvas.width, h = taskCanvas.height;
    taskContext.lineCap = 'round';
    taskContext.lineJoin = 'round';
  })();
*/
////////////////////////////////////////////////////////////////////////////////////////////// FINE OLD
function newMask(idTag, idImage, tagName, numAnnotations,im_width,im_height){

	
	//clear masks
	var maskCanvas = document.getElementById("maskNew");
	var maskContext = maskCanvas.getContext("2d");
	maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
	var maskCanvas2 = document.getElementById("mask");
	var maskContext2 = maskCanvas2.getContext("2d");
	maskContext2.clearRect(0,0,maskCanvas2.width,maskCanvas2.height);
	var maskCanvas3 = document.getElementById("maskFashion");
	var maskContext3 = maskCanvas3.getContext("2d");
	maskContext3.clearRect(0,0,maskCanvas3.width,maskCanvas3.height);
	var maskCanvas4 = document.getElementById("draws");
	var maskContext4 = maskCanvas4.getContext("2d");
	maskContext4.clearRect(0,0,maskCanvas4.width,maskCanvas4.height);
	
    var taskCanvas = document.getElementById("task");
    var taskContext = taskCanvas.getContext("2d");
	var taskImage=new Image();
    taskImage.src= $("#mediaLocator").val();
    taskContext.drawImage(taskImage,0,0,taskCanvas.width,taskCanvas.height);
	
	$.jAjax({
        url: "getTraces",
        headers : {
            "idImage" : idImage,
            "tagName" : tagName
        },
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

                	var canvas = document.getElementById("draws");
                	var ctx = canvas.getContext("2d");
                	var taskCanvas = document.getElementById("task");
                	var taskContext = taskCanvas.getContext("2d");
                	canvas.width = taskCanvas.width;
                	canvas.height = taskCanvas.height;
                	var maskCanvas = document.getElementById("mask");
                    var maskContext = maskCanvas.getContext("2d");
                	var result = JSON.parse(xhr.responseText);
                	numAnnotations = result.length;
                	
                	var tagTitle = $("#tagTitle");
                	tagTitle.replaceWith("<div id='tagTitle' class='panelTitle'> Tag '"+tagName+"' ("+numAnnotations+" annotations)</div>");
                	var maskButtons = $("#maskButtons");
                	maskButtons.replaceWith("<div class='span12' id='maskButtons'>"+
                	 						"<a class='btn' onclick=\"loadMask('"+tagName+"')\"><strong>View mask without spam detector</strong></a>"+
                	 						"<a class='btn' onclick=\"loadMaskFashionista('"+idImage+"','"+tagName+"')\"><strong>Cloth parser (Fashionista)</strong></a>"+
                	 						"</div>");

                	 
                	var fun = function(){
                     	var points = JSON.parse(result.shift().points);
                     	taskContext.lineJoin = "round";
                     	taskContext.beginPath();
                     	taskContext.strokeStyle = getRandomColor();

               	        var drawable=true;
               	     taskContext.moveTo(Math.round(points[0].x*taskCanvas.width/im_width), Math.round(points[0].y*taskCanvas.height/im_height));

               	        for(var i=0;i<points.length;i++)
               	        {
               	        	var p=points[i];
               	        	if(p.end!=true)
               	        	{
	     						if(!drawable)
	     						 {
	     							drawable=true;
	     							taskContext.beginPath();
	     							taskContext.moveTo(Math.round(p.x*taskCanvas.width/im_width),Math.round(p.y*taskCanvas.height/im_height));
	     						 }
	     						
	     						taskContext.lineWidth = 3;
	     						taskContext.lineTo(Math.round(p.x*taskCanvas.width/im_width), Math.round(p.y*taskCanvas.height/im_height));
	     					}
	     					else
	     					{
	     						if(drawable)
	     						{
	     							taskContext.stroke();
	     							drawable=false;
	     						}
	     					}

               	        }; 

                     	if (result.length == 0) return;
                     	setTimeout(fun , 100);
                     }
                     setTimeout(fun, 0);
                	
                    
                }
                else{
                	alert("Request was unsuccesfull: "+ xhr.status);

                }
                    

            }
        }
    });
	
	function getRandomColor() {
	    var letters = '0123456789ABCDEF'.split('');
	    var color = '#';
	    for (var i = 0; i < 6; i++ ) {
	        color += letters[Math.floor(Math.random() * 16)];
	    }
	    return color;
	}
	

	
	$.jAjax({
        url: "MaskAjax",
        headers : {
            "idImage" : idImage,
            "tag" : tagName
        },
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

               	 	var result = JSON.parse(xhr.responseText);
               	
                    var media = result[0].medialocator;
                    var quality = result[0].quality;
                    var canvasmasknew = document.createElement("canvas");
                    canvasmasknew.id = "maskNewCont";
                    canvasmasknew.style.visibility  = "hidden";
                    document.body.appendChild(canvasmasknew);

                    maskCanvasCont = document.getElementById("maskNewCont");
                    maskContextCont = maskCanvasCont.getContext("2d");

                    maskImage=new Image();
                    maskImage.src="/retrieveMaskImage?media="+media;
                     
                    maskImage.onload = function() {
                                                
                   	 							maskCanvasCont.width = window.innerWidth*0.8/4;
                   	 							maskCanvasCont.height = window.innerWidth*0.8/4*this.height/this.width;
                   	 							maskCanvas.width = window.innerWidth*0.8/4;
                   	 							maskCanvas.height = window.innerWidth*0.8/4*this.height/this.width;
   												
                   	 							maskContextCont.drawImage(maskImage,0,0,maskCanvasCont.width,maskCanvasCont.height);
   												 var imdata = maskContextCont.getImageData(0, 0, maskCanvasCont.width, maskCanvasCont.height);
   												 var r,g,b,a;
   												 for (var p=0;p<imdata.data.length;p+=4) {

   												  r = imdata.data[p];
   												  g = imdata.data[p+1];
   												  b = imdata.data[p+2];
   												  
   												  if((r==0)&&(g==0)&&(b==0))
   												  {
   													  imdata.data[p+3] = 170;

   												  }
   												  else
   												  {
   													  imdata.data[p+3] = 255;

   												  }

   												 }
   												 
   												 maskContextCont.putImageData(imdata,0,0);
   												 maskContext.globalCompositeOperation = "copy";
   												 maskContext.drawImage(maskCanvasCont, 0, 0);
   												 maskContext.globalCompositeOperation = 'darker';
   												 maskContext.drawImage(taskImage,0,0,maskCanvas.width,maskCanvas.height);
   												 maskContext.globalCompositeOperation = 'source-over';
                                                 maskContext.font="bold 15px Arial";
                                                 maskContext.fillStyle = 'white';
                                                 maskContext.fillText('Quality: ' + quality, 10,20);
                                                 
                                                };
                                                
                    
                }
                else{
                	//alert("Request was unsuccesfull: "+ xhr.status);
                	maskContext.font="15px Arial";
                	maskContext.fillText("Mask is not available",10,20);
                }
                    

            }
        }
    });
    

}
