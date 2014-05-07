/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function nuovoUTask(utaskType, selectionTask, taskType, taskStatus){
    
     $.jAjax({
        url: "AddUTask",
        headers : {
            "taskType" : utaskType,
            "selectionTask" : selectionTask
        },
        onComplete: function(xhr,status){
            $("#creanuovoUTask").dropkick();
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                   var newTaskId = xhr.responseText;
                   alert("New micro task code is: " + newTaskId);
                   taskPreview(selectionTask, taskType, taskStatus);
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });

}

