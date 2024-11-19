package com.logbook.async.logbookdemo;


import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.netty.LogbookClientHandler;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.function.Function;

@RestController
@RequestMapping(value = "/api",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class DemoApi {


    private static final Logger logger = LoggerFactory.getLogger(DemoApi.class);
    private final WebClient webClient;

    public DemoApi(Logbook logbook) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder(this.getClass().getSimpleName())
                .maxConnections(50)
                .pendingAcquireTimeout(Duration.ofSeconds(5)) // Reduce from default 45 seconds to 5 seconds for fast failover
                .maxIdleTime(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient =
                HttpClient.create(connectionProvider)
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // Reduce from default 30 seconds to 5 seconds for connection attempts
                        .doOnConnected(
                                (connection -> connection.addHandler(new LogbookClientHandler(logbook))
                                        .addHandler(new ReadTimeoutHandler(10))));

        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        this.webClient = WebClient.builder()
                .clientConnector(connector)
                .build();
    }

    @GetMapping(path = "/**")
    public Mono<ResponseEntity<?>> get(HttpServletRequest request) {
        return sendRequest(webClient -> webClient, request);
    }

    protected Mono<ResponseEntity<?>> sendRequest(Function<WebClient.RequestBodySpec, WebClient.RequestHeadersSpec<?>> f, HttpServletRequest request) {
        try {
            String uri = "http://localhost:8080/service/api";
            logger.info("redirecting {}, to complete uri: {}", request.getContextPath(), uri);

            return f.apply(webClient.method(HttpMethod.valueOf(request.getMethod()))
                            .uri(uri)
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .retrieve()
                    .toEntity(Object.class)
                    .map(this::handleResponse);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return Mono.error(e);
        }
    }

    private ResponseEntity<?> handleResponse(ResponseEntity<Object> response) {
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
