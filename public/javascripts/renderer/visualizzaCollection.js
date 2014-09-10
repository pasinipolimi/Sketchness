/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

function repopolate(){

		$('#images_scroll').unbind('inview');
		//reset annotations filter
        var min_ann = $("#minAnn").val();
        var max_ann = $("#maxAnn").val();

        var values = [min_ann,max_ann];
        $("#slider").val(values);
        $("#resSlider").replaceWith("<span id='resSlider'>"+min_ann+":"+max_ann+"</span>");
        
		popolaSelectionAjax(null,100,false);
};

function filter(){
	
		//reset image list
		var selection = $("#imageList");
		$('#images_scroll').unbind('inview');
		selection.children().remove();
	
		//reset collection filter
		$("input:checkbox[name=collection]:checked").each(function()
			{
				$(this).prop("checked", false); 
						
			});
		
		//reset annotations filter
        var min_ann = $("#minAnn").val();
        var max_ann = $("#maxAnn").val();

        var values = [min_ann,max_ann];
        $("#slider").val(values);
        $("#resSlider").replaceWith("<span id='resSlider'>"+min_ann+":"+max_ann+"</span>");


        //IMAGE ID FILTER
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

	var selection = $("#imageList")
    ,li,img,div,h3
    ,ids
    ,tasks
    ,result;

	//reset image list
	$('#images_scroll').unbind('inview');
	selection.children().remove();
	
	//reset image id filter
	$("input:text[name=imageIdSearch]").each(function()
			{
				$(this).val('');	
			});
	
	//reset annotations filter
    var min_ann = $("#minAnn").val();
    var max_ann = $("#maxAnn").val();

    var values = [min_ann,max_ann];
    $("#slider").val(values);
    $("#resSlider").replaceWith("<span id='resSlider'>"+min_ann+":"+max_ann+"</span>");
    
    
    //COLLECTION FILTER
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
	                    var imgs = result[0].images;
	                    
	                    $.each(imgs, function(i,d){

                    		li = document.createElement("li");
                    		selection.append(li);
                    		var html = "<a href='#'><img src='"+d.media+"'/><div><h3 class='imageId'>"+d.id+"</h3></div></a>";
                    		li.innerHTML = html;
                    		li.onclick = function() {
                    			imgPreview(d.media,d.id);

                    		};
                    	
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
