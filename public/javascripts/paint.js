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
			image: null,
			stopDrawing: false,
			lastSent: 0,
			traceNum: 1,
			isMobile: /ipad|iphone|android/i.test(navigator.userAgent)
		};

		var constants = {
			tagTime: 30,
			//taskTime: 120,
			taskTime: 5,
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
						    sketchness.image = content.id;
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
						e.preventDefault();
						setPoint(e);
						if (canSendNow()) 
							sendPoints();
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
					
					var that = this,
						painter = this.painter,
						sk = this.sketchness;

					painter.setName(sk.players[sk.sketcher].name);

					this.communicator.on({
						image: function(e, content) {
						    console.log("[RECEIVED MESSAGE] image");
						    var canvas = document.getElementById("draws");
		                	var ctx = canvas.getContext("2d");
		                	ctx.clearRect(0,0,canvas.width,canvas.height);
		                	sk.stopDrawing = false;
							painter.showImage(content.url, content.width, content.height);
							guessTag(content.id,that,sk);
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
						e.preventDefault();
						setPoint(e);
						if (canSendNow()) 
							sendPoints();
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
					drawSegmentation(sk.image, that, sk);

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
				 * Tear down of image viewing BOT state
				 */
				onleaveimageViewingBot: function() {
					this.write.top();
					this.write.time();
					console.log("[END] ImageViewingBot");
					this.clock.clearCountdown("solution");

					this.painter.hideImage();

					this.communicator.off("image leave leaderboard");
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

function guessTag(idselected, that, sk){
	
	$.jAjax({
	    url: "taggingImage",
	    headers : {"selected" : idselected},
	    onComplete: function(xhr,status){
	        if(xhr.readyState === 4){
	            if(xhr.status >= 200 && xhr.status < 300 || xhr.status === 304){

	                result = JSON.parse(xhr.responseText);

	                var tag = result[0].tag;
	                that.chat.guess(sk.players[sk.myself].name, tag, "hot", true);
	                //that.communicator.send("endSegmentation", {user: sk.myself});
	                /*
	                that.communicator.send("guessAttempt", {
					    user: sk.myself,
						word: $(this).val()
					});
					*/

	            }
	            else{
	                alert("Request was unsuccesfull: "+ xhr.status);
	            }
	        }
	    }
	});
}
