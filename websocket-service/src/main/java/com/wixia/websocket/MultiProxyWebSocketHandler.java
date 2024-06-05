package com.wixia.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class MultiProxyWebSocketHandler implements WebSocketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiProxyWebSocketHandler.class);

    private final ObjectMapper objectMapper;

    private final
    Flux<ServiceInfo> simulatedEndpoints = Flux.just(
        new ServiceInfo("http://localhost:8082/service", "A", 6),
        new ServiceInfo("http://localhost:8082/service", "B", 13),
        new ServiceInfo("http://localhost:8082/service", "C", 3),
        new ServiceInfo("http://localhost:8082/service", "D", 24),
        new ServiceInfo("http://localhost:8082/service", "E", 8),
        new ServiceInfo("http://localhost:8082/service", "F", 2),
        new ServiceInfo("http://localhost:8082/service", "G", 7),
        new ServiceInfo("http://localhost:8082/service", "H", 19),
        new ServiceInfo("http://localhost:8082/service", "I", 27),
        new ServiceInfo("http://localhost:8082/service", "J", 4));


    @Override
    public Mono<Void> handle(WebSocketSession webSocketSession) {

        Sinks.Many<QueryResponseMessage> sinks = Sinks.many().replay().limit(Duration.ZERO);
        Flux<QueryResponseMessage> outputMessages = sinks.asFlux();
        final ExecutorService executor = Executors.newSingleThreadExecutor();

        LOGGER.info("Handle web socket session");
        // ======================================================================================================= //
        // This is for receiving the input from the websocket and sending out the requests to the endpoints
        Flux<QueryResponseMessage> serviceResponses = webSocketSession
            .receive()
            .map(WebSocketMessage::getPayloadAsText)
            .map(this::readIncomingMessage)
            .log("server receiving::")
            .flatMap(
                queryRequest -> simulatedEndpoints
                    .flatMap(serviceInfo -> queryService(queryRequest.queryContent(), serviceInfo))
            )
            .map(this::readIncomingResponse)
            .doOnNext(queryResponseMessage -> {
                executor.execute(() -> {
                    sinks.emitNext(queryResponseMessage, Sinks.EmitFailureHandler.FAIL_FAST);
                });
            })
            .doOnError(error -> sinks.emitError(error, Sinks.EmitFailureHandler.FAIL_FAST));

        // ======================================================================================================= //
        // This is for handling the responses from the endpoints and forwarding them to the websocket return channel
        Mono<Void> sendMono = webSocketSession
            .send(
                Mono.delay(Duration.ofMillis(500))
                    .thenMany(
                        outputMessages
                            .map(queryResponseMessage ->
                                     webSocketSession.textMessage(toJsonString(queryResponseMessage)))))
            .log("server sending::")
            .onErrorResume(throwable -> webSocketSession.close())
            .then();

        return Mono.zip(serviceResponses.then(), sendMono).then();
//        return sendMono;
    }

    private Mono<String> queryService(String inputData,
                                      ServiceInfo serviceInfo) {
        return WebClient.create(serviceInfo.endpointAddress())
            .get()
            .uri(uriBuilder -> uriBuilder
                .queryParam("querystring", inputData)
                .queryParam("instno", serviceInfo.instNo())
                .queryParam("delay", serviceInfo.simulatedDelay())
                .build())
            .retrieve()
            .bodyToMono(String.class)
            .log("Sending request to service::");
    }

    @SneakyThrows
    private String toJsonString(QueryResponseMessage msg) {
        LOGGER.info("Outgoing message to client -> {}", msg.responseContent());
        return objectMapper.writeValueAsString(msg);
    }

    @SneakyThrows
    private QueryRequest readIncomingMessage(String text) {
        LOGGER.info("Incoming message from client -> {}", text);
        return objectMapper.readValue(text, QueryRequest.class);
    }

    @SneakyThrows
    private QueryResponseMessage readIncomingResponse(String text) {
        LOGGER.info("Incoming response from service -> {}", text);
        return objectMapper.readValue(text, QueryResponseMessage.class);
    }
}
