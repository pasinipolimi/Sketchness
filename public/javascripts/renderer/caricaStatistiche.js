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

             var result = JSON.parse(xhr.responseText);
             var totImg = result.totImg[0];
             var mediaTag = result.mediaTag[0];
             var numSegment = result.numSegment[0];
             var mediaSegmenti = result.mediaSegImg[0];
             var numberUsers = result.numberUsers[0];

             $("#numeroFoto").text(totImg);
             $("#mediaTagFoto").text(Number(mediaTag).toFixed(2));
             $("#numeroSegmenti").text(numSegment);
             $("#mediaSegmenti").text(Number(mediaSegmenti).toFixed(2));
             $("#numberUsers").text(numberUsers);
          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });
}

function displayGraphs(){
	  loadFirstGraph();
	  loadSecondGraph();
	$("#drawArea").show();
	$("#chart_div1").show();
	$("#chart_div2").show();
}

