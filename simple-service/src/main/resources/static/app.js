function init() {
 var ws = new WebSocket("ws://localhost:8080/inst-info-websocket");

    ws.onopen = (frame) => {
        setConnected(true);
        console.log('Connected: ' + frame);
    };

    ws.onmessage = (message) => {
        showResponse(JSON.parse(message.data).responseContent);
    };

    ws.onerror = (error) => {
        console.error('Error with websocket', error);
    };

    return ws;
}

var webSocketClient = init();

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
        console.log("Connected");
    }
    else {
        $("#conversation").hide();
        console.log("Disconnected");
    }
    $("#queryResponses").html("");
}

function connect() {
    webSocketClient = init();
}

function disconnect() {
    webSocketClient.close();
    setConnected(false);
}

function sendQuery() {
    webSocketClient.send(JSON.stringify({
        queryContent: $("#queryContent").val()
     }));
}

function showResponse(message) {
    $("#queryResponses").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#disconnect" ).click(() => disconnect());
    $( "#send" ).click(() => sendQuery());
});
