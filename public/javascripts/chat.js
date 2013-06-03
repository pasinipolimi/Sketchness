jQuery(function($) {

	// Show error message
	var setError = function(message) {
		$("#onError span").text(message);
		$("#onError").show();
		$("#pageheader").hide();
		$("#mainPage").hide();
	};

	// Capabilities testing
	if (!window.WebSocket) {
		setError("WebSockets are not supported by your browser.");
		return;
	}

	var canv = $("#me")[0];
	if (!(canv.getContext && canv.getContext('2d'))) {
		setError("Canvas is not supported by your browser.");
		return;
	}

	// Init jScrollPane
	$('.jscroll').jScrollPane({
		showArrows: true,
		mantainPosition: true,
		stickToBottom:true,
		animateScroll: true,
		horizontalGutter: 10
	});

	// Init WebSocket
	var chatSocket = new window.WebSocket($('#chatWebSocket').data('ws'));

	// Listen for message delivery
	$(chatSocket).on("message", function(event) {
		var data = event.data;

		// Handle errors
		if (data.error) {
			chatSocket.close();
			setError(data.error);
			return;
		} else {
			$("#onChat").show();
		}

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

		// Update the members list
		var userCounter = 0;
		$(data.members).each(function() {
			if (this !== '@username') {
				userCounter++;
				$("#opponent" + userCounter).text(this);
			}
		});
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
			chatSocket.send(JSON.stringify({ text: $(this).val() }));
			$(this).val("");
			$messages.animate({ scrollTop: $messages[0].scrollHeight }, 1000);
		}
	});
});
