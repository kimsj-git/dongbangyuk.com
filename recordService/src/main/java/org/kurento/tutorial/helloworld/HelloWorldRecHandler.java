package org.kurento.tutorial.helloworld;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import org.kurento.client.Continuation;
import org.kurento.client.EndOfStreamEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.MediaProfileSpecType;
import org.kurento.client.MediaType;
import org.kurento.client.PausedEvent;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.RecordingEvent;
import org.kurento.client.StoppedEvent;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * Hello World with recording handler (application and media logic).
 *
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernandez (d.fernandezlop@gmail.com)
 * @author Radu Tom Vlad (rvlad@naevatec.com)
 * @author Ivan Gracia (igracia@kurento.org)
 * @since 6.1.1
 */
public class HelloWorldRecHandler extends TextWebSocketHandler {

  private static String RECORDER_FILE_NAME;

  private final Logger log = LoggerFactory.getLogger(HelloWorldRecHandler.class);
  private static final Gson gson = new GsonBuilder().create();

  @Autowired
  private UserRegistry registry;

  @Autowired
  private KurentoClient kurento;

  @Override
  public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
    JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

    log.info("Incoming message: {}", jsonMessage);

    UserSession user = registry.getBySession(session);
    if (user != null) {
      log.debug("Incoming message from user '{}': {}", user.getId(), jsonMessage);
    } else {
      log.debug("Incoming message from new user: {}", jsonMessage);
    }

    // recording 시 record 실행 및 addEventListener 실행
    String msg = message.getPayload();

    System.out.println("msg :  " + msg);

