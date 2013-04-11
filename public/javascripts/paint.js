(function(){

  //Defining the websocket type based on the browser
  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket


  // Polyfills
  window.requestAnimFrame = (function(){
  return window.requestAnimationFrame ||
          window.webkitRequestAnimationFrame ||
          window.mozRequestAnimationFrame ||
          window.oRequestAnimationFrame ||
          window.msRequestAnimationFrame ||
          function( callback ){
            window.setTimeout(callback, 1000 / 60);
          };
          })();

  var insideIframe = (window.parent != window);
  var isMobile = /ipad|iphone|android/i.test(navigator.userAgent)

  // CONSTANTS
  var PEN = 'red';
  var ERASER = "rgba(255,255,255,1.0)";
  var TRACKER = 'red';
  var SIZE = 5;
  var MIN_SEND_RATE = 50; // the min interval in ms between 2 send
  
  // STATES
  var pid;
  var pname;
  var meCtx;
  var role;
  
  // TOOLS STATUS
  var color = PEN;
  var size = SIZE;
  
  //GAME STATUS
  var score = 0;
  var defaultRoundTime=90;
  var time = defaultRoundTime;
  var matchStarted = false;
  var guessWord="";
  var guessed=false;
  var drawTool = true;
  var taskImage;
  
  var trackX;
  var trackY;

  var numTrace = 1;
  var onSocketMessage;
  var dirtyPositions = false;

  // every player positions
  var players = new Array();

  var viewport = document.getElementById('viewport');
  
  var RemainingSeconds;
  
  
  function setError (message) {
      $("#onError span").text(message)
      $("#onError").show()
      $("#pageheader").hide()
      $("#mainPage").hide()
  }
    
    
    /**********UTILITY FUNCTION FOR TIMER CREATION********************/

    function TimerTick()
    {
        if (RemainingSeconds == 0) {
           //Round End
              send({type: 'roundEnded', player: pname});
              RemainingSeconds='waiting';
        }
        else {
            RemainingSeconds -= 1;
            UpdateTimer();
            window.setTimeout(TimerTick, 1000);
        }
    }

    function CreateTimer(Countdown)
    {
        RemainingSeconds = Countdown;
        UpdateTimer();
        window.setTimeout(TimerTick, 1000);
    }


    //Timing Functions
    function UpdateTimer()
    {
        time=RemainingSeconds;
    }
  
  /*****************************UTILITY FUNCTIONS********************************************/

  // Init pname
  function queryPname() {
   n=$('#currentNickname').text();
   if (n) {
      localStorage.setItem("pname", n);
	  pname=n;
    }
    return n || pname;
  }
  
 //pname = localStorage.getItem("pname");
  if (!insideIframe && !pname) {
     pname = queryPname();
  }
  if (!pname) {
    pname = "iam"+Math.floor(100*Math.random())
}


  // WebSocket
  var connected = false;


  function connect () {
    try {
	  var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket
	  socket = new WS($('#paintWebSocket').data('ws'));
      socket.onmessage = onSocketMessage;
	  queryPname();

      socket.onopen = function(evt) {
        connected = true;
        send({type: 'change', size: size, color: color, name: pname});
      }

      socket.onclose = function (evt) {
            connected = false;
			setError("Connection lost");
      };

      socket.onerror = function(evt) {console.error("error", evt);};
    }
    catch (e) {
      setError("WebSocket connection failed.");
    }
  }

  function send (o) {
    if (!connected) return;
    socket.send(JSON.stringify(o));
  }
  
  function getPlayer(username)
  {
	for (i=0;i<players.length;i++)
	{
		if(players[i].name.toLowerCase()==username.toLowerCase())
		{
			return players[i];
		}
	}
  }
  
  function deletePlayer(username)
  {
	for (i=0;i<players.length;i++)
	{
		if(players[i].name.toLowerCase()==username.toLowerCase())
		{
			delete players[i];
			return;
		}
	}
  }
  
  function playerExtend(message)
  {
	var username=message.name;
    for (i=0;i<players.length;i++)
	{
		if(players[i].name.toLowerCase()==username.toLowerCase())
		{
	          players[i]=$.extend(players[i], message);
		}
	}
  }
  




//***************************TAKING CARE OF THE LINE STROKES*****************************/
(function(){
  var canvas = document.getElementById("draws");
  var ctx = canvas.getContext("2d");
  var taskCanvas = document.getElementById("task");
  var taskContext = taskCanvas.getContext("2d");
  var positionCanvas = document.getElementById("positions");
  var positionContext = positionCanvas.getContext("2d");
  /*******************************MANAGING THE INCOMING MESSAGES*****************/
  onSocketMessage = function (e) {
    var m = JSON.parse(e.data);
	switch(m.type)
	{
		case "role":
					//Fix the drawing style for the player
					ctx.globalCompositeOperation = "destination-out";
					guessed=false;
					role=m.role;
					matchStarted=true;
					var player=getPlayer(pname);
					player.role=m.role;
					send({type: 'change', size: size, color: color, name: pname, role: role});
					if(role=="SKETCHER")
					{
						$('#roleSpan').text($.i18n.prop('sketcher'));
						$('#mainPage').removeClass('guesser');
						$('#mainPage').addClass('sketcher');
						$('#talk').attr('disabled', 'disabled');

					}
					else
					{
						$('#roleSpan').text($.i18n.prop('guesser'));
						$('#mainPage').removeClass('sketcher');
						$('#mainPage').addClass('guesser');
						$('#talk').removeAttr('disabled');

					}
					CreateTimer(defaultRoundTime);
					ctx.clearRect(0, 0, canvas.width, canvas.height);
					taskContext.clearRect(0, 0, canvas.width, canvas.height);
					positionContext.clearRect(0, 0, canvas.width, canvas.height);
			break;
			
		case "move"://We need to update the current position of the player
					trackX=m.x;
					trackY=m.y;
			break;
			
		case "task"://Send the current task (image + tag) to the player
					guessWord=m.word;
					taskImage=new Image();
					taskImage.src=m.image;
					//Wait for the image to be loaded before drawing it to canvas to avoid
					//errors
					taskImage.onload = function() {
							taskContext.save();
							taskContext.beginPath();
							x=0;
							y=0;
							var style = window.getComputedStyle(taskCanvas);
							var width=style.getPropertyValue('width');
							width= parseInt(width, 10);
							var height=style.getPropertyValue('height');
							height=parseInt(height, 10);
							radius=50;
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
							taskContext.drawImage(taskImage,0,0,width,height);
							taskContext.restore();
					};
			break;
			
		case "guesser":
					if(m.name==pname)
					{
						score=score+m.points;
						guessed=true;
					}
			break;
			
		case "sketcher":
					if(m.name==pname)
						score=score+m.points;
			break;
			
		case "timeChange":
					if(RemainingSeconds>m.amount)
						RemainingSeconds=m.amount;
			break;
			
		case "showImages":
					
					//We want to save the image that has been created by the sketcher
					if(role=="SKETCHER")
					{
						//Save the drawings done by the player
						var dataURL = canvas.toDataURL('image/png');
						send({type: 'segment', src: taskImage.src, image: dataURL, name: pname});
					}
					role="ROUNDCHANGE";
					CreateTimer(m.seconds);
			break;
		
		case "leaderboard":
					role="ENDED";
					//Clear all the canvas and draw the leaderboard
					time=0;
					ctx.clearRect(0, 0, canvas.width, canvas.height);
					taskContext.clearRect(0, 0, canvas.width, canvas.height);
					positionContext.clearRect(0, 0, canvas.width, canvas.height);
			   
					//Disable the chat
					$('#talk').attr('disabled', 'disabled');
					for(var i=1;i<=m.playersNumber;i++)
					{
						taskContext.fillStyle = "#000000";
						taskContext.font = "bold 30px sans-serif";
						taskContext.fillText(i+"): "+m.playerList[i-1].name+" = "+m.playerList[i-1].points+$.i18n.prop('points'),30,50+50*(i-1));
					}
			break;
		
		case "trace":
					player = getPlayer(m.name);
					if(player.color==ERASER)
						ctx.globalCompositeOperation = "destination-out";
					else
					   ctx.globalCompositeOperation = "source-over";
					ctx.lineWidth = player.size;
					ctx.beginPath();
						var points = m.points;
						points[0] && ctx.moveTo(points[0].x, points[0].y);
						points.forEach(function (p) {
						ctx.strokeStyle=p.color;
						ctx.lineTo(p.x, p.y);
						});
					ctx.stroke();
					
					trackX = points[points.length-1].x;
					trackY = points[points.length-1].y;
					
					// clear local canvas if synchronized
					if (m.name==pname && numTrace == m.num) {
					  meCtx.clearRect(0,0,meCtx.canvas.width,meCtx.canvas.height);
					}
			break;
			
		case "change":
				var player = getPlayer(m.name);
				if (player === undefined)
				{
					player = players[players.length] = m;
					console.log(player);
				}
				playerExtend(m);
			break;
			
		case "youAre":
				role = m.role;
			break;
		
		case "disconnect": //[TODO]
			deletePlayer(m.username);
			break;
		
	}
	dirtyPositions = true;
  }

  
  var w = canvas.width, h = canvas.height;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

})();





/*************************DRAWING PANEL**********************/
// The "me" canvas is where the sketcher draws before sending the update status to all the other players
(function(){
  var canvas = document.getElementById("me");
  var ctx = meCtx = canvas.getContext("2d");
  var pressed;
  var position;

  var points = [];
  var lastSent = 0;

  //Add the current point to the list of points to be sent
  function addPoint (x, y, size, color) {
    points.push({x: x, y: y, size: size, color: color});
  }
  
  //Send the points to the server as a trace message. It sends the points, the number of the trace sent, the name of the player that has sent the trace
  function sendPoints () {
    lastSent = +new Date(); //Refresh the countdown timer
    send({type: "trace", points: points, num: numTrace, name: pname});
    points = [];
  }
  
  //Send the tracking position of the player that is drawing, sending his current position and name
  function sendMove (x, y) {
    lastSent = +new Date();
    send({type: "move", x: x, y: y, name: pname});
  }

  //Can we send? If the current time - the last update is bigger than the treshold, we can send the packets
  function canSendNow () {
    return +new Date() - lastSent > MIN_SEND_RATE;
  }

  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

   //Create a line from the starting point to the end point
  function lineTo(x, y) {
      ctx.strokeStyle = color;
      ctx.lineWidth = size;
      ctx.beginPath();
      ctx.moveTo(position.x, position.y);
      ctx.lineTo(x, y);
      ctx.stroke();
  }

  //Handle the mouse movement when drawing
  function onMouseMove (e) {
    e.preventDefault();
	//Get the current position with respect to the canvas element we want to draw to
    var o = positionWithE(e,canvas);
	//If the mouse is pressed and the player is a sketcher
    if (pressed && role=="SKETCHER") {
	  //Draw the local line
      lineTo(o.x, o.y);
	  //Add the points to the points to be sent
      addPoint(o.x, o.y, size, color);
	  //We have created a trace
      ++ numTrace;
	  //Can we send the batch of points?
      if (canSendNow()) {
        sendPoints();
		sendMove(o.x, o.y);
        addPoint(o.x, o.y, size, color);
      }
    }
    else 
	{
	  //The mouse is not pressed, can we just send the position of the player?
      if (canSendNow() && role=="SKETCHER") {
        sendMove(o.x, o.y);
      }
    }
    position = o;
  }

  function onMouseDown (e) {
	//If the player is a sketcher, update the mouse pressed status to send his traces
    if(role=="SKETCHER")
        {
            var o = positionWithE(e,canvas);
            position = o;
            addPoint(o.x, o.y, size, color);
            pressed = true;
        }
  }

  function onMouseUp (e) {
    //If the player is the sketcher, send the last trace and disable the drawing function
    if(role=="SKETCHER")
        {
            lineTo(position.x, position.y);
            addPoint(position.x, position.y, size, color);
            sendPoints();
            pressed = false;
        }
  }


  //Add the event listeners to handle movements, mouse up and mouse down, eventually supporting also mobile devices
  document.addEventListener(isMobile ? "touchstart": "mousedown", onMouseDown);
  document.addEventListener(isMobile ? "touchend" : "mouseup", onMouseUp);
  viewport.addEventListener(isMobile ? "touchmove" : "mousemove", onMouseMove);

})();


//Return the current position of the cursor within the specified element
function positionWithE (e,obj) {
var leftm=topm=0;
var result= getPosition(obj);
        return {
          x: e.clientX-result.x,
          y: e.clientY-result.y
        };
  };
  
  //Return the position of the element in the page
  function getPosition(element) {
    var xPosition = 0;
    var yPosition = 0;
  
    while(element) {
        xPosition += (element.offsetLeft - element.scrollLeft + element.clientLeft);
        yPosition += (element.offsetTop - element.scrollTop + element.clientTop);
        element = element.offsetParent;
    }
    return { x: xPosition, y: yPosition };
};


/*************************DRAW PLAYERS POSITION**********************/

(function(){
  var canvas = document.getElementById("positions");
  var ctx = canvas.getContext("2d");

  ctx.font = "9px monospace";
  ctx.textAlign = "center";
  ctx.textBaseline = "bottom";
  function render ()
  {
    if(!dirtyPositions) return;
    dirtyPositions = false;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    if(matchStarted)
        for (i=0;i<players.length;i++)
        {
            var player = players[i];
            if (player.role!="SKETCHER") continue;
            ctx.beginPath();
            ctx.strokeStyle = TRACKER;
            ctx.arc(trackX, trackY, player.size/2, 0, 2*Math.PI);
            ctx.stroke();
            ctx.font = "10px sans-serif";
            ctx.fillStyle = TRACKER;
            ctx.fillText((player.name+"").substring(0,20), trackX, trackY-Math.round(player.size/2)-4);
        }
  }

  requestAnimFrame(function loop () {
    requestAnimFrame(loop);
    render();
  }, canvas);
})();




/*************************TOOLS PANEL MANAGEMENT**********************/
(function(){
  var hud = document.getElementById("hud");
  var ctx = hud.getContext("2d");
  
  //ICONS
  var penEnabled= new Image();
  var eraserEnabled = new Image();
  var penDisabled= new Image();
  var eraserDisabled = new Image();
  penEnabled.src = 'assets/images/UI/Controls/pencil.png';
  eraserEnabled.src = 'assets/images/UI/Controls/eraser.png';
  penDisabled.src = 'assets/images/UI/Controls/pencilD.png';
  eraserDisabled.src = 'assets/images/UI/Controls/eraserD.png';
  
  function setColor (c)
  {
    color = c;
    send({type: 'change', size: size, color: color, name: pname, role: role});
  }
  
  function setSize (s) {
    size = s;
    send({type: 'change', size: size, color: color, name: pname, role: role});
  }

  function setup() {
  
	//[TODO] It should be screen and resolution independent, it can't work like that
    hud.addEventListener("click", function (e) {
    var o = positionWithE(e,hud);
      if ((o.y>=130)&&(o.y<200))
      {
        setColor(PEN);
		setSize(SIZE);
		drawTool=true;
      }
      else if ((o.y>=200)&&(o.y<=270))
      {
		setColor(ERASER);
		setSize(25);
		drawTool=false;
      }
    });
  }

  
  /************TOOLS PANEL RENDERING***********************/
  function render()
  {
	ctx.clearRect(0, 0, hud.width, hud.height);
    if(!matchStarted)
    {
		var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('waiting')+"<b>"+"</font>";
		document.getElementById('topMessage').innerHTML=htmlMessage;
    }
    else
    {
		switch(role)
		{
			case "SKETCHER":
						var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('draw')+"<font color='red'>"+guessWord+"</font>"+"<b>"+"</font>";
						document.getElementById('topMessage').innerHTML=htmlMessage;
				break;
				
			case "GUESSER":
						if(!guessed)
						{
							var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('guess')+"<b>"+"</font>";
							document.getElementById('topMessage').innerHTML=htmlMessage;
						}
						else
						{
							var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('guessed')+"<font color='red'>"+guessWord+"</font>"+"<b>"+"</font>";
							document.getElementById('topMessage').innerHTML=htmlMessage;
						}
				break;
				
			case "ROUNDCHANGE":
						var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('solution')+"<font color='red'>"+guessWord+"</font>"+"<b>"+"</font>";
						document.getElementById('topMessage').innerHTML=htmlMessage;
				break;
				
			case "ENDED":
						var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('leaderboard')+"<b>"+"</font>";
						document.getElementById('topMessage').innerHTML=htmlMessage;
						$('#mainPage').removeClass('guesser');
						$('#mainPage').removeClass('sketcher');
				break;
				
		}
    }

	var htmlMessage="<font size='5'>"+"<b>"+score+"<b>"+"</font>";
	document.getElementById('score').innerHTML=htmlMessage;

	//Time positioning
	if(matchStarted)
	{
		var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('time')+time+"<b>"+"</font>";
		document.getElementById('timeCounter').innerHTML=htmlMessage;
	}
    if(!matchStarted || role=="GUESSER" || role=="ENDED" || role=="ROUNDCHANGE")
    {
              //Draw nothing till now
    }
    else
    {
		//Drawing tools
		if(drawTool)
		{
			ctx.drawImage(penEnabled,0,130,70,70);
			ctx.drawImage(eraserDisabled,0,200,70,70);
		}
		else
		{
			ctx.drawImage(penDisabled,0,130,70,70);
			ctx.drawImage(eraserEnabled,0,200,70,70);
		}
	}
  }

  setup();
  requestAnimFrame(function loop(){
    requestAnimFrame(loop);
    render();
  });

})();

  connect();
})();