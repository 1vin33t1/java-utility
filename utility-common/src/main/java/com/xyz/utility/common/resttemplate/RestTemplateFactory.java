package com.xyz.utility.common.resttemplate;

import com.xyz.utility.common.resttemplate.dto.RestTemplateProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * For using this Factory class
 * 1. Add enum(let us say NAME1(<name1>)) in RestTemplateName
 * 2. Add three properties in application properties
 * a. <name1>.connect.timeout
 * b. <name1>.read.timeout
 * c. <name1>.max.connection
 * d. <name1>.proxy.enabled
 */

@Slf4j
@Component
@ConditionalOnProperty(value = "rest-template-utils.enable", havingValue = "true")
public class RestTemplateFactory {

    private static final String CONNECT_TIMEOUT_SUFFIX = ".connect.timeout";
    private static final String READ_TIMEOUT_SUFFIX = ".read.timeout";
    private static final String MAX_CONNECTION_SUFFIX = ".max.connection";
    private static final String PROXY_ENABLE_SUFFIX = ".proxy.enabled";


    private final Map<RestTemplateName, RestTemplate> restTemplateMap;

    @Autowired
    public RestTemplateFactory(Environment environment,
                               @Value("${rest-template-utils.ssl-validation-disabled-profiles}") List<String> sslValidationDisableProfiles) {
        restTemplateMap = new EnumMap<>(RestTemplateName.class);
        for (RestTemplateName restTemplateName : RestTemplateName.values()) {
            RestTemplateProperties properties = getRestTemplateProperties(restTemplateName, environment);
            restTemplateMap.put(restTemplateName, RestTemplateUtil.createRestTemplate(properties,
                    environment.getActiveProfiles(), sslValidationDisableProfiles));
        }
    }

    private RestTemplateProperties getRestTemplateProperties(RestTemplateName restTemplateName, Environment environment) {
        RestTemplateProperties properties = new RestTemplateProperties();
        log.info("restTemplateName : [{}]", restTemplateName);
        try {
            properties.setConnectTimeout(environment.getProperty(restTemplateName.prefix + CONNECT_TIMEOUT_SUFFIX, Integer.class));
            properties.setReadTimeout(environment.getProperty(restTemplateName.prefix + READ_TIMEOUT_SUFFIX, Integer.class));
            properties.setMaxConnection(environment.getProperty(restTemplateName.prefix + MAX_CONNECTION_SUFFIX, Integer.class));
            properties.setSystemProxyEnable(environment.getProperty(restTemplateName.prefix + PROXY_ENABLE_SUFFIX, Boolean.class));

            log.info("properties for restTemplate {}", properties);
        } catch (Exception ex) {
            throw new RestClientException("Properties not available for " + restTemplateName);
        }
        return properties;
    }

    public RestTemplate getRestTemplate(RestTemplateName restTemplateName) {
        return restTemplateMap.getOrDefault(restTemplateName, null);
    }

}
