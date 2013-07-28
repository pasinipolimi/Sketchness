jQuery(function($) {

	// Show error message
	var setError = function(message) {
		$("#onError span").text(message);
		$("#onError").show();
		$("#pageheader").hide();
		$("#mainPage").hide();
	};
	
	//Popup for the game creation panel
	var options = { width:350, height:350, top:200,left:100 };
	$('.default_popup').popup(options);
	
	var chosenId=undefined;

	// WebSocket
	var communicator = new Communicator($('#lobbyWebSocket').data('ws'));

	$(communicator.websocket).on({
		open: function(evt) {
		},
		close: function(evt) {
			setError("Connection lost");
			write.error("Connection lost");
		},
		error: function(evt) {
			setError(evt);
			console.error("error", evt);
		}
	});
	
	//Set the game ID when the user select a row in the
	//game list; highlight the chosen row
	$('#gameList').on('click','tr', function (event) {
				//Pick the chosen row
				var $tr = $(this).closest('tr');
				//If it is not the head row of the table
				if($tr.attr('class') != 'head')
				{
					//Get the id of the game
					var id = $tr.attr('name');
					var $currentRoom = $("#roomId");
					$currentRoom.attr('value',id);
					//Remove the highlighting from the other rows
					$('#gameList')
						.children("tr")
						.removeClass("highlight");
					//Highlight the curront row
					$tr.addClass("highlight");
				}
			});
	
	communicator.on("updateList", function(e, data)
	{
		var $gameInstance = $("#"+data.id);
		//Modify existing row
		if($gameInstance.length>0)
		{
			//If the room is empty, it means that it has been destroyed, remove it from the list
			if(data.currentPlayers<=0||data.visible==false)
			{
				$gameInstance.remove();
			}
			//Otherwise update the player count
			else
			{
					var html = '';
					html += '<td>' + data.roomName + '</td><td>' + data.currentPlayers +'/' + data.maxPlayers + '</td>';
					$gameInstance.html(html);
			}
		}
		//Otherwise add a new game to the list
		else
		{
			if(data.visible!=false)
			{
				var html = '';
				html += '<tr id='+data.id+' name='+data.roomName+'><td>' + data.roomName + '</td><td>' + data.currentPlayers +'/' + data.maxPlayers + '</td></tr>';
				$('#gameList tbody').first().after(html);
			}
		}
	});
});
