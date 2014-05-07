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
	                 
	                 maskImage=new Image();
	                 maskImage.src= "assets/images/fashionista/"+url;
	                 maskImage.onload = function() {
	                                                 maskContext.save();
	                                                 maskContext.beginPath();
	                                                 maskCanvas.width=this.width;
	                                                 maskCanvas.height=this.height;
	                                                 canvas.width=this.width;
	                                                 canvas.height=this.height;
	                                                 maskContext.drawImage(taskImage,0,0,this.width,this.height);
	                                                 maskContext.globalCompositeOperation = 'darker';
	                                                 maskContext.drawImage(maskImage,0,0,this.width,this.height);
	                                                 maskContext.restore();
	                                                 maskContext.globalCompositeOperation = 'source-over';
	                                                 maskContext.font="bold 30px Arial";
	                                                 maskContext.fillStyle = 'white';
	                                                 if(quality=="u"){
	                                                	 maskContext.fillText('Quality: unknown', 10,50);
	                                                 }
	                                                 else{
	                                                	 maskContext.fillText('Quality: 0.'+quality, 10,50);
	                                                 }
	                                                 
	                                             };
	                
	            	 

	             }
	             else
	                 alert("Request was unsuccesfull: "+ xhr.status);
	         }
	     }
	 });

}