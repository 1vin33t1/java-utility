package com.xyz.utility.delay.queue.worker.controller;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.dto.DelayMessageRequest;
import com.xyz.utility.common.util.JsonConversionUtility;
import com.xyz.utility.delay.queue.worker.error.ErrorHandler;
import com.xyz.utility.delay.queue.worker.service.SupportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class SupportControllerTest {
    private final String DELAY_DB_SAVE_URL = "/support/aerospike/save/delay-message";
    private MockMvc mockMvc;
    @Mock
    private SupportService supportService;
    @InjectMocks
    private SupportController supportController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ErrorHandler(), supportController)
                .build();
        this.supportController = new SupportController(supportService);
    }

    @Test
    void saveToAerospikeDelaySet_noError() throws Exception {
        mockMvc.perform(post(DELAY_DB_SAVE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .param(UtilityConstant.Kafka.MESSAGE_ID, "1")
                .param("key", "2")
                .content(JsonConversionUtility.convertToJson(new DelayMessageRequest())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("true")));
        verify(supportService, times(1)).saveToDelayDb(Mockito.any(String.class),
                Mockito.any(String.class), Mockito.any(DelayMessageRequest.class));
        verifyNoMoreInteractions(supportService);
    }

    @Test
    void saveToAerospikeDelaySet_error() throws Exception {
        Mockito.doThrow(new IllegalArgumentException(UtilityConstant.SAVE_TO_DELAY_DB_VALIDATION_FAILED)).when(supportService).saveToDelayDb(Mockito.any(String.class),
                Mockito.any(String.class), Mockito.any(DelayMessageRequest.class));
        mockMvc.perform(post(DELAY_DB_SAVE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .param(UtilityConstant.Kafka.MESSAGE_ID, "1")
                .param("key", "2")
                .content(JsonConversionUtility.convertToJson(new DelayMessageRequest())))
                .andExpect(status().is4xxClientError())
                .andExpect(content().string(containsString("error")));
        verify(supportService, times(1)).saveToDelayDb(Mockito.any(String.class),
                Mockito.any(String.class), Mockito.any(DelayMessageRequest.class));
        verifyNoMoreInteractions(supportService);
    }
}