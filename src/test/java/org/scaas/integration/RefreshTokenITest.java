package org.scaas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.scaas.protocol.responses.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@TestPropertySource(
        properties = {
                "jwt.access-token-expiration=2000",
                "jwt.refresh-token-expiration=60000"
        }
)
public class RefreshTokenITest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper mapper = new ObjectMapper();

    RefreshTokenITest(){
       mapper.registerModule(new JavaTimeModule());
       mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void should_refresh_access_token_when_expired() throws Exception {

        String email = "sekk@email.com";

        mockMvc.perform(post("/auth/register")
                        .contentType(String.valueOf(org.junit.jupiter.api.MediaType.APPLICATION_JSON))
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

        String response = mockMvc.perform(post("/auth/login")
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

        AuthResponse authResponse = mapper.readValue(response, AuthResponse.class);

        assertNotNull(authResponse);
        assertNotNull(authResponse.accessToken());
        assertNotNull(authResponse.refreshToken());

        String accessToken = authResponse.accessToken();
        String refreshToken = authResponse.refreshToken();

        Thread.sleep(2000);

        mockMvc.perform(get("/functions")
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());

        String refreshResponse = mockMvc.perform(post("/auth/refresh")
                .contentType(String.valueOf(MediaType.APPLICATION_JSON))
                .content("""
                        {
                            "refreshToken": "%s"
                        }
                        """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AuthResponse refreshAuthResponse = mapper.readValue(refreshResponse, AuthResponse.class);

        assertNotNull(refreshAuthResponse);
        assertNotNull(refreshAuthResponse.accessToken());
        assertNotNull(refreshAuthResponse.refreshToken());

        String newAccessToken = refreshAuthResponse.accessToken();

        mockMvc.perform(get("/functions")
                        .header("Authorization", "Bearer " + newAccessToken))
                .andExpect(status().isOk());

    }

}
