package com.copick.connector.client;

import com.copick.connector.dto.ConnectorMovie;
import com.copick.connector.dto.DiscoverRequest;
import com.copick.connector.dto.DiscoverResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ConnectorClient {

    private final RestClient restClient;

    public ConnectorClient(@Value("${connector.url:http://connector:8080}") String connectorUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(connectorUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public List<ConnectorMovie> fetchMovies(DiscoverRequest request) {
        DiscoverResponse response = restClient.post()
                .uri("/discover")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    throw new RuntimeException("Ошибка валидации запроса к коннектору: " + res.getStatusCode());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    throw new RuntimeException("Внутренняя ошибка коннектора: " + res.getStatusCode());
                })
                .body(DiscoverResponse.class);

        return response != null ? response.movies() : List.of();
    }
}