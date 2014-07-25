/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function repopolate(){
		/*
		$("#imageList").children().show();
		//reset annotations filter
		var maxAnn = $("#maxAnn").val();
		$("#slider").replaceWith("<input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn+"' onchange='numberAnnotations();'>");
		$("#range").replaceWith("<output id='range'> "+maxAnn+"</output>");
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
		*/
		$('#images_scroll').unbind('inview');
		popolaSelectionAjax(null,100,false);
};

function filter(){
	
		//reset collection filter
		$("input:checkbox[name=collection]:checked").each(function()
			{
				$(this).prop("checked", false); 
						
			});
		
		//reset annotations filter
		/*
		var maxAnn = $("#maxAnn").val();
		$(".range").replaceWith("<span class='range'>Num. Annotations <input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn+"' onchange='numberAnnotations();'><output id='range'> "+maxAnn+"</output><input type='text' id='maxAnn' style='display:none;'/>");
		*/
		var maxAnn = $("#maxAnn").val();
		var values = [0,maxAnn];
		$("#slider").val(values);
		$("#resSlider").replaceWith("<span id='resSlider'>"+0+":"+maxAnn+"</span>");
		/*
		var idElements = $('.imageId');
		$(".cbp-rfgrid").children().hide();
		idElements.each( function(i, val) {
			
			if($(val).text()==$('#system-search').val())
			{
				
				$(".cbp-rfgrid").children().eq(i).show();
			}
		});
		*/
		var id_selected = $('#system-search').val();

		$.jAjax({
            url: "WebInfoAjax",
            headers : {
                "selected" : id_selected
            },
            onComplete: function(xhr,status){
                if(xhr.readyState === 4){
                    if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    	
                        result = JSON.parse(xhr.responseText);
                        media = result[0].medialocator;
                        $('#images_scroll').unbind('inview');
                        $("#imageList").children().remove();
                        
                        var selection = $("#imageList");
                        var li = document.createElement("li");
                		selection.append(li);
                		var html = "<a href='#'><img src='"+media+"'/><div><h3 class='imageId'>"+id_selected+"</h3></div></a>";
                		li.innerHTML = html;
                		li.onclick = function() {
                			imgPreview(media,id_selected);

                		};
                        
                        ids = result.image[0];
                        var new_max_id = result.max_id[0];
                        var new_count = result.count[0];
                        
                    }
                    else{
                        //alert("Request was unsuccesfull: "+ xhr.status);
                    	$('#images_scroll').unbind('inview');
                        $("#imageList").children().remove();
                        //image not found
                        var selection = $("#imageList");
                        var li = document.createElement("li");
                		selection.append(li);
                		var html = "Image not found";
                		li.innerHTML = html;
                		
                    }
                }
            }
        });
		
		
};
    
function visualizzaCollection(){

	//reset annotations filter
	var maxAnn = $("#maxAnn").val();
	$("#slider").replaceWith("<input id='slider' type='range' name='range' min='1' max='"+maxAnn+"' value='"+maxAnn+"' onchange='numberAnnotations();'>");
	$("#range").replaceWith("<output id='range'> "+maxAnn+"</output>");
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