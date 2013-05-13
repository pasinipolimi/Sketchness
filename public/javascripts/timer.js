function TimerObj(){

    this.createCountdown = createCountdown;
    this.retrieveCountdown = retrieveCountdown;
    this.changeCountdown = changeCountdown;
    this.createTimer = createTimer;
    this.retrieveTimer = retrieveTimer;
	this.deleteTimer = deleteTimer;
	this.deleteCountdown = deleteCountdown;

    var time = 0,
    seconds = 0.0, 
    timers = {};



    function clock(){  
        time += 100;  
        seconds = Math.floor(time / 100) / 10;    
        setTimeout(clock, (100));  
    };
	setTimeout(clock, 100);



    function executeFunctionByName(functionName){
        var args = Array.prototype.slice.call(arguments).splice(1);
        //debug
        console.log('args:', args);

        var namespaces = functionName.split(".");
        //debug
        console.log('namespaces:', namespaces);

        var func = namespaces.pop();
        //debug
        console.log('func:', func);

        ns = namespaces.join('.');
        //debug
        console.log('namespace:', ns);

        if(ns === '')
        {
            ns = 'window';
        }

        ns = eval(ns);
        //debug
        console.log('evaled namespace:', ns);

        return ns[func].apply(ns, args);
    };


    function createCountdown (name, secondsPar, triggerEvent){
      timers[name+'Countdown'] = secondsPar;
      timers[name+'Countdownstart'] = seconds;
      setTimeout(function() {
        countdownTick.call(this,name,triggerEvent);
      }, 1000);
    };

    countdownTick = function (name,triggerEvent){
	   if(timers[name+'Countdown']!=null){
		   timers[name+'Countdown'] = Math.floor(timers[name+'Countdown'] - (seconds-timers[name+'Countdownstart']));
		   timers[name+'Countdownstart'] = seconds;
		   if(timers[name+'Countdown']<=0)
						executeFunctionByName(triggerEvent);
		   else
			 setTimeout(function() {
						countdownTick.call(this,name,triggerEvent);
			 }, 1000);
	   }
    };

    function changeCountdown(name, value){
            timers[name+'Countdown'] = value;
    };


    function createTimer(name){
      timers[name+'Timer'] = 0;
      timers[name+'Timerstart'] = time;
      setTimeout(function() {
        timerTick.call(this,name);
      }, 100);
    };

    function retrieveCountdown(name){
			var result = timers[name+'Countdown'];
			if(result>0)
				return result;
			else
				return 0;
    };
	
	function deleteCountdown(name){
		var toCheck=name+'Countdown';
		if(toCheck in timers)
			timers[name+'Countdown']=null;
	};

    timerTick = function(name){
	   if(timers[name+'Timer']!=null){
		   timers[name+'Timer'] = (new Date().getTime() - timers[name+'Timerstart']) - time;
		   setTimeout(function() {
			timerTick.call(this,name);
		   }, 100);
	   }
    };

    function retrieveTimer(name){
            return timers[name+'Timer'];
    };
	
	function deleteTimer(name){
		var toCheck=name+'Timer';
		if(toCheck in timers)
			timers[name+'Timer']=null;
	};
};