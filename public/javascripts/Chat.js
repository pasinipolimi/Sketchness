define(["Class", "jquery", "i18n", "jscrollpane"], function(Class, $) {

	return new Class({
		_name: "Chat",

		_init: function(container, input) {
			this.container = $(container);
			this.input = $(input);

			this.container.jScrollPane({
				showArrows: true,
				mantainPosition: true,
				stickToBottom:true,
				animateScroll: true,
				horizontalGutter: 10
			});

			var $that = $(this);

			this.input.on("keypress", function(e) {
				if (e.which === 13 && $(this).val() !== "") {
					e.preventDefault();
					$that.trigger("send", [$(this).val()]);
					$(this).val("");
				}
			});

		},

		_proto: {
			appendElement: function(element) {
				var paneApi = this.container.data('jsp');
				paneApi.getContentPane().append(element);
				paneApi.reinitialise();
				paneApi.scrollToBottom();
			},

			message: function(name, message, myself, color) {
				var el = $('<div class="message"><div class="avatarChat"><span></span><br /><p></p></div></div>');
				$("span", el).text(name);
				$("p", el).text(message);
				$("p", el).css('color', color);
				if (myself) {
					el.addClass('me');
				}

				this.appendElement(el);
			},

			log: function(level, message) {
				var el = $('<div class="message"><span>Sketchness</span><br /><p></p></div>');
				$("p", el).text(message);
				el.addClass(level);

				this.appendElement(el);
			},

			guess: function(name, word, affinity, myself) {
				var el = $('<div class="message" style="height:100px"><div class="avatarChat"><span></span><p class="ico '+word+'_chat" style="width: 70px;height:70px; padding-top: 10px; padding-bottom: 10px; padding-left: 40px"></p></div></div>');
				$("span", el).text(name);
				if (myself) {
					el.addClass('me');
				}
				this.appendElement(el);
			},

			enable: function() {
				this.input.prop("disabled", false);
			},

			disable: function() {
				this.input.prop("disabled", true);
			}
		}
	});

});
