/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function popolaSelectionAjaxTasks(){


    var selection = $("#tasksList")
        ,li,img,div,h3
        ,ids
        ,tasks
        ,result;

    selection.children().remove();

    $.jAjax({
        url: "TaskSelection",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    result = JSON.parse(xhr.responseText);
                    
                    if(result.check == "true"){

                        tasks = result.task[0];
                        $.each(tasks, function(i,d){
                        		
                        	li = document.createElement("li");
                        	selection.append(li);
                        	var html = "<div><p class='taskId'>"+d.id.substring(1, d.id.length -1)+"</p></div>";
                    		li.innerHTML = html;
                    		li.onclick = function() {
                    			taskPreview(d.id.substring(1, d.id.length -1),d.taskType.substring(1, d.taskType.length -1),d.status.substring(1, d.status.length -1)); 
                    		};

                        });
                    }    
  
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
    
   
	 

     var selectionCollection = $("#collectionListTask");
     selectionCollection.children().remove();
	 
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
	 
	 selectionImages = $("#TaskImagesList");
	 selectionImages.children().remove();
	 
	 $.jAjax({
	        url: "WebToolAjax",
	        onComplete: function(xhr,status){
	            if(xhr.readyState === 4){
	                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

	                    result = JSON.parse(xhr.responseText);
	                    ids = result.image[0];
	                    $.each(ids, function(i,d){
	                    	
	                    	selectionImages.append("<li><label><input type='checkbox' name='image' value='"+d.id+"'> Image "+d.id+"</input></label></li>");
	                    	
	                    });
	              
	                    
	                }
	                else{
	                    alert("Request was unsuccesfull: "+ xhr.status);
	                }
	            }
	        }
	    });
    
    
}
