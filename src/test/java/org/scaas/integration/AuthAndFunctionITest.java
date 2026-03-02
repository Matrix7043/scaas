package org.scaas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.scaas.domain.entites.Function;
import org.scaas.domain.repositories.FunctionRepository;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthAndFunctionITest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FunctionRepository functionRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    AuthAndFunctionITest() {
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private String registerAndLogin(String email) throws Exception {

        mockMvc.perform(post("/auth/register")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("""
                        {
                            "username": "%s",
                            "firstName": "Test",
                            "lastName": "User",
                            "email": "%s",
                            "password": "password123"
                        }
                        """.formatted(email, email)))
                .andExpect(status().isCreated());

        return mockMvc.perform(post("/auth/login")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("""
                        {
                            "email": "%s",
                            "password": "password123"
                        }
                        """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    private UUID createFunction(String token, String name) throws Exception {

        String response = mockMvc.perform(post("/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("""
                        {
                            "name": "%s",
                            "runtime": "PYTHON"
                        }
                        """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FunctionResponse function =
                objectMapper.readValue(response, FunctionResponse.class);

        return function.id();
    }

    private UUID createDeployedFunction(String token) throws Exception {

        UUID id = createFunction(token, "deploy");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original.py",
                "text/plain",
                "print('hello world!!')".getBytes()
        );

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                        .header("Authorization", "Bearer " + token)
                        .file(file))
                .andExpect(status().isOk());

        mockMvc.perform(post("/functions/{id}/deploy", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return id;
    }

    @Test
    void fullFlow_register_login_createAndListFunction() throws Exception {

        String token = registerAndLogin("user@test.com");

        createFunction(token, "hello");
        createFunction(token, "world");
        createFunction(token, "!!!");

        mockMvc.perform(get("/functions?page=0&size=2")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @ParameterizedTest
    @MethodSource("org.scaas.testdata.Validation#invalidRegisterRequests")
    void register_validatesTheFieldAndGivesMessageIfInvalid(String requestJson) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("org.scaas.testdata.Validation#invalidLoginRequests")
    void login_validatesTheFieldAndGivesMessageIfValid(String requestJson) throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("org.scaas.testdata.Validation#invalidCreateFunctionRequests")
    void createFunction_validatesTheFieldAndGivesMessageIfValid(String requestJson) throws Exception {
        String token = registerAndLogin("validate@test.com");
        mockMvc.perform(post("/functions")
                .header("Authorization", "Bearer " + token)
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @MethodSource("org.scaas.testdata.Validation#invalidPatchRequests")
    void updateFunction_validatesTheFieldAndGivesMessageIfValid(String requestJson) throws Exception {
        String token = registerAndLogin("updateF@test.com");
        UUID id = createFunction(token, "updateF");
        mockMvc.perform(patch("/functions/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getById_returnsFunctionIfOwner_elseNotFound() throws Exception {

        String tokenA = registerAndLogin("a@test.com");
        String tokenB = registerAndLogin("b@test.com");

        UUID idA = createFunction(tokenA, "helloA");
        UUID idB = createFunction(tokenB, "helloB");

        mockMvc.perform(get("/functions/" + idA)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("helloA"));

        mockMvc.perform(get("/functions/" + idB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateById_updatesIfOwner_elseNotFound() throws Exception {

        String tokenA = registerAndLogin("owner@test.com");
        String tokenB = registerAndLogin("other@test.com");

        UUID id = createFunction(tokenA, "original");

        String updateJson = """
                {
                    "name": "updated",
                    "entryPoint": "handler"
                }
                """;

        mockMvc.perform(patch("/functions/" + id)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("updated"))
                .andExpect(jsonPath("$.runtime").value("PYTHON"));

        mockMvc.perform(patch("/functions/" + id)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(updateJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteById_deletesIfOwner_elseNotFound() throws Exception {

        String tokenA = registerAndLogin("owner@test.com");
        String tokenB = registerAndLogin("other@test.com");

        UUID id = createFunction(tokenA, "original");
        createFunction(tokenA, "Original");

        mockMvc.perform(delete("/functions/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/functions")
                        .header("Authorization", "Bearer " + tokenA))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.content[0].name").value("Original"))
                        .andExpect(jsonPath("$.content.length()").value(1));

        mockMvc.perform(get("/functions/" + id)
                .header("Authorization", "Bearer " + tokenA))
                        .andExpect(status().isNotFound());

        mockMvc.perform(delete("/functions/" + id)
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/functions/" + id)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isNotFound());
    }

    @Test
    void list_shouldRespectMaxPageSize() throws Exception {

        String token = registerAndLogin("maxPageSize@test.com");

        for (int i = 0; i < 200; i++){
            createFunction(token, "f" + i);
        }

        mockMvc.perform(get("/functions?page=0&size=1000")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(100));
    }

    @Test
    void uploadArtifact_success() throws Exception {

        String token = registerAndLogin("artifact@test.com");

        UUID id = createFunction(token, "artifact");

        MockMultipartFile file = new MockMultipartFile("file",
                "artifact.py",
                "text/plain",
                "print('hello world')".getBytes());

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                .header("Authorization", "Bearer " + token)
                .file(file))
                .andExpect(status().isOk());

        mockMvc.perform(get("/functions/" + id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("artifact"))
                .andExpect(jsonPath("$.hasArtifact").value(true));

        Function function = functionRepository.findById(id).orElse(null);
        assertNotNull(function);

        String path = function.getStoragePath();
        assertTrue(Files.exists(Paths.get(path)));
    }

    @Test
    void uploadArtifact_doesNotUploadIfFileIsTheSame() throws Exception {

        String token = registerAndLogin("artifact@test.com");

        UUID id = createFunction(token, "artifact");

        MockMultipartFile file = new MockMultipartFile("file",
                "artifact.py",
                "text/plain",
                "print('hello world')".getBytes());

        MockMultipartFile fileDuplicate = new MockMultipartFile("file",
                "artifact.py",
                "text/plain",
                "print('hello world')".getBytes());

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                        .header("Authorization", "Bearer " + token)
                        .file(file))
                .andExpect(status().isOk());

        mockMvc.perform(get("/functions/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("artifact"))
                .andExpect(jsonPath("$.hasArtifact").value(true));

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                .header("Authorization", "Bearer " + token)
                .file(fileDuplicate))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadArtifact_updatesIfAlreadyPresent() throws Exception {

        String token = registerAndLogin("artifact@test.com");

        UUID id = createFunction(token, "artifact");

        MockMultipartFile file = new MockMultipartFile("file",
                "artifact.py",
                "text/plain",
                "print('hello world')".getBytes());

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                        .header("Authorization", "Bearer " + token)
                        .file(file))
                .andExpect(status().isOk());

        mockMvc.perform(get("/functions/" + id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("artifact"))
                .andExpect(jsonPath("$.hasArtifact").value(true));

        MockMultipartFile file2 = new MockMultipartFile("file",
                "artifiact.py",
                "text/plain",
                "print('hello world!!')".getBytes());

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                .header("Authorization", "Bearer " + token)
                .file(file2))
                .andExpect(status().isOk());

        Function function = functionRepository.findById(id).orElse(null);
        assertNotNull(function);

        String path = function.getStoragePath();
        Path path1 = Paths.get(path);
        assertTrue(Files.exists(path1));
        String content = Files.readString(path1);
        assertEquals("print('hello world!!')", content);
    }

    @Test
    void functionDeploy_shouldNotDeployFunctionIfFileIsNotPresent() throws Exception {

        String token = registerAndLogin("deploy@test.com");
        UUID id = createFunction(token, "deploy");

        mockMvc.perform(post("/functions/{id}/deploy", id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void functionDeploy_shouldDeployFunction() throws Exception {

        String token = registerAndLogin("deploy@test.com");
        UUID id = createDeployedFunction(token);

        mockMvc.perform(get("/functions/{id}", id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deploymentStatus").value("DEPLOYED"))
                .andExpect(jsonPath("$.invocationURL").isNotEmpty());

    }

    @Test
    void functionDeploy_shouldNotDeployFunctionIfAlreadyPresent() throws Exception {
        String token = registerAndLogin("deploy@test.com");
        UUID id = createDeployedFunction(token);

        mockMvc.perform(post("/functions/{id}/deploy", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No visible changes found for redeployment"));
    }

    @Test
    void functionDeploy_shouldRedeployIfAlreadyDeployedAndIsNewFile() throws Exception {
        String token = registerAndLogin("deploy@test.com");
        UUID id = createDeployedFunction(token);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "original.py",
                "text/plain",
                "print('hello world!!!!')".getBytes()
        );

        mockMvc.perform(multipart("/functions/{id}/artifacts", id)
                .header("Authorization", "Bearer " + token)
                .file(file)).andExpect(status().isOk());

        mockMvc.perform(post("/functions/{id}/deploy", id)
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

    }


    @Test
    void functions_withoutAuthIsForbidden() throws Exception {

        mockMvc.perform(post("/functions")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
