require(["Communicator", "jquery", "i18n", "jscrollpane"], function(Communicator, $) {

<<<<<<< HEAD
	// Init jScrollPane
	$("#messages").jScrollPane({
		showArrows: true,
		mantainPosition: true,
		stickToBottom:true,
		animateScroll: true,
		horizontalGutter: 10
	});
	
	communicator.on("system", function(e, message) {
		sendMessage(message);
	});
	
	communicator.on("join", function(e, message) {
		sendMessage(message);
	});
	
	communicator.on("quit", function(e, message) {
		sendMessage(message);
	});
	
	communicator.on("talk", function(e, message) {
		sendMessage(message);
	});
	
	communicator.on("talkNear", function(e, message) {
		sendMessage(message);
	});
	
	communicator.on("talkWarning", function(e, message) {
		sendMessage(message);
	});
	
	communicator.on("talkError", function(e, message) {
		sendMessage(message);
	});
	
	var sendMessage=function(data)
	{
		// Create the message element
		var el = $('<div class="message"><span></span><p></p></div>');
		$("span", el).text(data.user);
		$("p", el).text(data.message);
		$(el).addClass(data.type);
		if (data.user === $('#currentNickname').text()) $(el).addClass('me');

		var paneApi = $("#messages").data('jsp');
		paneApi.getContentPane().append(el);
		paneApi.reinitialise();
		paneApi.scrollToBottom();
	};
	
	
	communicator.on("membersChange", function(e, data) {
		var $list = $("#unorderedUserList");
		// Update the members list
		var userCounter = 0;
		if($list.length>0)
		{
			$list.empty();
			$(data.members).each(function()
			{
				var el = $('<li class="avatarIcon">'+this+'</li>');
				$list.append(el);
			});
		}
		//Deal with a custom way of handling the userlist
		else
		{
			$(data.members).each(function() {
				if (this != $('#currentNickname').text()) {
					userCounter++;
					$("#opponent" + userCounter).attr('name',this);
					$("#opponent" + userCounter).attr('class',"opponentAvatar");
				}
			});
		}	
	});
=======
	$(function() {

		// Show error message
		var setError = function(message) {
			$("#onError span").text(message);
			$("#onError").show();
			$("#pageheader").hide();
			$("#mainPage").hide();
		};

		// WebSocket
		var communicator = new Communicator($('#chatWebSocket').data('ws'));

		$(communicator.websocket).on({
			close: function(evt) {
				setError("Connection lost");
			},
			error: function(evt) {
				setError(evt);
			}
		});

		// Init jScrollPane
		$("#messages").jScrollPane({
			showArrows: true,
			mantainPosition: true,
			stickToBottom:true,
			animateScroll: true,
			horizontalGutter: 10
		});

		communicator.on("system join quit talk talkNear talkWarning talkError", function(e, data) {
			// Create the message element
			var el = $('<div class="message"><span></span><p></p></div>');
			$("span", el).text(data.user);
			$("p", el).text(data.message);
			$(el).addClass(data.type);
			if (data.user === '@username') $(el).addClass('me');

			var paneApi = $("#messages").data('jsp');
			paneApi.getContentPane().append(el);
			paneApi.reinitialise();
			paneApi.scrollToBottom();
		});

		communicator.on("membersChange", function(e, data) {
			var $list = $("#unorderedUserList");
			// Update the members list
			var userCounter = 0;
			if($list.length>0)
			{
				$list.empty();
				$(data.members).each(function()
				{
					var el = $('<li class="avatarIcon">'+this+'</li>');
					$list.append(el);
				});
			}
			//Deal with a custom way of handling the userlist
			else
			{
				$(data.members).each(function() {
					if (this !== '@username') {
						userCounter++;
						$("#opponent" + userCounter).text(this);
					}
				});
			}
		});
>>>>>>> 5a4282b06d9deda7713e0757376e4363a7f4449a


		// Init text area placeholder
		var $talk = $("#talk");
		var placeholder = $.i18n.prop('text_area_message');

		$talk.val(placeholder);

		$talk.on("focus", function() {
			if($(this).val() == placeholder) {
				$(this).val("");
			}
		});

		$talk.on("blur", function() {
			if($(this).val() == "") {
				$(this).val(placeholder);
			}
		});

		// Listen for message sending
		var $messages = $("#messages");
		$talk.on("keypress", function(event) {
			if (event.which === 13) {
				event.preventDefault();
				communicator.send({ text: $(this).val() });
				$(this).val("");
				$messages.animate({ scrollTop: $messages[0].scrollHeight }, 1000);
			}
		});

	});

});
