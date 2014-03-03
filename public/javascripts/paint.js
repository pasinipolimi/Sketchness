require(["Class", "Chat", "StateMachine", "Communicator", "Time", "Writer", "canvas/Painter", "jquery", "i18n"],
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
			tagTime: 30,
			taskTime: 90,
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
			warnTag: $("#warnTag"),
			websocket: $('#paintWebSocket'),
			chatContainer: $("#messages"),
			chatInput: $("#talk"),
			opponents: $("#opponents"),
			wordInput: $("#wordInput"),
			skip: $("#skipTask"),
			endSegmentation: $("#endSegmentation"),
			questionMark: $('#questionMark'),
			hudArea: $("#hudArea"),
			hud: $("#hud"),
			tool: $("#tool"),
			size: $("#size"),
			color: $("#color"),
			viewport: $("#viewport"),
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

		var painter = new Painter(elements.task[0], elements.draws[0], elements.positions[0]);

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

					this.communicator.on({
						join: function(e, content) {
							sk.players[content.user] = {
								id: content.user,
								name: content.name,
								img: content.img,
								score: 0
							};
							write.players(sk.players);
						},
						leave: function(e, content) {
							delete sk.players[content.user];
							write.players(sk.players);
						},
						loading: function() {
							that.load();
						}
					});
				},

				/**
				 * Tear down of wait players state
				 */
				onleaveplayersWait: function() {
					this.write.top();

					this.communicator.off("join leave loading");
				},

				/**
				 * Setup of loading state
				 */
				onenterloading: function() {
					this.write.top($.i18n.prop('matchstarting'));

					var that = this;

					this.communicator.on("roundBegin", function(e, content) {
						that.beginRound(content.sketcher);
					});
				},

				/**
				 * Tear down of loading state
				 */
				onleaveloading: function() {
					this.write.top();

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
					this.communicator.on({
						tag: function() {
							that.tag();
						},
						task: function(e, content) {
							that.task(content.word);
						}
					});
				},

				/**
				 * Tear down of sketcher state
				 */
				onleaveSketcher: function() {
					this.communicator.off("tag task");
				},

				/**
				 * Setup of guesser state
				 */
				onenterGuesser: function() {
					var that = this;
					this.communicator.on({
						tag: function() {
							that.tag();
						},
						task: function() {
							that.task();
						}
					});
				},

				/**
				 * Tear down of guesser state
				 */
				onleaveGuesser: function() {
					this.communicator.off("tag task");
				},

				/**
				 * Utility method to send the timeout
				 * message to the server
				 */
				timeUp: function() {
					this.communicator.send("timer", {user: sketchness.myself});
				},

				/**
				 * Setup of tag insertion state
				 */
				onentertagInsertion: function() {
					var elements = this.elements;

					elements.main.addClass("sketcher");
					this.write.top($.i18n.prop("asktagsketcher"));
					this.write.warnTag($.i18n.prop("warnTag"));
					elements.skip.show();
					elements.wordInput.show();
					this.chat.disable();
    //uncomment when timer will work
					this.clock.setCountdown("tag", this.constants.tagTime * Time.second, Time.second, this.write.time, this.timeUp.bind(this));

					var that = this;

					this.communicator.on({
						image: function(e, content) {
							that.painter.showImage(content.url, content.width, content.height);
						},
						beginRound: function(e, content) {
							that.beginRound(content.sketcher);
						},
						leave: function(e, content) {
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                            that.quit(content);
                        },
						task: function(e, content) {
							that.task(content.word);
						}
					});

					elements.skip.one("click", function() {
						that.communicator.send("skip", {});
					});

					elements.wordInput.on("keypress", function(event) {
						if (event.which === 13 && $(this).val() !== "") {
							event.preventDefault();

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

					elements.main.removeClass("sketcher");
					this.write.top();
					this.write.warnTag();
					this.write.time();
					elements.skip.hide();
					elements.wordInput.hide();
					this.chat.enable();

					this.clock.clearCountdown("tag");

					this.painter.hideImage();

					this.communicator.off("image beginRound leave leaderboard task");
					elements.skip.off("click");
					elements.wordInput.off("keypress");
				},

				/**
				 * Setup of tag wait state
				 */
				onentertagWait: function() {
					this.write.top($.i18n.prop('asktag'));
					this.write.canvasMessage($.i18n.prop('sketchertagging'));

					var question = this.elements.questionMark;
					this.painter.showImage(question.attr("src"), question.attr("rwidth"), question.attr("rheight"));
    //uncomment when timer will work
	//				this.clock.setCountdown("tag", this.constants.tagTime * Time.second, Time.second, this.write.time, this.timeUp.bind(this));

					var that = this;

					this.communicator.on({
						beginRound: function(e, content) {
							that.beginRound(content.sketcher);
						},
						leave: function(e, content) {
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
                        leaderboard: function(e, content) {
                            that.quit(content);
                        },
						task: function() {
							that.task();
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
					elements.skip.show();
					elements.hudArea.show();
					this.chat.disable();

					this.clock.setCountdown("task", this.constants.taskTime * Time.second, Time.second, this.write.time, this.timeUp.bind(this));

					var that = this,
						painter = this.painter,
						sk = this.sketchness;

					painter.setName(sk.players[sk.sketcher].name);

					this.communicator.on({
						image: function(e, content) {
							painter.showImage(content.url, content.width, content.height);
						},
						timer: function(e, content) {
							that.time.changeCountdown("task", content.time * Time.second);
						},
						guess: function(e, content) {
							that.chat.guess(sk.players[content.user].name, content.word, content.affinity, content.user == sk.myself);
						},
						score: function(e, content) {
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
                            that.quit(content);
                        },
						roundEnd: function(e, content) {
						    sk.word = content.word;
							that.endRound();
						}
					});

					elements.skip.on("click", function() {
						that.communicator.send("skip", {});
					});

					if(this.sketchness.players.lenght === 1) {
						elements.endSegmentation.show();
						elements.endSegmentation.on("click", function() {
							that.communicator.send("endSegmentation", {});
						});
					}

					elements.hud.on("change", function() {
						var tool = {
							tool: elements.tool.val(),
							size: elements.size.val(),
							color: elements.color.val()
						};

						painter.setTool(tool);
						that.communicator.send("changeTool", tool);
					});
					elements.hud.trigger("change");

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

						if (point != null && point.x > 0 && point.x < elements.viewport.width() && point.y > 0) {
							//point.y > 0 && point.y < elements.viewport.height()) {

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

					this.clock.clearCountdown("task");

					elements.skip.off("click");
					elements.endSegmentation.off("click");
					elements.hud.off("change");

					elements.viewport.off(this.isMobile ? "touchstart" : "mousedown");
					elements.viewport.off(this.isMobile ? "touchmove" : "mousemove");
					$(document).trigger(this.isMobile ? "touchend" : "mouseup");
					$(document).off(this.isMobile ? "touchend" : "mouseup");

					this.painter.hideImage();
					this.painter.hidePosition();
					this.painter.hidePath();

					this.communicator.off("image timer guess guessed score leave leaderboard roundEnd");
				},

				/**
				 * Setup of task guessing state
				 */
				onentertaskGuessing: function() {
					var that = this,
						sk = this.sketchness,
						wordInput = this.elements.wordInput,
						painter = this.painter;

					this.write.top($.i18n.prop('guess'));
					wordInput.show();
        //uncomment when timer will work
		//			this.clock.setCountdown("task", this.constants.taskTime * Time.second, Time.second, this.write.time, this.timeUp.bind(this));

					painter.setName(sk.players[sk.sketcher].name);

					wordInput.on("keypress", function(event) {
						if (event.which === 13) {
							event.preventDefault();

							that.communicator.send("guessAttempt", {
							    user: sk.myself,
								word: $(this).val()
							});

							$(this).val("");
						}
					});

					this.communicator.on({
						timer: function(e, content) {
							that.time.changeCountdown("task", content.time * Time.second);
						},
						changeTool: function(e, tool) {
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
							that.chat.guess(sk.players[content.user].name, content.word, content.affinity, content.user == sk.myself);
						},
						guessed: function(e, content) {
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
                            that.quit(content);
                        },
						roundEnd: function(e, content) {
							that.endRound(content.word);
						}
					});
				},

				/**
				 * Tear down of task guessing state
				 */
				onleavetaskGuessing: function() {
					this.write.top();
					this.write.time();

					this.elements.wordInput.hide().off("keypress");

					this.clock.clearCountdown("task");

					this.painter.hidePosition();
					this.painter.hidePath();

					this.communicator.off("timer change beginPath point endPath guess guessed score leave leaderboard roundEnd");
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

	//				this.clock.setCountdown("solution", this.constants.solutionTime * Time.second, Time.second, write.time, this.timeUp.bind(this));

					var that = this;

					this.communicator.on({
						image: function(e, content) {
							that.painter.showImage(content.url, content.width, content.height);
						},
						beginRound: function(e, content) {
							that.beginRound(content.sketcher);
						},
						leave: function(e, content) {
                            delete sk.players[content.user];
                            write.players(sk.players);
                        },
						leaderboard: function(e, content) {
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

					this.clock.clearCountdown("solution");

					this.painter.hideImage();

					this.communicator.off("image beginRound leave leaderboard");
				},

				/**
				 * Transition method between image viewing and leatherboard
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

					for (var i = 0; i < this.scores.lenght; i++) {
						results += ":" + sk.players[this.scores[i].user].name + ":" + this.scores[i].points;
					}

					this.jsRoutes.controllers.Sketchness.leaderboard(sk.players[sk.myself].name, results).ajax({
						success: function(data) {
							that.elements.main.html(data);
						},
						error: function() {
							that.write.error("Error!");
						}
					});
				}
			}
		});

		StateMachine.create({
			target: GameFSM.prototype,

			events: [
				{ name: "startup", from: "none", to: "playersWait" },
				{ name: "load", from: "playersWait", to: "loading" },
				{ name: "beSketcher", from: ["loading", "imageViewing", "tagInsertion", "tagWait"], to: "Sketcher" },
				{ name: "beGuesser", from: ["loading", "imageViewing", "tagInsertion", "tagWait"], to: "Guesser" },
				{ name: "tag", from: "Sketcher", to: "tagInsertion" },
				{ name: "tag", from: "Guesser", to: "tagWait" },
				{ name: "task", from: ["Sketcher", "tagInsertion"], to: "taskDrawing" },
				{ name: "task", from: ["Guesser", "tagWait"], to: "taskGuessing" },
				{ name: "endRound", from: ["taskGuessing", "taskDrawing"], to: "imageViewing" },
				{ name: "quit", from: "imageViewing", to: "leaderboard" }
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


