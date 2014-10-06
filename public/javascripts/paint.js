require(["Class", "Chat", "StateMachine", "Communicator", "Time", "Writer", "canvas/Painter", "jquery", "nouislider", "spectrum", "i18n", "howler", "snow"],
function( Class,   Chat,   StateMachine,   Communicator,   Time,   Writer,   Painter,          $) {

	$(function() {

		var clock = new Time();

        var animation = {
            speed: 1,
            size: 20,
            count: 50,
            image: "assets/images/sket.png",
            timeOut: 5000
        }

        var guess_sound = new Howl({
			urls: ['assets/sounds/effects/guess_sound.ogg']
		});
        
        var guessed_sound = new Howl({
			urls: ['assets/sounds/effects/guessed_sound.ogg']
		});
	
		var sketchness = {
			players: [],
			myself: $('#currentNickname').text(),
			sketcher: null,
			task: null,
			word: null,
			points: [],
			lastSent: 0,
			traceNum: 1,
			isMobile: /ipad|iphone|android/i.test(navigator.userAgent)
		};

		var constants = {
			tagTime: 30,
			taskTime: 120,
			solutionTime: 3,
			minSendRate: 50,
			playersColors: ['DarkGreen','OrangeRed','Purple','Blue','AliceBlue']
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
			positions: $("#positions"),
            catSelector: $('#catSelector'),
            catContainer: $("#catContainer"),
            catClose: $('#catContainer-close'),
            cat1: $('#cat1'),
            cat2: $('#cat2'),
            cat3: $('#cat3')
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
			preferredFormat: 'rgb',
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
			if(tool.tool=="eraser")
				tool.color = "rgba(255,255,255,1.0)";
			painter.setTool(tool);
			communicator.send("changeTool", tool);
		};

		var chat = new Chat(elements.chatContainer, elements.chatInput);
		$(chat).on("send", function(e, message) {
			communicator.send("chat", { user: sketchness.myself, message: message });
		});

		communicator.on({
			chat: function(e, content) {
				chat.message(sketchness.players[content.user].name, content.message, content.user === sketchness.myself, sketchness.players[content.user].color);
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
									number : i,
									id: content[i].user,
									name: content[i].name,
									img: content[i].img,
									score: 0,
									color: constants.playersColors[i]
								};
							}
							write.players(sk.players,sketchness.myself);
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
							delete sk.players[content.user];
							write.players(sk.players,sketchness.myself);
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
					elements.pen.hide();
					elements.eraser.hide();
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
									number : i,
									id: content[i].user,
									name: content[i].name,
									img: content[i].img,
									score: 0,
									color: constants.playersColors[i]
								};
							}
							write.players(sk.players,sketchness.myself);
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
					var sk = this.sketchness;
					sk.sketcher = sketcher;
					write.top($.i18n.prop('matchstarting'));
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
					clock.setTimer("round");
					sketchness.points = [];
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

					//Getting the classes of the added buttons for the garments
					$("a").unbind( "click" );
					$("a").click(function() {
					   var myClasses = this.classList;
					   if(myClasses.length==2) {
					   	  var nick = that.sketchness.myself;
					   	  console.log("[SENDING MESSAGE] guessAttempt");
						  that.communicator.send("guessAttempt", {
							 user: nick,
							 word: myClasses[1]
						  });
						  elements.catContainer.fadeOut();
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
					elements.pen.hide();
					elements.eraser.hide();
					this.write.top($.i18n.prop("asktagsketcher"));

					//this.write.warnTag($.i18n.prop("warnTag"));
					elements.skip.show();
					elements.wordInput.show();
					sk = this.sketchness;

                    // -->MoonSUB
                    elements.catSelector.show();
                    elements.cat1.on('click',function(){
                        //var top = ($(window).height()-elements.catContainer.height())/2;
                        //$('#catContainer-wrap').css({'top' :top+'px'});
                        elements.catClose.on('click',function(){elements.catContainer.fadeOut()});
                        $('.icons').hide();
                        $('#ico1').show();
                        elements.catContainer.hide().fadeIn();
                    });


                    elements.cat2.on('click',function(){
                        //var top = ($(window).height()-elements.catContainer.height())/2;
                        //$('#catContainer-wrap').css({'top' :top+'px'});
                        elements.catClose.on('click',function(){elements.catContainer.fadeOut()});
                        $('.icons').hide();
                        $('#ico2').show();
                        elements.catContainer.hide().fadeIn();
                    });

                    elements.cat3.on('click',function(){
                        //var top = ($(window).height()-elements.catContainer.height())/2;
                        //$('#catContainer-wrap').css({'top' :top+'px'});
                        elements.catClose.on('click',function(){elements.catContainer.fadeOut()});
                        $('.icons').hide();
                        $('#ico3').show();
                        elements.catContainer.hide().fadeIn();
                    });
                    // <--MoonSUB

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
                            write.players(sk.players,sketchness.myself);
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

					//Getting the classes of the added buttons for the garments
                    $("a").unbind( "click" );
					$("a").click(function() {
					   var myClasses = this.classList;
					   if(myClasses.length==2) {
					   	  console.log("[SENDING MESSAGE] tag");
						  that.communicator.send("tag", {
								word: myClasses[1]
						  });
						  elements.catContainer.fadeOut();
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

                    //-->MoonSUB
                    elements.catContainer.hide();
                    elements.catSelector.hide();
                    elements.cat1.off('click');
                    //<--MoonSUB

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
					sk = this.sketchness;
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
                            write.players(sk.players,sketchness.myself);
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
							write.players(sk.players,sketchness.myself);
							if(content.user == sk.myself) {
								that.write.score(sk.players[content.user].score);
							}
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players,sketchness.myself);
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
						if (typeof element.offsetParent !== 'undefined') {
							do {
								offsetX += element.offsetLeft;
								offsetY += element.offsetTop;
							} while ((element = element.offsetParent));
						}
						if((typeof event != 'undefined') && (typeof event.originalEvent != 'undefined')) {
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

					var setPosition = function(event) {
						var point = relativePosition(event, elements.viewport);

						if (point != null && point.x > 0 && point.x < elements.viewport.width() && point.y > 0 && point.y < elements.viewport.height()) {
							painter.setPosition(point);
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
						setPosition(e);
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
					elements.pen.hide();
					elements.eraser.hide();
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

                    // -->MoonSUB
                    elements.catSelector.show();
                    elements.cat1.on('click',function(){
                        //var top = ($(window).height()-elements.catContainer.height())/2;
                        //$('#catContainer-wrap').css({'top' :top+'px'});
                        elements.catClose.on('click',function(){elements.catContainer.fadeOut()});
                        $('.icons').hide();
                        $('#ico1').show();
                        elements.catContainer.hide().fadeIn();
                    });

                    elements.cat2.on('click',function(){
                        //var top = ($(window).height()-elements.catContainer.height())/2;
                        //$('#catContainer-wrap').css({'top' :top+'px'});
                        elements.catClose.on('click',function(){elements.catContainer.fadeOut()});
                        $('.icons').hide();
                        $('#ico2').show();
                        elements.catContainer.hide().fadeIn();
                    });

                    elements.cat3.on('click',function(){
                        //var top = ($(window).height()-elements.catContainer.height())/2;
                        //$('#catContainer-wrap').css({'top' :top+'px'});
                        elements.catClose.on('click',function(){elements.catContainer.fadeOut()});
                        $('.icons').hide();
                        $('#ico3').show();
                        elements.catContainer.hide().fadeIn();
                    });
                    // <--MoonSUB

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
							guess_sound.play();
						},
						guessed: function(e, content) {
						    console.log("[RECEIVED MESSAGE] guessed");
						    if(sk.myself == content.user){
                                sk.word = content.word;
                                that.write.top($.i18n.prop('guessed'), sk.word);
                                wordInput.hide().off("keypress");
                                this.one("image", function(e, content) {
                                    painter.showImage(content.url, content.width, content.height);
                                    $('#canvaswindows').prepend('<canvas id="winner" style="z-index: 10;position: absolute;width: inherit;height: inherit;"/>');
                                    $('canvas#winner').let_it_snow({
                                        speed: animation.speed,
                                        size: animation.size,
                                        count: animation.count,
                                        image: animation.image

                                    });

                                    setTimeout(function(){
                                        $('#winner').remove();
                                    },animation.timeOut);
                                });

                                guessed_sound.play();

							} 
						},
						score: function(e, content) {
						    console.log("[RECEIVED MESSAGE] score");
							sk.players[content.user].score += content.score;
							write.players(sk.players,sketchness.myself);
							if(content.user == sk.myself) {
								that.write.score(sk.players[content.user].score);
							}
						},
						leave: function(e, content) {
						    console.log("[RECEIVED MESSAGE] leave");
                            delete sk.players[content.user];
                            write.players(sk.players,sketchness.myself);
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

                    //-->MoonSUB
                    elements.catContainer.hide();
                    elements.catSelector.hide();
                    elements.cat1.off('click');
                    //<--MoonSUB

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
					sk = this.sketchness;
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
                            write.players(sk.players,sketchness.myself);
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
				{ name: "nextRound", from: ["imageViewing", "taskDrawing",  "tagInsertion", "tagWait"], to: "waitRole"},
				{ name: "skipRound", from: ["taskGuessing", "taskDrawing", "tagInsertion", "tagWait"], to: "waitRole" },
				{ name: "tag", from: "Sketcher", to: "tagInsertion" },
				{ name: "tag", from: "Guesser", to: "tagWait" },
				{ name: "task", from: ["Sketcher", "tagInsertion"], to: "taskDrawing" },
				{ name: "task", from: ["Guesser", "tagWait"], to: "taskGuessing" },
				{ name: "endRound", from: ["taskGuessing", "taskDrawing"], to: "imageViewing" },
				{ name: "quit", from: ["imageViewing", "tagInsertion", "taskDrawing", "taskGuessing", "tagWait", "waitRole", "loading", "Sketcher", "Guesser"], to: "leaderboard" },
				{ name: "errorEvent", from: ["imageViewing", "tagInsertion", "taskDrawing", "taskGuessing", "tagWait", "waitRole", "loading", "Sketcher", "Guesser", "playersWait"], to: "handleError" },
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


