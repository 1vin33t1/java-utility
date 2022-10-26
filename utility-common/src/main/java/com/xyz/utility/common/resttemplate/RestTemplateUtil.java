package com.xyz.utility.common.resttemplate;


import com.xyz.utility.common.resttemplate.dto.RestTemplateProperties;
import io.opentracing.contrib.apache.http.client.TracingHttpClientBuilder;
import lombok.experimental.UtilityClass;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class RestTemplateUtil {

    static RestTemplate createRestTemplate(RestTemplateProperties restTemplateProperties, String[] activeProfiles, List<String> sslValidationDisableProfiles) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(getRequestFactory(restTemplateProperties, activeProfiles, sslValidationDisableProfiles));
        return restTemplate;
    }

    private static HttpComponentsClientHttpRequestFactory getRequestFactory(RestTemplateProperties restTemplateProperties,
                                                                            String[] activeProfiles, List<String> sslValidationDisableProfiles) {

        PoolingHttpClientConnectionManager connManager = getConnectionManager(activeProfiles, sslValidationDisableProfiles);
        connManager.setMaxTotal(restTemplateProperties.getMaxConnection());
        connManager.setDefaultMaxPerRoute(restTemplateProperties.getMaxConnection());
        HttpClientBuilder clientBuilder = TracingHttpClientBuilder.create()
                .setConnectionManager(connManager)
                .disableCookieManagement();

        if (restTemplateProperties.isSystemProxyEnable()) {
            clientBuilder.useSystemProperties();
        }
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(clientBuilder.build());
        factory.setConnectTimeout(restTemplateProperties.getConnectTimeout());
        factory.setReadTimeout(restTemplateProperties.getReadTimeout());
        return factory;
    }

    private static PoolingHttpClientConnectionManager getConnectionManager(String[] activeProfiles, List<String> sslValidationDisableProfiles) {
        PoolingHttpClientConnectionManager connManager;
        try {
            if (sslValidationDisableProfiles.containsAll(Arrays.asList(activeProfiles))) {
                Registry socketFactoryRegistry = RegistryBuilder.create().
                        register("https", getDisabledHostNameSSLContext())
                        .register("http", PlainConnectionSocketFactory.getSocketFactory()).build();
                connManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            } else {
                connManager = new PoolingHttpClientConnectionManager();
            }
        } catch (GeneralSecurityException e) {
            connManager = new PoolingHttpClientConnectionManager();
        }
        return connManager;
    }

    private static SSLConnectionSocketFactory getDisabledHostNameSSLContext() throws GeneralSecurityException {
        TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;

        SSLContext sslContext = org.apache.http.ssl.SSLContexts.custom()
                .loadTrustMaterial(null, acceptingTrustStrategy)
                .build();

        return new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
    }
}
