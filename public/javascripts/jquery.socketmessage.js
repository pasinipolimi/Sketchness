(function($) {

	/**
	 * Handle in a special way the websocket on message event
	 * (It copy the message data in the jQuery event data object,
	 * then let the event handling continue normally)
	 * @see jQuery internal events dispatching mechanism
	 */
	$.event.special.message = {
		handle: function(event) {
			if(event.target instanceof WebSocket) {
				event.data = $.extend(true, event.data, JSON.parse(event.originalEvent.data));
			}
			return event.handleObj.handler.apply(this, arguments);
		}
	};

})(jQuery);
