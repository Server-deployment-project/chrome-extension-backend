package com.extension.backend.controller;

import com.extension.backend.dto.VisionRequest;
import com.extension.backend.entity.Conversation;
import com.extension.backend.entity.User;
import com.extension.backend.exception.ApiException;
import com.extension.backend.service.ConversationService;
import com.extension.backend.service.SiliconFlowService;
import com.extension.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.timeout;

@ExtendWith(MockitoExtension.class)
class VisionControllerTest {

    @Mock
    private SiliconFlowService siliconFlowService;

    @Mock
    private UserService userService;

    @Mock
    private ConversationService conversationService;

    @InjectMocks
    private VisionController visionController;

    private User user;
    private Conversation conversation;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setDailyQuota(10);
        user.setRequestsToday(0);
        user.setLastRequestAt(LocalDateTime.now());

        conversation = new Conversation();
        conversation.setId("conv-1");
        conversation.setUserId(1L);
        conversation.setTitle("图片分析");
    }

    @Test
    void shouldForwardAllImagesWhenImagesFieldProvided() {
        mockSuccessPath();

        VisionRequest request = new VisionRequest();
        request.setImages(List.of("https://a.png", "https://b.png"));
        request.setContent("请分析");

        visionController.vision(request, withUser(user));

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(siliconFlowService).visionStream(
                captor.capture(),
                anyString(),
                anyString(),
                anyList(),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(),
                any()
        );
        assertEquals(List.of("https://a.png", "https://b.png"), captor.getValue());
        verify(conversationService, timeout(1000)).addMessage(eq("conv-1"), eq("user"), eq("[图片x2] 请分析"));
    }

    @Test
    void shouldFallbackToSingleImageFieldWhenImagesMissing() {
        mockSuccessPath();

        VisionRequest request = new VisionRequest();
        request.setImage("https://single.png");
        request.setContent("请分析");

        visionController.vision(request, withUser(user));

        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(siliconFlowService).visionStream(
                captor.capture(),
                anyString(),
                anyString(),
                anyList(),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(),
                any()
        );
        assertEquals(List.of("https://single.png"), captor.getValue());
        verify(conversationService, timeout(1000)).addMessage(eq("conv-1"), eq("user"), eq("[图片x1] 请分析"));
    }

    @Test
    void shouldRejectWhenBothImageAndImagesMissing() {
        VisionRequest request = new VisionRequest();

        ApiException ex = assertThrows(ApiException.class, () -> visionController.vision(request, withUser(user)));
        assertEquals("image or images is required", ex.getMessage());
    }

    private ServerWebExchange withUser(User currentUser) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/vision").build()
        );
        exchange.getAttributes().put("USER", currentUser);
        return exchange;
    }

    private void mockSuccessPath() {
        when(conversationService.createConversation(eq(1L), anyString())).thenReturn(conversation);
        when(conversationService.getMessages(eq("conv-1"), anyInt())).thenReturn(Collections.emptyList());
        when(siliconFlowService.visionStream(
                anyList(),
                anyString(),
                anyString(),
                anyList(),
                nullable(String.class),
                nullable(String.class),
                nullable(String.class),
                any(),
                any()
        )).thenReturn(Flux.just("data: [DONE]\n\n"));
    }
}
