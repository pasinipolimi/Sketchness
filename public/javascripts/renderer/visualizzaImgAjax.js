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
                    var tags = JSON.parse(xhr.getResponseHeader("listaTag"));
                    
                    if(tags.length === 0)
                        tagContainer.append("<span>No tags available</span>");
                    $.each(tags,function(i,d){
                        tagContainer.append("<div class='listItem'>"+ d.tag.toString() +"</div>");
                    });
                    
                    url += xhr.getResponseHeader("medialocator");
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
