require(["Class", "Chat", "StateMachine", "Communicator", "Time", "Writer", "canvas/Painter", "jquery", "nouislider", "spectrum", "i18n", "howler"],
function( Class,   Chat,   StateMachine,   Communicator,   Time,   Writer,   Painter,          $) {

	$(function() {

		var clock = new Time();

		//var background_music = new Howl({
		//	urls: ['assets/sounds/music/background.ogg']
		//});
	
		var sketchness = {
			players: [],
			myself: $('#currentNickname').text(),
			sketcher: null,
			task: null,
			word: null,
			points: [],
			pointsList: [],
			ratio: 0,
			currentArea: 0,
			currentPosition: null,
			image: null,
			stopDrawing: false,
			stopGuessing: false,
			singlePlayerName: "",
			lastSent: 0,
			traceNum: 1,
			isMobile: /ipad|iphone|android/i.test(navigator.userAgent)
		};
		
		var currentImage = {
			id: null,
			url: null,
			width: null,
			height: null
		};
		
		var pose = {
				head_x0: 0,
				head_y0: 0,
				head_x1: 0,
				head_y1: 0,
				torso_x0: 0,
				torso_y0: 0,
				torso_x1: 0,
				torso_y1: 0,
				left_arm_x0: 0,
				left_arm_y0: 0,
				left_arm_x1: 0,
				left_arm_y1: 0,
				right_arm_x0: 0,
				right_arm_y0: 0,
				right_arm_x1: 0,
				right_arm_y1: 0,
				legs_x0: 0,
				legs_y0: 0,
				legs_x1: 0,
				legs_y1: 0,
				feet_x0: 0,
				feet_y0: 0,
				feet_x1: 0,
				feet_y1: 0,
				bodyArea: 0
					
		}
		
		var possibleGuesses = {
				last: ["bracelet","watch","ring","gloves"],
				shirt: ["coat","cardigan","cape","hoodie","jacket","jumper","blazer","blouse","bodysuit","suit", "sweater", "sweatshirt", "t-shirt", "top", "vest"],
				socks: ["leggings", "stockings", "tights"],
				shoes: ["boots", "flats", "clogs", "heels", "loafers","pumps","sandals","sneakers","wedges"],
				bag: ["handbag", "purse"],
				head: ["glasses","necklace","scarf","hat","earrings"],
				torso: ["shirt","bag","dress","tie","bodysuit","wallet","intimate"],
				legs: ["shorts","skirt","belt","pants","jeans"],
				feet: ["socks", "shoes"]
		}
		
		var guesses = [];

		var constants = {
			tagTime: 30,
			//taskTime: 120,
			taskTime: 15,
			solutionTime: 3,
			minSendRate: 50
		};

		var elements = {
			header: $("#pageheader"),
			main: $("#mainPage"),
			score: $("#score"),
			time: $("#timeCounter"),
			top: $("#topMessage"),
			error: $("#onError"),
			canvasMessage: $("#canvasMessage"),
			websocket: $('#paintWebSocket'),
			chatContainer: $("#messages"),
			chatInput: $("#talk"),
			opponents: $("#opponents"),
			wordInput: $("#wordInput"),
			skip: $("#skipTask"),
			endSegmentation: $("#endSegmentation"),
			questionMark: $('#questionMark'),
			hudArea: $("#hudArea"),
			pen: $("#pen"),
			eraser: $("#eraser"),
			tool: "pen",
			size: $("#size"),
			color: $("#picker"),
			viewport: $("#canvaswindows"),
			task: $("#task"),
			draws: $("#draws"),
			positions: $("#positions")
		};
		
		var write = new Writer(elements, sketchness.myself);
		
		var areaFunction;
		
		var communicator = new Communicator(elements.websocket.data('ws'));
		$(communicator.websocket).on({
			close: function(evt) {
				write.error("Connection lost");
			},
			error: function(evt) {
				console.error("error", evt);
			}
		});
		
		//Use noUISlider to prepare the size slider
		$("#size").noUiSlider({
			start: [ 5 ],
			step: 1,
			orientation: 'vertical',
			range: {
				'min': [  3 ],
				'max': [ 20 ]
			}
		});
		
		$("#size").change(function(){
			toolChange();
		});
		
		//Use spectrum for the color picker
		$("#picker").spectrum({
			showPaletteOnly: true,
			showPalette:true,
			color: 'red',
			palette: [
				['red', 'yellow', 'green', 'blue']
			],
			change: function(color) {
				toolChange();
			},
			className: 'picker'
		});
		
		function toolChange() {
			var tool = {
							tool: elements.tool,
							size: elements.size.val(),
							color: elements.color.spectrum("get").toRgbString()
						};
			painter.setTool(tool);
			communicator.send("changeTool", tool);
		};

		var chat = new Chat(elements.chatContainer, elements.chatInput);
		$(chat).on("send", function(e, message) {
			communicator.send("chat", { user: sketchness.myself, message: message });
		});

		communicator.on({
			chat: function(e, content) {
				chat.message(sketchness.players[content.user].name, content.message, content.user === sketchness.myself);
			},
			log: function(e, content) {
				chat.log(content.level, content.message);
			}
		});

		var painter = new Painter(elements.task[0], elements.draws[0], elements.positions[0])
		toolChange();
		Object.size = function(obj) {
			var size = 0, key;
			for (key in obj) {
				if (obj.hasOwnProperty(key)) size++;
			}
			return size;
		};
		
		
		
		
		/**
		 * Finite state machine that handle
		 * each game phase.
		 */
		var GameFSM = new Class({
			_name: "GameFSM",

			/**
			 * Initialize the component with the given options
			 *
			 * @param options :Object The configuration object for the FSM
			 *     @property sketchness :Object The game variables
			 *     @property constants :Object The game constants
			 *     @property communicator :Communicator The communicator to listen
			 *     @property write :Writer The DOM writer utility
			 *     @property chat :Chat The chat handler
			 *     @property painter :Painter The canvas handler
			 *     @property elements :Chat The DOM elements to manipulate
			 */
			_init: function(options) {
				$.extend(this, options);
				this.startup();
			},

			_proto: {
				/**
				 * Setup of wait players state
				 */
				onenterplayersWait: function() {
					var that = this,
						write = this.write,
						sk = this.sketchness;

					
					write.score('0');
					console.log("[BEGIN] PlayersWait");
					elements.pen.hide();
					elements.eraser.hide();
					
					this.communicator.on({
						join: function(e, content) {
						    console.log("[RECEIVED MESSAGE] join");
						    sk.players = [];
						    for(var i in content)
							{
								sk.players[content[i].user] = {
									id: content[i].user,
									name: content[i].name,
									img: content[i].img,
									score: 0
								};
							}
							write.players(sk.players);
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
							delete sk.players[content.user];
							write.players(sk.players);
						},
						loading: function() {
						    console.log("[RECEIVED MESSAGE] loading");
						   	write.top($.i18n.prop('matchstarting'));
							that.load();
						},
						waiting: function() {
							console.log("[RECEIVED MESSAGE] waiting");
							write.top($.i18n.prop('waiting'));
						},
						roundBegin: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundBegin");
							that.beginRound(content.sketcher);
						},
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of wait players state
				 */
				onleaveplayersWait: function() {
					write.top($.i18n.prop('matchstarting'));
					this.communicator.off("join leave loading roundBegin leaderboard");
					console.log("[LEAVE] PlayerWait");
				},

				/**
				 * Setup of loading state
				 */
				onenterloading: function() {
					this.write.top($.i18n.prop('matchstarting'));
					elements.endSegmentation.hide();
					elements.skip.hide();
					console.log("[ENTER] Loading");
					var that = this,
						sk = this.sketchness;

					this.communicator.on({
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
							},
						join: function(e, content) {
						    console.log("[RECEIVED MESSAGE] join");
						    sk.players = [];
						    for(var i in content)
							{
								sk.players[content[i].user] = {
									id: content[i].user,
									name: content[i].name,
									img: content[i].img,
									score: 0
								};
							}
							write.players(sk.players);
						},
						roundBegin: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundBegin");
							that.beginRound(content.sketcher);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}

					});
					
				},

				/**
				 * Tear down of loading state
				 */
				onleaveloading: function() {
					this.write.top();
					console.log("[LEAVE] Loading");
					this.communicator.off("leaderboard roundBegin join");
				},

				/**
				 * Utility method to trigger the event for
				 * a new round automatically choosing between
				 * sketcher and guesser state.
				 *
				 * @param sketcher :String The sketcher ID
				 */
				beginRound: function(sketcher) {
					//alert((Object.size(this.sketchness.players)));
					var sk = this.sketchness;
					sk.sketcher = sketcher;
					write.top($.i18n.prop('matchstarting'));
					if((sk.sketcher === sk.myself)&&((Object.size(this.sketchness.players))==1)) {
						//alert("sketcher bot");
						this.beSketcherBot();
					}
					else if(sk.sketcher === sk.myself) {
						//alert("sketcher");
						this.beSketcher();
					} 
					else if((sk.sketcher != sk.myself)&&((Object.size(this.sketchness.players))==1)) {
						//alert("guesser bot");
						this.beGuesserBot();
					} 
					else {
						//alert("guesser");
						this.beGuesser();
					}
					/*
					if(sk.sketcher === sk.myself) {
						this.beSketcher();
					} else {
						this.beGuesser();
					}
					*/
				},

				/**
				 * Setup of sketcher state
				 */
				onenterSketcher: function() {

					var that = this;
					console.log("[BEGIN] Sketcher");
					this.communicator.on({
						tag: function() {
						    console.log("[RECEIVED MESSAGE] tag");
							that.tag();
						},
						task: function(e, content) {
						    console.log("[RECEIVED MESSAGE] task");
							that.task(content.word);
						},
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of sketcher state
				 */
				onleaveSketcher: function() {

					this.communicator.off("tag task leaderboard");
					this.painter.hideImage();
					console.log("[LEAVE] Sketcher");
				},

				/**
				 * Setup of guesser state
				 */
				onenterGuesser: function() {

					var that = this;
					that.painter.hideImage();
					console.log("[BEGIN] Guesser");
					this.communicator.on({
						tag: function() {
						    console.log("[RECEIVED MESSAGE] tag");
							that.tag();
						},
						task: function() {
						    console.log("[RECEIVED MESSAGE] task");
							that.task();
						},
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of guesser state
				 */
				onleaveGuesser: function() {

					this.communicator.off("tag task leaderboard");
					console.log("[LEAVE] Guesser");
				},
				
				/**
				 * Setup of sketcher BOT state
				 */
				onenterSketcherBot: function() {

					var that = this;
					console.log("[BEGIN] SketcherBot");
					this.communicator.on({
						tag: function() {
						    console.log("[RECEIVED MESSAGE] tag");
							that.tag();
						},
						task: function(e, content) {
						    console.log("[RECEIVED MESSAGE] task");
						    currentImage.id = content.id;
						    currentImage.url = content.url;
						    currentImage.height = content.height;
						    currentImage.width = content.width;
						    sketchness.word = content.word;
							that.taskBot(content.word);
						},
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of sketcher BOT state
				 */
				onleaveSketcherBot: function() {

					this.communicator.off("tag task leaderboard");
					this.painter.hideImage();
					console.log("[LEAVE] SketcherBot");
				},

				/**
				 * Setup of guesser BOT state
				 */
				onenterGuesserBot: function() {

					var that = this;
					that.painter.hideImage();
					console.log("[BEGIN] GuesserBot");
					this.communicator.on({
						tag: function() {
						    console.log("[RECEIVED MESSAGE] tag");
							that.tag();
						},
						task: function(e, content) {
						    console.log("[RECEIVED MESSAGE] task");
						    //sketchness.image = content.id;
						    currentImage.id = content.id;
						    currentImage.url = content.url;
						    currentImage.height = content.height;
						    currentImage.width = content.width;
						    sketchness.word = content.word;
							that.taskBot();
						},
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of guesser BOT state
				 */
				onleaveGuesserBot: function() {

					this.communicator.off("tag task leaderboard");
					console.log("[LEAVE] GuesserBot");
				},

				/**
				 * Utility method to send the timeout
				 * message to the server
				 */
				timeUp: function() {

					if(sketchness.sketcher == "bot"){
						sketchness.stopDrawing = true;
					}
					if(Object.size(this.sketchness.players)==1) {
						//this.skipRound();
						//console.log("[SENDING MESSAGE] skip");
						//this.communicator.send("skip", {});
						clearInterval(areaFunction);
						sketchness.pointsList = [];
						this.nextRound();
					}
					else{
					    console.log("[SENDING MESSAGE] timer");
						this.communicator.send("timer", {user: sketchness.myself});
					}
				},
				
				/**
				 * Utility method to send the timeout
				 * message to the server
				 */
				nextRoundCall: function() {
					this.nextRound();
					console.log("[SENDING MESSAGE] timer");
					this.communicator.send("timer", {user: sketchness.myself});
				},
				

				/**
				 * Setup of tag insertion state
				 */
				onentertagInsertion: function() {

					var elements = this.elements;
					console.log("[BEGIN] TagInsertion");
					elements.pen.hide();
					elements.eraser.hide();
					this.write.top($.i18n.prop("asktagsketcher"));
					//this.write.warnTag($.i18n.prop("warnTag"));
					elements.skip.show();
					elements.wordInput.show();
					this.chat.disable();
					
					this.clock.setCountdown("tag", this.constants.tagTime * Time.second, Time.second, this.write.time.bind(this.write), this.timeUp.bind(this));

					var that = this;

					this.communicator.on({
						image: function(e, content) {
						    console.log("[RECEIVED MESSAGE] image");
							that.painter.showImage(content.url, content.width, content.height);
						},
						beginRound: function(e, content) {
						    console.log("[RECEIVED MESSAGE] beginRound");
							that.beginRound(content.sketcher);
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                            console.log("[RECEIVED MESSAGE] leaderboard");
                            that.quit(content);
                        },
						task: function(e, content) {
						    console.log("[RECEIVED MESSAGE] task");
							that.task(content.word);
						},
						skipTask: function(e, content) {
						    console.log("[RECEIVED MESSAGE] skipTask");
							that.skipRound();
						},
						noTag: function(e, content) {
						    console.log("[RECEIVED MESSAGE] noTag");
							that.nextRound();
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});

					elements.skip.one("click", function() {
					    console.log("[SENDING MESSAGE] skip");
						that.communicator.send("skip", {});
					});

					elements.wordInput.on("keypress", function(event) {
						if (event.which === 13 && $(this).val() !== "") {
							event.preventDefault();
                            console.log("[SENDING MESSAGE] tag");
							that.communicator.send("tag", {
								word: $(this).val()
							});

							$(this).val("");

							$(this).off("keypress");
						}
					});
				},

				/**
				 * Tear down of tag insertion state
				 */
				onleavetagInsertion: function() {

					var elements = this.elements;
					console.log("[END] TagInsertion");
					this.write.top();
					//this.write.warnTag();
					this.write.time();
					elements.skip.hide();
					elements.wordInput.hide();
					this.chat.enable();

					this.clock.clearCountdown("tag");

					this.painter.hideImage();

					this.communicator.off("image beginRound leave leaderboard task skipTask noTag");
					elements.skip.off("click");
					elements.wordInput.off("keypress");
				},

				/**
				 * Setup of tag wait state
				 */
				onentertagWait: function() {

					this.write.top($.i18n.prop('asktag'));
					this.write.canvasMessage($.i18n.prop('sketchertagging'));
					console.log("[BEGIN] TagWait");
					var question = this.elements.questionMark;
					this.painter.showImage(question.attr("src"), question.attr("rwidth"), question.attr("rheight"));

					this.clock.setCountdown("tag", this.constants.tagTime * Time.second, Time.second, this.write.time.bind(this.write), this.timeUp.bind(this));

					var that = this;

					this.communicator.on({
						beginRound: function(e, content) {
						    console.log("[RECEIVED MESSAGE] beginRound");
							that.beginRound(content.sketcher);
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                            console.log("[RECEIVED MESSAGE] leaderboard");
                            that.quit(content);
                        },
						task: function() {
						    console.log("[RECEIVED MESSAGE] task");
							that.task();
						},
						noTag: function(e, content) {
						    console.log("[RECEIVED MESSAGE] noTag");
							that.nextRound();
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of tag wait state
				 */
				onleavetagWait: function() {

					this.write.top();
					this.write.canvasMessage();
					this.write.time();
					console.log("[END] TagWait");
					this.clock.clearCountdown("tag");

					this.painter.hideImage();

					this.communicator.off("beginRound leave leaderboard task noTag");
				},

				/**
				 * Transition method between tag and task
				 * which saves the task word if given.
				 *
				 * @param word :String The word to draw
				 */
				onbeforetask: function(evt, from, to, word) {
					this.sketchness.word = word || null;
				},

				/**
				 * Setup of task drawing state
				 */
				onentertaskDrawing: function() {

					var elements = this.elements;
					elements.main.addClass("sketcher");
					this.write.top($.i18n.prop("draw"), this.sketchness.word);
					toolChange();
					elements.skip.show();
					elements.pen.show();
					elements.eraser.show();
					elements.hudArea.show();
					elements.endSegmentation.hide();
					this.chat.disable();
					console.log("[BEGIN] TaskDrawing");
					this.clock.setCountdown("task", this.constants.taskTime * Time.second, Time.second, this.write.time.bind(this.write), this.timeUp.bind(this));

					var that = this,
						painter = this.painter,
						sk = this.sketchness;

					painter.setName(sk.players[sk.sketcher].name);

					this.communicator.on({
						image: function(e, content) {
						    console.log("[RECEIVED MESSAGE] image");
							painter.showImage(content.url, content.width, content.height);
						},
						timer: function(e, content) {
						    console.log("[RECEIVED MESSAGE] timer");
							that.clock.changeCountdown("task", content.time * Time.second);
						},
						guess: function(e, content) {
						    console.log("[RECEIVED MESSAGE] guess");
							that.chat.guess(sk.players[content.user].name, content.word, content.affinity, content.user == sk.myself);
						},
						score: function(e, content) {
						    console.log("[RECEIVED MESSAGE] score");
							sk.players[content.user].score += content.score;

							if(content.user == sk.myself) {
								that.write.score(sk.players[content.user].score);
							}
						},
						leave: function(e, content) {
							alert("leave");
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                            console.log("[RECEIVED MESSAGE] leaderboard");
                            that.quit(content);
                        },
						roundEnd: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundEnd");
						    sk.word = content.word;
							that.endRound(content.word);
						},
						skipTask: function(e, content) {
						    console.log("[RECEIVED MESSAGE] skipTask");
							that.skipRound();
						},
						endSegmentationC: function(e, content) {
						    console.log("[RECEIVED MESSAGE] endSegmentationC");
							that.nextRound();
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});

					elements.skip.on("click", function() {
					    console.log("[SENDING MESSAGE] skip");
						that.communicator.send("skip", {});
					});
					
					elements.pen.on("click", function() {
						elements.tool = "pen";
						elements.eraser.css('background-image', 'url(assets/images/UI/controls/eraserD.png)'); 
						elements.pen.css('background-image', 'url(assets/images/UI/controls/pencil.png)'); 
						toolChange();
					});
					
					elements.eraser.on("click", function() {
						elements.tool = "eraser";
						elements.eraser.css('background-image', 'url(assets/images/UI/controls/eraser.png)'); 
						elements.pen.css('background-image', 'url(assets/images/UI/controls/pencilD.png)'); 
						toolChange();
					});

					if(Object.size(this.sketchness.players)==1) {
						elements.endSegmentation.show();
						elements.endSegmentation.on("click", function() {
						    console.log("[SENDING MESSAGE] endSegmentation");
							that.communicator.send("endSegmentation", {user: sk.myself});
						});
					}

					var started = false;

					var relativePosition = function(event, element) {
						element = element[0];
						var offsetX = 0, offsetY = 0;

						// Compute the total offset
						if (element.offsetParent !== undefined) {
							do {
								offsetX += element.offsetLeft;
								offsetY += element.offsetTop;
							} while ((element = element.offsetParent));
						}
						if(undefined!=event) {
							if(!sk.isMobile) {
									if(event.originalEvent.type !== "mouseup")
									  return {
										x: (event.pageX - offsetX),
										y: (event.pageY - offsetY),
										size: elements.size.val(),
										color: elements.color.spectrum("get").toRgbString()
									  };
									else
									  return {
										x: (event.pageX - offsetX),
										y: (event.pageY - offsetY),
										size: elements.size.val(),
										color: "end"
									  };  
							} else {
								// Touchend does not have the position of when we lifted our finger
								if(event.originalEvent.type !== "touchend" && event.originalEvent.touches) {
									return {
										x: (event.originalEvent.touches[0].pageX - offsetX),
										y: (event.originalEvent.touches[0].pageY - offsetY),
										size: elements.size.val(),
										color: elements.color.spectrum("get").toRgbString()
									};
								} else if(event.originalEvent.type == "touchend"){
									return {
										x: (event.pageX - offsetX),
										y: (event.pageY - offsetY),
										size: elements.size.val(),
										color: "end"
									};
								}
								else
									return null;
							}
						}
						else {
							return {
										x: 0,
										y: 0,
										size: elements.size.val(),
										color: elements.color.spectrum("get").toRgbString()
								    
								    };
						}
					};

					var setPoint = function(event) {
						var point = relativePosition(event, elements.viewport);

						if (point != null && point.x > 0 && point.x < elements.viewport.width() && point.y > 0 && point.y < elements.viewport.height()) {
							painter.setPoint(point);
							++(sketchness.traceNum);
							addPoint(point.x, point.y, point.size, point.color);
							that.communicator.send("point", point);
						}
					};
					
					var addPoint = function(x, y, size, color) {
						sketchness.points.push({
							x: x,
							y: y,
							size: size,
							color: color
						});
					
					};
					
					var canSendNow = function() {
							return Date.now() - sketchness.lastSent > constants.minSendRate;
					};
					
					var sendPoints = function() {
						sketchness.lastSent = Date.now(); //Refresh the countdown timer
						var toBeSent = {
							points: sketchness.points,
							num: sketchness.traceNum,
							name: sketchness.sketcher,
							time: clock.getTimer("round")
						};
						communicator.send("trace",toBeSent);
						sketchness.points = [];
					};

					elements.viewport.on((sk.isMobile ? "touchstart" : "mousedown"), function(e) {
						if(!started) {
							painter.beginPath();
							started=true;
							setPoint(e);
							that.communicator.send("beginPath", {});
						}
					});

					elements.viewport.on((sk.isMobile ? "touchmove" : "mousemove"), function(e) {
						if(started) {
							e.preventDefault();
							setPoint(e);
							if (canSendNow()) 
								sendPoints();
						}
					});

					$(document).on((sk.isMobile ? "touchend" : "mouseup"), function(e) {
						if(started) {
							setPoint(e);
							var point = relativePosition(e, elements.viewport);
							addPoint(point.x, point.y, point.size, "end");
							sendPoints();
							started=false;
							that.communicator.send("endPath", {});
							painter.endPath();
						}
					});

				},

				/**
				 * Tear down of task drawing state
				 */
				onleavetaskDrawing: function() {

					var elements = this.elements;
					elements.main.removeClass('sketcher');
					this.write.top();
					this.write.time();
					elements.skip.hide();
					elements.endSegmentation.hide();
					elements.hudArea.hide();
					this.chat.enable();
					console.log("[END] TaskDrawing");
					this.clock.clearCountdown("task");

					elements.skip.off("click");
					elements.endSegmentation.off("click");

					elements.viewport.off(this.isMobile ? "touchstart" : "mousedown");
					elements.viewport.off(this.isMobile ? "touchmove" : "mousemove");
					$(document).trigger(this.isMobile ? "touchend" : "mouseup");
					$(document).off(this.isMobile ? "touchend" : "mouseup");

					this.painter.hideImage();

					this.communicator.off("image timer guess score leave leaderboard roundEnd skipTask endSegmentationC");
				},

				/**
				 * Setup of task guessing state
				 */
				onentertaskGuessing: function() {

					var that = this,
						sk = this.sketchness,
						wordInput = this.elements.wordInput,
						painter = this.painter;
						
					elements.pen.hide();
					elements.eraser.hide();
					this.write.top($.i18n.prop('guess'));
					that.painter.hideImage();
					wordInput.show();
					console.log("[BEGIN] TaskGuessing");
					this.clock.setCountdown("task", this.constants.taskTime * Time.second, Time.second, this.write.time.bind(this.write), this.timeUp.bind(this));

					painter.setName(sk.players[sk.sketcher].name);

					wordInput.on("keypress", function(event) {
						if (event.which === 13) {
							event.preventDefault();
                            console.log("[SENDING MESSAGE] guessAttempt");
							that.communicator.send("guessAttempt", {
							    user: sk.myself,
								word: $(this).val()
							});

							$(this).val("");
						}
					});

					this.communicator.on({
						timer: function(e, content) {
						    console.log("[RECEIVED MESSAGE] timer");
							that.clock.changeCountdown("task", content.time * Time.second);
						},
						changeTool: function(e, tool) {
						    console.log("[RECEIVED MESSAGE] changeTool");
							painter.setTool(tool);
						},
						beginPath: function() {
							painter.beginPath();
						},
						point: function(e, point) {
							painter.setPoint(point);
						},
						endPath: function() {
							painter.endPath();
						},
						guess: function(e, content) {
						    console.log("[RECEIVED MESSAGE] guess");
							that.chat.guess(sk.players[content.user].name, content.word, content.affinity, content.user == sk.myself);
						},
						guessed: function(e, content) {
							
						    console.log("[RECEIVED MESSAGE] guessed");
						    if(sk.myself == content.user){
                                sk.word = content.word;
                                that.write.top($.i18n.prop('guessed'), sk.word);
                                wordInput.hide().off("keypress");
                                this.one("image", function(e, content) {
                                    painter.showImage(content.url, content.width, content.height);
                                });
							}
						},
						score: function(e, content) {
						    console.log("[RECEIVED MESSAGE] score");
							sk.players[content.user].score += content.score;

							if(content.user == sk.myself) {
								that.write.score(sk.players[content.user].score);
							}
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                            console.log("[RECEIVED MESSAGE] leaderboard");
                            that.quit(content);
                        },
						roundEnd: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundEnd");
							that.endRound(content.word);
						},
						skipTask: function(e, content) {
						    console.log("[RECEIVED MESSAGE] skipTask");
							that.skipRound();
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of task guessing state
				 */
				onleavetaskGuessing: function() {

					this.write.top();
					this.write.time();
					console.log("[END] TaskGuessing");
					this.elements.wordInput.hide().off("keypress");

					this.clock.clearCountdown("task");

					this.communicator.off("timer changeTool beginPath point endPath guess guessed score leave leaderboard roundEnd skipTask");
				},
				
				/**
				 * Setup of task drawing BOT state
				 */
				onentertaskDrawingBot: function() {

					var that = this,
					painter = this.painter,
					sk = this.sketchness;
					var area_polygon = 0;
					guesses = [];
					areaFunction = setInterval(function(){checkArea()}, 5000);
					
					function checkArea() {

						if(sk.pointsList.length>1){
							//checkAreaBentley(sk);
							//getCurrentPosition(sk.pointsList[sk.pointsList.length-1],pose, sk, currentImage);
							guessWordBot(sk.pointsList[sk.pointsList.length-1], pose, sk, currentImage, that, possibleGuesses, guesses);
						}
						
					}                    

					var elements = this.elements;
					elements.main.addClass("sketcher");
					this.write.top($.i18n.prop("draw"), this.sketchness.word);
					toolChange();
					elements.skip.show();
					elements.pen.show();
					elements.eraser.show();
					elements.hudArea.show();
					elements.endSegmentation.hide();
					this.chat.disable();
					console.log("[BEGIN] TaskDrawingBot");
					this.clock.setCountdown("task", this.constants.taskTime * Time.second, Time.second, this.write.time.bind(this.write), this.timeUp.bind(this));

					//painter.setName(sk.players[sk.sketcher].name);

					this.communicator.on({
						image: function(e, content) {
						    console.log("[RECEIVED MESSAGE] image");
						    var canvas = document.getElementById("draws");
		                	var ctx = canvas.getContext("2d");
		                	ctx.clearRect(0,0,canvas.width,canvas.height);
		                	//compute scaling factor to compute area
		                	
		                	var image_original_width = content.width;
		        			var image_original_height = content.height;
		        			currentImage.width = content.width;
		        			currentImage.height = content.height;

		        			if(image_original_width >= image_original_height){

		        				var image_scaled_width = canvas.width;
		        				var image_scaled_height = canvas.width * image_original_height / image_original_width;
		        			}
		        			else{
		        				var image_scaled_width = canvas.height * image_original_width / image_original_height;
		        				var image_scaled_height = canvas.height;

		        			}

		        			sk.ratio = image_scaled_width/image_original_width;
		        			getPose(content.id, pose);
		                	sk.stopDrawing = false;
							painter.showImage(content.url, content.width, content.height);
					
						},
						timer: function(e, content) {
							sk.stopGuessing = true;
						    console.log("[RECEIVED MESSAGE] timer");
							that.clock.changeCountdown("task", content.time * Time.second);
						},
						guess: function(e, content) {

						    console.log("[RECEIVED MESSAGE] guess");
							//that.chat.guess(sk.players[content.user].name, content.word, content.affinity, content.user == sk.myself);
						    that.chat.guess("bot", content.word, content.affinity, false);
						},
						score: function(e, content) {
							sk.stopGuessing = true;
						    console.log("[RECEIVED MESSAGE] score");
							sk.players[content.user].score += content.score;

							if(content.user == sk.myself) {
								that.write.score(sk.players[content.user].score);
							}
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                        	sk.singlePlayerName = content.singlePlayerName;
                            console.log("[RECEIVED MESSAGE] leaderboard");
                            that.quit(content);
                        },
						roundEnd: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundEnd");
						    sk.word = content.word;
							that.endRound(content.word);
						},
						skipTask: function(e, content) {
						    console.log("[RECEIVED MESSAGE] skipTask");
							that.skipRound();
						},
						endSegmentationC: function(e, content) {
						    console.log("[RECEIVED MESSAGE] endSegmentationC");
							that.nextRound();
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});

					elements.skip.on("click", function() {
					    console.log("[SENDING MESSAGE] skip");
						that.communicator.send("skip", {});
					});
					
					elements.pen.on("click", function() {
						elements.tool = "pen";
						elements.eraser.css('background-image', 'url(assets/images/UI/controls/eraserD.png)'); 
						elements.pen.css('background-image', 'url(assets/images/UI/controls/pencil.png)'); 
						toolChange();
					});
					
					elements.eraser.on("click", function() {
						elements.tool = "eraser";
						elements.eraser.css('background-image', 'url(assets/images/UI/controls/eraser.png)'); 
						elements.pen.css('background-image', 'url(assets/images/UI/controls/pencilD.png)'); 
						toolChange();
					});

					/*
					if(Object.size(this.sketchness.players)==1) {
						elements.endSegmentation.show();
						elements.endSegmentation.on("click", function() {
						    console.log("[SENDING MESSAGE] endSegmentation");
							that.communicator.send("endSegmentation", {user: sk.myself});
						});
					}
					*/

					var started = false;

					var relativePosition = function(event, element) {
						element = element[0];
						var offsetX = 0, offsetY = 0;

						// Compute the total offset
						if (element.offsetParent !== undefined) {
							do {
								offsetX += element.offsetLeft;
								offsetY += element.offsetTop;
							} while ((element = element.offsetParent));
						}
						if(undefined!=event) {
							if(!sk.isMobile) {
									if(event.originalEvent.type !== "mouseup")
									  return {
										x: (event.pageX - offsetX),
										y: (event.pageY - offsetY),
										size: elements.size.val(),
										color: elements.color.spectrum("get").toRgbString()
									  };
									else
									  return {
										x: (event.pageX - offsetX),
										y: (event.pageY - offsetY),
										size: elements.size.val(),
										color: "end"
									  };  
							} else {
								// Touchend does not have the position of when we lifted our finger
								if(event.originalEvent.type !== "touchend" && event.originalEvent.touches) {
									return {
										x: (event.originalEvent.touches[0].pageX - offsetX),
										y: (event.originalEvent.touches[0].pageY - offsetY),
										size: elements.size.val(),
										color: elements.color.spectrum("get").toRgbString()
									};
								} else if(event.originalEvent.type == "touchend"){
									return {
										x: (event.pageX - offsetX),
										y: (event.pageY - offsetY),
										size: elements.size.val(),
										color: "end"
									};
								}
								else
									return null;
							}
						}
						else {
							return {
										x: 0,
										y: 0,
										size: elements.size.val(),
										color: elements.color.spectrum("get").toRgbString()
								    
								    };
						}
					};

					var setPoint = function(event) {
						var point = relativePosition(event, elements.viewport);

						if (point != null && point.x > 0 && point.x < elements.viewport.width() && point.y > 0 && point.y < elements.viewport.height()) {
							painter.setPoint(point);
							++(sketchness.traceNum);
							addPoint(point.x, point.y, point.size, point.color);
							that.communicator.send("point", point);
						}
					};
					
					var addPoint = function(x, y, size, color) {
						sketchness.points.push({
							x: x,
							y: y,
							size: size,
							color: color
						});
						if(sketchness.pointsList.length==0){
							sketchness.pointsList.push({
								x: x,
								y: y,
								size: size,
								color: color
							});
						}
						else{
							if((sketchness.pointsList[sketchness.pointsList.length - 1].x != x)&&(sketchness.pointsList[sketchness.pointsList.length - 1].y != y)){
								sketchness.pointsList.push({
									x: x,
									y: y,
									size: size,
									color: color
								});
							}
						}

					};
					
					var canSendNow = function() {
							return Date.now() - sketchness.lastSent > constants.minSendRate;
					};
					
					var sendPoints = function() {
						sketchness.lastSent = Date.now(); //Refresh the countdown timer
						var toBeSent = {
							points: sketchness.points,
							num: sketchness.traceNum,
							name: sketchness.sketcher,
							time: clock.getTimer("round")
						};
						communicator.send("trace",toBeSent);
						sketchness.points = [];
					};

					elements.viewport.on((sk.isMobile ? "touchstart" : "mousedown"), function(e) {
						if(!started) {
							painter.beginPath();
							started=true;
							setPoint(e);
							that.communicator.send("beginPath", {});
						}
					});

					elements.viewport.on((sk.isMobile ? "touchmove" : "mousemove"), function(e) {
						if(started) {
							e.preventDefault();
							setPoint(e);
							if (canSendNow()) 
								sendPoints();
						}
					});

					$(document).on((sk.isMobile ? "touchend" : "mouseup"), function(e) {
						if(started) {
							setPoint(e);
							var point = relativePosition(e, elements.viewport);
							addPoint(point.x, point.y, point.size, "end");
							sendPoints();
							started=false;
							that.communicator.send("endPath", {});
							painter.endPath();
						}
					});

				},

				/**
				 * Tear down of task drawing BOT state
				 */
				onleavetaskDrawingBot: function() {

					var elements = this.elements;
					elements.main.removeClass('sketcher');
					this.write.top();
					this.write.time();
					elements.skip.hide();
					elements.endSegmentation.hide();
					elements.hudArea.hide();
					this.chat.enable();
					console.log("[END] TaskDrawingBot");
					this.clock.clearCountdown("task");

					elements.skip.off("click");
					elements.endSegmentation.off("click");

					elements.viewport.off(this.isMobile ? "touchstart" : "mousedown");
					elements.viewport.off(this.isMobile ? "touchmove" : "mousemove");
					$(document).trigger(this.isMobile ? "touchend" : "mouseup");
					$(document).off(this.isMobile ? "touchend" : "mouseup");

					this.painter.hideImage();

					this.communicator.off("image timer guess score leave leaderboard roundEnd skipTask endSegmentationC");
				},

				/**
				 * Setup of task guessing BOT state
				 */
				onentertaskGuessingBot: function() {

					var that = this,
						sk = this.sketchness,
						wordInput = this.elements.wordInput,
						painter = this.painter;
						
					elements.pen.hide();
					elements.eraser.hide();
					this.write.top($.i18n.prop('guess'));
					that.painter.hideImage();
					wordInput.show();
					console.log("[BEGIN] TaskGuessingBot");
					this.clock.setCountdown("task", this.constants.taskTime * Time.second, Time.second, this.write.time.bind(this.write), this.timeUp.bind(this));

					//painter.setName(sk.players[sk.sketcher].name);
					//drawSegmentation(sk.image, that, sk);
					drawSegmentation(currentImage.id, that, sk);

					wordInput.on("keypress", function(event) {
						if (event.which === 13) {
							event.preventDefault();
                            console.log("[SENDING MESSAGE] guessAttempt");
							that.communicator.send("guessAttempt", {
							    user: sk.myself,
								word: $(this).val()
							});

							$(this).val("");
						}
					});

					this.communicator.on({
						timer: function(e, content) {
						    console.log("[RECEIVED MESSAGE] timer");
							that.clock.changeCountdown("task", content.time * Time.second);
						},
						changeTool: function(e, tool) {
						    console.log("[RECEIVED MESSAGE] changeTool");
							painter.setTool(tool);
						},
						beginPath: function() {
							painter.beginPath();
						},
						point: function(e, point) {
							painter.setPoint(point);
						},
						endPath: function() {
							painter.endPath();
						},
						guess: function(e, content) {

						    console.log("[RECEIVED MESSAGE] guess");
							that.chat.guess(sk.players[content.user].name, content.word, content.affinity, content.user == sk.myself);
						},
						guessed: function(e, content) {

						    console.log("[RECEIVED MESSAGE] guessed");
						    //sk.stopDrawing = true;
						    //if(sk.myself == content.user){
                                sk.word = content.word;
                                that.write.top($.i18n.prop('guessed'), sk.word);
                                wordInput.hide().off("keypress");
                                this.one("image", function(e, content) {
                                    painter.showImage(content.url, content.width, content.height);
                                });
							//}
						},
						score: function(e, content) {
						    console.log("[RECEIVED MESSAGE] score");
							sk.players[content.user].score += content.score;

							if(content.user == sk.myself) {
								that.write.score(sk.players[content.user].score);
							}
						},
						leave: function(e, content) {
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                        	sk.singlePlayerName = content.singlePlayerName;
                            console.log("[RECEIVED MESSAGE] leaderboard");
                            that.quit(content);
                        },
						roundEnd: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundEnd");
							that.endRound(content.word);
						},
						skipTask: function(e, content) {
						    console.log("[RECEIVED MESSAGE] skipTask");
							that.skipRound();
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of task guessing BOT state
				 */
				onleavetaskGuessingBot: function() {
					this.write.top();
					this.write.time();
					console.log("[END] TaskGuessingBot");
					this.elements.wordInput.hide().off("keypress");

					this.clock.clearCountdown("task");

					this.communicator.off("timer changeTool beginPath point endPath guess guessed score leave leaderboard roundEnd skipTask");
				},

				/**
				 * Transition method between task and image viewing
				 * which saves the solution word.
				 *
				 * @param word :String The solution word
				 */
				onbeforeendRound: function(event, from, to, word) {
					this.sketchness.word = word;
				},

				/**
				 * Setup of image viewing state
				 */
				onenterimageViewing: function() {
					this.write.top($.i18n.prop('solution'), this.sketchness.word);
					this.clock.setCountdown("solution", this.constants.solutionTime * Time.second, Time.second, this.write.time.bind(this.write), this.nextRoundCall.bind(this));
					console.log("[BEGIN] ImageViewing");
					var that = this;
					var elements = that.elements;
					elements.skip.hide();
					elements.endSegmentation.hide();
					elements.hudArea.hide();
					elements.pen.hide();
					elements.eraser.hide();

					this.communicator.on({
						image: function(e, content) {
						    console.log("[RECEIVED MESSAGE] image");
							that.painter.showImage(content.url, content.width, content.height);
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
						leaderboard: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leaderboard");
							that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},

				/**
				 * Tear down of image viewing state
				 */
				onleaveimageViewing: function() {
					this.write.top();
					this.write.time();
					console.log("[END] ImageViewing");
					this.clock.clearCountdown("solution");

					this.painter.hideImage();

					this.communicator.off("image leave leaderboard");
				},
				
				/**
				 * Setup of image viewing BOT state
				 */
				onenterimageViewingBot: function() {
					this.write.top($.i18n.prop('solution'), this.sketchness.word);
					this.clock.setCountdown("solution", this.constants.solutionTime * Time.second, Time.second, this.write.time.bind(this.write), this.nextRoundCall.bind(this));
					console.log("[BEGIN] ImageViewingBot");
					var that = this;
					var elements = that.elements;
					elements.skip.hide();
					elements.endSegmentation.hide();
					elements.hudArea.hide();
					elements.pen.hide();
					elements.eraser.hide();
					
					that.painter.showImage(currentImage.url, currentImage.width, currentImage.height);
					
					this.communicator.on({
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
						leaderboard: function(e, content) {
							sk.singlePlayerName = content.singlePlayerName;
						    console.log("[RECEIVED MESSAGE] leaderboard");
							that.quit(content);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
					
				},

				/**
				 * Tear down of image viewing BOT state
				 */
				onleaveimageViewingBot: function() {
					this.write.top();
					this.write.time();
					console.log("[END] ImageViewingBot");
					this.clock.clearCountdown("solution");

					this.painter.hideImage();

					//this.communicator.off("image leave leaderboard");
				},

				/**
				 * Transition method between image viewing and leaderboard
				 * which stores the players scores.
				 *
				 * @param scores :String The scores
				 */
				onbeforequit: function(event, from, to, scores) {
					this.scores = scores;
				},

				/**
				 * Setup of leaderboard state
				 */
				onenterleaderboard: function() {
					var results="",
						that = this,
						sk = this.sketchness;
					console.log("[BEGIN] Leaderboard");
					for (var i = 0; i < this.scores.playerList.length; i++) {
						results += ":" + this.scores.playerList[i].name + ":" + this.scores.playerList[i].points;
					}
					
					if((Object.size(this.sketchness.players))==1){
						sk.players[sk.myself].name = sk.singlePlayerName;
					}
					else{
						jsRoutes.controllers.Sketchness.leaderboard(sk.players[sk.myself].name, results).ajax({
							success: function(data) {
								that.elements.main.html(data);
							},
							error: function() {
								that.write.error("Error!");
							}
						});
					}
					jsRoutes.controllers.Sketchness.leaderboard(sk.players[sk.myself].name, results).ajax({
						success: function(data) {
							that.elements.main.html(data);
						},
						error: function() {
							that.write.error("Error!");
						}
					});
				},

				onenterhandleError: function() {
					var results="",
						that = this,
						sk = this.sketchness;
					console.log("[BEGIN] HandleError");

					jsRoutes.controllers.Sketchness.handleError().ajax({
						success: function(data) {
							that.elements.main.html(data);
						},
						error: function() {
							that.write.error("Error!");
						}
					});
				},
				
				onenterwaitRole: function() {
					var that = this;
					console.log("[BEGIN] EnterWaitRole");
					that.painter.hidePath();
					elements.endSegmentation.hide();
					elements.skip.hide();
					this.communicator.on({
						leaderboard: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leaderboard");
							that.quit(content);
						},
						roundBegin: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundBegin");
							that.beginRound(content.sketcher);
						},
						error: function(e, content) {
						    console.log("[RECEIVED MESSAGE] error");
							that.errorEvent();
						}
					});
				},
				
				onleavewaitRole: function() {
					console.log("[END] LeaveWaitRole");
					clock.setTimer("round");
					this.communicator.off("leaderboard roundBegin");
				}
			}
		});

		StateMachine.create({
			target: GameFSM.prototype,

			events: [
				{ name: "startup", from: "none", to: "playersWait" },
				{ name: "load", from: ["none", "playersWait"], to: "loading" },
				{ name: "beSketcher", from: ["loading", "playersWait", "waitRole", "tagInsertion", "tagWait" ], to: "Sketcher" },
				{ name: "beGuesser", from: ["loading", "playersWait", "waitRole", "tagInsertion", "tagWait" ], to: "Guesser" },
				{ name: "beSketcherBot", from: ["loading", "playersWait", "waitRole", "tagInsertion", "tagWait" ], to: "SketcherBot" },
				{ name: "beGuesserBot", from: ["loading", "playersWait", "waitRole", "tagInsertion", "tagWait" ], to: "GuesserBot" },
				//{ name: "nextRound", from: ["imageViewing", "taskDrawing", "taskDrawingBot", "taskGuessingBot",  "tagInsertion", "tagWait"], to: "waitRole"},
				{ name: "nextRound", from: ["imageViewing", "imageViewingBot", "taskDrawing", "tagInsertion", "tagWait"], to: "waitRole"},
				{ name: "nextRound", from: ["taskDrawingBot", "taskGuessingBot"], to: "imageViewingBot"},
				{ name: "skipRound", from: ["taskGuessing", "taskGuessingBot", "taskDrawing", "taskDrawingBot", "tagInsertion"], to: "waitRole" },
				{ name: "tag", from: "Sketcher", to: "tagInsertion" },
				{ name: "tag", from: "Guesser", to: "tagWait" },
				{ name: "task", from: ["Sketcher", "tagInsertion"], to: "taskDrawing" },
				{ name: "task", from: ["Guesser", "tagWait"], to: "taskGuessing" },
				{ name: "taskBot", from: ["SketcherBot", "tagInsertion"], to: "taskDrawingBot" },
				{ name: "taskBot", from: ["GuesserBot", "tagWait"], to: "taskGuessingBot" },
				{ name: "endRound", from: ["taskGuessing", "taskGuessingBot", "taskDrawing", "taskDrawingBot"], to: "imageViewing" },
				{ name: "quit", from: ["imageViewing", "tagInsertion", "taskDrawing", "taskDrawingBot",  "taskGuessing", "taskGuessingBot", "tagWait", "waitRole", "loading", "Sketcher", "SketcherBot", "Guesser", "GuesserBot"], to: "leaderboard" },
				{ name: "errorEvent", from: ["imageViewing", "tagInsertion", "taskDrawing", "taskDrawingBot", "taskGuessing", "taskGuessingBot", "tagWait", "waitRole", "loading", "Sketcher", "SketcherBot", "Guesser", "GuesserBot", "playersWait"], to: "handleError" },
				{ name: "toLobby", from: "leaderboard", to: "lobby" }
			]
		});

		var game = new GameFSM({
			sketchness: sketchness,
			constants: constants,
			communicator: communicator,
			write: write,
			chat: chat,
			painter: painter,
			elements: elements,
			clock: clock
		});
	});
});


function drawSegmentation(idselected, that, sk) {

	$.jAjax({
        url: "segmentationImage",
        headers : {"selected" : idselected},
        onComplete: function(xhr,status){
            if(xhr.readyState === 4){
                if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
                    result = JSON.parse(xhr.responseText);
                    //get image size
                    var imageW = result[0].width;
                    var imageH = result[0].height;
   
                    //get polyline
                    var listPoints = result[0].polyline;
                    var obj = JSON.parse(listPoints);

                    //variables to compute delay in drawing
                    var time, timeNext, delta, increment, numPoints;
                    //point parameters
                    var x, y, color, size, ts;
                    
                    var canvas = document.getElementById("draws");
                	var ctx = canvas.getContext("2d");
                	
                	ctx.beginPath();
                	
                	//canvas.width = imageW;
                	//canvas.height = imageH;

                	var points = [];
                	
                	//retrieve timestamps of each set of points
                	var timestamps = [];
                	var j = 0;
                	for (var key in obj) {
                		timestamps[j] = obj[key].time;
                		j++;
                	}

                	var h = 0;
                    for (var key in obj) {
                    	
                    	//retrieve timestamp of the current set of points
                    	time = timestamps[h];
                    	//retrieve timestamp of the next set of points
                    	timeNext = timestamps[h+1];
                    	//compute time to draw the entire set of points
                    	delta = timeNext - time;
                    	
                    	//retrieve number of points
                    	numPoints = Object.keys(obj[key].points).length;
                    	
                    	//compute ts value for singular point
                    	increment = Math.round(delta/numPoints);
                    	
                    	//build points array
                    	for(var i = 0; i < numPoints; i++)
                    	{
                    		    if(obj[key].points[i].color!='end')
                    		    {
                    		    	x = obj[key].points[i].x;
                        			y = obj[key].points[i].y;
                    		    	color=obj[key].points[i].color;
                    		    	size = obj[key].points[i].size;
                    		    	ts = time+increment*i;
                    		    	points.push({ts : ts, x : x, y : y, size : size, color: color})

                    		    }

                    	}

						h++;
                    }
                    
                    
                    //Start drawing
                    ctx.lineJoin = "round";
	           	   	ctx.beginPath(); 
	           	   	var drawable=true;
	           	   	ctx.moveTo(points[0].x, points[0].y);

	           	   	//variable to change color when points will be erased
	           	   	var eraser = false;
	           	   	
	           	   	//variables to check consecutive points positions
	           	   	var previousX = -100;
	           	   	var previousY = -100;
	           	   	
	           	   	//recursive function to draw each point using timestamp
	           	   	var drawPoint = function(){
	           	   		//retrieve point coordinates, color, size, timestamp
                    	var p = points.shift();
                    	
                    	//check if consecutive points are in the same neighborhood, otherwise new path
                    	if(!(inRange(p.x, p.y, previousX, previousY, 15))){
                    		drawable = false;
                    	}
                    	
                    	//if eraser
                    	if(p.color=='rgba(255,255,255,1.0)')
                    	{
                    		//if first point of eraser
                    		if(eraser==false)
                    		{	//start new path
                    			drawable = false;
                    		}
                    		eraser=true;
                    		
                    	}
                    	else{
                    		//if point before was eraser
                    		if(eraser==true)
                    		{	//start new path
                    			eraser = false;
                    			drawable = false;
                    		}
                    	}
                    	
                    	if(p.color!='end')
	           	    	 {
	           	    		 if(!drawable)
	           	    		 {
	           	    			 drawable=true;
	           	    			 ctx.beginPath();
	           	    			 ctx.moveTo(p.x,p.y);
	           	    		 }
	           	    		 //store point 
	           	    		 previousX = p.x;
	           	    		 previousY = p.y;
	           	    		 //draw point
	           	    		 
	           	    		 if(p.color=='rgba(255,255,255,1.0)'){
	           	    			ctx.strokeStyle = "rgb(255, 255, 255)";
	           	    			ctx.globalCompositeOperation = "copy";  
	           	    			p.color = "rgba(255,255,255,0)";
	           	    		 }
	           	    		 
	           	    		 ctx.strokeStyle=p.color;
	           	    		 ctx.lineWidth = p.size;
	           	    		 ctx.lineTo(p.x, p.y);
	           	    		 ctx.stroke();
	           	    	 }
                    	
	           	    	 else
	           	    	 {
	           	    		 if(drawable)
	           	    		 {
	           	    			 ctx.stroke();
	           	    			 drawable=false;
	           	    		 }
	           	    	 }
                           
                    	//end of points array
                    	if ((points.length == 0)||sk.stopDrawing == true){
                    		//that.communicator.send("endSegmentation", {user: sk.myself});
                    		sk.stopDrawing = false;
                    		
                    		return;
                    	}
                    	//recursive call passing time delay for the next point to draw
                    	setTimeout(drawPoint , points[0].ts - p.ts);
                    }
	           	   	//first call of drawPoint
                    setTimeout(drawPoint, 0);
                    
                }
                else{
                    alert("Request was unsuccesfull: "+ xhr.status);
                }
            }
        }
    });


}

//check if consecutive points (x,y) and (cx,cy) are in the same neighborhood
function inRange(cx, cy, x, y, size) {
    //Check to see the distance between the point and the center of the circle
    var d = Math.sqrt(Math.pow((x - cx), 2) + Math.pow((y - cy), 2));
    if (d <= (size / 2)) {
        return true;
    } else {
        return false;
    }
}




function checkAreaBentley(sk){

	var total_area = 0;
	var pointsList = sk.pointsList;
	var ratio = sk.ratio;

	function polygonArea(X, Y, numPoints) 
	{ 
	  area = 0;         // Accumulates area in the loop
	  j = numPoints-1;  // The last vertex is the 'previous' one to the first

	  for (i=0; i<numPoints; i++)
	    { 
		  area = area +  (X[j]+X[i]) * (Y[j]-Y[i]); 
	      j = i;  //j is previous vertex to i
	    }
	  if(area<0){
		  final_area = - area/2;
	  }
	  else{
		  final_area = area/2;
	  }
	 
	  total_area = total_area + final_area;

	}
	
	var pointsJson = JSON.stringify(pointsList);

	$.jAjax({
	    url: "bentleyOttmann",
	    headers : {"points" : pointsJson},
	    onComplete: function(xhr,status){
	        if(xhr.readyState === 4){
	            if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

	                var result = JSON.parse(xhr.responseText);
	                var polygons = result.polygons[0];
	                
	                $.each(polygons, function(i,d){
	                	
	                	var Xarray = [];
	                	var Yarray = [];

	                	 $.each(d, function(j,k){

	                		 Xarray.push(parseInt(k.x));
	                 		 Yarray.push(parseInt(k.y));

	                	 });

	                	polygonArea(Xarray, Yarray, Xarray.length);
	                	
	                });
	                //Scaling "square law"
	                sk.currentArea = total_area * ratio * ratio;
	            }
	            else{
	                alert("Request was unsuccesfull: "+ xhr.status);
	            }
	        }
	    }
	});

	
}

function poseClassifier(pose, ratio){
	//var pose = "feet";
	//var ratio = 0.168619;

	$.jAjax({
	    url: "poseClassifier",
	    headers : {
	        "pose" : pose,
	        "ratio" : ratio
	    },
	    onComplete: function(xhr,status){
	        if(xhr.readyState === 4){
	            if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

	                var result = JSON.parse(xhr.responseText);
	                alert(result.cloth);
	            }
	            else{
	                alert("Request was unsuccesfull: "+ xhr.status);
	            }
	        }
	    }
	});
}


function getPose(idselected, pose){


	$.jAjax({
	    url: "getPose",
	    headers : {"idselected" : idselected},
	    onComplete: function(xhr,status){
	        if(xhr.readyState === 4){
	            if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){
	            	
	            	var result = JSON.parse(xhr.responseText);
                    var obj;
                    var x0,x1,y0,y1,location;
                    var minX = Number.MAX_VALUE;
	            	var maxX = Number.MIN_VALUE;
	            	var minY = Number.MAX_VALUE;
	            	var maxY = Number.MIN_VALUE;

	            	for(var key in result){

                    	obj = result[key];
                    	location = obj.location;

                    	pose[location + "_x0"] = obj.x0;
                    	if(pose[location + "_x0"]>maxX)
                    		maxX = pose[location + "_x0"];
                    	if(pose[location + "_x0"]<minX)
                    		minX = pose[location + "_x0"];
                
                    	pose[location + "_x1"] = obj.x1;
                    	if(pose[location + "_x1"]>maxX)
                    		maxX = pose[location + "_x1"];
                    	if(pose[location + "_x1"]<minX)
                    		minX = pose[location + "_x1"];
                    	
                    	pose[location + "_y0"] = obj.y0;
                    	if(pose[location + "_y0"]>maxY)
                    		maxY = pose[location + "_y0"];
                    	if(pose[location + "_y0"]<minY)
                    		minY = pose[location + "_y0"];
                    	
                    	pose[location + "_y1"] = obj.y1;
                    	if(pose[location + "_y1"]>maxY)
                    		maxY = pose[location + "_y1"];
                    	if(pose[location + "_y1"]<minY)
                    		minY = pose[location + "_y1"];
						
                    }
	            	pose["bodyArea"] = (maxY - minY) * (maxX - minX);
	            	
	            }
	            else{
	                alert("Request was unsuccesfull: "+ xhr.status);
	            }
	        }
	    }
	});
}

function getCurrentPosition(point, pose, sk, currentImage){


	var x = Math.round(point.x/sk.ratio);
	var y = Math.round(point.y/sk.ratio);

	/*
	//head
	//var xMin = Math.min(pose.head_x0, pose.head_x1);
	//var xMax = Math.max(pose.head_x0, pose.head_x1);
	var yMax = Math.max(pose.head_y0, pose.head_y1);
	if((y<yMax)){
		alert("head");
	}
	
	//left_arm
	var xMax = Math.max(pose.left_arm_x0, pose.left_arm_x1);
	var yMin = Math.min(pose.left_arm_y0, pose.left_arm_y1);
	var yMax = Math.max(pose.left_arm_y0, pose.left_arm_y1);
	if((x<xMax)&&(y>yMin)&&(y<yMax)){
		alert("left arm");
	}
	
	//right_arm
	var xMin = Math.min(pose.right_arm_x0, pose.right_arm_x1);
	var yMin = Math.min(pose.right_arm_y0, pose.right_arm_y1);
	var yMax = Math.max(pose.right_arm_y0, pose.right_arm_y1);
	if((x>xMin)&&(y>yMin)&&(y<yMax)){
		alert("right arm");
	}
	
	//feet
	var yMin = Math.min(pose.feet_y0, pose.feet_y1);
	if(y>yMin){
		alert("feet");
	}
	
	//torso
	var xMin = Math.min(pose.torso_x0, pose.torso_x1);
	var xMax = Math.max(pose.torso_x0, pose.torso_x1);
	var yMin = Math.min(pose.torso_y0, pose.torso_y1);
	var yMax = Math.max(pose.torso_y0, pose.torso_y1);
	if((x>xMin)&&(x<xMax)&&(y>yMin)&&(y<yMax)){
		alert("torso");
	}
	//feet
	var xMin = Math.min(pose.legs_x0, pose.legs_x1);
	var xMax = Math.max(pose.legs_x0, pose.legs_x1);
	var yMin = Math.min(pose.legs_y0, pose.legs_y1);
	var yMax = Math.max(pose.legs_y0, pose.legs_y1);
	if((x>xMin)&&(x<xMax)&&(y>yMin)&&(y<yMax)){
		alert("legs");
	}
	
	*/
	var r_s_x = pose.head_x1;
	var r_s_y = pose.torso_y1;
	var l_s_x = pose.head_x0;
	var l_h_y = pose.legs_x0;
	var r_a_y = pose.feet_y1;
	
	if((y<r_s_y)){
		alert("head");
	}
	//else if((y<l_h_y)&&(y>r_s_y)&&(x<l_s_x)&&(x>r_s_x)){
	else if((y<l_h_y)&&(y>r_s_y)){
		alert("torso");
	}
	//else if((y<r_a_y)&&(y>l_h_y)&&(x<l_s_x)&&(x>r_s_x)){
	else if((y<r_a_y)&&(y>l_h_y)){
		alert("legs");
	}
	else if((y>r_a_y)){
		alert("feet");
	}
	else{
		alert("arms");
	}
	
	
}



function guessWordBot(point, pose, sk, currentImage, that, possibleGuesses, guesses){
	
	//get current position
	var x = Math.round(point.x/sk.ratio);
	var y = Math.round(point.y/sk.ratio);
	
	//check bounding boxes
	var r_s_x = pose.head_x1;
	var r_s_y = pose.torso_y1;
	var l_s_x = pose.head_x0;
	var l_h_y = pose.legs_x0;
	var r_a_y = pose.feet_y1;
	
	var bodyPart;

	
	if((y<r_s_y)){
		//head
		bodyPart = "head";
	}
	//else if((y<l_h_y)&&(y>r_s_y)&&(x<l_s_x)&&(x>r_s_x)){
	else if((y<l_h_y)&&(y>r_s_y)){
		//torso
		bodyPart = "torso";
	}
	//else if((y<r_a_y)&&(y>l_h_y)&&(x<l_s_x)&&(x>r_s_x)){
	else if((y<r_a_y)&&(y>l_h_y)){
		//legs
		bodyPart = "legs";
	}
	else if((y>r_a_y)){
		//feet
		bodyPart = "feet";
	}
	else{
		//arms
		bodyPart = "arms";
	}
	
	//Compute current Area
	var total_area = 0;
	var pointsList = sk.pointsList;
	var ratio = sk.ratio;

	function polygonArea(X, Y, numPoints) 
	{ 
	  area = 0;         // Accumulates area in the loop
	  j = numPoints-1;  // The last vertex is the 'previous' one to the first

	  for (i=0; i<numPoints; i++)
	    { 
		  area = area +  (X[j]+X[i]) * (Y[j]-Y[i]); 
	      j = i;  //j is previous vertex to i
	    }
	  if(area<0){
		  final_area = - area/2;
	  }
	  else{
		  final_area = area/2;
	  }
	 
	  total_area = total_area + final_area;

	}
	
	var pointsJson = JSON.stringify(pointsList);

	$.ajax({type:"POST",url:"bentleyOttmann",

		  //data:'{"points":'+pointsJson+'}',
		  data:pointsJson,
		  dataType:"json",

		  contentType:"application/json",

		success:function(d){
			
			 var result = d;
             var polygons = result.polygons[0];
             
             $.each(polygons, function(i,d){
             	
             	var Xarray = [];
             	var Yarray = [];

             	 $.each(d, function(j,k){

             		 Xarray.push(parseInt(k.x));
              		 Yarray.push(parseInt(k.y));

             	 });

             	polygonArea(Xarray, Yarray, Xarray.length);
             	
             });
             //Scaling "square law"
             sk.currentArea = total_area * ratio * ratio;
		},
		error:function(e){console.log("err : ", e);
		}
	});
	/*
	$.jAjax({
	    url: "bentleyOttmann",
	    headers : {"points" : pointsJson},
	    onComplete: function(xhr,status){
	        if(xhr.readyState === 4){
	            if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

	                var result = JSON.parse(xhr.responseText);
	                var polygons = result.polygons[0];
	                
	                $.each(polygons, function(i,d){
	                	
	                	var Xarray = [];
	                	var Yarray = [];

	                	 $.each(d, function(j,k){

	                		 Xarray.push(parseInt(k.x));
	                 		 Yarray.push(parseInt(k.y));

	                	 });

	                	polygonArea(Xarray, Yarray, Xarray.length);
	                	
	                });
	                //Scaling "square law"
	                sk.currentArea = total_area * ratio * ratio;
	            }
	            else{
	                alert("Request was unsuccesfull: "+ xhr.status);
	            }
	        }
	    }
	});
	*/
	var areasRatio = sk.currentArea / pose.bodyArea;
	
	
	
	
	$.jAjax({
	    url: "poseClassifier",
	    headers : {
	        "pose" : bodyPart,
	        "ratio" : areasRatio
	    },
	    onComplete: function(xhr,status){
	        if(xhr.readyState === 4){
	            if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

	            	var cloth = xhr.responseText;
	            	
	            	if((guesses.indexOf(cloth) == -1)&&(!sk.stopGuessing)){
	            		guesses.push(cloth);
	            		console.log("[GUESS 1] " + cloth);
	            		that.communicator.send("guessAttempt", {
						    user: "bot",
							word: cloth
						});
            		}
	            	else{
	            		console.log("[REPEATED GUESS 1] " + cloth);
	            	}
	                /*
	            	//finish all possibilities for that bodypart --> guess last word: "bracelet","watch","ring","gloves"
	            	if((possibleGuesses[cloth].length + possibleGuesses[bodyPart].length) == guesses.length){
	            		
	            		var temps = 0;
						var max_index = possibleGuesses["last"].length -1;
						var possGuesses = possibleGuesses["last"];
						//shuffle array
						for (var g = possGuesses.length - 1; g > 0; g--) {
							var h = Math.floor(Math.random() * (g + 1));
							var temp = possGuesses[g];
							possGuesses[g] = possGuesses[h];
							possGuesses[h] = temp;
						}
						
						var nextGuess = function(){
							temps ++;

							var next_guess = possGuesses[temps];
							if(guesses.indexOf(next_guess) == -1){
								guesses.push(next_guess);
								if(next_guess != undefined){
									console.log("[GUESS LAST] " + next_guess);
									that.communicator.send("guessAttempt", {
										user: "bot",
										word: next_guess
									});
								}
								
							}
							
							if ((temps == max_index)||(sk.stopGuessing)){
								return;
							}
							//recursive call passing time delay for the next guess
							setTimeout(nextGuess , 1000);
						}
						//first call of nextGuess
						setTimeout(nextGuess, 0);
	            	}
	            	*/
	            	//GUESS SYNONYMOUS OR SIMILAR CLOTHS (coat/shirt)
	            	if(possibleGuesses[cloth]!== undefined){
						
						var temps = 0;
						var max_index = possibleGuesses[cloth].length -1;
						var possGuesses = possibleGuesses[cloth];
						//shuffle array
						for (var g = possGuesses.length - 1; g > 0; g--) {
							var h = Math.floor(Math.random() * (g + 1));
							var temp = possGuesses[g];
							possGuesses[g] = possGuesses[h];
							possGuesses[h] = temp;
						}
						
						var nextGuess = function(){
							temps ++;

							var next_guess = possGuesses[temps];
							if((guesses.indexOf(next_guess) == -1)&&(!sk.stopGuessing)){
								guesses.push(next_guess);
								if(next_guess != undefined){
									console.log("[GUESS 2] " + next_guess);
									that.communicator.send("guessAttempt", {
										user: "bot",
										word: next_guess
									});
								}
								
							}
							else{
								console.log("[REPETED GUESS 2] " + next_guess);
							}
							
							if ((temps == max_index)||(sk.stopGuessing)){
								return;
							}
							//recursive call passing time delay for the next guess
							setTimeout(nextGuess , 1000);
						}
						//first call of nextGuess
						setTimeout(nextGuess, 0);
	            	}
	            	//GUESS CLOTH OF SAME BODY PART (belt/skirt/shorts)
	            	else{
						var temps = 0;
						var max_index = possibleGuesses[bodyPart].length -1;
						var possGuesses = possibleGuesses[bodyPart];
						//shuffle array
						for (var g = possGuesses.length - 1; g > 0; g--) {
							var h = Math.floor(Math.random() * (g + 1));
							var temp = possGuesses[g];
							possGuesses[g] = possGuesses[h];
							possGuesses[h] = temp;
						}
						var nextGuessBodyPart = function(){
							temps ++;

							var next_guess = possGuesses[temps];
							if((guesses.indexOf(next_guess) == -1)&&(!sk.stopGuessing)){
								guesses.push(next_guess);
								console.log("[GUESS 3] " + next_guess);
									that.communicator.send("guessAttempt", {
										user: "bot",
										word: next_guess
									});
								//return;
							}
							else{
								console.log("[REPETED GUESS 3] " + next_guess);
							}
							
							if ((temps == max_index)||(sk.stopGuessing)){
								return;
							}
							//recursive call passing time delay for the next guess
							setTimeout(nextGuessBodyPart , 1000);
						}
						//first call of nextGuess
						setTimeout(nextGuessBodyPart, 0);
						
					}
	            }
	            else{
	                alert("Request was unsuccesfull: "+ xhr.status);
	            }
	        }
	    }
	});
	
}