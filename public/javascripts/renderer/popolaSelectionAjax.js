/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function popolaSelectionAjax(){
    var selection = $("#selection")
        ,selectionTask = $("#selectionTask")
        ,option
        ,ids
        ,tasks;
    
    selection.append("<option value='' selected disabled>Select Image</option>");
    selectionTask.append("<option value='' selected disabled>Select Task</option>");
    $('.customSelect').dropkick();

    $.jAjax({
        url: "WebToolAjax",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    ids = JSON.parse(xhr.getResponseHeader("opzioni"));
                    $.each(ids, function(i,d){
                        option=document.createElement("option");
                        option.value = option.text = d.id.toString();
                        selection.append(option);
                    });
                    
                    if(xhr.getResponseHeader("check") === "1"){
                        tasks = JSON.parse(xhr.getResponseHeader("opzioniTask"));
                        $.each(tasks, function(i,d){
                            option=document.createElement("option");
                            option.value= d.id.toString();
                            option.text= d.id.toString() + " ( " + d.taskType.toString() + " )";
                            selectionTask.append(option);
                        });
                    }
                    $('.customSelect').dropkick("refresh");
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}
