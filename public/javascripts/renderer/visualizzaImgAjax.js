
function visualizzaImgAjax(idselected, mediaLocator){
		
   //clear tag
   var tagTitle = $("#tagTitle");
   tagTitle.hide();
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
   //clear buttons
   var maskButtons = $("#maskButtons");
   maskButtons.children().remove();
   

   
   var tagContainer = $(".tag.listBody")
       //,url= "http://54.228.220.100/"
   	   ,url= "http://localhost:3000"
       ,taskImage
       ,canvas = document.getElementById("draws")
       ,ctx = canvas.getContext("2d")
       ,taskCanvas = document.getElementById("task")
       ,taskContext = taskCanvas.getContext("2d")
       ,graph1 = document.getElementById("chart_div1")
       ,graph2 = document.getElementById("chart_div2")
       ,viewport = document.getElementById("viewport");



   $.jAjax({
        url: "WebInfoAjax",
        headers : {"selected" : idselected},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                	
                    var result = JSON.parse(xhr.responseText);
                    var tags = result[0].tags;
                    var annotations = result[0].annotations;
                    var width = result[0].width;
                    var height = result[0].height;
                    $(".infoTable").show();
                    $("#imageAnnotations").text(annotations);
                    $("#imageId").text(idselected);
                    $("#imageWidth").text(width + " px");
                    $("#imageHeight").text(height+" px");
                    
                    
                    var tagList = $("#imageTags");
                    tagList.children().remove();
                    
                    var tr = document.createElement("tr");
					tagList.append(tr);
					tr.innerHTML = "<td class='infoLabel'>Image Tags:</td>";
                    
                    if(tags.length === 0){
                    	var tr = document.createElement("tr");
     					tagList.append(tr);
     					tr.innerHTML = "no tags available";
                    }
                    	
                    
                    $.each(tags,function(i,d){
                    	/*
                    	if(d.valid.substring(1, d.valid.length -1)=="1"){
                    		tagList.append("<tr><td class='infoValue' onclick=\"newMask('"+d.annotationId.substring(1, d.annotationId.length -1)+"','"+idselected+"','"+d.tag.substring(1, d.tag.length -1)+"','"+d.numAnnotations+"')\">"+ d.tag.substring(1, d.tag.length -1) +" ( "+d.lang.substring(1, d.lang.length -1)+" )"+"</td>"+
                    				"<td class='infoValue' ><a href='#' class='btn' onclick=\"invalidateTag('"+d.tagId+"')\"><strong>Invalidate</strong></a></td></tr>");
                    			
                    		
                    	}
                    	*/
                    	tagList.append("<tr><td class='infoValue' onclick=\"newMask('"+d.tagId+"','"+idselected+"','"+d.tag+"','"+d.numAnnotations+"')\">"+ d.tag+" ( "+d.lang+" )"+"</td>"+
                				"<td class='infoValue' ><a href='#' class='btn' onclick=\"invalidateTag('"+d.tagId+"')\"><strong>Invalidate</strong></a></td></tr>");
                    });
                    
                    /*
   					$(".createTask").show();
   					var createTask = $(".createTask");
   					createTask.children().remove();
   					createTask.append("<div class='span12' id='statsButton'>"+
   									"<a id='newUtask' class='btn' onclick=\"nuovoTask('"+"segmentation"+"','"+idselected+"')\"><strong>New Segmentation Task</strong></a>"+
   									"<a id='newUtask' class='btn' onclick=\"nuovoTask('"+"tagging"+"','"+idselected+"')\"><strong>New Tagging Task</strong></a>"
   									+"</div>");
                    */
                    
                    url += result[0].medialocator.substring(1, result[0].medialocator.length -1);
                    taskImage=new Image();
                    taskImage.src=url;
                    $("#immagine").attr("src",url);
                    
                    $("#dati").show();
                    
                    $("#img").hide();
                    $("#ImgAttivattiva").val(idselected);
                    $("#mediaLocator").val(url);
                    
                    taskImage.onload = function() {
                                                    taskContext.save();
                                                    taskContext.beginPath();
                                                    //taskCanvas.width=this.width;
                                                    //taskCanvas.height=this.height;
                                                    //canvas.width=this.width;
                                                    //canvas.height=this.height;
													taskCanvas.width = window.innerWidth*0.8/4;
												    taskCanvas.height = window.innerWidth*0.8/4*this.height/this.width;
                                                    taskContext.drawImage(taskImage,0,0,taskCanvas.width,taskCanvas.height);
                                                    taskContext.restore();
													//Clear the mask canvas
													var maskCanvas = document.getElementById("mask");
													var maskContext = maskCanvas.getContext("2d");
													maskContext.clearRect(0,0,maskCanvas.width,maskCanvas.height);
                                                };
                      

                }
                else
                    alert("Request was unsuccesfull: "+ xhr.status);
            }
        }
    });
}