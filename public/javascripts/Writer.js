define(["Class", "Time", "jquery", "jquery-ui", "i18n"], function(Class, Time, $) {

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
					html = "<p>" + $.i18n.prop('time') + Time.round(time, Time.second) + "</p>";
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
                    html = "<div class='todraw'>" + text;
                }
                else {
                    html = "<div class='todraw'>";
                }
                if(kind !== undefined) {
                    html += "<div class='ico "+kind+"'></div>";
                }
                html += "</div>";

				this.elements.top.html(html);
                // --> MoonSUB
                var test = setTimeout(function(){
                    for(var i=0;i<5;i++) {
                        $('#topMessage .todraw').animate({color:'#f00',fontSize:'24px'}).animate({color:'#000',fontSize:'20px'});
                    }
                },1000);
                // <-- MoonSUB
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
				var count = 0;
				for(var i in players) {
					count++;
					switch(players[i].number) {
						case "0": $('#currentNickname').css('color', players[i].color);break;
						case "1": $('#player2').css('background-color', players[i].color);$('#player2').text(players[i].score);break;
						case "2": $('#player3').css('background-color', players[i].color);$('#player3').text(players[i].score);break;
						case "3": $('#player4').css('background-color', players[i].color);$('#player4').text(players[i].score);break;
						case "4": $('#player5').css('background-color', players[i].color);$('#player5').text(players[i].score);break;
					}
				}
				for(var j=count; j<4;j++) {
					switch(j) {
						case 0: $('#currentNickname').removeAttr('color');break;
						case 1: $('#player2').removeAttr('style');$('#player2').text("");break;
						case 2: $('#player3').removeAttr('style');$('#player3').text("");break;
						case 3: $('#player4').removeAttr('style');$('#player4').text("");break;
						case 4: $('#player5').removeAttr('style');$('#player5').text("");break;
					}
				}
			}
		}
	});
});
