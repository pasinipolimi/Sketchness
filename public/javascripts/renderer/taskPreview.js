function taskPreview(idPreview, taskType, taskStatus){
	
	var taskCont = $("#taskCont");
	$("#taskCont").show();
	taskCont.children().remove();
	

	 $.jAjax({
	        url: "WebTaskAjax",
	        headers : {"selected" : idPreview},
	        onComplete: function(xhr,status){
	            if(xhr.readyState === 4){
	                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
	                	 
	                	
	                	 $("#taskTitle").replaceWith("<div id='taskTitle' class='panelTitle'>Task "+idPreview+"</div>");
	                	
	                	 var table2 = $("#taskInfo");
	                	 table2.children().remove();
	                	 $("#taskInfo").show();
	                	 var tr = document.createElement("tr");
	                	 table2.append(tr);	
	                	 tr.innerHTML = "<tr><td class='infoLabel'>Task ID</td><td class='infoLabel'>TaskType</td><td class='infoLabel'>Status</td></tr>";
	                	 var tr2 = document.createElement("tr");
	                	 table2.append(tr2);	
	                	 tr2.innerHTML = "<tr><td class='infoValue'>"+idPreview+"</td><td class='infoValue'>"+taskType+"</td><td class='infoValue'>"+taskStatus+"</td>";
	                	 
	                	 var createTask = $(".createTask");
	                	 createTask.children().remove();
	                	 $(".createTask").show();
	                    
	                	 createTask.append("<div class='span12' id='statsButton'>"+
	                			 "<a id='newUtask' class='btn' onclick=\"nuovoUTask('"+"segmentation"+"','"+idPreview+"','"+taskType+"','"+taskStatus+"')\"><i class='icon-plus'></i> <strong>New Segmentation Utask</strong></a>"+
	 							 "<a id='newUtask' class='btn' onclick=\"nuovoUTask('"+"tagging"+"','"+idPreview+"','"+taskType+"','"+taskStatus+"')\"><i class='icon-plus'></i> <strong>New Tagging Utask</strong></a>"
	                			 +"</div>");
	                	 
	                	
	                     
	         			var table = $("#taskUtasks");
	                     $("#taskUtasks").show();
	                     var result = JSON.parse(xhr.responseText);
	                     var utasks = result[0].utasks;
	                     
	                     var tr = document.createElement("tr");
	                	 table2.append(tr);	
	                	 tr.innerHTML = "<tr><td id='emptyRow'></td></tr>";
	                     
	                     if(utasks == ""){

		                	 var tr2 = document.createElement("tr");
		                	 table2.append(tr2);	
		                	 tr2.innerHTML = "<tr><td class='infoValue'>No utasks available</td></tr>";
		                    }
	                     else{

		                	 var tr = document.createElement("tr");
		                	 table2.append(tr);	
		                	 tr.innerHTML = "<tr><td class='infoLabel'>Utask ID</td><td class='infoLabel'>UtaskType</td><td class='infoLabel'>Status</td></tr>";

		                     $.each(utasks,function(i,d){
		                     		
		                     		table2.append("<tr><td class='infoValue'>"+d.id+"</td><td class='infoValue'>"+d.utaskType+"</td><td class='infoValue'>"+d.status+"</td></tr>");
		                     			
		                     });
	                    	 
	                     }
	                	 
	                     
	   
	                }
	                else{
	                    alert("Request was unsuccesfull: "+ xhr.status);
	                }
	            }
	        }
	    });
	 

}