jQuery(function($) {

	var time = new Time();

	var isIframe = (window.parent !== window);
	var isMobile = /ipad|iphone|android/i.test(navigator.userAgent);

	// CONSTANTS
	var CONSTANTS = {
		PEN_COLOR: 'red',
		PEN_SIZE: 5,
		ERASER_COLOR: "rgba(255,255,255,1.0)",
		ERASER_SIZE: 25,
		TRACKER_COLOR: 'red',
		MIN_SEND_RATE: 50, // the min interval in ms between 2 send
		DEFAULT_ROUND_TIME: 90,
		DEFAULT_ASK_TIME: 20
	};

	// STATES
	var user = {
		id: null,
		name: null,
	}

	// CANVASES
	var canvases = {
		me: {
			el: $("#me"),
			ctx: $("#me")[0].getContext("2d")
		},
		positions: {
			el: $("#positions"),
			ctx: $("#positions")[0].getContext("2d")
		},
		draws: {
			el: $("#draws"),
			ctx: $("#draws")[0].getContext("2d")
		},
		task: {
			el: $("#task"),
			ctx: $("#task")[0].getContext("2d")
		},
		hud: {
			el: $("#hud"),
			ctx: $("#hud")[0].getContext("2d")
		},
	}

	// TOOLS STATUS
	var tool = {
		color: CONSTANTS.PEN_COLOR,
		size: CONSTANTS.PEN_SIZE,
		draw: true
	}

	//GAME STATUS
	var game = {
		role: null,
		score: 0,
		matchStarted: false,
		guessWord: "",
		guessed: false,
		taskImage: null,
		tagging: false,
		traceNum: 1
	}

	var track = {
		x: null,
		y: null
	}

	var dirtyPositions = false;

	// every player positions
	var players = [];

	// Dunno, variables before in an enclosure causing problems
	var skipButton;
	var roundEnd;

	var write = {
		score: function(score) {
			var html = "<font size='5'><b>" + score + "</b></font>";
			$("#score").html(html);
		},
		time: function(time) {
			var html = "";
			if(time || time === 0) {
				html = "<font size='5'><b>" + $.i18n.prop('time') + Time.round(time, Time.second) + "</b></font>";
			}
			$("#timeCounter").html(html);
		},
		top: function(text, red) {
			var html = "<font size='5'><b>" + text;
			if(red !== undefined) {
				html += "<font color='red'>" + red + "</font>";
			}
			html += "</b></font>";

			$("#topMessage").html(html);
		},
		error: function(message) {
			$("#onError span").text(message);
			$("#onError").show();
			$("#pageheader").hide();
			$("#mainPage").hide();
		}
	}

	if (!game.matchStarted) write.top($.i18n.prop('waiting'));

	var setColor = function(c) {
		tool.color = c;
		communicator.send({
			type: 'change',
			size: tool.size,
			color: tool.color,
			name: user.name,
			role: game.role
		});
	};

	var setSize = function(s) {
		tool.size = s;
		communicator.send({
			type: 'change',
			size: tool.size,
			color: tool.color,
			name: user.name,
			role: game.role
		});
	};

	var hud = {
		tools: {
			pen: {
				enabled: "assets/images/UI/Controls/pencil.png",
				disabled: "assets/images/UI/Controls/pencilD.png",
				size: { x: 70, y: 70 },
				position: { x: 0, y: 130 }
			},
			eraser: {
				enabled: "assets/images/UI/Controls/eraser.png",
				disabled: "assets/images/UI/Controls/eraserD.png",
				size: { x: 70, y: 70 },
				position: { x: 0, y: 200 }
			}
		},
		preload: function(canvas) {
			$.each(this.tools, function(name, tool) {
				$.each(["enabled", "disabled"], function(key, status) {
					var src = tool[status];
					tool[status] = new Image();
					tool[status].src = src;
				});
			});
		},
		clear: function() {
			canvases.hud.ctx.clearRect(0, 0, canvases.hud.el.width(), canvases.hud.el.height());
		},
		paint: function(active) {
			this.clear();
			$.each(this.tools, function(name, tool) {
				var status = (name === active ? "enabled" : "disabled");
				canvases.hud.ctx.drawImage(tool[status], tool.position.x, tool.position.y, tool.size.x, tool.size.y);
			});
		},
		bindClick: function() {
			var self = this;
			//[TODO] It should be screen and resolution independent, it can't work like that
			canvases.hud.el.on("click", function (e) {
				var o = relativePosition(e, canvases.hud.el);
				if ((o.y >= 130) && (o.y < 200)) {
					setColor(CONSTANTS.PEN_COLOR);
					setSize(CONSTANTS.PEN_SIZE);
					self.paint("pen");
				} else if ((o.y >= 200) && (o.y <= 270)) {
					setColor(CONSTANTS.ERASER_COLOR);
					setSize(CONSTANTS.ERASER_SIZE);
					self.paint("eraser");
				}
			});
		},
		unbindClick: function() {
			canvases.hud.el.off("click");
		}
	}
	hud.preload();

	/*****************************UTILITY FUNCTIONS********************************************/

	if (!user.name) {
		user.name = $('#currentNickname').text() ||
			localStorage.getItem("pname") ||
			("iam" + Math.floor(100 * Math.random()));
		localStorage.setItem("pname", user.name);
	}

	// WebSocket
	var communicator = new Communicator($('#paintWebSocket').data('ws'));

	$(communicator.websocket).on({
		open: function(evt) {
			communicator.send({
				type: 'change',
				size: tool.size,
				color: tool.color,
				name: user.name
			});
		},
		close: function(evt) {
			write.error("Connection lost");
		},
		error: function(evt) {
			console.error("error", evt);
		},
		message: function() {
			dirtyPositions = true;
		}
	});

	var getPlayer = function(username) {
		for (i = 0; i < players.length; i++) {
			if (players[i].name.toLowerCase() === username.toLowerCase()) {
				return players[i];
			}
		}
	};

	var deletePlayer = function(username) {
		for (i = 0; i < players.length; i++) {
			if (players[i].name.toLowerCase() === username.toLowerCase()) {
				delete players[i];
				return;
			}
		}
	};

	var playerExtend = function(message) {
		var username = message.name;
		for (i = 0; i < players.length; i++) {
			if (players[i].name.toLowerCase() === username.toLowerCase()) {
				players[i] = $.extend(players[i], message);
			}
		}
	};

	//***************************TAKING CARE OF THE LINE STROKES*****************************/
	(function() {
		skipButton = $('#skipTask');
		skipButton.hide();
		skipButton.on("click", function() {
			if (game.role === "SKETCHER") {
				if (game.tagging) {
					communicator.send({ type: 'skipTask', timerObject: 'tag' });
				} else {
					communicator.send({ type: 'skipTask', timerObject: 'round' });
				}
			}
		});
		/*******************************MANAGING THE INCOMING MESSAGES*****************/
		communicator.on("role", function(e, message) {
			game.tagging = false;
			//Fix the drawing style for the player
			canvases.draws.ctx.globalCompositeOperation = "destination-out";
			game.guessed = false;
			game.role = message.role;
			game.matchStarted = true;
			var player = getPlayer(user.name);
			player.role = message.role;
			communicator.send({
				type: 'change',
				size: tool.size,
				color: tool.color,
				name: user.name,
				role: game.role
			});
			if (game.role === "SKETCHER") {
				$('#roleSpan').text($.i18n.prop('sketcher'));
				$('#mainPage').removeClass('guesser');
				$('#mainPage').addClass('sketcher');
				$('#talk').attr('disabled', 'disabled');
				skipButton.show();
			} else {
				$('#roleSpan').text($.i18n.prop('guesser'));
				$('#mainPage').removeClass('sketcher');
				$('#mainPage').addClass('guesser');
				$('#talk').removeAttr('disabled');
				skipButton.hide();

				write.top($.i18n.prop('guess'));
			}
			time.setCountdown("round", CONSTANTS.DEFAULT_ROUND_TIME * Time.second, Time.second, write.time, roundEnd);
			time.setTimer("round");
			time.clearCountdown("tag");
			canvases.draws.ctx.clearRect(0, 0, canvases.draws.el.width(), canvases.draws.el.height());
			canvases.task.ctx.clearRect(0, 0, canvases.task.el.width(), canvases.task.el.height());
			canvases.positions.ctx.clearRect(0, 0, canvases.positions.el.width(), canvases.positions.el.height());
		});

		communicator.on("move", function(e, message) {
			track.x = message.x;
			track.y = message.y;
		});

		communicator.on("task", function(e, message) {
			game.tagging = false;
			game.guessWord = message.tag;

			if (game.role === "SKETCHER") {
				hud.paint("pen");
				hud.bindClick();
				write.top($.i18n.prop('draw'), game.guessWord);
			} else if (game.guessed) {
				write.top($.i18n.prop('guessed'), game.guessWord);
			} else {
				write.top($.i18n.prop('solution'), game.guessWord);
			}

			game.taskImage = new Image();
			game.taskImage.src = message.image;
			//Wait for the image to be loaded before drawing it to canvas to avoid errors
			$(game.taskImage).on("load", function () {
				canvases.task.ctx.save();
				canvases.task.ctx.beginPath();
				var x = 0;
				var y = 0;
				var width = Math.round(canvases.task.el.width());
				var height = Math.round(canvases.task.el.height());
				radius = 50;
				canvases.task.ctx.moveTo(x + radius, y);
				canvases.task.ctx.lineTo(x + width - radius, y);
				canvases.task.ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
				canvases.task.ctx.lineTo(x + width, y + height - radius);
				canvases.task.ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
				canvases.task.ctx.lineTo(x + radius, y + height);
				canvases.task.ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
				canvases.task.ctx.lineTo(x, y + radius);
				canvases.task.ctx.quadraticCurveTo(x, y, x + radius, y);
				canvases.task.ctx.closePath();
				canvases.task.ctx.clip();
				canvases.task.ctx.drawImage(game.taskImage, 0, 0, width, height);
				canvases.task.ctx.restore();
			});
		});

		communicator.on("tag", function(e, message) {
			game.tagging = true;
			game.matchStarted = true;
			write.time(null);
			time.setCountdown("tag", CONSTANTS.DEFAULT_ASK_TIME * Time.second, Time.second, write.time, roundEnd);
			game.role = message.role;
			if (message.role === "SKETCHER") {
				$('#talk').removeAttr('disabled');
				skipButton.show();
				write.top($.i18n.prop('asktagsketcher'));
				canvases.draws.ctx.clearRect(0, 0, canvases.draws.el.width(), canvases.draws.el.height());
				canvases.task.ctx.clearRect(0, 0, canvases.task.el.width(), canvases.task.el.height());
				game.taskImage = new Image();
				game.taskImage.src = message.image;
				//Wait for the image to be loaded before drawing it to canvas to avoid
				//errors
				$(game.taskImage).on("load", function () {
					canvases.task.ctx.save();
					canvases.task.ctx.beginPath();
					var x = 0;
					var y = 0;
					var width = Math.round(canvases.task.el.width());
					var height = Math.round(canvases.task.el.height());
					radius = 50;
					canvases.task.ctx.moveTo(x + radius, y);
					canvases.task.ctx.lineTo(x + width - radius, y);
					canvases.task.ctx.quadraticCurveTo(x + width, y, x + width, y + radius);
					canvases.task.ctx.lineTo(x + width, y + height - radius);
					canvases.task.ctx.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
					canvases.task.ctx.lineTo(x + radius, y + height);
					canvases.task.ctx.quadraticCurveTo(x, y + height, x, y + height - radius);
					canvases.task.ctx.lineTo(x, y + radius);
					canvases.task.ctx.quadraticCurveTo(x, y, x + radius, y);
					canvases.task.ctx.closePath();
					canvases.task.ctx.clip();
					canvases.task.ctx.drawImage(game.taskImage, 0, 0, width, height);
					canvases.task.ctx.restore();
				});
			} else {
				$('#talk').removeAttr('disabled');
				skipButton.hide();
				write.top($.i18n.prop('asktag'));
				canvases.draws.ctx.clearRect(0, 0, canvases.draws.el.width(), canvases.draws.el.height());
				canvases.task.ctx.clearRect(0, 0, canvases.task.el.width(), canvases.task.el.height());
			}
		});

		communicator.on("points", function(e, message) {
			if (message.name === user.name) {
				game.score += message.points;
				game.guessed = true;
				skipButton.hide();
			}
		});

		communicator.on("timeChange", function(e, message) {
			if (time.getCountdown(message.timeObject) > (message.amount * Time.second)) {
				time.changeCountdown(message.timeObject, message.amount * Time.second);
			}
		});

		communicator.on("showImages", function(e, message) {
			game.role = "ROUNDCHANGE";
			skipButton.hide();
			time.clearCountdown("round");
			time.setCountdown("round", message.seconds * Time.second, Time.second, write.time, roundEnd);
			$('#mainPage').removeClass('sketcher');
			$('#mainPage').addClass('guesser');
		});

		communicator.on("leaderboard", function(e, message) {
			game.role = "ENDED";

			write.top($.i18n.prop('leaderboard'));
			write.time(null);
			$('#mainPage').removeClass('guesser');
			$('#mainPage').removeClass('sketcher');
			skipButton.hide();

			//Clear all the canvas and draw the leaderboard
			canvases.draws.ctx.clearRect(0, 0, canvases.draws.el.width(), canvases.draws.el.height());
			canvases.task.ctx.clearRect(0, 0, canvases.task.el.width(), canvases.task.el.height());
			canvases.positions.ctx.clearRect(0, 0, canvases.positions.el.width(), canvases.positions.el.height());

			//Disable the chat
			$('#talk').attr('disabled', 'disabled');
			for (var i = 1; i <= message.playersNumber; i++) {
				canvases.task.ctx.fillStyle = "#000000";
				canvases.task.ctx.font = "bold 30px sans-serif";
				canvases.task.ctx.fillText(i + "): " + message.playerList[i - 1].name + " = " + message.playerList[i - 1].points + $.i18n.prop('points'), 30, 50 + 50 * (i - 1));
			}
		});

		communicator.on("trace", function(e, message) {
			player = getPlayer(message.name);
			if (player.color === CONSTANTS.ERASER_COLOR) {
				canvases.draws.ctx.globalCompositeOperation = "destination-out";
			} else {
				canvases.draws.ctx.globalCompositeOperation = "source-over";
			}
			canvases.draws.ctx.lineWidth = player.size;
			canvases.draws.ctx.beginPath();
			var points = message.points;
			points[0] && canvases.draws.ctx.moveTo(points[0].x, points[0].y);
			points.forEach(function (p) {
				canvases.draws.ctx.strokeStyle = p.color;
				canvases.draws.ctx.lineTo(p.x, p.y);
			});
			canvases.draws.ctx.stroke();

			track.x = points[points.length - 1].x;
			track.y = points[points.length - 1].y;

			// clear local canvas if synchronized
			if (message.name === user.name && game.traceNum === message.num) {
				canvases.me.ctx.clearRect(0, 0, canvases.me.el.width(), canvases.me.el.height());
			}
		});

		communicator.on("change", function(e, message) {
			var player = getPlayer(message.name);
			if (player === undefined) {
				player = players[players.length] = message;
			}
			playerExtend(message);
		});

		communicator.on("disconnect", function(e, message) {
			//[TODO]
			//deletePlayer(message.username);
		});

		canvases.draws.ctx.lineCap = 'round';
		canvases.draws.ctx.lineJoin = 'round';

		roundEnd = function() {
			if (game.role === "SKETCHER") {
				hud.clear();
				hud.unbindClick();
			}
			write.time(null);
			communicator.send({ type: 'roundEnded', player: user.name });
		};

	})();

	/*************************DRAWING PANEL**********************/

	//Return the current position of the cursor within the specified element
	var relativePosition = function(event, element) {
		var offset = $(element).offset();
		return {
			x: (event.pageX - offset.left),
			y: (event.pageY - offset.top)
		}
	};

	// The "me" canvas is where the sketcher draws before sending the update status to all the other players
	(function() {
		var pressed;
		var position;

		var points = [];
		var lastSent = 0;

		//Add the current point to the list of points to be sent

		var addPoint = function(x, y, size, color) {
			points.push({
				x: x,
				y: y,
				size: size,
				color: color
			});
		};

		//Send the points to the server as a trace message. It sends the points, the number of the trace sent, the name of the player that has sent the trace

		var sendPoints = function() {
			lastSent = Date.now(); //Refresh the countdown timer
			var gameTimer = time.getTimer("round");
			communicator.send({
				type: "trace",
				points: points,
				num: game.traceNum,
				name: user.name,
				time: gameTimer
			});
			points = [];
		};

		//Send the tracking position of the player that is drawing, sending his current position and name

		var sendMove = function(x, y) {
			lastSent = Date.now();
			communicator.send({
				type: "move",
				x: x,
				y: y,
				name: user.name
			});
		};

		//Can we send? If the current time - the last update is bigger than the treshold, we can send the packets

		var canSendNow = function() {
			if (!game.tagging)
				return Date.now() - lastSent > CONSTANTS.MIN_SEND_RATE;
			else
				return false;
		};

		canvases.me.ctx.lineCap = 'round';
		canvases.me.ctx.lineJoin = 'round';

		//Create a line from the starting point to the end point

		var lineTo = function(x, y) {
			canvases.me.ctx.strokeStyle = tool.color;
			canvases.me.ctx.lineWidth = tool.size;
			canvases.me.ctx.beginPath();
			canvases.me.ctx.moveTo(position.x, position.y);
			canvases.me.ctx.lineTo(x, y);
			canvases.me.ctx.stroke();
		};

		//Handle the mouse movement when drawing

		//Add the event listeners to handle movements, mouse up and mouse down, eventually supporting also mobile devices
		$(document).on((isMobile ? "touchstart" : "mousedown"), function(e) {
			//If the player is a sketcher, update the mouse pressed status to send his traces
			if (game.role === "SKETCHER" && !game.tagging) {
				var o = relativePosition(e, canvases.me.el);
				position = o;
				addPoint(o.x, o.y, tool.size, tool.color);
				pressed = true;
			}
		});

		$("#viewport").on((isMobile ? "touchmove" : "mousemove"), function(e) {
			e.preventDefault();
			//Get the current position with respect to the canvas element we want to draw to
			var o = relativePosition(e, canvases.me.el);
			//If the mouse is pressed and the player is a sketcher
			if (pressed && game.role === "SKETCHER") {
				//Draw the local line
				lineTo(o.x, o.y);
				//Add the points to the points to be sent
				addPoint(o.x, o.y, tool.size, tool.color);
				//We have created a trace
				++(game.traceNum);
				//Can we send the batch of points?
				if (canSendNow()) {
					sendPoints();
					sendMove(o.x, o.y);
					addPoint(o.x, o.y, tool.size, tool.color);
				}
			} else {
				//The mouse is not pressed, can we just send the position of the player?
				if (canSendNow() && game.role === "SKETCHER") {
					sendMove(o.x, o.y);
				}
			}
			position = o;
		});

		$(document).on((isMobile ? "touchend" : "mouseup"), function(e) {
			//If the player is the sketcher, send the last trace and disable the drawing function
			if (game.role === "SKETCHER" && !game.tagging) {
				lineTo(position.x, position.y);
				addPoint(position.x, position.y, tool.size, tool.color);
				addPoint(position.x, position.y, tool.size, "end");
				sendPoints();
				pressed = false;
			}
		});

	})();

	/*************************DRAW PLAYERS POSITION**********************/

	(function () {
		canvases.positions.ctx.font = "9px monospace";
		canvases.positions.ctx.textAlign = "center";
		canvases.positions.ctx.textBaseline = "bottom";

		var render = function() {
			if (game.tagging) {
				canvases.positions.ctx.clearRect(0, 0, canvases.positions.el.width(), canvases.positions.el.height());
				return;
			}
			if (!dirtyPositions) return;
			dirtyPositions = false;
			canvases.positions.ctx.clearRect(0, 0, canvases.positions.el.width(), canvases.positions.el.height());
			if (game.matchStarted) {
				for (i = 0; i < players.length; i++) {
					var player = players[i];
					if (player.role !== "SKETCHER") continue;
					canvases.positions.ctx.beginPath();
					canvases.positions.ctx.strokeStyle = CONSTANTS.TRACKER_COLOR;
					canvases.positions.ctx.arc(track.x, track.y, player.size / 2, 0, 2 * Math.PI);
					canvases.positions.ctx.stroke();
					canvases.positions.ctx.font = "10px sans-serif";
					canvases.positions.ctx.fillStyle = CONSTANTS.TRACKER_COLOR;
					canvases.positions.ctx.fillText((player.name + "").substring(0, 20), track.x, track.y - Math.round(player.size / 2) - 4);
				}
			}
		};

		window.requestAnimationFrame(function loop() {
			window.requestAnimationFrame(loop);
			render();
		}, canvases.positions.el[0]);
	})();

});
