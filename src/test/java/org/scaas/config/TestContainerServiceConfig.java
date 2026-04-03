package org.scaas.config;

import org.scaas.exceptions.DeploymentServiceException;
import org.scaas.services.DeploymentService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.util.UUID;

@TestConfiguration
public class TestContainerServiceConfig {

    @Bean
    @Primary
    public DeploymentService deploymentService() {
        return new DeploymentService() {
            @Override
            public String deploy(UUID id, String hashCode, File file,
                                 double cpuCores, int memory, int pids)
                    throws DeploymentServiceException {
                return "deployment_" + id + "_" + hashCode;
            }

            @Override
            public void deleteDeployment(UUID id, String hashCode) {
                // no-op
            }
        };
    }
}