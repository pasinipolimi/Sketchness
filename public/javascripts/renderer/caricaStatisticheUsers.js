function caricaStatisticheUsers(){

  $.jAjax({
    url: "LoadUsersStats",
    onComplete: function(xhr,status){
      if(xhr.readyState === 4){
          if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

             var result = JSON.parse(xhr.responseText);
             
             users = result.usersInfo[0];
             var cubrik_userid, name, app_id, actions, sessions, quality;
             var $table = $("#statisticheUsers");
             var $body = $("#statisticheUsersbody");
             
             
             $.each(users, function(i,d){
            	    
            	 	cubrik_userid = d.id;
            	 	app_id = d.app_id;
            	 	name = d.app_user_id;
            	 	sessions = d.num_sessions;
            	 	actions = d.num_actions;
            	 	quality = d.quality;
            	 	id = d.id;
             		//table.append("<tr><td class='infoValue'>"+cubrik_userid+"</td><td class='infoValue'>"+app_id+"</td><td class='infoValue'>"+name+"</td><td class='infoValue'>"+sessions+"</td><td class='infoValue'>"+actions+"</td><td class='infoValue'>"+quality+"</td></tr>");
            	 	$body.append("<tr><td class='id'>"+id+"</td><td class='infoValue'>"+name+"</td><td class='infoValue'>"+sessions+"</td><td class='infoValue'>"+actions+"</td><td class='infoValue'>"+quality+"</td></tr>");
            	 	//table.append("<tr><td class='infoValue'>"+cubrik_userid+"</td><td class='infoValue'>"+app_id+"</td><td class='infoValue'>"+app_user_id+"</td><td class='infoValue'>"+d.number_of_plays+"</td><td class='infoValue'>"+d.number_of_annotations+"</td><td class='infoValue'>"+0+"</td></tr>");
             });
             
             $table.DataTable( {"pageLength": 100, "order": []});


          }
          else{
              alert("Request was unsuccesfull: "+ xhr.status);
          }
      }
    }
  });

}