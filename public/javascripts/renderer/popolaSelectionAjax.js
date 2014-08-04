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
                			
                			$.jAjax({
                		        url: "initializeSlider",
                		        onComplete: function(xhr,status){
                		            if(xhr.readyState === 4){
                		                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                		                	
                		                    result = JSON.parse(xhr.responseText);
                		                    var min_ann = parseInt(result[0].min);
                		                    var max_ann = parseInt(result[0].max);
                		                  //annotation filter
                                    		$(".range").replaceWith("<span class='range'><div id='slider'></div><span id='resSlider'>"+min_ann+":"+max_ann+"</span><a class='btn' id='filter'><strong>Filter Num Annotations</strong></a><input type='text' id='maxAnn' value='"+max_ann+"' style='display:none;'/></span>");
                                            
                                            $("#slider").noUiSlider({
                                            	start: [min_ann, max_ann],
                                            	connect: true,
                                            	step: 1,
                                            	range: {
                                            		'min': min_ann,
                                            		'max': max_ann
                                            	}
                                            });


                                            $("#slider").on('slide', function(){
                                            	
                                                var str = String($("#slider").val());
                                                var res = str.split(",");
                                                var min = res[0].split(".")[0];
                                                var max = res[1].split(".")[0];
                                                $("#resSlider").replaceWith("<span id='resSlider'>"+min+":"+max+"</span>");
                                                $("#filter").replaceWith("<a class='btn' id='filter' onclick='imagesScrollAnn("+min+","+ max+"," + null + "," + 100 + ");'><strong>Filter Num Annotations</strong></a>");
                                                
                                            });
                		                }
                		                else{
                		                    alert("Request was unsuccesfull: "+ xhr.status);
                		                }
                		            }
                		        }
                		    });
                			
                			
                            first = false;
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

//IMAGE SCROLL WHEN ANNOTATION FILTER
function imagesScrollAnn(min_ann, max_ann,max_id,count){

	$('#images_scroll').unbind('inview');
	var selection = $("#imageList");
	selection.children().remove();

    $.jAjax({
        url: "annotationRange",
        headers : {
            "max" : max_ann,
            "min" : min_ann,
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
                     

                     $.each(ids, function(i,d){

                     		li = document.createElement("li");
                     		selection.append(li);
                     		var html = "<a href='#'><img src='"+d.media+"'/><div><h3 class='imageId'>"+d.id+"</h3></div></a>";
                     		li.innerHTML = html;
                     		li.onclick = function() {
                     			imgPreview(d.media,d.id);

                     		};
                     });
                     
                     if(new_max_id!=""){
                    	 $("#images_scroll").replaceWith("<span id='images_scroll' style='float:left;color: rgb(255,255,255);'>Loading...</span>");
                         $('#images_scroll').one('inview', function(event, isInView, visiblePartX, visiblePartY) {
                         	  if (isInView) {
                         		  imagesScroll(min_ann, max_ann, new_max_id, new_count);
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
