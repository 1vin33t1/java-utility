package com.xyz.utility.persistence.config;

import com.aerospike.client.Host;
import com.aerospike.client.policy.ClientPolicy;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.aerospike.config.AbstractAerospikeDataConfiguration;
import org.springframework.data.aerospike.repository.config.EnableAerospikeRepositories;

import java.util.Collection;

@Data
@Configuration
@EqualsAndHashCode(callSuper = true)
@EnableConfigurationProperties(AerospikeRepositoryConfiguration.class)
@ConditionalOnProperty(value = "aerospike.enable", havingValue = "true")
@ConfigurationProperties("aerospike")
@EnableAerospikeRepositories("com.xyz.utility.persistence.repository.aerospike")
public class AerospikeRepositoryConfiguration extends AbstractAerospikeDataConfiguration {

    private String hosts;
    private String namespace;
    private boolean authRequired;
    private String user;
    private String password;


    @Override
    protected Collection<Host> getHosts() {
        return Host.parseServiceHosts(hosts);
    }

    @Override
    protected String nameSpace() {
        return namespace;
    }

    @Override
    protected ClientPolicy getClientPolicy() {
        ClientPolicy clientPolicy = new ClientPolicy();
        if (authRequired) {
            clientPolicy.user = user;
            clientPolicy.password = password;
        }
        return clientPolicy;
    }
}