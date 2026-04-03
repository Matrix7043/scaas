package org.scaas.services.impl;

import org.scaas.exceptions.DeploymentServiceException;
import org.scaas.services.DeploymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.File;
import java.util.Map;
import java.util.UUID;

@Service
public class ContainerService implements DeploymentService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${runner.base-url}")
    private String runnerBaseUrl;

    @Override
    public String deploy(UUID id, String hashCode, File file,
                         double cpuCores, int memory, int pids)
            throws DeploymentServiceException {
        try {
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(file));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            String url = UriComponentsBuilder
                    .fromUriString(runnerBaseUrl + "/deploy")
                    .queryParam("function_id", id.toString())
                    .queryParam("hash_code", hashCode)
                    .queryParam("entry_point", "main.handler")
                    .queryParam("cpu_cores", cpuCores)
                    .queryParam("memory_mb", memory)
                    .queryParam("pid_limit", pids)
                    .toUriString();

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new DeploymentServiceException("Deploy failed");
            }

            assert response.getBody() != null;
            return (String) response.getBody().get("invocation_url");

        } catch (DeploymentServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentServiceException("Deploy failed");
        }
    }

    @Override
    public void deleteDeployment(UUID id, String hashCode) {
        try {
            String deploymentId = "deployment_" + id + "_" + hashCode;
            restTemplate.delete(runnerBaseUrl + "/deployments/" + deploymentId);
        } catch (Exception e) {
            // Swallow — idempotent, may already be deleted
        }
    }
}