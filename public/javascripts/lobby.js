require(["Communicator", "jquery", "popup", "jscrollpane"], function(Communicator, $) {

	$(function() {

		var sketchness = {
			players: [],
			myself: $('#username').val(),
		};

		// Show error message
		var setError = function(message) {
			$("#onError span").text(message);
			$("#onError").show();
			$("#pageheader").hide();
			$("#mainPage").hide();
		};

		$("#gameListBody").jScrollPane({
			showArrows: true,
			mantainPosition: true,
			stickToBottom: true,
			animateScroll: true,
			horizontalGutter: 10
		});

		//Popup for the game creation panel
		var options = { width: 350, height: 350, top: 200, left: 100 };
		$('.default_popup').popup(options);

		var chosenId = undefined;

		//Set the game ID when the user select a row in the
		//game list; highlight the chosen row
		$('#gameList').on('click', 'tr', function(event) {
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

		// WebSocket
		var communicator = new Communicator($('#lobbyWebSocket').data('ws'));
		$(communicator.websocket).on({
			close: function(evt) {
				setError("Connection lost");
			},
			error: function(evt) {
				setError(evt);
			}
		});

		var chat = new Chat($("#messages"), $("#talk"));
		$(chat).on("send", function(e, message) {
			communicator.send("chat", { message: message });
		});

		communicator.on({
			chat: function(e, content) {
				chat.message(sketchness.players[content.user].name, content.message, content.user === sketchness.myself);
			},
			log: function(e, content) {
				chat.log(content.level, content.message);
			}
		});

		var writePlayers = function(players, myself) {
			var container = $("#unorderedUserList");
			container.empty();
			$.each(players, function(id, player) {
				if(id !== myself) {
					container.append($('<li class="avatarIcon">' + player.name + '</li>'));
				}
			});
		};

		communicator.on({
			join: function(e, content) {
				sketchness.players[content.user] = {
					id: content.user,
					name: content.name,
					img: content.img,
				};

				writePlayers(sketchness.players, sketchness.myself);
			},
			leave: function(e, content) {
				delete sketchness.players[content.user];
				writePlayers(sketchness.players, sketchness.myself);
			}
		});

		communicator.on("updateList", function(e, data) {
			var $gameInstance = $("#" + data.id);
			//Modify existing row
			if($gameInstance.length > 0) {
				//If the room is empty, it means that it has been destroyed, remove it from the list
				if(data.currentPlayers <= 0 || data.visible == false) {
					$gameInstance.remove();
				} else { //Otherwise update the player count
					var html = '';
					html += '<td>' + data.roomName + '</td><td>' + data.currentPlayers +'/' + data.maxPlayers + '</td>';
					$gameInstance.html(html);
				}
			} else { //Otherwise add a new game to the list
				if(data.visible != false) {
					var html = '';
					html += '<tr id=' + data.id + ' name=' + data.roomName + '><td>' + data.roomName + '</td><td>' + data.currentPlayers + '/' + data.maxPlayers + '</td></tr>';
					var paneApi = $("#gameListBody").data('jsp');
					paneApi.getContentPane().append(html);
					paneApi.reinitialise();
					paneApi.scrollToBottom();
					//$("#gameList tbody").append(html);
				}
			}
		});

	});

});
