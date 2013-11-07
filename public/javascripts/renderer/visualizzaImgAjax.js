/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function visualizzaImgAjax(){
   var tagContainer = $(".tag.listBody")
       ,idselected = $("#selection").val()
       ,url= "http://54.228.220.100/"
       ,taskImage
       ,canvas = document.getElementById("draws")
       ,ctx = canvas.getContext("2d")
       ,taskCanvas = document.getElementById("task")
       ,taskContext = taskCanvas.getContext("2d");
   
   tagContainer.children().remove();
    
   $.jAjax({
        url: "WebInfoAjax",
        headers : {"selected" : idselected},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    var result = JSON.parse(xhr.responseText);
                    var tags = result[0].tags;
                    
                    if(tags.length === 0)
                        tagContainer.append("<span>No tags available</span>");
                    $.each(tags,function(i,d){
                        tagContainer.append("<div class='listItem' onclick=\"loadMask('"+d.tag.substring(1, d.tag.length -1)+"')\">"+ d.tag.substring(1, d.tag.length -1) +"</div>");
                    });
                    
                    url += result[0].medialocator.substring(1, result[0].medialocator.length -1)
                    taskImage=new Image();
                    taskImage.src=url;
                    $("#immagine").attr("src",url);
                    $("#imageId").text(idselected);
                    $("#dati").show();
                    tagContainer.parent().show();
                    $("#img").hide();
                    $("#ImgAttivattiva").val(idselected);
                    taskImage.onload = function() {
                                                    taskContext.save();
                                                    taskContext.beginPath();
                                                    taskCanvas.width=this.width;
                                                    taskCanvas.height=this.height;
                                                    canvas.width=this.width;
                                                    canvas.height=this.height;
                                                    taskContext.drawImage(taskImage,0,0,this.width,this.height);
                                                    taskContext.restore();
                                                };

                }
                else
                    alert("Request was unsuccesfull: "+ xhr.status);
            }
        }
    });
}
