var stompClient = null;

var socket = new SockJS('/meteo');
stompClient = Stomp.over(socket);
stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
    stompClient.subscribe('/topic/meteo', function(messageOutput) {
        showMeteoData(JSON.parse(messageOutput.body));
    });
});

function showMeteoData(data) {
    for (var key in data) {
        try {
            document.getElementById(key).innerHTML = data[key];
        } catch (e) {
            continue;
        }
    }
}