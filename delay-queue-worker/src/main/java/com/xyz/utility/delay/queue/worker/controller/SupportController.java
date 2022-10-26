package com.xyz.utility.delay.queue.worker.controller;

import com.xyz.utility.common.constant.UtilityConstant;
import com.xyz.utility.common.kafka.dto.DelayMessageRequest;
import com.xyz.utility.common.schema.ApiResponse;
import com.xyz.utility.delay.queue.worker.service.SupportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/support")
@RequiredArgsConstructor
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/aerospike/save/delay-message")
    public ApiResponse<Boolean> saveToAerospikeDelaySet(@RequestParam(UtilityConstant.Kafka.MESSAGE_ID) String messageId,
                                                        @RequestParam("key") String key,
                                                        @RequestBody DelayMessageRequest request) {
        supportService.saveToDelayDb(messageId, key, request);
        return new ApiResponse<>(true);
    }

    @GetMapping("/aerospike/save/delay-message/load")
    public ApiResponse<Boolean> saveToAerospikeDelaySet(@RequestParam("count") int count) {
        supportService.saveToDelayDb(count);
        return new ApiResponse<>(true);
    }

}
