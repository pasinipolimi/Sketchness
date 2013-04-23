(function() {
                            
    function setError (message) {
        $("#onError span").text(message);
        $("#onError").show();
        $("#pageheader").hide();
        $("#mainPage").hide();
    }
                                        
    if (!window.WebSocket) {
        if (!window.MozWebSocket)
        {
            setError("WebSockets are not supported by your browser.");
            return;
        }
    }

    if (!(function(e){return e.getContext && e.getContext('2d');}(document.getElementById("me")))) {
        setError("Canvas is not supported by your browser.");
        return;
    }
                                        
                                        
                                        
    var WS = window['MozWebSocket'] ? MozWebSocket : WebSocket;
    var chatSocket = new WS($('#chatWebSocket').data('ws'));

    var sendMessage = function() {
        chatSocket.send(JSON.stringify({text: $("#talk").val()}));
        $("#talk").val('');
        $("#messages").animate({scrollTop:$("#messages")[0].scrollHeight}, 1000);
    };

    var receiveEvent = function(event) {
        var data = JSON.parse(event.data);

        // Handle errors
        if(data.error) {
            chatSocket.close();
            $("#onError span").text(data.error);
            $("#onError").show();
            $("#pageheader").hide();
            $("#mainPage").hide();
            return;
        } else {
            $("#onChat").show();
        }
        var pane = $('.jscroll');
        // Create the message element
        var api = pane.data('jsp');
        var el = $('<div class="message"><span></span><p></p></div>');
        $("span", el).text(data.user);
        $("p", el).text(data.message);
        $(el).addClass(data.kind);
        if(data.user === '@username') $(el).addClass('me');
        {
            api.getContentPane().append(el);
            api.reinitialise();
            api.scrollToBottom();
        }

        // Update the members list
        var userCounter=0;
        $(data.members).each(function() {
            if(this !== '@username')
            {
                userCounter++;
                $("#opponent"+userCounter).text(this);
            }
        });
    };

    var handleReturnKey = function(e) {
        if(e.charCode === 13 || e.keyCode === 13) {
            e.preventDefault();
            sendMessage();
        }
    };

    $("#talk").keypress(handleReturnKey);

    chatSocket.onmessage = receiveEvent;

})();