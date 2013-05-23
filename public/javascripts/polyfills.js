// TODO: chose which functionality support and maybe use modernizr for tests

if (!window.requestAnimationFrame) {
	window.requestAnimationFrame = window.webkitRequestAnimationFrame ||
		window.mozRequestAnimationFrame ||
		window.oRequestAnimationFrame ||
		window.msRequestAnimationFrame ||
		function (callback) {
			window.setTimeout(callback, 1000 / 60);
		};
}

if(!window.WebSocket && window.MozWebSocket) {
	window.WebSocket = window.MozWebSocket;
}

if(false) { // TODO: I'm not sure if it's useful, neither where to place it..think about it!
	var typeOf = function(obj) {
		if (obj === null) return "null";
		else {
			var type = typeof(obj);
			if (type != "object") return type;
			else {
				var toString = Object.prototype.toString;
				var str = toString.apply(obj);
				if(str == "[object String]") return "string";
				else if (str == "[object Array]") return "array";
				else if (str == "[object Date]") return "date";
				else if (obj instanceof RegExp) return "regexp";
				else return "object";
			}
		}
	};
}
