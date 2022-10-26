package com.xyz.utility.common.jaeger;

import io.jaegertracing.internal.JaegerTracer;
import io.jaegertracing.internal.MDCScopeManager;
import io.jaegertracing.internal.reporters.RemoteReporter;
import io.jaegertracing.internal.samplers.ProbabilisticSampler;
import io.jaegertracing.spi.Reporter;
import io.jaegertracing.spi.Sender;
import io.jaegertracing.thrift.internal.senders.HttpSender;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import lombok.Data;
import org.apache.thrift.transport.TTransportException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("jaeger")
public class JaegerConfig {

    private String agentEndpoint;
    private double samplingProbability;
    private String serviceName;

    @Bean
    public Tracer jaegerTracer() throws TTransportException {
        MDCScopeManager scopeManager = new MDCScopeManager.Builder().build();

        ProbabilisticSampler sampler = new ProbabilisticSampler(samplingProbability);
        Sender sender = new HttpSender.Builder(agentEndpoint).build();

        Reporter reporter = new RemoteReporter.Builder()
                .withSender(sender)
                .build();

        JaegerTracer tracer = new JaegerTracer.Builder(serviceName)
                .withScopeManager(scopeManager)
                .withSampler(sampler)
                .withReporter(reporter)
                .build();

        GlobalTracer.registerIfAbsent(tracer);
        return tracer;
    }

}
