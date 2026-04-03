package org.scaas.concurrency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.scaas.config.TestContainerServiceConfig;
import org.scaas.domain.entites.Function;
import org.scaas.domain.enumerations.Runtime;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.protocol.requests.CreateFunctionRequest;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.scaas.protocol.requests.UpdateFunctionRequest;
import org.scaas.protocol.responses.AuthResponse;
import org.scaas.protocol.responses.FunctionResponse;
import org.scaas.testdata.Action;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestContainerServiceConfig.class)
class FunctionConcurrencyITest {

    @LocalServerPort
    private int port;

    @Autowired
    private FunctionRepository functionRepository;

    private RestClient restClient;
    private HttpHeaders headers;
    private FunctionResponse function;
    private String token;

    private Callable<ResponseEntity<Void>> actionToCallable(Action action) {
        return switch (action) {
            case DEPLOY -> this::deploy;
            case UPDATE -> this::update;
        };
    }

    private ResponseEntity<Void> deploy() {
        return restClient.post()
                .uri("/functions/" + function.id() + "/deploy")
                .headers(h -> h.addAll(headers))
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());
    }

    private ResponseEntity<Void> update() {
        return restClient.patch()
                .uri("/functions/" + function.id())
                .headers(h -> h.addAll(headers))
                .body(UpdateFunctionRequest.builder()
                        .name("Updated")
                        .build())
                .exchange((req, res) -> ResponseEntity.status(res.getStatusCode()).build());
    }

    @BeforeEach
    void setup() throws Exception {

        this.restClient = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .build();

        UUID id = UUID.randomUUID();

        restClient.post()
                .uri("/auth/register")
                .body(RegisterRequest.builder()
                        .email("sekk" + id + "@test.com")
                        .firstName("sekk")
                        .lastName("appan")
                        .username("blank" + id)
                        .password("password123")
                        .build())
                .retrieve()
                .toBodilessEntity();

        AuthResponse authResponse = restClient.post()
                .uri("/auth/login")
                .body(LoginRequest.builder()
                        .email("sekk" + id + "@test.com")
                        .password("password123")
                        .build())
                .retrieve()
                .body(AuthResponse.class);

        assertNotNull(authResponse);
        assertNotNull(authResponse.refreshToken());

        token = authResponse.accessToken();

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

    private void runConcurrent(
            java.util.concurrent.Callable<ResponseEntity<Void>> action1,
            java.util.concurrent.Callable<ResponseEntity<Void>> action2
    ) {

        List<ResponseEntity<Void>> responses =
                Collections.synchronizedList(new ArrayList<>());

        try(ExecutorService executor = Executors.newFixedThreadPool(2)) {
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);

            executor.execute(() -> {
                try {
                    startLatch.await();
                    responses.add(action1.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });

            executor.execute(() -> {
                try {
                    startLatch.await();
                    responses.add(action2.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown();
            doneLatch.await();
        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }

        assertEquals(2, responses.size());

        long success = responses.stream()
                .filter(r -> r.getStatusCode().is2xxSuccessful())
                .count();

        long conflict = responses.stream()
                .filter(r -> r.getStatusCode().value() == 409)
                .count();

        assertEquals(1, success);
        assertEquals(1, conflict);
    }

    @ParameterizedTest
    @MethodSource("org.scaas.testdata.MatrixGenerator#actionPairs")
    void stateTransitionMatrix(Action a1, Action a2, int minDelta, int maxDelta) {

        long originalVersion = functionRepository
                .findById(function.id())
                .orElseThrow()
                .getVersion();

        runConcurrent(
                actionToCallable(a1),
                actionToCallable(a2)
        );

        Function fresh = functionRepository
                .findById(function.id())
                .orElseThrow();

        long versionDelta = fresh.getVersion() - originalVersion;

        assertTrue(minDelta <= versionDelta);
        assertTrue(maxDelta >= versionDelta);
    }
}
