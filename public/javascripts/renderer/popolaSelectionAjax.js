/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function popolaSelectionAjax(){

	var selection = $("#imageList")
        ,selectionCollection = $("#collectionList")
        ,li,img,div,h3
        ,ids
        ,tasks
        ,result;

    selection.children().remove();
    selectionCollection.children().remove();
    
    
    var maxAnn = 0;

    $.jAjax({
        url: "WebToolAjax",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    result = JSON.parse(xhr.responseText);
                    ids = result.image[0];
                    $.each(ids, function(i,d){
                    	
                    		li = document.createElement("li");
                    		selection.append(li);
                    		var html = "<a href='#'><img src='"+d.media+"'/><div><h3 class='imageId'>"+d.id+"</h3></div></a>";
                    		li.innerHTML = html;
                    		li.onclick = function() {
                    			imgPreview(d.media,d.id);

                    		};
                    		if(d.numAnnotations>maxAnn)
                    		{
                    			maxAnn = d.numAnnotations;
                    		}
                    	
                    });
                    
                    $("#slider").replaceWith("Num. Annotations <input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn+"' onchange='numberAnnotations();'><output id='range'> "+maxAnn+"</output>");
                    $("#maxAnn").val(maxAnn);
                    
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
    
    var option;
    
    $.jAjax({
        url: "CollectionAjax",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                	
                    result = JSON.parse(xhr.responseText);
                    var collIds = result.collections[0];
                    $.each(collIds, function(i,d){
					
                    		selectionCollection.append("<label><input type='checkbox' name='collection' value='"+d.id.substring(1, d.id.length -1)+"'> Collection "+d.id.substring(1, d.id.length -1)+"</input></label>");
	
                    });
          
                    
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
    
    
}
