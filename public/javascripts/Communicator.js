define(["Class","jquery"], function(Class, $) {

	/**
	 * Handles comunications with the server
	 */
	return new Class({
		_name: "Communicator",

		/**
		 * Initializes the WebSocket connection and
		 * the event firing mechanism
		 *
		 * @param url :String The url of the server
		 */
		_init: function(url) {
			this.websocket = new window.WebSocket(url);

			var $this = $(this);
			$(this.websocket).on("message", function(event) {
				var message = JSON.parse(event.originalEvent.data).message;
				event.type = message.type;
				$this.trigger(event, [message.content]);
				console.log(event);
			});
		},

		_proto: {
			/**
			 * Check if the socket is connected
			 *
			 * @return :Boolean True if the socket is open
			 */
			isConnected: function() {
				return this.websocket.readyState === 1;
			},

			/**
			 * Sends an object through the socket
			 *
			 * @param type :String The type of the message to send
			 * @param content :Object The content of the message to send
			 *
			 * @return :Communicator The communicator itself for chaining
			 */
			send: function(type, content) {
				if(this.isConnected()) {
					var message = {
						message: {
							type: type,
							content: content
						}
					};

					this.websocket.send(JSON.stringify(message));
				}
				return this;
			},

			/**
			 * Closes the connection
			 *
			 * @param code :Number The disconnection code to send
			 * @param reason :String The reason of the disconnection
			 *
			 * @return :Communicator The communicator itself for chaining
			 */
			close: function(code, reason) {
				if(this.isConnected()) this.websocket.close(code, reason);
				return this;
			},

			/**
			 * Binds an event listener for a specific message type
			 *
			 * @see jQuery.fn.on For clarification of the model
			 *
			 * @param type :String The type of the message to listen
			 * @param data :Object The eventual data to pass
			 * @param handler :Function([event, message]) The callback function
			 *        Receives as parameters the event and the message object
			 *
			 * @return :Communicator The communicator itself for chaining
			 */
			on: function(type, data, handler) {
				$(this).on(type, data, handler);
				return this;
			},

			/**
			 * Binds an event listener for a specific message type
			 * and the event is esecuted almost once.
			 *
			 * @see jQuery.fn.one For clarification of the model
			 *
			 * @param type :String The type of the message to listen
			 * @param data :Object The eventual data to pass
			 * @param handler :Function([event, message]) The callback function
			 *        Receives as parameters the event and the message object
			 *
			 * @return :Communicator The communicator itself for chaining
			 */
			one: function(type, data, handler) {
				$(this).one(type, data, handler);
				return this;
			},

			/**
			 * Unbind an event listener for a specific message type
			 *
			 * @see jQuery.fn.off For clarification of the model
			 *
			 * @param type :String The type of the message to unbind
			 * @param handler :Function([event, message]) The callback function
			 *
			 * @return :Communicator The communicator itself for chaining
			 */
			off: function(type, handler) {
				$(this).off(type, handler);
				return this;
			}
		}
	});

});
