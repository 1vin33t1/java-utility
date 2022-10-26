package com.xyz.utility.delay.queue.worker.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.xyz.utility.common.resttemplate.RestTemplateFactory;
import com.xyz.utility.common.resttemplate.RestTemplateName;
import com.xyz.utility.delay.queue.worker.dto.FailedMessageRequest;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class HttpDelayFailureHandlerTest {

    private HttpDelayFailureHandler httpDelayFailureHandler;
    private KafkaMessageCache kafkaMessageCache;
    private RestTemplateFactory restTemplateFactory;
    private RestTemplate restTemplate;
    private static ListAppender<ILoggingEvent> listAppender;

    @BeforeAll
    static void beforeAll() {
        Logger logger = (Logger) LoggerFactory.getLogger(HttpDelayFailureHandler.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @AfterAll
    static void afterAll() {
        listAppender.stop();
    }

    @BeforeEach
    public void setUp() {
        listAppender.list.clear();
        restTemplateFactory = mock(RestTemplateFactory.class);
        restTemplate = mock(RestTemplate.class);
        httpDelayFailureHandler = new HttpDelayFailureHandler(restTemplateFactory);
        Mockito.lenient().when(restTemplateFactory.getRestTemplate(RestTemplateName.DELAY_KAFKA_FAILURE)).thenReturn(restTemplate);
        kafkaMessageCache = new KafkaMessageCache();
        kafkaMessageCache.setFailurePostCallbackUrl("https://www.testurl.com");
    }

    @Test
    public void notifyClient() {
        Mockito.when(restTemplate.exchange(
                Mockito.eq("https://www.testurl.com"),
                Mockito.eq(HttpMethod.POST),
                Mockito.<HttpEntity<FailedMessageRequest>>any(),
                Mockito.eq(String.class))
        ).thenReturn(new ResponseEntity<>("success", HttpStatus.OK));
        httpDelayFailureHandler.notifyClient(kafkaMessageCache, "test", new Exception("test exception"));
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("failed to publish delayed message"));
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("successfully notify"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
    }

    @Test
    public void notifyClient_errorHttpStatusCodeException() {
        Mockito.when(restTemplate.exchange(
                Mockito.eq("https://www.testurl.com"),
                Mockito.eq(HttpMethod.POST),
                Mockito.<HttpEntity<FailedMessageRequest>>any(),
                Mockito.eq(String.class))
        ).thenThrow(new TestHttpCodeException(HttpStatus.INTERNAL_SERVER_ERROR));
        httpDelayFailureHandler.notifyClient(kafkaMessageCache, "test", new Exception("test exception"));
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("failed to publish delayed message"));
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("http-code:"));
        assertEquals(Level.ERROR, logsList.get(1).getLevel());
    }

    @Test
    public void notifyClient_errorException() {
        Mockito.when(restTemplate.exchange(
                Mockito.eq("https://www.testurl.com"),
                Mockito.eq(HttpMethod.POST),
                Mockito.<HttpEntity<FailedMessageRequest>>any(),
                Mockito.eq(String.class))
        ).thenThrow(new RuntimeException("123"));
        httpDelayFailureHandler.notifyClient(kafkaMessageCache, "test", new Exception("test exception"));
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("failed to publish delayed message"));
        assertEquals(Level.ERROR, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("Not able to send"));
        assertEquals(Level.ERROR, logsList.get(1).getLevel());
    }

    @Test
    public void notifyClient_nullUrl() {
        kafkaMessageCache.setFailurePostCallbackUrl(null);
        httpDelayFailureHandler.notifyClient(kafkaMessageCache, "test", new Exception("test exception"));
        Mockito.verifyNoInteractions(restTemplateFactory);
    }

    private class TestHttpCodeException extends HttpStatusCodeException {

        protected TestHttpCodeException(HttpStatus statusCode) {
            super(statusCode);
        }
    }

}