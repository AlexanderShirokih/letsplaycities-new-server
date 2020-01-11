class Player {
  constructor(name, me, title) {
    this.title = title;
    this.name = name;
    this.me = me;
    title.value = name;
  }

  set Name(name) {
    this.name = name;
    this.title.textContent = name;
  }
}

let playing;
let input;
let connection;
let timer;
let gameTime;
let maxTime = 92;

let player;
let other;
let isMyMovement = true;
let isMessageMode = false;
let currentDialog;

let playerInfo = {
  "action": "login",
  "version": 4,
  "clientVersion": "v2.4.0web",
  "clientBuild": 240
};

let oppInfo;

/* General functions */

window.onload = function() {
  input = document.getElementById("city");
  input.addEventListener("keypress", function(e) {
    var keycode = (e.keyCode ? e.keyCode : e.which);
    if (keycode === 13) {
      onEnter();
    }
  });
  earlyInit();
  showWelcomeScreen();
  // initGame();
}

function appendWord(word, me) {
  var wrapper = document.createElement("div");
  var item = document.createElement("span");
  item.className = me ? "me" : "other";
  item.textContent = formatWord(word);
  wrapper.appendChild(item);
  document.getElementById("cont").appendChild(wrapper);
  gameTime = maxTime;
  isMyMovement = !me;
}

function appendMessage(msg, me) {
  var msgBlock = document.createElement("div");
  var msgStrong = document.createElement("strong");
  var msgContent = document.createTextNode(msg);
  msgStrong.textContent = "СООБЩ:";
  msgBlock.className = "float-msg msg-" + (me ? "me" : "other");
  msgBlock.appendChild(msgStrong);
  msgBlock.appendChild(msgContent);
  document.getElementById('main').appendChild(msgBlock);
  setTimeout(function() {
    fade(msgBlock);
  }, 5000);
}

function onConnect() {
  if (playing)
    return;
  hideDialog();
  initGame();
}

function onEnter() {
  if (input.value.length > 0) {
    if (!playing)
      return;
    if (isMessageMode) {
      appendMessage(input.value, true);
      switchInputMode();
      send({
        "action": "msg",
        "msg": input.value
      });
      input.value = "";
    } else if (isMyMovement)
      send({
        "action": "word",
        "word": input.value.toLowerCase()
      });
    else
      showMessage("Сейчас не ваш ход");
  }
}

function onSurr() {
  if (playing) {
    var msg = '<p style="text-align:center;">Вы уверенны, что хотите завершить игру?</p><div class="confirm-dialog"><button onclick="onSurrBtnResult(true)">Да</button><button onclick="onSurrBtnResult(false)">Нет</button></div>';
    showDialog("Завершить игру?", msg);
  }
}

function onSurrBtnResult(result) {
  if (playing) {
    hideDialog();
    if (result)
      finishGame();
  }
}

function onMsg() {
  switchInputMode();
}

/* Core functions */
function earlyInit() {
  createRandomImage();
  var userId = getCookie("user_id");
  var accHash = getCookie("acc_hash");

  if (userId && accHash) {
    playerInfo.uid = userId;
    playerInfo.hash = accHash;
  } else {
    playerInfo.authType = "Native";
    playerInfo.snUID = "225";
    playerInfo.accToken = "none";
  }

  playerInfo.login = "Flames";
  player = new Player("Player", true, document.getElementById("n_me"));
  other = new Player("Player", false, document.getElementById("n_other"));
  timer = document.getElementById("timer");
}

function showWelcomeScreen() {
  // // TODO:
  var img = "img/player.png";
  var login = "My Username";

  var msg = '<img class="avatar" src="' + img + '"><span id="userspan">' + login + '</span><div><button class="btn" onclick="onConnect()">НАЧАТЬ ИГРУ</button></div>';
  showDialog("", msg);
}

