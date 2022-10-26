package com.xyz.utility.delay.queue.worker.listener;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.xyz.utility.delay.queue.worker.service.DelayQueueService;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import com.xyz.utility.persistence.repository.aerospike.KafkaMessageCacheRepository;
import com.xyz.utility.persistence.repository.aerospike.KafkaProcessedIdRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class DelayQueueProcessorTest {

    @Mock
    private DelayQueueService delayQueueService;
    @Mock
    private KafkaMessageCacheRepository kafkaMessageCacheRepository;
    @Mock
    private KafkaProcessedIdRepository kafkaProcessedIdRepository;
    @InjectMocks
    private DelayQueueProcessor delayQueueProcessor;

    private KafkaMessageCache messageCache;
    private static ListAppender<ILoggingEvent> listAppender;
    private ConsumerRecord<String, String> asWriteRecord;
    private ConsumerRecord<String, String> asDeleteRecord;

    @BeforeAll
    static void beforeAll() {
        Logger logger = (Logger) LoggerFactory.getLogger(DelayQueueProcessor.class);
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
        listAppender.list.clear();

        messageCache = new KafkaMessageCache();
        messageCache.setKey("test-key");
        messageCache.setTopic("test");
        messageCache.setValue("test-value");
        messageCache.setFailurePostCallbackUrl("https://www.testurl.com");
        messageCache.setPostTime(System.currentTimeMillis() + 60000);

        asWriteRecord = new ConsumerRecord<>("test", 0, 0, "{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\"}",
                "{\"metadata\":{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\",\"msg\":\"write\",\"gen\":1,\"lut\":0,\"exp\":1621333416},\"@user_key\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"@_class\":\"com.xyz.utility.persistence.model.KafkaMessageId\"}");
        asDeleteRecord = new ConsumerRecord<>("test", 0, 1, "{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\"}",
                "{\"metadata\":{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\",\"msg\":\"delete\",\"durable\":false}}");
    }

    @Test
    public void delayPreProcessor() {
        delayQueueProcessor.delayPreProcessor(null, null, messageCache);
        Mockito.verify(delayQueueService).preProcess(null, null, messageCache);
    }

    @Test
    public void delayPostProcessor_nullMeta() {
        delayQueueProcessor.delayPostProcessor(new ConsumerRecord<>("test", 0, 0, null, "{\"metadata\":null}"));
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
        verifyNoInteractions(delayQueueService);
    }

    @Test
    public void delayPostProcessor_nullKeyId() {
        asDeleteRecord = new ConsumerRecord<>("test", 0, 0, "{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":null,\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\"}",
                "{\"metadata\":{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\",\"msg\":\"delete\",\"durable\":false}}");
        delayQueueProcessor.delayPostProcessor(asDeleteRecord);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
        verifyNoInteractions(delayQueueService);
    }

    @Test
    public void delayPostProcessor_skipWriteRecord() {
        delayQueueProcessor.delayPostProcessor(asWriteRecord);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
        verifyNoInteractions(delayQueueService);
    }

    @Test
    public void delayPostProcessor_skipNullValue() {
        delayQueueProcessor.delayPostProcessor(new ConsumerRecord<>("test", 0, 0, null, null));
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
        verifyNoInteractions(delayQueueService);
    }

    @Test
    public void delayPostProcessor_skipNullKey() {
        delayQueueProcessor.delayPostProcessor(new ConsumerRecord<>("test", 0, 0, null,
                "{\"metadata\":{\"namespace\":\"test\",\"set\":\"kafka_message_id\",\"userKey\":\"user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619\",\"digest\":\"Wxl54xzDkCg+R4/hZmHbmCU69BU=\",\"msg\":\"delete\",\"durable\":false}}"));
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
        verifyNoInteractions(delayQueueService);
    }

    @Test
    public void delayPostProcessor_skipDuplicate() {
        String id = "user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619";
        messageCache.setId(id);
        Mockito.when(kafkaMessageCacheRepository.findById(id)).thenReturn(Optional.of(messageCache));
        Mockito.when(kafkaProcessedIdRepository.existsById(id)).thenReturn(true);
        delayQueueProcessor.delayPostProcessor(asDeleteRecord);
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("post processing detected a duplicate event"));
        assertEquals(Level.WARN, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
        verify(delayQueueService).deleteCache(messageCache);
    }

    @Test
    public void delayPostProcessor_errorOnCheckingDuplicate() {
        String id = "user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619";
        messageCache.setId(id);
        Mockito.when(kafkaMessageCacheRepository.findById(id)).thenReturn(Optional.of(messageCache));
        Mockito.when(kafkaProcessedIdRepository.existsById(id)).thenThrow(new RuntimeException("123"));
        delayQueueProcessor.delayPostProcessor(asDeleteRecord);
        verify(delayQueueService).postProcess(eq(id), any(), any());
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertTrue(logsList.get(1).getMessage().contains("Error while checking existence of processed-id"));
        assertEquals(Level.ERROR, logsList.get(1).getLevel());
        assertEquals(2, logsList.size());
    }

    @Test
    public void delayPostProcessor_successDeleteRecord() {
        String id = "user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619";
        messageCache.setId(id);
        Mockito.when(kafkaMessageCacheRepository.findById(id)).thenReturn(Optional.of(messageCache));
        delayQueueProcessor.delayPostProcessor(asDeleteRecord);
        verify(delayQueueService).postProcess(eq(id), any(), any());
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
    }

    @Test
    public void delayPostProcessor_successDeleteRecordWithHeader() {
        String id = "user:1621333407204:96ee745a-0a0e-455a-8915-23b99e94d619";
        messageCache.setId(id);
        messageCache.setHeaders(new HashMap<>());
        messageCache.getHeaders().put("test", null);
        messageCache.getHeaders().put("test2", "");
        messageCache.getHeaders().put("test3", "123");
        Mockito.when(kafkaMessageCacheRepository.findById(id)).thenReturn(Optional.of(messageCache));
        delayQueueProcessor.delayPostProcessor(asDeleteRecord);
        verify(delayQueueService).postProcess(eq(id), any(), any());
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
    }

    @Test
    public void delayPostProcessor_successScheduler() {
        String id = "open_to_test:1630329733223:3a469e02-15da-49ee-a512-69ff7ce38242";
        messageCache.setId(id);
        Mockito.when(kafkaMessageCacheRepository.findById(id)).thenReturn(Optional.of(messageCache));
        ConsumerRecord<String, String> schedulerRecord = new ConsumerRecord<>("test", 0, 0, "{\"userKey\":\"open_to_test:1630329733223:3a469e02-15da-49ee-a512-69ff7ce38242\"}", "{\"metadata\":{\"msg\":\"delete\",\"userKey\":\"open_to_test:1630329733223:3a469e02-15da-49ee-a512-69ff7ce38242\"}}");
        delayQueueProcessor.delayPostProcessor(schedulerRecord);
        verify(delayQueueService).postProcess(eq(id), any(), any());
        List<ILoggingEvent> logsList = listAppender.list;
        assertTrue(logsList.get(0).getMessage().contains("message received by delay-post-processor value"));
        assertEquals(Level.INFO, logsList.get(0).getLevel());
        assertEquals(1, logsList.size());
    }

}