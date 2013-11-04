/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function  visualizzaUTask(){
    
    var idselected = $("#selectionTask").val()
        ,uTaskContainer = $("#uTaskContainer");
    
    uTaskContainer.find(".tableRow").remove();
    
    if(idselected === "" ){
        alert("Please select a Task!");
        return;
    }
    $.jAjax({
        url: "WebTaskAjax",
        headers : {"selected" : idselected},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    var stringaTask= xhr.getResponseHeader("uTask");
                    var aperto = xhr.getResponseHeader("aperto");
                    
                    if(stringaTask.length === 4){
                        $("#noMicrotaskLabel").show();
                    }
                    else{
                        $("#noMicrotaskLabel").hide();
                        var uTask = JSON.parse(stringaTask);
                        $.each(uTask, function(i,d){
                            var tableRow = "<tr class='tableRow'>"+
                                                "<td>"+ d.id.toString() +"</td>"+
                                                "<td>"+ d.taskType.toString() +"</td>"+
                                                "<td>"+ d.status.toString() +"</td>"+
                                            "</tr>";
                            uTaskContainer.append(tableRow);
                        });
                        
                        if(aperto === "1"){
                            $("#closeTaskButton").show();
                            $("#closedTaskLabel").hide();
                        }
                        else{
                            $("#closeTaskButton").hide();
                            $("#closedTaskLabel").show();
                        }
                    }//fine else
                    
                    $("#TaskAttivo").val(idselected);
                    $("#risultatitask").show();
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
}
