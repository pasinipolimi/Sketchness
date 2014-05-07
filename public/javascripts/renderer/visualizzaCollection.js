/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function repopolate(){
		$("#imageList").children().show();
		//reset annotations filter
		var maxAnn = $("#maxAnn").val();
		$("#slider").replaceWith("<input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn/2+"' onchange='numberAnnotations();'>");
		$("#range").replaceWith("<output id='range'> "+maxAnn/2+"</output>");
		//reset collection filter
		$("input:checkbox[name=collection]:checked").each(function()
		{
			$(this).prop("checked", false); 
					
		});
		//reset image id filter
		$("input:text[name=imageIdSearch]").each(function()
				{
					$(this).val('');	
				});
		

};

function filter(){
	
		//reset collection filter
		$("input:checkbox[name=collection]:checked").each(function()
			{
				$(this).prop("checked", false); 
						
			});
		
		//reset annotations filter
		var maxAnn = $("#maxAnn").val();
		$("#slider").replaceWith("<input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn/2+"' onchange='numberAnnotations();'>");
		$("#range").replaceWith("<output id='range'> "+maxAnn/2+"</output>");
	
		var idElements = $('.imageId');
		$(".cbp-rfgrid").children().hide();
		idElements.each( function(i, val) {
			
			if($(val).text()==$('#system-search').val())
			{
				
				$(".cbp-rfgrid").children().eq(i).show();
			}
		});
		
		
};
    
function visualizzaCollection(){

	//reset annotations filter
	var maxAnn = $("#maxAnn").val();
	$("#slider").replaceWith("<input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn/2+"' onchange='numberAnnotations();'>");
	$("#range").replaceWith("<output id='range'> "+maxAnn/2+"</output>");
	//reset image id filter
	$("input:text[name=imageIdSearch]").each(function()
			{
				$(this).val('');	
			});
	
	var selection = $("#imageList")
        ,li,img,div,h3
        ,ids
        ,tasks
        ,result;

	$(".cbp-rfgrid").children().hide();
	
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

	    				var idElements = $('.imageId');
	    	    		idElements.each( function(i, val) {
	    	    			$.each(ids, function(j,d){
	    	    				if($(val).text()==d.id)
	        	    			{
	    	    					 $(".cbp-rfgrid").children().eq(i).show();
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
 
}

function numberAnnotations(){
	
	//reset collection filter
	$("input:checkbox[name=collection]:checked").each(function()
			{
				$(this).prop("checked", false); 
						
			});
	//reset image id filter
	$("input:text[name=imageIdSearch]").each(function()
			{
				$(this).val('');	
			});
	
	$("#range").replaceWith("<output id='range'>"+$('#slider').val()+"</output>");
	var selection = $("#imageList")
	,li,img,div,h3
    ,ids
    ,tasks
    ,result;
	
	$.jAjax({
        url: "WebToolAjax",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    result = JSON.parse(xhr.responseText);
                    ids = result.image[0];
                    
                    $(".cbp-rfgrid").children().hide();
                    
                    var idElements = $('.imageId');
    	    		idElements.each( function(i, val) {
    	    			$.each(ids, function(j,d){
    	    				if($(val).text()==d.id)
        	    			{	
    	    					if(d.numAnnotations<=$('#slider').val())
    	    					{
    	    						$(".cbp-rfgrid").children().eq(i).show();
    	    					}
    	    					 
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
};