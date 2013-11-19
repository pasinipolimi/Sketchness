function download1(){

    $.jAjax({
        url: "downloadStats1",
        onComplete: function(xhr,status){
          if(xhr.readyState === 4){
              if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

                  var result = xhr.responseText;
                  var blob = new Blob([result], {
                      type: "text/plain;charset=utf-8;",
                  });
                  saveAs(blob, "stats1.json");
              }
              else{
                  alert("Request was unsuccesfull: "+ xhr.status);
              }
          }
        }
      });





}