package org.scaas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.scaas.protocol.responses.FunctionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthAndFunctionITest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();


    @Test
    void fullFlow_register_login_createAndListFunction() throws Exception {

        String registerJson = """
                {
                    "username": "testUser",
                    "firstName": "Test",
                    "lastName": "User",
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(registerJson))
                .andExpect(status().isOk());

        String loginJson = """
                {
                    "email": "test@test.com",
                    "password": "password123"
                }
                """;

        String token = mockMvc.perform(post("/auth/login")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String functionJson = """
                {
                    "name": "hello",
                    "runtime": "java17",
                    "entryPoint": "handler"
                }
                """;

        mockMvc.perform(post("/functions")
                .header("Authorization", "Bearer " + token)
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content(functionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hello"));

        mockMvc.perform(get("/functions")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

    }

    @Test
    void getById_returnsFunctionThatBelongsToUserOrForbids() throws Exception {

        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        String registerJson = """
                {
                    "username": "testUserA",
                    "firstName": "Test",
                    "lastName": "User",
                    "email": "testA@test.com",
                    "password": "password123"
                }
                """;

        String registerJson2 = """
                {
                    "username": "testUserB",
                    "firstName": "Test",
                    "lastName": "User",
                    "email": "testB@test.com",
                    "password": "password123"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(registerJson))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/register")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(registerJson2))
                .andExpect(status().isOk());

        String loginJson = """
                {
                    "email": "testA@test.com",
                    "password": "password123"
                }
                """;

        String loginJson2 = """
                {
                    "email": "testB@test.com",
                    "password": "password123"
                }
                """;

        String token = mockMvc.perform(post("/auth/login")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String token2 = mockMvc.perform(post("/auth/login")
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(loginJson2))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String functionJson = """
                {
                    "name": "hello",
                    "runtime": "java17",
                    "entryPoint": "handler"
                }
                """;

        String functionJson2 = """
                {
                    "name": "hello2",
                    "runtime": "java17",
                    "entryPoint": "handler"
                }
                """;


        String response = mockMvc.perform(post("/functions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(functionJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hello"))
                        .andReturn()
                                .getResponse()
                                        .getContentAsString();


        String response2 = mockMvc.perform(post("/functions")
                        .header("Authorization", "Bearer " + token2)
                        .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                        .content(functionJson2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hello2"))
                        .andReturn()
                                .getResponse()
                                        .getContentAsString();


        FunctionResponse functionResponse = objectMapper.readValue(response, FunctionResponse.class);
        FunctionResponse functionResponse2 = objectMapper.readValue(response2, FunctionResponse.class);

        UUID Fid = functionResponse.id();
        UUID Fid2 = functionResponse2.id();

        mockMvc.perform(get("/functions/"+Fid.toString())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("hello"));

        // TODO: Add Global Exception Handler and change this mock
        // TODO: Unit Test for the new org.scaas.service.FunctionService.getById method
        assertThrows(ServletException.class, () -> mockMvc.perform(get("/functions/"+Fid2.toString())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().is5xxServerError()));
    }

    @Test
    void functions_withoutAuthIsForbidden() throws Exception {

        mockMvc.perform(post("/functions")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content("{}"))
                .andExpect(status().isForbidden());

    }
}
