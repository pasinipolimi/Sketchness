define(["Class", "Time", "jquery", "i18n"], function(Class, Time, $) {

	/**
	 * Component that handles the DOM manipulation
	 */
	return new Class({
		_name: "Writer",

		/**
		 * Initialize the component given the elements
		 * to manipulate and the user ID.
		 *
		 * @param elements :Object The HTML elements to manipulate
		 *     @property score :jQuery.element The score element
		 *     @property time :jQuery.element The time element
		 *     @property top :jQuery.element The top element
		 *     @property error :jQuery.element The error element
		 *     @property canvasMessage :jQuery.element The canvasMessage element
		 *     @property warnTag :jQuery.element The warnTag element
		 *     @property main :jQuery.element The main page element
		 *     @property header :jQuery.element The header element
		 *     @property opponents :jQuery.element The opponents list element
		 * @param myself :String The ID of the current user
		 */
		_init: function(elements, myself) {
			this.elements = elements;
			this.myself = myself;
		},

		_proto: {
			/**
			 * Writes the score of the current player
			 *
			 * @param score :Number The score
			 */
			score: function(score) {
				var html = "<font size='5'><b>" + score + "</b></font>";
				this.elements.score.html(html);
			},

			/**
			 * Writes the time left for the countdown
			 *
			 * @param time :Number The time in ms
			 */
			time: function(time) {
				var html = "";
				if(time || time === 0) {
					html = "<font size='5'><b>" + $.i18n.prop('time') + Time.round(time, Time.second) + "</b></font>";
				}
				this.elements.time.html(html);
			},

			/**
			 * Writes the top message
			 *
			 * @param text :String The text of the main message
			 * @param red :String The text of the additional red message
			 */
			top: function(text, kind) {
				var html = "";
				if(text !== undefined) {
					html = "<font size='5'><b>" + text;
				}
				else {
					html = "<font size='5'><b>";
				}
				if(kind !== undefined) {
					html += "<div class='ico "+kind+"'></div>";
				}
				html += "</b></font>";

				this.elements.top.html(html);
			},

			/**
			 * Writes the error message
			 *
			 * @param message :String The message to write
			 */
			error: function(message) {
				var elements = this.elements;
				$("span", elements.error).text(message);
				elements.error.show();
				elements.header.hide();
				elements.main.hide();
			},

			/**
			 * Writes a message over the canvas
			 *
			 * @param message :String The message to write
			 */
			canvasMessage: function(message) {
				var canvasMessage = this.elements.canvasMessage;
				if(message) {
					var html = "<font size='5'><b><pre id='canvasPre'>" + message + "</pre></b></font>";
					canvasMessage.show();
					canvasMessage.html(html);
				} else {
					canvasMessage.hide();
				}
			},

			/**
			 * Writes a message near the tag input field
			 *
			 * @param message :String The message to write
			 *
			warnTag: function(message) {
				var warnTag = this.elements.warnTag;
				if(message) {
					$("pre", warnTag).html(message);
					warnTag.show();

					var that = this;
					warnTag.on("click", function() {
						that.warnTag();
					});
				} else {
					warnTag.hide();
					warnTag.off("click");
				}
			},*/

			/**
			 * Write the list of the opponents avatars
			 *
			 * @param players :Array The opponents lists
			 */
			players: function(players) {
				var myself = this.myself,
					opponents = this.elements.opponents;

				opponents.empty();
				$.each(players, function(id, player) {
					if (id != myself) {
						var element = $("<li></li>");
						element.attr("title", player.name);
						opponents.append(element);
					}
				});
			}
		}
	});
});
