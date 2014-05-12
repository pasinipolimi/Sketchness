function loadMaskFashionista(idImage,idTag){
	

	$.jAjax({
	     url: "MaskAjaxFashionista",
	     headers : {
	         "idImage" : idImage,
	         "idTag" : idTag
	     },
	     onComplete: function(xhr,status){
	         if(xhr.readyState === 4){
	             if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
	            	 
	            	 
	            	 var result = JSON.parse(xhr.responseText);
	            	 var url = result[0].url;
	            	 var quality = result[0].quality;
	            	 
	            	 var selectionimg = $("#ImgAttivattiva").val()
	                 ,mediaimg = $("#mediaLocator").val()
	                 ,canvas = document.getElementById("draws")
	                 ,ctx = canvas.getContext("2d")
	                 ,taskCanvas = document.getElementById("task")
	                 ,taskContext = taskCanvas.getContext("2d")
	                 ,taskImage=new Image();
	                 taskImage.src=mediaimg;
	                 

	                 var maskImage;
	                 var canvas = document.getElementById("maskFashion");
	                 var ctx = canvas.getContext("2d");
	                 var maskCanvas = document.getElementById("maskFashion");
	                 var maskContext = maskCanvas.getContext("2d");
	                 var viewport = document.getElementById("viewport");
	                 
	                 var canvasmasknew = document.createElement("canvas");
	                 canvasmasknew.id = "maskNewContFashion";
	                 canvasmasknew.style.visibility  = "hidden";
	                 document.body.appendChild(canvasmasknew);

	                 maskCanvasCont = document.getElementById("maskNewContFashion");
	                 maskContextCont = maskCanvasCont.getContext("2d");
	                 
	                 maskImage=new Image();
	                 maskImage.src= "assets/images/fashionista/"+url;
					 
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
		                                                 if(quality=="u"){
		                                                	 maskContext.fillText('Quality: unknown', 10,20);
		                                                 }
														 else if(quality=="10")
														 {
															maskContext.fillText('Quality: 1', 10,20);
														 }
		                                                 else{
		                                                	 maskContext.fillText('Quality: 0.'+quality, 10,20);
		                                                 }
	                	 
	                	 
	                	 							
	                                                 
	                                             };
	                
	            	 

	             }
	             else
	                 alert("Request was unsuccesfull: "+ xhr.status);
	         }
	     }
	 });

}