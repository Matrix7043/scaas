package org.scaas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthAndFunctionITest {

    @Autowired
    private MockMvc mockMvc;

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
                .andExpect(status().isOk());

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
                            "runtime": "PYTHON",
                            "entryPoint": "handler"
                        }
                        """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        FunctionResponse function =
                objectMapper.readValue(response, FunctionResponse.class);

        return function.id();
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

        mockMvc.perform(delete("/functions/" + id)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("original"))
                .andExpect(jsonPath("$.runtime").value("PYTHON"));

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
    void functions_withoutAuthIsForbidden() throws Exception {

        mockMvc.perform(post("/functions")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
