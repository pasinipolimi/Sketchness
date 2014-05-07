		function filter(){
    		var idElements = $('.taskId');
    		$("#tasksList").children().hide();
    		idElements.each( function(i, val) {

    			if($(val).text()==$('#system-search').val())
    			{
    				$("#tasksList").children().eq(i).show();
    			}
    		});
    		
    	};
    	
    	
    	function repopolate(){
    		$("#tasksList").children().show();
    		$("input:text[name=taskIdSearch]").each(function()
    				{
    					$(this).val('');	
    				});
    	};
    	
    	function visualizzaCollectionTask(){
    		
    		$("input:checkbox[name=image]").each(function()
        	{	
		    	$(this).prop("checked", false);

        	});
    		$("input:checkbox[name=collection]:checked").each(function()
    				{
    					
    					var idselected = $(this).val(); 
    					$.jAjax({
    				        url: "CollectionImagesAjax",
    				        headers : {"selected" : idselected},
    				        onComplete: function(xhr,status){
    				            if(xhr.readyState === 4){
    				                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
    				                	
    				                    result = JSON.parse(xhr.responseText);
    				                    var ids = result[0].images;
    				                    
    				                    $.each(ids, function(j,d){
    				                    	$("input:checkbox[name=image]").each(function()
        				                			{	
		    				                    		if($(this).val()==d.id){
		    												$(this).prop("checked", true);
		    											}
        				                			});
	                       			 	});
  
    				                }
    				                else{
    				                    alert("Request was unsuccesfull: "+ xhr.status);
    				                }
    				            }
    				        }
    				    });  
    				});
    	};