/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function caricaStatistiche(){
  $.jAjax({
    url: "LoadStats",
    onComplete: function(xhr,status){
      if(xhr.readyState === 4){
          if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
             var totImg = xhr.getResponseHeader("totImg");
             var mediaTag = xhr.getResponseHeader("mediaTag");
             var numSegment = xhr.getResponseHeader("numSegment");
             var mediaSegmenti = xhr.getResponseHeader("mediaSegImg");
             
             $("#statisticheSistema").show();
             $("#numeroFoto").text(totImg);
             $("#mediaTagFoto").text(Number(mediaTag).toFixed(2));
             $("#numeroSegmenti").text(numSegment);
             $("#mediaSegmenti").text(Number(mediaSegmenti).toFixed(2));
          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });
}

