(function(){

  


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
    if(m.type=="role")
    {
        //Fix the drawing style for the player
        ctx.globalCompositeOperation = "destination-out";
        guessed=false;
        role=m.role;
        matchStarted=true;
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
    }
    //Managing the task to provide to the users
    else if(m.type=="task")
    {
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
    }
    else if(m.type=="guesser")
    {
        if(m.name==pname)
        {
            score=score+m.points;
            guessed=true;
        }
    }
    
    else if(m.type=="sketcher")
    {
        if(m.name==pname)
            score=score+m.points;
    }
    
    else if(m.type=="timeChange")
    {
        if(RemainingSeconds>m.amount)
            RemainingSeconds=m.amount;
    }

else if(m.type=="showImages")
{
role="ROUNDCHANGE";
CreateTimer(m.seconds);
}
    
    else if(m.type=="leaderboard")
    {
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
    }
    
    else if (m.type != "disconnect") {
      if (m.type == "trace")
      {
	    player = getPlayer(m.name);
        ctx.strokeStyle = player.color;
        if(player.color=="rgba(255,255,255,1.0)")
            ctx.globalCompositeOperation = "destination-out";
        else
           ctx.globalCompositeOperation = "source-over";
        ctx.lineWidth = player.size;
        ctx.beginPath();
            var points = m.points;
            points[0] && ctx.moveTo(points[0].x, points[0].y);
            points.forEach(function (p) {
            ctx.lineTo(p.x, p.y);
            });
        ctx.stroke();
        player.x = m.x = points[points.length-1].x;
        player.y = m.y = points[points.length-1].y;

        // clear local canvas if synchronized
        if (m.name==pname && numTrace == m.num) {
          meCtx.clearRect(0,0,meCtx.canvas.width,meCtx.canvas.height);
        }
      }
	else if(m.type=="change")
    {
        var player = getPlayer(m.name);
        if (player === undefined)
        {
            player = players[players.length] = m;
			console.log(player);
        }
        playerExtend(m);
    }
	else if (m.type=="youAre")
        {
            role = m.role;
        }
	else {
	  //TODO CHANGE TO NOT WORK WITH PID
      //delete players[m.pid];
    }
	}
	}
	  
    
    

    dirtyPositions = true;

  var w = canvas.width, h = canvas.height;
  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

})();






// "me" canvas is where you draw before the painter sends your own events (before synchronization)
(function(){
  var canvas = document.getElementById("me");
  var ctx = meCtx = canvas.getContext("2d");
  var pressed;
  var position;

  var points = [];
  var lastSent = 0;

  function addPoint (x, y) {
    points.push({x: x, y: y});
  }
  function sendPoints () {
    lastSent = +new Date();
    send({type: "trace", points: points, num: numTrace, name: pname});
    points = [];
  }
  function sendMove (x, y) {
    lastSent = +new Date();
    send({type: "move", x: x, y: y});
  }

  function canSendNow () {
    return +new Date() - lastSent > MIN_SEND_RATE;
  }

  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';


  function lineTo(x, y) {
      ctx.strokeStyle = color;
      ctx.lineWidth = size;
      ctx.beginPath();
      ctx.moveTo(position.x, position.y);
      ctx.lineTo(x, y);
      ctx.stroke();
  }

  function onMouseMove (e) {
    e.preventDefault();
    var o = positionWithE(e,canvas);
    if (pressed && role=="SKETCHER") {
      lineTo(o.x, o.y);
      addPoint(o.x, o.y);
      ++ numTrace;
      if (canSendNow()) {
        sendPoints();
        addPoint(o.x, o.y);
      }
    }
    else {
      if (canSendNow() && role=="SKETCHER") {
        sendMove(o.x, o.y);
      }
    }
    position = o;
  }

  function onMouseDown (e) {
    if(role=="SKETCHER")
        {
            var o = positionWithE(e,canvas);
            position = o;
            addPoint(o.x, o.y);
            pressed = true;
        }
  }

  function onMouseUp (e) {
    if(role=="SKETCHER")
        {
            lineTo(position.x, position.y);
            addPoint(position.x, position.y);
            sendPoints();
            pressed = false;
        }
  }



  document.addEventListener(isMobile ? "touchstart": "mousedown", onMouseDown);
  document.addEventListener(isMobile ? "touchend" : "mouseup", onMouseUp);
  viewport.addEventListener(isMobile ? "touchmove" : "mousemove", onMouseMove);

})();

function positionWithE (e,obj) {
var leftm=topm=0;
var result= getPosition(obj);
        return {
          x: e.clientX-result.x,
          y: e.clientY-result.y
        };
  };
  
  
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
            if (!player || player.x===undefined) continue;
            ctx.beginPath();
            ctx.strokeStyle = TRACKER;
            ctx.arc(player.x, player.y, player.size/2, 0, 2*Math.PI);
            ctx.stroke();
            ctx.font = "10px sans-serif";
            ctx.fillStyle = TRACKER;
            ctx.fillText((player.name+"").substring(0,20), player.x, player.y-Math.round(player.size/2)-4);
            players[i].x=-200;
            players[i].y=-200;
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
    send({type: 'change', size: size, color: color, name: pname});
  }
  
  function setSize (s) {
    size = s;
    send({type: 'change', size: size, color: color, name: pname});
  }

  function setup() {
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
        if(role=="SKETCHER")
        {
var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('draw')+"<font color='red'>"+guessWord+"</font>"+"<b>"+"</font>";
document.getElementById('topMessage').innerHTML=htmlMessage;
        }
        else if (role=="GUESSER")
        {
            ctx.fillStyle = "#000000";
ctx.lineStyle="#ffff00";
ctx.font = "normal 20px Verdana";
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
        }
else if (role=="ROUNDCHANGE")
{
var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('solution')+"<font color='red'>"+guessWord+"</font>"+"<b>"+"</font>";
document.getElementById('topMessage').innerHTML=htmlMessage;
}
        else if (role=="ENDED")
        {
var htmlMessage="<font size='5'>"+"<b>"+$.i18n.prop('leaderboard')+"<b>"+"</font>";
document.getElementById('topMessage').innerHTML=htmlMessage;
        }
    }

var htmlMessage="<font size='5'>"+"<b>"+score+"<b>"+"</font>";
document.getElementById('score').innerHTML=htmlMessage;

//Time positioning
if(matchStarted)
{
//ctx.fillText(time,35,50);
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