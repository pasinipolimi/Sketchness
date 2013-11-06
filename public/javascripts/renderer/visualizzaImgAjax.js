/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function visualizzaImgAjax(){
   var tagContainer = $(".tag.listBody")
       ,idselected = $("#selection").val()
       ,url= "http://54.228.220.100/";
   
   tagContainer.children().remove();
    
   $.jAjax({
        url: "WebInfoAjax",
        headers : {"selected" : idselected},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    var result = JSON.parse(xhr.responseText);
                    var tags = result[0].tags
                    
                    if(tags.length === 0)
                        tagContainer.append("<span>No tags available</span>");
                    $.each(tags,function(i,d){
                        tagContainer.append("<div class='listItem'>"+ d.tag.substring(1, d.tag.length -1) +"</div>");
                    });
                    
                    url += result[0].medialocator.substring(1, result[0].medialocator.length -1)
                    $("#immagine").attr("src",url);
                    $("#imageId").text(idselected);
                    $("#dati").show();
                    tagContainer.parent().show();
                    $("#img").show();
                    $("#ImgAttivattiva").val(idselected);
                }
                else
                    alert("Request was unsuccesfull: "+ xhr.status);
            }
        }
    });
}
