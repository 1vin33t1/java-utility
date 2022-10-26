package com.xyz.utility.delay.queue.worker.service;

import com.xyz.utility.common.resttemplate.RestTemplateFactory;
import com.xyz.utility.common.resttemplate.RestTemplateName;
import com.xyz.utility.delay.queue.worker.dto.FailedMessageRequest;
import com.xyz.utility.persistence.model.KafkaMessageCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;

@Slf4j
@Service
public class HttpDelayFailureHandler implements DelayFailureHandler {

    private final RestTemplateFactory restTemplateFactory;
    private final RetryTemplate retryTemplate;

    public HttpDelayFailureHandler(RestTemplateFactory restTemplateFactory) {
        this.restTemplateFactory = restTemplateFactory;

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(4);
        FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(100);
        retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        retryTemplate.setThrowLastExceptionOnExhausted(true);
    }

    @Override
    public void notifyClient(KafkaMessageCache messageCache, String source, Exception ex) {
        log.error("failed to publish delayed message {}, source {} ", messageCache, source, ex);
        if (messageCache.getFailurePostCallbackUrl() != null) {
            FailedMessageRequest failedMessageRequest = new FailedMessageRequest(messageCache, Arrays.toString(ex.getStackTrace()), source);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            RestTemplate restTemplate = restTemplateFactory.getRestTemplate(RestTemplateName.DELAY_KAFKA_FAILURE);
            try {
                final ResponseEntity<String> responseEntity = retryTemplate.execute(retryContext -> restTemplate.exchange(messageCache.getFailurePostCallbackUrl(), HttpMethod.POST,
                        new HttpEntity<>(failedMessageRequest, headers), String.class));
                log.info("successfully notify the client for failure-message: {}, code: {}, body: {}", failedMessageRequest,
                        responseEntity.getStatusCode(), responseEntity.getBody());
            } catch (HttpStatusCodeException httpStatusCodeException) {
                log.error("Not able to send the failure notification to the client, failed-message:{}, http-code: {}",
                        failedMessageRequest, httpStatusCodeException.getStatusCode(), httpStatusCodeException);
            } catch (Exception retryException) {
                log.error("Not able to send the failure notification to the client, failed-message:{}",
                        failedMessageRequest, retryException);
            }
        }
    }

}
