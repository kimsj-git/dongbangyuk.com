// import React, { useState, useEffect } from "react";
import $ from "jquery";
import kurentoUtils from "kurento-utils";

function RecordTestPage() {
  var configuration = {
    iceServers: [
      {
        urls: "turn:k8a305.p.ssafy.io:3478",
        username: "root",
        credential: "a305"
      }
    ]
  };
  var ws = new WebSocket("wss://k8a305.p.ssafy.io:8030/recording", configuration);
  var videoInput;
  var videoOutput;
  var webRtcPeer;
  var state;

  const NO_CALL = 0;
  const IN_CALL = 1;
  const POST_CALL = 2;
  const DISABLED = 3;
  const IN_PLAY = 4;

  window.onload = function () {
    // console = new Console();
    console.log("Page loaded ...");
    videoInput = document.getElementById("videoInput");
    videoOutput = document.getElementById("videoOutput");
    setState(NO_CALL);
  };

  window.onbeforeunload = function () {
    ws.close();
  };

  function setState(nextState) {
    switch (nextState) {
      case NO_CALL:
        $("#start").attr("disabled", false);
        $("#stop").attr("disabled", true);
        $("#play").attr("disabled", true);
        break;
      case DISABLED:
        $("#start").attr("disabled", true);
        $("#stop").attr("disabled", true);
        $("#play").attr("disabled", true);
        break;
      case IN_CALL:
        $("#start").attr("disabled", true);
        $("#stop").attr("disabled", false);
        $("#play").attr("disabled", true);
        break;
      case POST_CALL:
        $("#start").attr("disabled", false);
        $("#stop").attr("disabled", true);
        $("#play").attr("disabled", false);
        break;
      case IN_PLAY:
        $("#start").attr("disabled", true);
        $("#stop").attr("disabled", false);
        $("#play").attr("disabled", true);
        break;
      default:
        onError("Unknown state " + nextState);
        return;
    }
    state = nextState;
  }

  ws.onmessage = function (message) {
    var parsedMessage = JSON.parse(message.data);
    console.info("Received message: " + message.data);

    switch (parsedMessage.id) {
      case "startResponse":
        startResponse(parsedMessage);
        break;
      case "playResponse":
        playResponse(parsedMessage);
        break;
      case "playEnd":
        playEnd();
        break;
      case "error":
        setState(NO_CALL);
        onError("Error message from server: " + parsedMessage.message);
        break;
      case "iceCandidate":
        webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
          if (error) return console.error("Error adding candidate: " + error);
        });
        break;
      case "stopped":
        break;
      case "paused":
        break;
      case "recording":
        break;
      default:
        setState(NO_CALL);
        onError("Unrecognized message", parsedMessage);
    }
  };

  function start() {
    console.log("Starting video call ...");

    // Disable start button
    setState(DISABLED);
    showSpinner(videoInput, videoOutput);
    console.log("Creating WebRtcPeer and generating local sdp offer ...");

    var options = {
      localVideo: videoInput,
      remoteVideo: videoOutput,
      mediaConstraints: getConstraints(),
      onicecandidate: onIceCandidate,
    };

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(
      options,
      function (error) {
        if (error) return console.error(error);
        webRtcPeer.generateOffer(onOffer);
      }
    );
  }

  function onOffer(error, offerSdp) {
    if (error) return console.error("Error generating the offer");
    console.log(
      "Invoking SDP offer callback function " + "k8a305.p.ssafy.io:8030"
    );
    var message = {
      id: "start",
      sdpOffer: offerSdp,
      mode: $('input[name="mode"]:checked').val(),
    };
    sendMessage(message);
  }

  function onError(error) {
    console.error(error);
  }

  function onIceCandidate(candidate) {
    console.log("Local candidate" + JSON.stringify(candidate));

    var message = {
      id: "onIceCandidate",
      candidate: candidate,
    };
    sendMessage(message);
  }

  function startResponse(message) {
    setState(IN_CALL);
    console.log("SDP answer received from server. Processing ...");

    webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
      if (error) return console.error(error);
    });
  }

  function stop() {
    var stopMessageId = state === IN_CALL ? "stop" : "stopPlay";
    console.log("Stopping video while in " + state + "...");
    setState(POST_CALL);
    if (webRtcPeer) {
      webRtcPeer.dispose();
      webRtcPeer = null;

      var message = {
        id: stopMessageId,
      };
      sendMessage(message);
    }
    hideSpinner(videoInput, videoOutput);
  }

  function play() {
    console.log("Starting to play recorded video...");

    // Disable start button
    setState(DISABLED);
    showSpinner(videoOutput);

    console.log("Creating WebRtcPeer and generating local sdp offer ...");

    var options = {
      remoteVideo: videoOutput,
      mediaConstraints: getConstraints(),
      onicecandidate: onIceCandidate,
    };

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(
      options,
      function (error) {
        if (error) return console.error(error);
        webRtcPeer.generateOffer(onPlayOffer);
      }
    );
  }

  function onPlayOffer(error, offerSdp) {
    if (error) return console.error("Error generating the offer");
    console.info(
      "Invoking SDP offer callback function " + "k8a305.p.ssafy.io:8030"
    );
    var message = {
      id: "play",
      sdpOffer: offerSdp,
    };
    sendMessage(message);
  }

  function getConstraints() {
    var mode = $('input[name="mode"]:checked').val();
    var constraints = {
      audio: true,
      video: true,
    };

    if (mode === "video-only") {
      constraints.audio = false;
    } else if (mode === "audio-only") {
      constraints.video = false;
    }

    return constraints;
  }

  function playResponse(message) {
    setState(IN_PLAY);
    webRtcPeer.processAnswer(message.sdpAnswer, function (error) {
      if (error) return console.error(error);
    });
  }

  function playEnd() {
    setState(POST_CALL);
    hideSpinner(videoInput, videoOutput);
  }

  function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    console.log("Sending message: " + jsonMessage);
    ws.send(jsonMessage);
  }

  function showSpinner() {
    for (var i = 0; i < arguments.length; i++) {
      arguments[i].poster = "./img/transparent-1px.png";
      arguments[i].style.background =
        "center transparent url('./img/spinner.gif') no-repeat";
    }
  }

  function hideSpinner() {
    for (var i = 0; i < arguments.length; i++) {
      arguments[i].src = "";
      arguments[i].poster = "./img/webrtc.png";
      arguments[i].style.background = "";
    }
  }

  return (
    <div>
      <div class="container">
        <div class="row">
          <div class="col-md-5">
            <h3>Local stream</h3>
            <video
              id="videoInput"
              autoplay
              width="480px"
              height="360px"
              poster="img/webrtc.png"
            ></video>
          </div>
          <div class="col-md-2">
            <button id="start" onClick={start}>
              Start
            </button>
            <button id="start" onClick={stop}>
              stop
            </button>
            <button id="start" onClick={play}>
              play
            </button>
          </div>
          <div class="col-md-5">
            <h3>Remote stream</h3>
            <video
              id="videoOutput"
              autoplay
              width="480px"
              height="360px"
              poster="img/webrtc.png"
            ></video>
          </div>
        </div>
      </div>
    </div>
  );
}

export default RecordTestPage;
