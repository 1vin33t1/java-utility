package com.xyz.utility.delay.queue.worker.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.xyz.utility.common.kafka.ProducerTemplate;
import com.xyz.utility.delay.queue.worker.config.RuntimeProperties;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageIdAndCacheRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DelayQueueServiceTest {

    @Mock
    private RuntimeProperties runtimeProperties;
    @Mock
    private AsyncTaskExecutor asyncTaskExecutor;
    @Mock
    private ProducerTemplate producerTemplate;
    @Mock
    private KafkaMessageIdAndCacheRepository kafkaMessageIdAndCacheRepository;
    @Mock
    private DelayFailureHandler failureHandler;
    @InjectMocks
    private DelayQueueService delayQueueService;
    private KafkaMessageCache messageCache;
    private static ListAppender<ILoggingEvent> listAppender;

    @BeforeAll
    static void beforeAll() {
        Logger logger = (Logger) LoggerFactory.getLogger(DelayQueueService.class);
        listAppender = new ListAppender<>();
        logger.addAppender(listAppender);
        listAppender.start();
    }

    @AfterAll
    static void afterAll() {
        listAppender.stop();
    }

    @BeforeEach
    public void setUp() {
        Mockito.lenient().when(runtimeProperties.getDelayKafkaTopic()).thenReturn("event_delayer");
        Mockito.lenient().when(runtimeProperties.getToleranceTime()).thenReturn(5000L);
        Mockito.lenient().when(runtimeProperties.getTriesMaxCount()).thenReturn(4);
        Mockito.lenient().when(runtimeProperties.getPostDelayKafkaTopicName()).thenReturn("nothing");
        listAppender.list.clear();

        messageCache = new KafkaMessageCache();
        messageCache.setKey("test-key");
        messageCache.setTopic("test");
        messageCache.setValue("test-value");
        messageCache.setFailurePostCallbackUrl("https://www.testurl.com");
        messageCache.setPostTime(System.currentTimeMillis() + 60000);
    }

    @Test
    public void delayPreProcessor_emptyRequestId() {
        messageCache.setId("");
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
    }

    @Test
    public void delayPreProcessor_retryRequestId() {
        messageCache.setId("1231");
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
    }

    @Test
    public void delayPreProcessor_withHeader() {
        messageCache.setHeaders(new HashMap<>());
        messageCache.getHeaders().put("test", "123");
        messageCache.getHeaders().put("test2", null);
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
    }

    @Test
    public void delayPreProcessor_withHeaderAndNoFailureCallback() {
        messageCache.setHeaders(new HashMap<>());
        messageCache.getHeaders().put("test", "123");
        messageCache.getHeaders().put("test2", null);
        messageCache.setFailurePostCallbackUrl(null);
        messageCache.setPostTime(System.currentTimeMillis());
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("no delay required so posting the message right now"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        Mockito.verify(producerTemplate).send(any());
    }

    @Test
    public void delayPreProcessor_toleranceFailed() {
        messageCache.setPostTime(System.currentTimeMillis() + 3000);
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("no delay required so posting the message right now"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
    }

    @Test
    public void delayPreProcessor_aerospikeSaveFailedAndRetry() {
        Mockito.doThrow(new RuntimeException()).when(kafkaMessageIdAndCacheRepository).save(Mockito.any(), anyInt());
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertTrue(logsList.get(2).getMessage().contains("exception occurred"));
        assertEquals(Level.ERROR, logsList.get(2).getLevel());
        assertTrue(logsList.get(3).getMessage().contains("failed to save message sending for retry"));
        assertEquals(Level.INFO, logsList.get(3).getLevel());
    }

    @Test
    public void delayPreProcessor_aerospikeSaveFailedAndNoRetry() {
        Mockito.doThrow(new RuntimeException()).when(kafkaMessageIdAndCacheRepository).save(Mockito.any(), anyInt());
        messageCache.setTries(4);
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertTrue(logsList.get(2).getMessage().contains("exception occurred"));
        assertEquals(Level.ERROR, logsList.get(2).getLevel());
        assertEquals(3, logsList.size());
    }

    @Test
    public void delayPreProcessor_success() {
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
    }

    @Test
    public void delayPreProcessor_success2() {
        delayQueueService.preProcess(null, null, messageCache);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received for delay request"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("saving message"));
        assertEquals(Level.INFO, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
    }

    @Test
    public void deleteCache_success() {
        delayQueueService.deleteCache(new KafkaMessageCache());
        Mockito.verify(kafkaMessageIdAndCacheRepository, times(1)).deleteById(any());
    }

    @Test
    public void delayPostProcessor_success() {
        delayQueueService.postProcess(null, null, messageCache);
        Mockito.verify(producerTemplate).send(any(ProducerRecord.class));
        Mockito.verify(asyncTaskExecutor).execute(any());
    }

    @Test
    public void delayPostProcessor_successWithHeader() {
        messageCache.setHeaders(new HashMap<>());
        messageCache.getHeaders().put("test", "123");
        delayQueueService.postProcess(null, null, messageCache);
        Mockito.verify(producerTemplate).send(any(ProducerRecord.class));
        Mockito.verify(asyncTaskExecutor).execute(any());
    }

}