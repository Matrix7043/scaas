package org.scaas.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.scaas.domain.enumerations.Runtime;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
class FunctionConcurrencyIT {

    @LocalServerPort
    private int port;

    private RestClient restClient;
    private HttpHeaders headers;
    private FunctionResponse function;
    private String token;

    @BeforeEach
    void setup() throws Exception {

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        restClient.post()
                .uri("/auth/register")
                .body(RegisterRequest.builder()
                        .email("sekk@test.com")
                        .firstName("sekk")
                        .lastName("appan")
                        .username("blank")
                        .password("password123")
                        .build())
                .retrieve()
                .toBodilessEntity();

        token = restClient.post()
                .uri("/auth/login")
                .body(LoginRequest.builder()
                        .email("sekk@test.com")
                        .password("password123")
                        .build())
                .retrieve()
                .body(String.class);

        assertNotNull(token);

        headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        function = restClient.post()
                .uri("/functions")
                .headers(h -> h.addAll(headers))
                .body(CreateFunctionRequest.builder()
                        .name("test1")
                        .runtime(Runtime.PYTHON)
                        .build())
                .retrieve()
                .body(FunctionResponse.class);

        assertNotNull(function);

        byte[] fileBytes = Files.readAllBytes(
                Path.of("src/test/resources/test.py")
        );

        ByteArrayResource resource = new ByteArrayResource(fileBytes) {
            @Override
            public String getFilename() {
                return "test.py";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);

        ResponseEntity<Void> response =
                restClient.post()
                        .uri("/functions/" + function.id() + "/artifacts")
                        .headers(h -> h.setBearerAuth(token))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();

        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

    @Test
    void deploymentAndUpdateConcurrently_shouldReturnValidResponse()
            throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);

        List<ResponseEntity<Void>> responses =
                Collections.synchronizedList(new ArrayList<>());

        executor.execute(() -> {
            try {
                startLatch.await();

                ResponseEntity<Void> deployResponse =
                        restClient.post()
                                .uri("/functions/" + function.id() + "/deploy")
                                .headers(h -> h.addAll(headers))
                                .exchange((req, res) ->
                                        ResponseEntity.status(res.getStatusCode()).build());

                responses.add(deployResponse);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.execute(() -> {
            try {
                startLatch.await();

                ResponseEntity<Void> updateResponse =
                        restClient.patch()
                                .uri("/functions/" + function.id())
                                .headers(h -> h.addAll(headers))
                                .body(UpdateFunctionRequest.builder()
                                        .name("Test3")
                                        .build())
                                .exchange((req, res) ->
                                        ResponseEntity.status(res.getStatusCode()).build());

                responses.add(updateResponse);

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();


        assertEquals(2, responses.size());

        long successCount = responses.stream()
                .filter(r -> r.getStatusCode().is2xxSuccessful())
                .count();

        long conflictCount = responses.stream()
                .filter(r -> r.getStatusCode().value() == 409)
                .count();

        assertEquals(1, successCount);
        assertEquals(1, conflictCount);
    }
}
