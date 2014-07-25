/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function popolaSelectionAjax(max_id, count, first){

	var selection = $("#imageList")
        ,selectionCollection = $("#collectionList")
        ,li,img,div,h3
        ,ids
        ,tasks
        ,result;

    selection.children().remove();
    selectionCollection.children().remove();
    
    var maxAnn = 0;

    /*
    $.jAjax({
        url: "WebToolAjax",
        headers : {
            "max_id" : max_id,
            "count" : count
        },
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    result = JSON.parse(xhr.responseText);
                    ids = result.image[0];
                    var new_max_id = result.max_id[0];
                    var new_count = result.count[0];
                    alert("max_id " + new_max_id + " count " + new_count);
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

                    
                    $('#images_scroll').bind('inview', function(event, isInView, visiblePartX, visiblePartY) {
                    	  if (isInView) {
                    	    // element is now visible in the viewport
                    	    if ((visiblePartY == 'top')||(visiblePartY == 'bottom')) {
                    	      // top part of element is visible
                    	    	alert("visible");
                    	    }
                    	  }
                    	});
                    
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
    */
    function imagesScroll(max_id,count){

    	$.jAjax({
            url: "WebToolAjax",
            headers : {
                "max_id" : max_id,
                "count" : count
            },
            onComplete: function(xhr,status){
                if(xhr.readyState === 4){
                    if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                        result = JSON.parse(xhr.responseText);
                        ids = result.image[0];
                        var new_max_id = result.max_id[0];
                        var new_count = result.count[0];
                        maxAnn = $("#maxAnn").val();
 
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
                                    $("#maxAnn").val(maxAnn);
                        		}
                        	
                        });
                        //id filter
                		$("input:text[name=imageIdSearch]").each(function(){
                					$(this).val('');	
                		});
                        
                		if(first){
                			//annotation filter
                    		$(".range").replaceWith("<span class='range'><span>Num Annotations</span><div id='slider'></div><span id='resSlider'>0:"+maxAnn+"</span><input type='text' id='maxAnn' value='"+maxAnn+"' style='display:none;'/></span>");
                            
                            $("#slider").noUiSlider({
                            	start: [0, maxAnn],
                            	connect: true,
                            	step: 1,
                            	range: {
                            		'min': 0,
                            		'max': maxAnn
                            	}
                            });


                            $("#slider").on('slide', function(){
                            	
                                var str = String($("#slider").val());
                                var res = str.split(",");
                                var min = res[0].split(".")[0];
                                var max = res[1].split(".")[0];
                                $("#resSlider").replaceWith("<span id='resSlider'>"+min+":"+max+"</span>");
                                
                            });
                            first = false;
                		}
                		else{
                			maxAnn = 20;
                			var values = [0,maxAnn];
                			$("#slider").val(values);
                			$("#resSlider").replaceWith("<span id='resSlider'>"+0+":"+maxAnn+"</span>");
                			
                		}

                        //collection filter
                        /*
                		$("input:checkbox[name=collection]:checked").each(function()
                		{
                			$(this).prop("checked", false); 
                					
                		});
						*/
  
 
                        if(new_max_id!=""){
                        	 $("#images_scroll").replaceWith("<span id='images_scroll' style='float:left;color: rgb(255,255,255);'>Loading...</span>");
                             $('#images_scroll').one('inview', function(event, isInView, visiblePartX, visiblePartY) {
                             	  if (isInView) {
                             		  imagesScroll(new_max_id, new_count);
                             		  $('#images_scroll').unbind('inview');
                             	    
                             	    }
                             	  
                             	});
                        }
                        else{
                        	$("#images_scroll").replaceWith("<span id='images_scroll' style='float:left;'></span>");
                        }
                       
                    }
                    else{
                        alert("Request was unsuccesfull: "+ xhr.status);
                    }
                }
            }
        });
    }
    
    imagesScroll(max_id,count);
    
    var option;
    
    $.jAjax({
        url: "CollectionAjax",
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                	
                    result = JSON.parse(xhr.responseText);
                    var collIds = result.collections[0];
                    $.each(collIds, function(i,d){
                    		selectionCollection.append("<label><input type='checkbox' name='collection' value='"+d.id.substring(1, d.id.length -1)+"'> Collection: "+d.name.substring(1, d.name.length -1)+"</input></label>");
	
                    });
          
                    
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });
    
    
}
