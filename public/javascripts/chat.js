jQuery(function($) {

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

	// Init jScrollPane
	$('.jscroll').jScrollPane({
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
		$(el).addClass(data.kind);
		if (data.user === '@username') $(el).addClass('me');

		var paneApi = $('.jscroll').data('jsp');
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
				if (this !== '@username') {
					userCounter++;
					$("#opponent" + userCounter).text(this);
				}
			});
		}	
	});


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
