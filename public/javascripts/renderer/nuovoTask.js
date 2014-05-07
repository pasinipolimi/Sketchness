/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function nuovoTask(taskType, selectionimg){

    var xhr = new XMLHttpRequest();
    
     $.jAjax({
        url: "AddTask",
        headers : {
            "taskType" : taskType,
            "selectionimg" : selectionimg
        },
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                   var newTaskId = xhr.responseText;
                   alert("New task code is: " + newTaskId);
                   popolaTask();
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}

function newSegmentationTask(){

	var images;
	var first=1;
	$("input:checkbox[name=image]:checked").each(function(){
				if(first){
					images = $(this).val();
					first=0;
				}
					 
				else{
					images = images + "," + $(this).val();	
				}		
	});

	if(first){
		alert("Select at least one image");
	}
	else{
		nuovoTask("segmentation",images);
	}
	
}

function newTaggingTask(){
	var images;
	var first=1;
	$("input:checkbox[name=image]:checked").each(function(){
				if(first){
					images = $(this).val();
					first=0;
				}
					 
				else{
					images = images + "," + $(this).val();	
				}		
	});
	if(first){
		alert("Select at least one image");
	}
	else{
		nuovoTask("tagging",images);
	}
}