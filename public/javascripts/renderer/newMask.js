
function newMask(idTag, idImage, tagName){
	
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
	   
	//display quality
	$("#qualityTable").show();

    var url= "http://54.228.220.100/";
    var maskImage;
    var canvas = document.getElementById("maskNew");
    var ctx = canvas.getContext("2d");
    var maskCanvas = document.getElementById("maskNew");
    var maskContext = maskCanvas.getContext("2d");
    var viewport = document.getElementById("viewport");

    var tagTitle = $("#tagTitle");
    tagTitle.replaceWith("<div id='tagTitle' class='panelTitle'> Tag: "+tagName+"</div>");
    var maskButtons = $("#maskButtons");
    maskButtons.replaceWith("<div class='span12' id='maskButtons'>"+
    						"<a class='btn' onclick=\"loadMask('"+tagName+"')\"><i class='icon-picture'></i> <strong>View mask without spam detector</strong></a>"+
    						"<a class='btn' onclick=\"loadMaskFashionista('"+idImage+"','"+idTag+"')\"><i class='icon-picture'></i> <strong>Cloth parser (Fashionista)</strong></a>"+
    						"</div>");


    $.jAjax({
     url: "MaskAjax",
     headers : {
         "idImage" : idImage,
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
                 
                 maskImage=new Image();
                 maskImage.src=media;
                 maskImage.onload = function() {
                                                 maskContext.save();
                                                 maskContext.beginPath();
												 maskCanvas.width = window.innerWidth*0.8/4;
												 maskCanvas.height = window.innerWidth*0.8/4*this.height/this.width;
												 
												 
                                                 //maskCanvas.width=this.width;
                                                 //maskCanvas.height=this.height;
                                                 //canvas.width=this.width;
                                                 //canvas.height=this.height;
												 //maskCanvas.width=canvas.width;
                                                 //maskCanvas.height=canvas.height;
                                                 maskContext.drawImage(taskImage,0,0,maskCanvas.width,maskCanvas.height);
                                                 maskContext.globalCompositeOperation = 'darker';
                                                 maskContext.drawImage(maskImage,0,0,maskCanvas.width,maskCanvas.height);
                                                 maskContext.restore();
                                                 maskContext.globalCompositeOperation = 'source-over';
                                                 maskContext.font="bold 15px Arial";
                                                 maskContext.fillStyle = 'white';
                                                 maskContext.fillText('Quality: ' + quality, 10,20);
                                             };
                 
             }
             else
                 alert("Request was unsuccesfull: "+ xhr.status);
         }
     }
 });
}
