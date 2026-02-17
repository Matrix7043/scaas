package org.scaas.integration;

import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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
    void functions_withoutAuthIsForbidden() throws Exception {

        mockMvc.perform(post("/functions")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content("{}"))
                .andExpect(status().isForbidden());

    }
}
