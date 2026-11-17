package com.copick.connector.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.copick.connector.dto.ConnectorMovie;
import com.copick.connector.dto.DiscoverRequest;
import com.copick.connector.dto.GenrePreference;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ConnectorClient: HTTP-взаимодействие с Go-коннектором")
class ConnectorClientTest {

    private HttpServer server;
    private ConnectorClient client;

    /** Ответ, который тестовый сервер вернёт на следующий запрос. */
    private volatile int responseStatus = 200;
    private volatile String responseBody = "{}";

    /** Последний запрос, полученный тестовым сервером. */
    private final AtomicReference<String> lastRequestBody = new AtomicReference<>();
    private final AtomicReference<String> lastContentType = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            try (InputStream in = exchange.getRequestBody()) {
                lastRequestBody.set(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            }
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));

            byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, payload.length == 0 ? -1 : payload.length);
            if (payload.length > 0) {
                exchange.getResponseBody().write(payload);
            }
            exchange.close();
        });
        server.start();

        client = new ConnectorClient("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    private static DiscoverRequest sampleRequest() {
        return new DiscoverRequest(
                List.of(new GenrePreference("action", 50), new GenrePreference("comedy", 30)),
                List.of("horror"));
    }

    @Test
    @DisplayName("200 OK -> фильмы парсятся из ответа, запрос уходит как JSON")
    void fetchMoviesParsesResponse() throws Exception {
        responseBody = """
                {
                  "movies": [
                    {
                      "id": 42,
                      "title": "Movie A",
                      "original_title": "Original A",
                      "poster_url": "https://poster",
                      "backdrop_url": "https://backdrop",
                      "rating": 8.1,
                      "release_year": 2024,
                      "genres": ["action", "adventure"],
                      "overview": "Some text",
                      "requested_genre": "action"
                    }
                  ],
                  "total_count": 1
                }
                """;

        List<ConnectorMovie> movies = client.fetchMovies(sampleRequest());

        assertThat(movies).hasSize(1);
        ConnectorMovie movie = movies.get(0);
        assertThat(movie.id()).isEqualTo(42);
        assertThat(movie.title()).isEqualTo("Movie A");
        assertThat(movie.rating()).isEqualTo(8.1);
        assertThat(movie.genres()).containsExactly("action", "adventure");
        assertThat(movie.release_year()).isEqualTo(2024);

        assertThat(lastContentType.get()).isEqualTo("application/json");
        assertThat(lastRequestBody.get()).contains("\"action\"").contains("\"horror\"");
    }

    @Test
    @DisplayName("пустой список movies -> пустой результат")
    void emptyMoviesListReturnsEmptyResult() throws Exception {
        responseBody = "{\"movies\": [], \"total_count\": 0}";

        assertThat(client.fetchMovies(sampleRequest())).isEmpty();
    }

    @Test
    @DisplayName("4xx от коннектора -> RuntimeException про ошибку валидации")
    void clientErrorThrowsValidationException() throws Exception {
        responseStatus = 400;
        responseBody = "{}";

        assertThatThrownBy(() -> client.fetchMovies(sampleRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Ошибка валидации запроса к коннектору")
                .hasMessageContaining("400");
    }

    @Test
    @DisplayName("5xx от коннектора -> RuntimeException про внутреннюю ошибку")
    void serverErrorThrowsInternalException() throws Exception {
        responseStatus = 502;
        responseBody = "{}";

        assertThatThrownBy(() -> client.fetchMovies(sampleRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Внутренняя ошибка коннектора")
                .hasMessageContaining("502");
    }

    @Test
    @DisplayName("JSON-null вместо тела -> пустой список")
    void nullBodyResultsInEmptyList() throws Exception {
        responseStatus = 200;
        responseBody = "null";

        assertThat(client.fetchMovies(sampleRequest())).isEmpty();
    }
}
