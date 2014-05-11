require(["Communicator", "Chat", "jquery", "popup", "jscrollpane", "howler"], function(Communicator, Chat, $) {

	$(function() {
	
		var login_sound = new Howl({
			urls: ['assets/sounds/effects/login_ring.ogg']
		});

		var sketchness = {
			players: {},
			myself: $('#currentNickname').text()
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

		$("#unorderedUserList").jScrollPane({
			showArrows: true,
			mantainPosition: true,
			stickToBottom: true,
			animateScroll: true,
			horizontalGutter: 10
		});


		//Popup for the game creation panel
		var options = { width: 350, height: 350, top: 200, left: 100 };
		$('.default_popup').popup(options);
		$('#joinGame').hide();

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
				$('#joinGame').show();
			}
			else {
				$('.highlight').removeClass("highlight");
				$('#joinGame').hide();
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
			communicator.send("chat", { user: sketchness.myself, message: message });
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
			var paneApi = $("#unorderedUserList").data('jsp');
			paneApi.getContentPane().empty();
			var el;
			for(var id in players) {
				if(id !== myself) {
					el = $('<div class="avatarIcon"><span></span></li>');
					$("span", el).text(players[id].name);
					paneApi.getContentPane().append(el);
					paneApi.reinitialise();
					paneApi.scrollToBottom();
				}
			};
			el = $('<div class="avatarIcon"><span></span></li>');
			$("span", el).text(players[myself].name);
			paneApi.getContentPane().append(el);
			paneApi.reinitialise();
			paneApi.scrollToBottom();
		};

		communicator.on({
			join: function(e, content) {
				sketchness.players = [];
			    for(var i in content)
				{
					sketchness.players[content[i].user] = {
						id: content[i].user,
						name: content[i].name,
						img: content[i].img
					};
				}
				writePlayers(sketchness.players, sketchness.myself);
				login_sound.play();
			},
			leave: function(e, content) {
				delete sketchness.players[content.user];
				writePlayers(sketchness.players, sketchness.myself);
			}
		});

		communicator.on( "updateList", function(e, data) {
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
