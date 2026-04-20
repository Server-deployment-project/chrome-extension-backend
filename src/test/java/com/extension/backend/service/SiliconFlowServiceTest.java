package com.extension.backend.service;

import com.extension.backend.config.LlmConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SiliconFlowServiceTest {

    private LlmConfig llmConfig;
    private AtomicInteger requestCount;
    private SiliconFlowService service;

    @BeforeEach
    void setUp() {
        llmConfig = new LlmConfig();
        requestCount = new AtomicInteger();

        WebClient webClient = WebClient.builder()
                .exchangeFunction(request -> {
                    requestCount.incrementAndGet();
                    return Mono.just(
                            ClientResponse.create(HttpStatus.OK)
                                    .header("Content-Type", MediaType.TEXT_EVENT_STREAM_VALUE)
                                    .body("data: [DONE]\n\n")
                                    .build()
                    );
                })
                .build();

        service = new SiliconFlowService(webClient, llmConfig);
    }

    @Test
    void shouldReturnConfigErrorWhenDefaultApiKeyIsPlaceholder() {
        llmConfig.setApiKey("sk-your-key");
        llmConfig.setApiBase("https://api.siliconflow.cn/v1");
        llmConfig.setVisionModel("Qwen/Qwen3.5-35B-A3B");

        String response = service.visionStream(
                List.of("https://example.com/a.png"),
                "system",
                "content",
                List.of(),
                null,
                null,
                null,
                null,
                null
        ).blockFirst();

        assertTrue(response.contains("config/application-private.yml"));
        assertEquals(0, requestCount.get());
    }

    @Test
    void shouldReturnConfigErrorWhenDefaultApiKeyIsBlank() {
        llmConfig.setApiKey("");
        llmConfig.setApiBase("https://api.siliconflow.cn/v1");
        llmConfig.setDefaultModel("deepseek-ai/DeepSeek-V3.2");

        String response = service.chatStream(
                List.of(Map.of("role", "user", "content", "hello")),
                null,
                null,
                null,
                null,
                null
        ).blockFirst();

        assertTrue(response.contains("LLM API Key"));
        assertEquals(0, requestCount.get());
    }
}
