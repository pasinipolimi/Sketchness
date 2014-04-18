require(["Class", "Chat", "StateMachine", "Communicator", "Time", "Writer", "canvas/Painter", "jquery", "nouislider", "spectrum", "i18n"],
function( Class,   Chat,   StateMachine,   Communicator,   Time,   Writer,   Painter,          $) {

	$(function() {

		var clock = new Time();
	
		var sketchness = {
			players: [],
			myself: $('#currentNickname').text(),
			sketcher: null,
			task: null,
			word: null,
			isMobile: /ipad|iphone|android/i.test(navigator.userAgent)
		};

		var constants = {
			tagTime: 25,
			taskTime: 120,
			solutionTime: 5
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

					write.top($.i18n.prop('waiting'));
					write.score('0');
					console.log("[BEGIN] PlayersWait");
					elements.pen.hide();
					elements.eraser.hide();
					
					this.communicator.on({
						join: function(e, content) {
						    console.log("[RECEIVED MESSAGE] join");
							sk.players[content.user] = {
								id: content.user,
								name: content.name,
								img: content.img,
								score: 0
							};
							write.players(sk.players);
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
							delete sk.players[content.user];
							write.players(sk.players);
						},
						loading: function() {
						    console.log("[RECEIVED MESSAGE] loading");
							that.load();
						},
						roundBegin: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundBegin");
							that.beginRound(content.sketcher);
						}
					});
				},

				/**
				 * Tear down of wait players state
				 */
				onleaveplayersWait: function() {
					this.write.top();
					this.communicator.off("join leave loading");
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
					var that = this;

					this.communicator.on({
						leaderboard: function(e, content) {
						        console.log("[RECEIVED MESSAGE] leaderboard");
								that.quit(content);
							},
						roundBegin: function(e, content) {
						    console.log("[RECEIVED MESSAGE] roundBegin");
							that.beginRound(content.sketcher);
						}
					});
					
				},

				/**
				 * Tear down of loading state
				 */
				onleaveloading: function() {
					this.write.top();
					console.log("[LEAVE] Loading");
					this.communicator.off("beginRound");
				},

				/**
				 * Utility method to trigger the event for
				 * a new round automatically choosing between
				 * sketcher and guesser state.
				 *
				 * @param sketcher :String The sketcher ID
				 */
				beginRound: function(sketcher) {
					var sk = this.sketchness;
					sk.sketcher = sketcher;
					if(sk.sketcher === sk.myself) {
						this.beSketcher();
					} else {
						this.beGuesser();
					}
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
						}
					});
				},

				/**
				 * Tear down of sketcher state
				 */
				onleaveSketcher: function() {
					this.communicator.off("tag task");
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
						}
					});
				},

				/**
				 * Tear down of guesser state
				 */
				onleaveGuesser: function() {
					this.communicator.off("tag task");
					console.log("[LEAVE] Guesser");
				},

				/**
				 * Utility method to send the timeout
				 * message to the server
				 */
				timeUp: function() {
					if(Object.size(this.sketchness.players)==1) {
						this.skipRound();
						console.log("[SENDING MESSAGE] skip");
						this.communicator.send("skip", {});
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
					elements.main.addClass("sketcher");
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
					elements.main.removeClass("sketcher");
					this.write.top();
					//this.write.warnTag();
					this.write.time();
					elements.skip.hide();
					elements.wordInput.hide();
					this.chat.enable();

					this.clock.clearCountdown("tag");

					this.painter.hideImage();

					this.communicator.off("image beginRound leave leaderboard task skipTask");
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

					this.communicator.off("beginRound leave leaderboard task");
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

						if(!sk.isMobile) {
							return {
								x: (event.pageX - offsetX),
								y: (event.pageY - offsetY)
							};
						} else {
							// Touchend does not have the position of when we lifted our finger
							if(event.originalEvent.type !== "touchend" && event.originalEvent.touches) {
								return {
									x: (event.originalEvent.touches[0].pageX - offsetX),
									y: (event.originalEvent.touches[0].pageY - offsetY)
								};
							} else {
								return null;
							}
						}
					};

					var setPoint = function(event) {
						var point = relativePosition(event, elements.viewport);

						if (point != null && point.x > 0 && point.x < elements.viewport.width() && point.y > 0 && point.y < elements.viewport.height()) {

							painter.setPoint(point);
							that.communicator.send("point", point);
						}
					};

					elements.viewport.on((sk.isMobile ? "touchstart" : "mousedown"), function(e) {
						if(!started) {
							that.communicator.send("beginPath", {});
							painter.beginPath();
							started=true;
							setPoint(e);
						}
					});

					elements.viewport.on((sk.isMobile ? "touchmove" : "mousemove"), function(e) {
						e.preventDefault();
						setPoint(e);
					});

					$(document).on((sk.isMobile ? "touchend" : "mouseup"), function(e) {
						if(started) {
							setPoint(e);
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
					this.painter.hidePosition();
					//this.painter.hidePath();

					this.communicator.off("image timer guess guessed score leave leaderboard roundEnd skipTask");
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

					this.painter.hidePosition();
					//this.painter.hidePath();

					this.communicator.off("timer change beginPath point endPath guess guessed score leave leaderboard roundEnd skipTask");
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

					this.communicator.off("image beginRound leave leaderboard");
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
						}
					});
				},
				
				onleavewaitRole: function() {
					console.log("[END] LeaveWaitRole");
					this.communicator.off("leaderboard");
				}
			}
		});

		StateMachine.create({
			target: GameFSM.prototype,

			events: [
				{ name: "startup", from: "none", to: "playersWait" },
				{ name: "load", from: ["none", "playersWait"], to: "loading" },
				{ name: "beSketcher", from: ["loading", "playersWait", "waitRole", "tagInsertion", "tagWait"], to: "Sketcher" },
				{ name: "beGuesser", from: ["loading", "playersWait", "waitRole", "tagInsertion", "tagWait"], to: "Guesser" },
				{ name: "nextRound", from: ["imageViewing", "taskDrawing",  "tagInsertion", "tagWait"], to: "waitRole"},
				{ name: "skipRound", from: ["taskGuessing", "taskDrawing"], to: "waitRole" },
				{ name: "tag", from: "Sketcher", to: "tagInsertion" },
				{ name: "tag", from: "Guesser", to: "tagWait" },
				{ name: "task", from: ["Sketcher", "tagInsertion"], to: "taskDrawing" },
				{ name: "task", from: ["Guesser", "tagWait"], to: "taskGuessing" },
				{ name: "endRound", from: ["taskGuessing", "taskDrawing"], to: "imageViewing" },
				{ name: "quit", from: ["imageViewing", "tagInsertion", "taskDrawing", "taskGuessing", "tagWait", "waitRole", "loading"], to: "leaderboard" },
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