    // // 파일 이름 지정
    // RECORDER_FILE_NAME = jsonMessage.get("userEmail").getAsString() + ".webm";
    switch (jsonMessage.get("id").getAsString()) {
      case "start":
        // 파일 이름 지정
        RECORDER_FILE_NAME = jsonMessage.get("userEmail").getAsString() + ".webm";
        start(session, jsonMessage);
        break;
      case "stop":
        if (user != null) {
          stop();
          user.stop();
        }
      case "stopPlay":
        if (user != null) {
          user.release();
        }
        break;
      case "play":
        play(user, session, jsonMessage);
        break;
      case "onIceCandidate": {
        JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();
        System.out.println(jsonCandidate.toString());

        System.out.println("candidate: " + jsonCandidate.get("candidate").getAsString());
        System.out.println("sdpMid: " + jsonCandidate.get("sdpMid").getAsString());
        System.out.println("sdpMLineIndex: " + jsonCandidate.get("sdpMLineIndex").getAsInt());

        if (user != null) {
          IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
              jsonCandidate.get("sdpMid").getAsString(),
              jsonCandidate.get("sdpMLineIndex").getAsInt());
          user.addCandidate(candidate);
        }
        break;
      }
      default:
        sendError(session, "Invalid message with id " + jsonMessage.get("id").getAsString());
        break;
    }
  }

  @Override
  public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
    super.afterConnectionClosed(session, status);
    registry.removeBySession(session);
  }

  private void start(final WebSocketSession session, JsonObject jsonMessage) {

    try {
      long startTime = System.currentTimeMillis();
      System.out.println("======= startTime: " + startTime);
      AtomicBoolean isRecordingStarted = new AtomicBoolean(false);



      // 1. Media logic (webRtcEndpoint in loopback)
      MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
      webRtcEndpoint.connect(webRtcEndpoint);

      MediaProfileSpecType profile = getMediaProfileFromMessage(jsonMessage);

      String filePath = "file:///tmp/" + RECORDER_FILE_NAME;


      System.out.println("**************************************************");
      System.out.println("**************************************************");
      System.out.println("**************************************************");
      System.out.println("**************** " + startTime + " *****************");
      System.out.println("**************************************************");
      System.out.println("**************************************************");
      System.out.println("**************************************************");


      System.out.println("filePath: " + filePath);
      RecorderEndpoint recorder = new RecorderEndpoint.Builder(pipeline, filePath)
          .withMediaProfile(profile).build();

      pipeline.addErrorListener(new EventListener<ErrorEvent>() {
        @Override
        public void onEvent(ErrorEvent ev) {
          log.error(
              "[MediaPipeline::ErrorEvent] Error code {}: '{}', source: {}, timestamp: {}, tags: {}, description: {}",
              ev.getErrorCode(), ev.getType(), ev.getSource().getName(),
              ev.getTimestampMillis(), ev.getTags(), ev.getDescription());
          sendError(session, "[MediaPipeline] " + ev.getDescription());
        }
      });

      webRtcEndpoint.addErrorListener(new EventListener<ErrorEvent>() {
        @Override
        public void onEvent(ErrorEvent ev) {
          log.error(
              "[WebRtcEndpoint::ErrorEvent] Error code {}: '{}', source: {}, timestamp: {}, tags: {}, description: {}",
              ev.getErrorCode(), ev.getType(), ev.getSource().getName(),
              ev.getTimestampMillis(), ev.getTags(), ev.getDescription());
          sendError(session, "[WebRtcEndpoint] " + ev.getDescription());
        }
      });

      recorder.addErrorListener(new EventListener<ErrorEvent>() {
        @Override
        public void onEvent(ErrorEvent ev) {
          log.error(
              "[RecorderEndpoint::ErrorEvent] Error code {}: '{}', source: {}, timestamp: {}, tags: {}, description: {}",
              ev.getErrorCode(), ev.getType(), ev.getSource().getName(),
              ev.getTimestampMillis(), ev.getTags(), ev.getDescription());
          sendError(session, "[RecorderEndpoint] " + ev.getDescription());
        }
      });


      // recorder.addRecordingListener(new EventListener<RecordingEvent>() {
      //   @Override
      //   public void onEvent(RecordingEvent event) {
      //     System.out.println("recording.addRecordingListener 진입");
      //     JsonObject response = new JsonObject();
      //     response.addProperty("id", "recording");
      //     try {
      //       System.out.println("record try");
      //       synchronized (session) {
      //         session.sendMessage(new TextMessage(response.toString()));
      //       }
      //     } catch (IOException e) {
      //       log.error(e.getMessage());
      //     }
      //   }
      // });

      recorder.addStoppedListener(new EventListener<StoppedEvent>() {

        @Override
        public void onEvent(StoppedEvent event) {
          System.out.println("recording.addStoppedListener 진입");
          JsonObject response = new JsonObject();
          response.addProperty("id", "stopped");
          try {
            System.out.println("stop try");
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.error(e.getMessage());
          }
        }

      });

      recorder.addPausedListener(new EventListener<PausedEvent>() {

        @Override
        public void onEvent(PausedEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "paused");
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.error(e.getMessage());
          }
        }
      });

      connectAccordingToProfile(webRtcEndpoint, recorder, profile);

      // 2. Store user session
      UserSession user = new UserSession(session);
      user.setMediaPipeline(pipeline);
      user.setWebRtcEndpoint(webRtcEndpoint);
      user.setRecorderEndpoint(recorder);

      registry.register(user);

      // 3. SDP negotiation
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      // 4. Gather ICE candidates
      webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

        @Override
        public void onEvent(IceCandidateFoundEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "iceCandidate");
          response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.error(e.getMessage());
          }
        }
      });

      JsonObject response = new JsonObject();
      response.addProperty("id", "startResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      synchronized (user) {
        session.sendMessage(new TextMessage(response.toString()));
      }

      webRtcEndpoint.gatherCandidates();




      recorder.record(new Continuation<Void>() {
        @Override
        public void onSuccess(Void result) {
          // 녹화 시작 성공 시 실행할 코드
          System.out.println("Recording started successfully");

          try {
            Thread.sleep(3000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }

          long endTime = System.currentTimeMillis();  // 종료 시간 기록
          long elapsedTime = endTime - startTime;  // 경과 시간 계산
          System.out.println("elapsedTime: " + elapsedTime);
          isRecordingStarted.set(true);
          System.out.println("녹화요청 성공함: " + isRecordingStarted.get());
        }

        @Override
        public void onError(Throwable cause) {
          // 녹화 시작 실패 시 실행할 코드
          System.out.println("Failed to start recording: " + cause.getMessage());
        }
      });

      long maxWaitTime = 5000;
      long elapsedTime = 0;

      while (!isRecordingStarted.get() && elapsedTime <= maxWaitTime) {
        Thread.sleep(100);  //100ms 간격
        elapsedTime += 100;
      }

      System.out.println("녹화 시작까지 걸린 시간: " + elapsedTime + " __ " + isRecordingStarted.get() );

      if (isRecordingStarted.get()) {
        System.out.println("Listener 실행됨" + recorder.getState());

        recorder.addRecordingListener(new EventListener<RecordingEvent>() {
          @Override
          public void onEvent(RecordingEvent event) {
            System.out.println("recording.addRecordingListener 진입" + event);
            JsonObject response = new JsonObject();
            response.addProperty("id", "recording");
            try {
              System.out.println("record try");
              synchronized (session) {
                session.sendMessage(new TextMessage(response.toString()));
              }
            } catch (IOException e) {
              log.error(e.getMessage());
            }
          }
        });
      } else {
        System.out.println("============ 시간 초과 ============");
      }
    } catch (Throwable t) {
      log.error("Start error", t);
      sendError(session, t.getMessage());
    }
    System.out.println("============ catch 뒤 ============");
  }

  private MediaProfileSpecType getMediaProfileFromMessage(JsonObject jsonMessage) {
    return MediaProfileSpecType.WEBM;
  }

  private void connectAccordingToProfile(WebRtcEndpoint webRtcEndpoint, RecorderEndpoint recorder,
      MediaProfileSpecType profile) {

    System.out.println("connectAccordingToProfile webRtcEndpoint : " + webRtcEndpoint.getConnectionState());
    System.out.println("connectAccordingToProfile recorder : " + recorder.getState());
    System.out.println("connectAccordingToProfile profile : " + profile.toString());

    switch (profile) {
      case WEBM:
        webRtcEndpoint.connect(recorder, MediaType.AUDIO);
        webRtcEndpoint.connect(recorder, MediaType.VIDEO);
        break;
      case WEBM_AUDIO_ONLY:
        webRtcEndpoint.connect(recorder, MediaType.AUDIO);
        break;
      case WEBM_VIDEO_ONLY:
        webRtcEndpoint.connect(recorder, MediaType.VIDEO);
        break;
      default:
        throw new UnsupportedOperationException("Unsupported profile for this tutorial: " + profile);
    }
  }

  private void play(UserSession user, final WebSocketSession session, JsonObject jsonMessage) {
    System.out.println("진입");
    try {
      System.out.println("flag 1");
      // 1. Media logic
      final MediaPipeline pipeline = kurento.createMediaPipeline();
      WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();


      String filePath = "/recordvideo/" + RECORDER_FILE_NAME;
      System.out.println("play: " + filePath);
      PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, filePath).build();
      player.connect(webRtcEndpoint);

      System.out.println("flag 2");
      // Player listeners
      player.addErrorListener(new EventListener<ErrorEvent>() {
        @Override
        public void onEvent(ErrorEvent event) {
          log.info("ErrorEvent for session '{}': {}", session.getId(), event.getDescription());
          sendPlayEnd(session, pipeline);
        }
      });

      player.addEndOfStreamListener(new EventListener<EndOfStreamEvent>() {
        @Override
        public void onEvent(EndOfStreamEvent event) {
          log.info("EndOfStreamEvent for session '{}'", session.getId());
          sendPlayEnd(session, pipeline);
        }
      });

      System.out.println("flag 3");
      // 2. Store user session
      user.setMediaPipeline(pipeline);
      user.setWebRtcEndpoint(webRtcEndpoint);

      System.out.println("flag 4");
      // 3. SDP negotiation
      String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
      String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

      JsonObject response = new JsonObject();
      response.addProperty("id", "playResponse");
      response.addProperty("sdpAnswer", sdpAnswer);

      // 4. Gather ICE candidates
      webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

        @Override
        public void onEvent(IceCandidateFoundEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "iceCandidate");
          response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
            log.error(e.getMessage());
          }
        }
      });

      // 5. Play recorded stream
      player.play();

      System.out.println("flag 6");
      // String mediaUrl = "ws:///localhost:8080/images/upload";
      // webRtcEndpoint.connect(mediaUrl);

      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }
      webRtcEndpoint.gatherCandidates();

      System.out.println("flag 7");

      // 6. Send video to Spring
      // String videoPath = "kms:///tmp/testRecord_" + sequence + ".webm";
      // String videoPath = "kms:/tmp/testRecord_" + sequence + ".webm";

      String videoPath = "/recordvideo/" + RECORDER_FILE_NAME;
      System.out.println(videoPath);
      System.out.println(Paths.get(videoPath));
      try {
        System.out.println("진입 22222");
        Path videoFilePath = Paths.get(videoPath);
        System.out.println(videoFilePath);
        if(!Files.exists(videoFilePath)) {
          throw new FileNotFoundException("없어!" + videoFilePath);
        }

        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("file", new FileSystemResource(videoFilePath.toFile()));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

        HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);

        // String serverUrl = "http://localhost:8080/images/upload";
        // String serverUrl = "http://13.125.6.24:8081/images/upload";
        // String serverUrl = "http://k8a305.p.ssafy.io:8081/images/upload";
        String serverUrl = "https://k8a305.p.ssafy.io/images/upload";
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.exchange(serverUrl, HttpMethod.POST, requestEntity, String.class);

        log.info("Response from server: {}", responseEntity.getBody());

      } catch (IOException e) {
        log.error("Failed to send video to Spring: {}", e.getMessage());
      }
    } catch (Throwable t) {
      log.error("Play error", t);
      sendError(session, t.getMessage());
    }
  }

  private void stop() {
    System.out.println("진입");
    try {
      System.out.println("flag 1");

      // 6. Send video to Spring
      String videoPath = "/recordvideo/" + RECORDER_FILE_NAME;
      System.out.println("filePath: " + videoPath);

      Path videoFilePath = Paths.get(videoPath);
      System.out.println("videoFilePath = " + videoFilePath);
      if (!Files.exists(videoFilePath)) {
        throw new FileNotFoundException("없어!" + videoFilePath);
      }
      System.out.println("flag 2");
      LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
      map.add("file", new FileSystemResource(videoFilePath.toFile()));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(org.springframework.http.MediaType.MULTIPART_FORM_DATA);

      HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<>(map, headers);
      System.out.println("flag 3");
      // String serverUrl = "http://localhost:8080/images/upload";
      // String serverUrl = "http://13.125.6.24:8081/images/upload";
      // String serverUrl = "http://k8a305.p.ssafy.io:8081/images/upload";
      String serverUrl = "https://k8a305.p.ssafy.io/images/upload";
      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<String> responseEntity = restTemplate.exchange(serverUrl, HttpMethod.POST, requestEntity, String.class);
      log.info("Response from server: {}", responseEntity.getBody());
    } catch (IOException e) {
      log.error("Failed to send video to Spring: {}", e.getMessage());
    }
  }

  public void sendPlayEnd(WebSocketSession session, MediaPipeline pipeline) {
    try {
      JsonObject response = new JsonObject();
      response.addProperty("id", "playEnd");
      session.sendMessage(new TextMessage(response.toString()));
    } catch (IOException e) {
      log.error("Error sending playEndOfStream message", e);
    }
    // Release pipeline
    pipeline.release();
  }

  private void sendError(WebSocketSession session, String message) {
    JsonObject response = new JsonObject();
    response.addProperty("id", "error");
    response.addProperty("message", message);

    try {
      synchronized (session) {
        session.sendMessage(new TextMessage(response.toString()));
      }
    } catch (IOException e) {
      log.error("Exception sending message", e);
    }
  }

}