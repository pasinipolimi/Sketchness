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
	var pid;
	var pname;
	var meCtx;
	var role;
	var tagging = false;

	// TOOLS STATUS
	var color = CONSTANTS.PEN_COLOR;
	var size = CONSTANTS.PEN_SIZE;

	//GAME STATUS
	var score = 0;
	var matchStarted = false;
	var guessWord = "";
	var guessed = false;
	var drawTool = true;
	var taskImage;

	var trackX;
	var trackY;

	var numTrace = 1;
	var onSocketMessage;
	var dirtyPositions = false;

	// every player positions
	var players = [];

	var viewport = $("#viewport")[0];

	// Dunno, variables before in an enclosure causing problems
	var skipButton;
	var roundEndMessage;
	var socket;

	var setError = function(message) {
		$("#onError span").text(message);
		$("#onError").show();
		$("#pageheader").hide();
		$("#mainPage").hide();
		throw new Error(message);
	};

	/*****************************UTILITY FUNCTIONS********************************************/

	if (!pname) {
		pname = $('#currentNickname').text() ||
			localStorage.getItem("pname") ||
			("iam" + Math.floor(100 * Math.random()));
		localStorage.setItem("pname", pname);
	}

	// WebSocket
	var communicator = new Communicator($('#paintWebSocket').data('ws'));

	$(communicator.websocket).on({
		open: function(evt) {
			communicator.send({
				type: 'change',
				size: size,
				color: color,
				name: pname
			});
		},
		close: function (evt) {
			setError("Connection lost");
		},
		error: function (evt) {
			console.error("error", evt);
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
		var canvas = $("#draws")[0];
		var ctx = canvas.getContext("2d");
		var taskCanvas = $("#task")[0];
		var taskContext = taskCanvas.getContext("2d");
		var positionCanvas = $("#positions")[0];
		var positionContext = positionCanvas.getContext("2d");
		skipButton = $('#skipTask');
		skipButton.hide();
		skipButton.on("click", function() {
			if (role === "SKETCHER") {
				if (tagging) {
					send({ type: 'skipTask', timerObject: 'tag' });
				} else {
					send({ type: 'skipTask', timerObject: 'round' });
				}
			}
		});
		/*******************************MANAGING THE INCOMING MESSAGES*****************/
		communicator.on("role", function(e, message) {
			tagging = false;
			//Fix the drawing style for the player
			ctx.globalCompositeOperation = "destination-out";
			guessed = false;
			role = message.role;
			matchStarted = true;
			var player = getPlayer(pname);
			player.role = message.role;
			send({
				type: 'change',
				size: size,
				color: color,
				name: pname,
				role: role
			});
			if (role === "SKETCHER") {
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
			}
			time.setCountdown("round", CONSTANTS.DEFAULT_ROUND_TIME * Time.second, Time.second, $.noop, roundEndMessage);
			time.setTimer("round");
			time.clearCountdown("tag");
			ctx.clearRect(0, 0, canvas.width, canvas.height);
			taskContext.clearRect(0, 0, canvas.width, canvas.height);
			positionContext.clearRect(0, 0, canvas.width, canvas.height);
		});

		communicator.on("move", function(e, message) {
			trackX = message.x;
			trackY = message.y;
		});

		communicator.on("task", function(e, message) {
			tagging = false;
			guessWord = message.tag;
			taskImage = null;
			taskImage = new Image();
			taskImage.src = message.image;
			//Wait for the image to be loaded before drawing it to canvas to avoid errors
			$(taskImage).on("load", function () {
				taskContext.save();
				taskContext.beginPath();
				var x = 0;
				var y = 0;
				var style = window.getComputedStyle(taskCanvas);
				var width = style.getPropertyValue('width');
				width = parseInt(width, 10);
				var height = style.getPropertyValue('height');
				height = parseInt(height, 10);
				radius = 50;
				taskContext.moveTo(x + radius, y);
				taskContext.lineTo(x + width - radius, y);
				taskContext.quadraticCurveTo(x + width, y, x + width, y + radius);
				taskContext.lineTo(x + width, y + height - radius);
				taskContext.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
				taskContext.lineTo(x + radius, y + height);
				taskContext.quadraticCurveTo(x, y + height, x, y + height - radius);
				taskContext.lineTo(x, y + radius);
				taskContext.quadraticCurveTo(x, y, x + radius, y);
				taskContext.closePath();
				taskContext.clip();
				taskContext.drawImage(taskImage, 0, 0, width, height);
				taskContext.restore();
			});
		});

		communicator.on("tag", function(e, message) {
			tagging = true;
			matchStarted = true;
			$("#timeCounter").html("");
			time.setCountdown("tag", CONSTANTS.DEFAULT_ASK_TIME * Time.second, Time.second, $.noop, roundEndMessage);
			role = message.role;
			if (message.role === "SKETCHER") {
				$('#talk').removeAttr('disabled');
				skipButton.show();
				var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('asktagsketcher') + "</font>";
				$("#topMessage").html(htmlMessage);
				ctx.clearRect(0, 0, canvas.width, canvas.height);
				taskContext.clearRect(0, 0, canvas.width, canvas.height);
				taskImage = null;
				taskImage = new Image();
				taskImage.src = message.image;
				//Wait for the image to be loaded before drawing it to canvas to avoid
				//errors
				taskImage.on("load", function () {
					taskContext.save();
					taskContext.beginPath();
					var x = 0;
					var y = 0;
					var style = window.getComputedStyle(taskCanvas);
					var width = style.getPropertyValue('width');
					width = parseInt(width, 10);
					var height = style.getPropertyValue('height');
					height = parseInt(height, 10);
					radius = 50;
					taskContext.moveTo(x + radius, y);
					taskContext.lineTo(x + width - radius, y);
					taskContext.quadraticCurveTo(x + width, y, x + width, y + radius);
					taskContext.lineTo(x + width, y + height - radius);
					taskContext.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
					taskContext.lineTo(x + radius, y + height);
					taskContext.quadraticCurveTo(x, y + height, x, y + height - radius);
					taskContext.lineTo(x, y + radius);
					taskContext.quadraticCurveTo(x, y, x + radius, y);
					taskContext.closePath();
					taskContext.clip();
					taskContext.drawImage(taskImage, 0, 0, width, height);
					taskContext.restore();
				});
			} else {
				$('#talk').removeAttr('disabled');
				skipButton.hide();
				var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('asktag') + "</font>" + "<b>" + "</font>";
				$("#topMessage").html(htmlMessage);
				taskContext.clearRect(0, 0, canvas.width, canvas.height);
				ctx.clearRect(0, 0, canvas.width, canvas.height);
			}
		});

		communicator.on("points", function(e, message) {
			if (message.name === pname) {
				score = score + message.points;
				guessed = true;
				skipButton.hide();
			}
		});

		communicator.on("timeChange", function(e, message) {
			if (time.getCountdown(message.timeObject) > (message.amount * Time.second)) {
				time.changeCountdown(message.timeObject, message.amount * Time.second);
			}
		});

		communicator.on("showImages", function(e, message) {
			role = "ROUNDCHANGE";
			skipButton.hide();
			time.clearCountdown("round");
			time.setCountdown("round", message.seconds * Time.second, Time.second, $.noop, roundEndMessage);
			$('#mainPage').removeClass('sketcher');
			$('#mainPage').addClass('guesser');
		});

		communicator.on("leaderboard", function(e, message) {
			role = "ENDED";
			//Clear all the canvas and draw the leaderboard
			ctx.clearRect(0, 0, canvas.width, canvas.height);
			taskContext.clearRect(0, 0, canvas.width, canvas.height);
			positionContext.clearRect(0, 0, canvas.width, canvas.height);

			//Disable the chat
			$('#talk').attr('disabled', 'disabled');
			for (var i = 1; i <= message.playersNumber; i++) {
				taskContext.fillStyle = "#000000";
				taskContext.font = "bold 30px sans-serif";
				taskContext.fillText(i + "): " + message.playerList[i - 1].name + " = " + message.playerList[i - 1].points + $.i18n.prop('points'), 30, 50 + 50 * (i - 1));
			}
		});

		communicator.on("trace", function(e, message) {
			player = getPlayer(message.name);
			if (player.color === CONSTANTS.ERASER_COLOR) {
				ctx.globalCompositeOperation = "destination-out";
			} else {
				ctx.globalCompositeOperation = "source-over";
			}
			ctx.lineWidth = player.size;
			ctx.beginPath();
			var points = message.points;
			points[0] && ctx.moveTo(points[0].x, points[0].y);
			points.forEach(function (p) {
				ctx.strokeStyle = p.color;
				ctx.lineTo(p.x, p.y);
			});
			ctx.stroke();

			trackX = points[points.length - 1].x;
			trackY = points[points.length - 1].y;

			// clear local canvas if synchronized
			if (message.name === pname && numTrace === message.num) {
				meCtx.clearRect(0, 0, meCtx.canvas.width, meCtx.canvas.height);
			}
		});

		communicator.on("change", function(e, message) {
			var player = getPlayer(message.name);
			if (player === undefined) {
				player = players[players.length] = message;
				console.log(player);
			}
			playerExtend(message);
		});

		communicator.on("disconnect", function(e, message) {
			//[TODO]
			//deletePlayer(message.username);
		});

		onSocketMessage = function(e) {
			dirtyPositions = true; //RIVEDERE L'AGGIORNAMENTO DEL TRACKING
		};

		ctx.lineCap = 'round';
		ctx.lineJoin = 'round';

		roundEndMessage = function() {
			send({ type: 'roundEnded', player: pname });
		};

	})();

	/*************************DRAWING PANEL**********************/

	//Return the current position of the cursor within the specified element
	var positionWithE = function(e, obj) {
		var leftm = topm = 0;
		var result = getPosition(obj);
		return {
			x: e.clientX - result.x,
			y: e.clientY - result.y
		};
	};

	//Return the position of the element in the page
	var getPosition = function(element) {
		var xPosition = 0;
		var yPosition = 0;

		while (element) {
			xPosition += (element.offsetLeft - element.scrollLeft + element.clientLeft);
			yPosition += (element.offsetTop - element.scrollTop + element.clientTop);
			element = element.offsetParent;
		}
		return {
			x: xPosition,
			y: yPosition
		};
	};

	// The "me" canvas is where the sketcher draws before sending the update status to all the other players
	(function() {
		var canvas = $("#me")[0];
		var ctx = meCtx = canvas.getContext("2d");
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
			send({
				type: "trace",
				points: points,
				num: numTrace,
				name: pname,
				time: gameTimer
			});
			points = [];
		};

		//Send the tracking position of the player that is drawing, sending his current position and name

		var sendMove = function(x, y) {
			lastSent = Date.now();
			send({
				type: "move",
				x: x,
				y: y,
				name: pname
			});
		};

		//Can we send? If the current time - the last update is bigger than the treshold, we can send the packets

		var canSendNow = function() {
			if (!tagging)
				return Date.now() - lastSent > CONSTANTS.MIN_SEND_RATE;
			else
				return false;
		};

		ctx.lineCap = 'round';
		ctx.lineJoin = 'round';

		//Create a line from the starting point to the end point

		var lineTo = function(x, y) {
			ctx.strokeStyle = color;
			ctx.lineWidth = size;
			ctx.beginPath();
			ctx.moveTo(position.x, position.y);
			ctx.lineTo(x, y);
			ctx.stroke();
		};

		//Handle the mouse movement when drawing

		var onMouseMove = function(e) {
			e.preventDefault();
			//Get the current position with respect to the canvas element we want to draw to
			var o = positionWithE(e, canvas);
			//If the mouse is pressed and the player is a sketcher
			if (pressed && role === "SKETCHER") {
				//Draw the local line
				lineTo(o.x, o.y);
				//Add the points to the points to be sent
				addPoint(o.x, o.y, size, color);
				//We have created a trace
				++numTrace;
				//Can we send the batch of points?
				if (canSendNow()) {
					sendPoints();
					sendMove(o.x, o.y);
					addPoint(o.x, o.y, size, color);
				}
			} else {
				//The mouse is not pressed, can we just send the position of the player?
				if (canSendNow() && role === "SKETCHER") {
					sendMove(o.x, o.y);
				}
			}
			position = o;
		};

		var onMouseDown = function(e) {
			//If the player is a sketcher, update the mouse pressed status to send his traces
			if (role === "SKETCHER" && !tagging) {
				var o = positionWithE(e, canvas);
				position = o;
				addPoint(o.x, o.y, size, color);
				pressed = true;
			}
		};

		var onMouseUp = function(e) {
			//If the player is the sketcher, send the last trace and disable the drawing function
			if (role === "SKETCHER" && !tagging) {
				lineTo(position.x, position.y);
				addPoint(position.x, position.y, size, color);
				addPoint(position.x, position.y, size, "end");
				sendPoints();
				pressed = false;
			}
		};

		//Add the event listeners to handle movements, mouse up and mouse down, eventually supporting also mobile devices
		document.addEventListener(isMobile ? "touchstart" : "mousedown", onMouseDown);
		document.addEventListener(isMobile ? "touchend" : "mouseup", onMouseUp);
		viewport.addEventListener(isMobile ? "touchmove" : "mousemove", onMouseMove);

	})();

	/*************************DRAW PLAYERS POSITION**********************/

	(function () {
		var canvas = $("#positions")[0];
		var ctx = canvas.getContext("2d");

		ctx.font = "9px monospace";
		ctx.textAlign = "center";
		ctx.textBaseline = "bottom";

		var render = function() {
			if (tagging) {
				ctx.clearRect(0, 0, canvas.width, canvas.height);
				return;
			}
			if (!dirtyPositions) return;
			dirtyPositions = false;
			ctx.clearRect(0, 0, canvas.width, canvas.height);
			if (matchStarted) {
				for (i = 0; i < players.length; i++) {
					var player = players[i];
					if (player.role !== "SKETCHER") continue;
					ctx.beginPath();
					ctx.strokeStyle = CONSTANTS.TRACKER_COLOR;
					ctx.arc(trackX, trackY, player.size / 2, 0, 2 * Math.PI);
					ctx.stroke();
					ctx.font = "10px sans-serif";
					ctx.fillStyle = CONSTANTS.TRACKER_COLOR;
					ctx.fillText((player.name + "").substring(0, 20), trackX, trackY - Math.round(player.size / 2) - 4);
				}
			}
		};

		window.requestAnimationFrame(function loop() {
			window.requestAnimationFrame(loop);
			render();
		}, canvas);
	})();

	/*************************TOOLS PANEL MANAGEMENT**********************/
	(function () {
		var hud = $("#hud")[0];
		var ctx = hud.getContext("2d");

		//ICONS
		var penEnabled = new Image();
		var eraserEnabled = new Image();
		var penDisabled = new Image();
		var eraserDisabled = new Image();
		penEnabled.src = 'assets/images/UI/Controls/pencil.png';
		eraserEnabled.src = 'assets/images/UI/Controls/eraser.png';
		penDisabled.src = 'assets/images/UI/Controls/pencilD.png';
		eraserDisabled.src = 'assets/images/UI/Controls/eraserD.png';

		var setColor = function(c) {
			color = c;
			send({
				type: 'change',
				size: size,
				color: color,
				name: pname,
				role: role
			});
		};

		var setSize = function(s) {
			size = s;
			send({
				type: 'change',
				size: size,
				color: color,
				name: pname,
				role: role
			});
		};

		var setup = function() {

			//[TODO] It should be screen and resolution independent, it can't work like that
			hud.addEventListener("click", function (e) {
				var o = positionWithE(e, hud);
				if ((o.y >= 130) && (o.y < 200)) {
					setColor(CONSTANTS.PEN_COLOR);
					setSize(CONSTANTS.PEN_SIZE);
					drawTool = true;
				} else if ((o.y >= 200) && (o.y <= 270)) {
					setColor(CONSTANTS.ERASER_COLOR);
					setSize(CONSTANTS.ERASER_SIZE);
					drawTool = false;
				}
			});
		};

		/************TOOLS PANEL RENDERING***********************/

		var render = function() {
			ctx.clearRect(0, 0, hud.width, hud.height);
			if (!matchStarted && !tagging) {
				var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('waiting') + "<b>" + "</font>";
				$("#topMessage").html(htmlMessage);
			} else {
				switch (role) {
					case "SKETCHER":
						if (!tagging) {
							var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('draw') + "<font color='red'>" + guessWord + "</font>" + "<b>" + "</font>";
							$("#topMessage").html(htmlMessage);
						}
						break;

					case "GUESSER":
						if (!tagging) {
							if (!guessed) {
								var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('guess') + "<b>" + "</font>";
								$("#topMessage").html(htmlMessage);
							} else {
								var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('guessed') + "<font color='red'>" + guessWord + "</font>" + "<b>" + "</font>";
								$("#topMessage").html(htmlMessage);
							}
						}
						break;

					case "ROUNDCHANGE":
						if (!tagging) {
							var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('solution') + "<font color='red'>" + guessWord + "</font>" + "<b>" + "</font>";
							$("#topMessage").html(htmlMessage);
						}
						break;

					case "ENDED":
						var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('leaderboard') + "<b>" + "</font>";
						$("#topMessage").html(htmlMessage);
						htmlMessage = "<font size='5'>" + "<b><b>" + "</font>";
						$("#timeCounter").html(htmlMessage);
						$('#mainPage').removeClass('guesser');
						$('#mainPage').removeClass('sketcher');
						skipButton.hide();
						break;
				}
			}

			var htmlMessage = "<font size='5'>" + "<b>" + score + "<b>" + "</font>";
			$("#score").html(htmlMessage);

			//Time positioning
			if (matchStarted) {
				if (!tagging) {
					var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('time') + Time.round(time.getCountdown('round'), Time.second) + "<b>" + "</font>";
					$("#timeCounter").html(htmlMessage);
				} else {
					var htmlMessage = "<font size='5'>" + "<b>" + $.i18n.prop('time') + Time.round(time.getCountdown('tag'), Time.second) + "<b>" + "</font>";
					$("#timeCounter").html(htmlMessage);
				}
			}
			if (!matchStarted || tagging || role === "GUESSER" || role === "ENDED" || role === "ROUNDCHANGE" || role === undefined) {
				//Draw nothing till now
			} else {
				//Drawing tools
				if (drawTool) {
					ctx.drawImage(penEnabled, 0, 130, 70, 70);
					ctx.drawImage(eraserDisabled, 0, 200, 70, 70);
				} else {
					ctx.drawImage(penDisabled, 0, 130, 70, 70);
					ctx.drawImage(eraserEnabled, 0, 200, 70, 70);
				}
			}
		};

		setup();
		window.requestAnimationFrame(function loop() {
			window.requestAnimationFrame(loop);
			render();
		});

	})();

	connect();
});
