function caricaStatisticheUsers(){

  $.jAjax({
    url: "LoadUsersStats",
    onComplete: function(xhr,status){
      if(xhr.readyState === 4){
          if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

             var result = JSON.parse(xhr.responseText);
             
             users = result.usersInfo[0];
             var id, name, numPlays, numAnn;
             var table = $("#statisticheUsers");

             $.each(users, function(i,d){
            	    
            	 	cubrik_userid = d.cubrik_userid.substring(1, d.cubrik_userid.length -1);
            	 	app_id = d.app_id.substring(1, d.app_id.length -1);
            	 	app_user_id = d.app_user_id.substring(1, d.app_user_id.length -1);
            	 	number_of_plays = d.number_of_plays.substring(1, d.number_of_plays.length -1);
            	 	number_of_annotations = d.number_of_annotations.substring(1, d.number_of_annotations.length -1);
             		table.append("<tr><td class='infoValue'>"+cubrik_userid+"</td><td class='infoValue'>"+app_id+"</td><td class='infoValue'>"+app_user_id+"</td><td class='infoValue'>"+number_of_plays+"</td><td class='infoValue'>"+number_of_annotations+"</td><td class='infoValue'>"+0+"</td></tr>");
            	 	//table.append("<tr><td class='infoValue'>"+cubrik_userid+"</td><td class='infoValue'>"+app_id+"</td><td class='infoValue'>"+app_user_id+"</td><td class='infoValue'>"+d.number_of_plays+"</td><td class='infoValue'>"+d.number_of_annotations+"</td><td class='infoValue'>"+0+"</td></tr>");
             });


          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });

}