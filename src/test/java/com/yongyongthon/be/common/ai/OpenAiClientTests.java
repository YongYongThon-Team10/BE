package com.yongyongthon.be.common.ai;

import com.sun.net.httpserver.HttpServer;
import com.yongyongthon.be.common.ai.exception.AiServerException;
import com.yongyongthon.be.common.ai.exception.AiTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 OpenAI 대신 로컬 HTTP 서버로 OpenAiClient의 요청 형태와 오류 변환을 확인한다.
 */
class OpenAiClientTests {

    private HttpServer server;
    private final AtomicReference<String> requestBody = new AtomicReference<>();
    private final AtomicReference<String> authorization = new AtomicReference<>();
    private volatile int status = 200;
    private volatile String responseBody = "";
    private volatile long delayMillis = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private OpenAiClient client(String apiKey) {
        return new OpenAiClient(JsonMapper.builder().build(), apiKey, "test-model",
                "http://127.0.0.1:" + server.getAddress().getPort() + "/v1");
    }

    @Test
    void 성공하면_message_content를_돌려주고_JSON모드로_요청한다() {
        responseBody = "{\"choices\":[{\"message\":{\"content\":\"{\\\"budget\\\":\\\"free\\\"}\"}}]}";

        String content = client("sk-test").completeJson("시스템", "무료 행사", Duration.ofSeconds(5));

        assertThat(content).isEqualTo("{\"budget\":\"free\"}");
        assertThat(authorization.get()).isEqualTo("Bearer sk-test");
        assertThat(requestBody.get())
                .contains("\"model\":\"test-model\"")
                .contains("\"json_object\"")
                .contains("무료 행사");
    }

    @Test
    void 키가_없으면_호출하지_않고_E5021() {
        assertThatThrownBy(() -> client("").completeJson("s", "u", Duration.ofSeconds(5)))
                .isInstanceOf(AiServerException.class);
        assertThat(requestBody.get()).isNull();
    }

    @Test
    void 오류_응답이나_빈_content는_E5021() {
        status = 500;
        responseBody = "{\"error\":{}}";
        assertThatThrownBy(() -> client("sk-test").completeJson("s", "u", Duration.ofSeconds(5)))
                .isInstanceOf(AiServerException.class);

        status = 200;
        responseBody = "{\"choices\":[]}";
        assertThatThrownBy(() -> client("sk-test").completeJson("s", "u", Duration.ofSeconds(5)))
                .isInstanceOf(AiServerException.class);
    }

    @Test
    void 시간을_넘기면_E5041() {
        delayMillis = 1500;
        responseBody = "{\"choices\":[{\"message\":{\"content\":\"{}\"}}]}";

        assertThatThrownBy(() -> client("sk-test").completeJson("s", "u", Duration.ofMillis(300)))
                .isInstanceOf(AiTimeoutException.class);
    }

    @Test
    void 서버에_연결할_수_없으면_E5021() {
        OpenAiClient unreachable = new OpenAiClient(JsonMapper.builder().build(), "sk-test", "m", "http://127.0.0.1:1/v1");
        assertThatThrownBy(() -> unreachable.completeJson("s", "u", Duration.ofSeconds(2)))
                .isInstanceOf(AiServerException.class);
    }
}