function initGame() {
  connection = new WebSocket("ws://localhost:8080/game");
  showWaitingDialog("Подключение к серверу...");
  connection.onopen = function() {
    // Send auth request
    send(playerInfo);
    showWaitingDialog("Ожидание оппонента...", "Игра начнётся сразу после того, как вам найдется оппонент. Не забывайте, что на ответ у вас есть полторы минуты");
  }
  connection.onclose = function(event) {}
  connection.onerror = function(error) {
    showError("Произошла ошибка: " + error.message);
  }
  connection.onmessage = function(event) {
	console.log(event.data);
    handleMessage(JSON.parse(event.data));
  }
  timer.textContent = "";
  gameTime = maxTime;
}

function handleMessage(data) {
	switch(data.action) {
	case "logged_in":
		if (data.newerBuild <= playerInfo.clientBuild) {
	          setCookie("user_id", data.userId, {
	            expires: 3600 * 24 * 10
	          });
	          setCookie("acc_hash", data.accHash, {
	            expires: 3600 * 24 * 10
	          });
	        // Successfully authorized, join the game
	        send({
	          "action": "play",
	          "mode": "RANDOM_PAIR"
	        });
	      } else {
	        showMessage("Вы используете устаревшую версию игры. Обновите страницу для использования новой версии");
	        finishGame();
	      }
		break;
	case "login_error":
		 showMessage(data.banReason);
	     finishGame();
		break;
	case "join":
		 oppInfo = {
			      "login": data.login,
			      "canReceiveMessages": data.canReceiveMessages,
			      "clientBuild": data.clientBuild,
			      "clientVersion": data.clientVersion
			    };
		beginGame(Boolean(data.youStarter));
		break;
	case "word":
		switch (data.result) {
	      case "RECEIVED": // Received
	        appendWord(data.word, false);
	        break
	      case "ACCEPTED": // Accepted
	        appendWord(data.word, true);
	        input.value = "";
	        break;
	      case "ALREADY": // Already
	        showMessage("Город \"" + formatWord(input.value) + "\" уже был загадан");
	        break;
	      case "NO_WORD": // NoWord
	        showMessage("Город \"" + formatWord(input.value) + "\" не существует");
	        break;
	      case "WRONG_MOVE": // WrongMove
	        showMessage("Не ваш ход!");
	        break;
	      default:
	        protocolError();
	    }
		break;
	case "msg":
	    appendMessage(data.msg, false);
		break;
	case "timeout":
		finishGame("Время вышло");
		break;
	case "leave":
		 finishGame("Оппонент сдался");
		 break;
	case "friend_request":
		if(result === "NEW_REQUEST") {
			 // TODO Handle friend request
		    send({
		      "action": "friend",
		      "type": "ACCEPT"
		    });
		}
		break;
	default:
		console.log("Unknown action="+JSON.stringify(data));
	}
}

function beginGame(youStarter) {
  hideDialog(); // Hide waiting dialog
  playing = true;
  isMyMovement = youStarter;
  player.Name = playerInfo.login;
  other.Name = oppInfo.login;
  showMessage("Подключено к игроку: " + oppInfo.login + ". Сейчас " + (youStarter ? "ваш ход" : "ход оппонента"));
  startTimer();
}

function finishGame(reason) {
  playing = false;
  // TODO Finish game session
  // TODO Game stat dialog
  showDialog(reason, "Игра завершена!");
  connection.close();
}

/* Misc functions */
function protocolError() {
  showError("Proctocol error!");
}

function showMessage(message) {
  var msgBlock = document.createElement("div");
  msgBlock.className = "float-msg";
  msgBlock.textContent = message;
  document.getElementById('main').appendChild(msgBlock);
  setTimeout(function() {
    fade(msgBlock);
  }, 2000);
}

function showError(message) {
  showDialog("ОШИБКА :(", message);
  finishGame();
}

function showDialog(title, message) {
  hideDialog();
  var titleSpan = document.createElement("span");
  var content = document.createElement("div");
  currentDialog = document.createElement("div");
  currentDialog.className = "dialog";
  titleSpan.textContent = title;
  content.innerHTML = message;
  currentDialog.appendChild(titleSpan);
  currentDialog.appendChild(content);
  document.getElementById('main').appendChild(currentDialog);
}

