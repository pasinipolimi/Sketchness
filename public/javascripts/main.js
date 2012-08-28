(function(){
	
  function removeError () {
    $('#error').fadeOut(500);
  }
  function setError (message) {
    $('#error').empty().append($('<span class="error" />').text(message)).fadeIn(500);
  }

  // Polyfills
  window.requestAnimFrame = (function(){
  return  window.requestAnimationFrame       || 
          window.webkitRequestAnimationFrame || 
          window.mozRequestAnimationFrame    || 
          window.oRequestAnimationFrame      || 
          window.msRequestAnimationFrame     || 
          function( callback ){
            window.setTimeout(callback, 1000 / 60);
          };
          })();
		  

		  
  

  if (!window.WebSocket) {
    if (window.MozWebSocket)
      window.WebSocket = window.MozWebSocket
  }

  if (!window.WebSocket) {
    setError("WebSocket is not supported by your browser.");
    return;
  }

  if (!(function(e){ return e.getContext && e.getContext('2d') }(document.getElementById("me")))) {
    setError("Canvas is not supported by your browser.");
    return;
  }

  var insideIframe = (window.parent != window);
  var isMobile = /ipad|iphone|android/i.test(navigator.userAgent)

  // CONSTANTS
  var PEN = 'red';
  var ERASER = 'white';
  var SIZE = 5;
  var MIN_SEND_RATE = 50; // the min interval in ms between 2 send

  // STATES
  var color = PEN;
  var size = SIZE;
  var pid;
  var pname;
  var meCtx;
  
  //GAME STATUS
  var score = 0;
  var time = 90;

  var numTrace = 1;
  var onSocketMessage;
  var dirty_positions = false;

  // every player positions
  var players = {};

  var viewport = document.getElementById('viewport');
  
  
  
  
  
  /*****************************UTILITY FUNCTIONS********************************************/
  CanvasRenderingContext2D.prototype.roundRect = function(x, y, width, height, radius, fill, stroke) {
    if (typeof stroke == "undefined") {
        stroke = true;
    }
    if (typeof radius === "undefined") {
        radius = 5;
    }
    this.beginPath();
    this.moveTo(x + radius, y);
    this.lineTo(x + width - radius, y);
    this.quadraticCurveTo(x + width, y, x + width, y + radius);
    this.lineTo(x + width, y + height - radius);
    this.quadraticCurveTo(x + width, y + height, x + width - radius, y + height);
    this.lineTo(x + radius, y + height);
    this.quadraticCurveTo(x, y + height, x, y + height - radius);
    this.lineTo(x, y + radius);
    this.quadraticCurveTo(x, y, x + radius, y);
    this.closePath();
    if (stroke) {
        this.stroke(stroke);
    }
    if (fill) {
        this.fill(fill);
    }
  };

  // Init pname
  function queryPname() {
	n=$('#nickname').text();
    if (n) {
      localStorage.setItem("pname", n);
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
  var socket;
  var connected = false;

  var reconnectInterval = 1000;
  var reconnection = false;

  function tryConnectAgain () {
    reconnection = true;
    setError("WebSocket connection is down. reconnecting...");
    setTimeout(function() {
      reconnectInterval *= (1.5+0.2*Math.random());
      connect();
    }, reconnectInterval);
  }

  function connect () {
    try {
      socket = new WebSocket("ws://"+location.host+"/stream");
      socket.onmessage = onSocketMessage;
	  
      socket.onopen = function(evt) {
        if (reconnection) {
          window.location = window.location; // Reloading the page to reset states
          return;
        }
        connected = true;
        send({ type: 'change', size: size, color: color, name: pname });
      }
	  
      socket.onclose = function(evt) { 
        connected = false;
        tryConnectAgain();
      };
	  
      socket.onerror = function(evt) { console.error("error", evt); };
    }
    catch (e) {
      setError("WebSocket connection failed.");
    }
  }

  function send (o) {
    if (!connected) return;
    socket.send(JSON.stringify(o));
  }


(function(){
  var canvas = document.getElementById("draws");
  var ctx = canvas.getContext("2d");

  onSocketMessage = function (e) {
    var m = JSON.parse(e.data);
    var player = players[m.pid];
    if (player === undefined) {
      player = players[m.pid] = m;
    }
    if (m.type=="youAre") pid = m.pid;
    
    if (m.type != "disconnect") {
      if (m.type == "trace") {
        ctx.strokeStyle = player.color;
        ctx.lineWidth = player.size;
        ctx.beginPath();
        var points = m.points;
        points[0] && ctx.moveTo(points[0].x, points[0].y);
        points.forEach(function (p) {
          ctx.lineTo(p.x, p.y);
        });
        ctx.stroke();
        m.x = points[points.length-1].x;
        m.y = points[points.length-1].y;

        // clear local canvas if synchronized
        if (m.pid==pid && numTrace == m.num) {
          meCtx.clearRect(0,0,meCtx.canvas.width,meCtx.canvas.height);
        }
      }

      players[m.pid] = $.extend(players[m.pid], m);
    }
    else {
      delete players[m.pid];
    }

    dirty_positions = true;
  }

  var w = canvas.width, h = canvas.height;
  ctx.fillStyle = 'white';
  ctx.fillRect(0, 0, w, h);
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
    points.push({ x: x, y: y });
  }
  function sendPoints () {
    lastSent = +new Date();
    send({ type: "trace", points: points, num: numTrace });
    points = [];
  }
  function sendMove (x, y) {
    lastSent = +new Date();
    send({ type: "move", x: x, y: y });
  }

  function canSendNow () {
    return +new Date() - lastSent > MIN_SEND_RATE;
  }

  ctx.lineCap = 'round';
  ctx.lineJoin = 'round';

  function positionWithE (e) {
    var o = $(canvas).offset();
    var x = e.clientX, y = e.clientY;
    if (e.touches) {
      var touch = e.touches[0];
      if (touch) {
        x = touch.pageX;
        y = touch.pageY;
      }
    }
    return { x: x-(o.left-$(window).scrollLeft()), y: y-(o.top-$(window).scrollTop()) };
  }

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
    var o = positionWithE(e);
    if (pressed) {
      lineTo(o.x, o.y);
      addPoint(o.x, o.y);
      ++ numTrace;
      if (canSendNow()) {
        sendPoints();
        addPoint(o.x, o.y);
      }
    }
    else {
      if (canSendNow()) {
        sendMove(o.x, o.y);
      }
    }
    position = o;
  }

  function onMouseDown (e) {
    //e.preventDefault();
    var o = positionWithE(e);
    position = o;
    addPoint(o.x, o.y);
    pressed = true;
  }

  function onMouseUp (e) {
    //e.preventDefault();
    lineTo(position.x, position.y);
    addPoint(position.x, position.y);
    sendPoints();
    pressed = false;
  }



  document.addEventListener(isMobile ? "touchstart": "mousedown", onMouseDown);
  document.addEventListener(isMobile ? "touchend"  : "mouseup",   onMouseUp);
  viewport.addEventListener(isMobile ? "touchmove" : "mousemove", onMouseMove);

})();

(function(){
  var canvas = document.getElementById("positions");
  var ctx = canvas.getContext("2d");

  ctx.font = "9px monospace";
  ctx.textAlign = "center";
  ctx.textBaseline = "bottom";
  function render () {
    if(!dirty_positions) return;
    dirty_positions = false;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    for (var pid in players) { var player = players[pid];
      if (!player || player.x===undefined) continue;
      ctx.beginPath();
      ctx.strokeStyle = player.color;
      ctx.arc(player.x, player.y, player.size/2, 0, 2*Math.PI);
      ctx.stroke();
      ctx.fillStyle = player.color;
      ctx.fillText((player.name+"").substring(0,20), player.x, player.y-Math.round(player.size/2)-4);
    }
  }

  requestAnimFrame(function loop () {
    requestAnimFrame(loop);
    render();
  }, canvas);
})();

(function(){
  var canvas = document.getElementById("controls");
  var ctx = canvas.getContext("2d");
  var drawTool = true;
  var dirty = true;
  
  //ICONS
  var penEnabled = new Image();
  var penDisabled = new Image();
  var eraserEnabled = new Image();
  var eraserDisabled = new Image();
  penEnabled.src = 'assets/images/Pen-2-icon.jpg';
  eraserEnabled.src = 'assets/images/Eraser-icon.jpg';
  penDisabled.src='assets/images/Pen-2-iconWhite.jpg';
  eraserDisabled.src='assets/images/Eraser-iconWhite.jpg';
  
  var BUTTON = 40;
  var RADIUS = 10;
  var SELECT = 4;

  var COLOR_PREV = 37, SIZE_UP = 38, COLOR_NEXT = 39, SIZE_DOWN = 40;

  function setColor (c) {
    color = c;
    send({ type: 'change', color: color });
  }
  function setSize (s) {
    size = s;
    send({ type: 'change', size: size });
  }

  function setup() {
    canvas.addEventListener("click", function (e) {
      var o = $(canvas).offset();
      var p = { x: e.clientX-o.left, y: e.clientY-o.top };
      if ((p.y>=245)&&(p.y<345)) 
	  {
        setColor(PEN);
		setSize(SIZE);
		drawTool=true;
      }
	  else if ((p.y>=345)&&(p.y<=450))
	  {
		setColor(ERASER);
		setSize(25);
		drawTool=false;
	  }
      dirty = true;
    });
  }

  function render() 
  {
	
	
	//Score rectangle
	ctx.strokeStyle = "#abc";
	ctx.fillStyle = "#abc";
	ctx.roundRect(5, 5, 80, 90, 5, true);
	ctx.font = "bold 18px sans-serif";
	ctx.fillStyle = "#000000";
	ctx.fillRect(10,15,70,25);
	ctx.fillStyle = "#FFFFFF";
	ctx.fillText("Score", 20, 33);
	ctx.fillStyle = "#000000";
	ctx.font = "bold 30 px sans-serif";
	ctx.fillText(score,40,70);
	
	//Time rectangle
	ctx.strokeStyle = "#abc";
	ctx.fillStyle = "#abc";
	ctx.roundRect(5, 110, 80, 90, 5, true);
	ctx.font = "bold 18px sans-serif";
	ctx.fillStyle = "#000000";
	ctx.fillRect(10,120,70,25);
	ctx.fillStyle = "#FFFFFF";
	ctx.fillText("Time", 20, 138);
	ctx.fillStyle = "#000000";
	ctx.font = "bold 30 px sans-serif";
	ctx.fillText(time,35,175);
	
	//Drawing tools
	if(drawTool)
	{
		ctx.drawImage(penEnabled,0,250,90,90);
		ctx.drawImage(eraserDisabled,0,350,90,90);
	}
	else
	{
		ctx.drawImage(penDisabled,0,250,90,90);
		ctx.drawImage(eraserEnabled,0,350,90,90);
	}
  }
  /*function render() {
    if (!dirty) return;
    dirty = false;
    var w = canvas.width, h = canvas.height;
    var x, y, radius;

    ctx.fillStyle = 'white';
    ctx.fillRect(0, 0, w, h);

    // draw colors
    x = BUTTON/2, y = h/2, radius = RADIUS;
    COLORS.forEach(function(c) {
      ctx.fillStyle = c;
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, 2*Math.PI);
      ctx.fill();
      ctx.lineWidth = 1;
      ctx.strokeStyle = 'rgba(0,0,0,0.5)';
      ctx.stroke();
      if (c == color) {
        ctx.lineWidth = 2;
        ctx.strokeStyle = 'black';
        ctx.beginPath();
        ctx.arc(x, y, radius+SELECT, 0, 2*Math.PI);
        ctx.stroke();
      }
      x += BUTTON;
    });

    // draw sizes
    ctx.fillStyle = 'black';
    SIZES.forEach(function(s) {
      ctx.beginPath();
      ctx.arc(x, y, s, 0, 2*Math.PI);
      ctx.fill();
      if (s == size) {
        ctx.lineWidth = 2;
        ctx.strokeStyle = 'black';
        ctx.beginPath();
        ctx.arc(x, y, s+SELECT, 0, 2*Math.PI);
        ctx.stroke();
      }
      x += BUTTON;
    });
  }*/

  setup();
  requestAnimFrame(function loop(){
    requestAnimFrame(loop);
    render();
  });

})();

  connect();
})();
