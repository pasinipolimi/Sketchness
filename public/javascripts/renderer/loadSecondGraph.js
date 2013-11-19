/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
function loadSecondGraph(){
  $.jAjax({
    url: "loadSecond",
    onComplete: function(xhr,status){
      if(xhr.readyState === 4){
          if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

             var result = JSON.parse(xhr.responseText);

                   drawChart();

                   function drawChart() {
                     var data = google.visualization.arrayToDataTable([]);
                     data.addColumn('number', 'Images');
                     data.addColumn('number', 'Users');

                     for(var i= 0; i < result.length ; i++){
                        data.addRow([result[i].images , result[i].users ]);
                     }
                             var options = {
                               title: 'Number of users that have annotated X images',
                               hAxis: {title: 'image number', titleTextStyle: {color: 'red'}}
                             };

                     var chart = new google.visualization.ColumnChart(document.getElementById('chart_div2'));
                     chart.draw(data, options);
                   }
          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });
}