function showWaitingDialog(msg, desc) {
  var cont = '<div class="loader"></div><div style="margin: 0px;">' + desc + '</div>';
  showDialog(msg, cont);
}

function hideDialog() {
  if (currentDialog) {
    currentDialog.remove();
    currentDialog = undefined;
  }
}

function formatWord(string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

function send(data) {
  if (connection.readyState == WebSocket.OPEN) {
    connection.send(JSON.stringify(data));
    console.log(JSON.stringify(data));
  }
}

function getCookie(name) {
  var matches = document.cookie.match(new RegExp(
    "(?:^|; )" + name.replace(/([\.$?*|{}\(\)\[\]\\\/\+^])/g, '\\$1') + "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : undefined;
}

function setCookie(name, value, options) {
  options = options || {};

  var expires = options.expires;

  if (typeof expires == "number" && expires) {
    var d = new Date();
    d.setTime(d.getTime() + expires * 1000);
    expires = options.expires = d;
  }
  if (expires && expires.toUTCString) {
    options.expires = expires.toUTCString();
  }

  value = encodeURIComponent(value);

  var updatedCookie = name + "=" + value;

  for (var propName in options) {
    updatedCookie += "; " + propName;
    var propValue = options[propName];
    if (propValue !== true) {
      updatedCookie += "=" + propValue;
    }
  }

  document.cookie = updatedCookie;
}

function deleteCookie(name) {
  setCookie(name, "", {
    expires: -1
  })
}

function switchInputMode() {
  var inputBlock = document.getElementById('ib');
  var input = document.getElementById('city');
  if (isMessageMode) {
    input.placeholder = "Введите город";
    input.style = "background-color: #FFF;";
    var rmBtn = document.getElementById("rmbtn");
    console.log("T=" + rmBtn);
    if (rmBtn) rmBtn.remove();
  } else {
    input.placeholder = "Введите сообщение";
    input.style = "background-color: #AAFDE7;";
    var rmBtn = document.createElement("button");
    rmBtn.id = "rmbtn";
    rmBtn.onclick = function() {
      switchInputMode();
    }
    rmBtn.style = "right: 35px;important!;";
    var img = document.createElement("img");
    img.src = "img/ic_cancel.png";
    rmBtn.appendChild(img);
    inputBlock.prepend(rmBtn);
  }
  input.focus();
  isMessageMode = !isMessageMode;
}

function fade(element) {
  var op = 1;
  var timer = setInterval(function() {
    if (op <= 0.1) {
      clearInterval(timer);
      element.remove();
    }
    element.style.opacity = op;
    element.style.filter = 'alpha(opacity=' + op * 100 + ")";
    op -= op * 0.1;
  }, 50);
}

function startTimer() {
  var t = setInterval(function() {
    if (playing) {
      formatTime(gameTime);
      gameTime--;
    } else {
      clearInterval(t);
    }
  }, 1000);
}

function formatTime(time) {
  var str = "";
  var t = time;
  if (t < 0)
    t = 0;
  var min = Math.floor(t / 60);
  t -= min * 60;
  if (min > 0)
    str = min + "мин ";
  str += t + "сек";
  timer.textContent = str;
}

function createRandomImage() {
	var canvas = document.createElement('canvas');
	var height=128;
	var width=128;

	canvas.height=height;
	canvas.width=width;
	
	var context = canvas.getContext("2d");

	var imageData=context.createImageData(width, height);
	
	var data=imageData.data;
	for (var i=0; i<height*width; i++) {
	     data[i*4+0]=Math.random()*100+100 | 0; // Red
	     data[i*4+1]=Math.random()*100+100 | 0; // Green
	     data[i*4+2]=Math.random()*100+100 | 0; // Blue
	     data[i*4+3]=100; // alpha (transparency)
	}
	context.putImageData(imageData, 0, 0);

	canvas.toBlob(function(blob) {
		var reader = new FileReader();
		 reader.readAsDataURL(blob); 
		 reader.onloadend = function() {
		    var base64 = reader.result;
		    base64 = base64.substring(base64.indexOf(",")+1);
		    playerInfo.avatar = base64;
		 }
	}, "image/png");
}
