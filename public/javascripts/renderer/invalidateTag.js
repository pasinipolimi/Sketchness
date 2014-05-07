/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function invalidateTag(tagId){
    
    var xhr = new XMLHttpRequest();
	var selectionimg = $("#ImgAttivattiva").val();
    
     $.jAjax({
        url: "InvalidateTag",
        headers : {
            "tagId" : tagId,
        },
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                   alert("Annotation: " + tagId + "has been invalidated");
				   visualizzaImgAjax(selectionimg);
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
	
	
}