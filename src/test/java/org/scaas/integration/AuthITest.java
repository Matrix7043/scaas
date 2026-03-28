package org.scaas.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.MediaType;
import org.junit.jupiter.api.Test;
import org.scaas.protocol.requests.LoginRequest;
import org.scaas.protocol.requests.RefreshTokenRequest;
import org.scaas.protocol.requests.RegisterRequest;
import org.scaas.protocol.responses.AuthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthITest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final String EMAIL = "test@mail.com";
    private final String PASSWORD = "password123";


    private void register() throws Exception {

        RegisterRequest request = RegisterRequest.builder()
                .email(EMAIL)
                .firstName("test")
                .lastName("test")
                .username("test")
                .password(PASSWORD).build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }


    private AuthResponse login() throws Exception {

        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password(PASSWORD).build();

        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(response, AuthResponse.class);
    }


    @Test
    void should_register_user() throws Exception {
        register();
    }


    @Test
    void should_login_successfully() throws Exception {
        register();
        login();
    }


    @Test
    void should_fail_login_with_wrong_password() throws Exception {

        register();

        LoginRequest request = LoginRequest.builder()
                .email(EMAIL)
                .password("wrong_password")
                .build();

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void refresh_should_work_before_logout() throws Exception {

        register();
        AuthResponse login = login();

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(
                                RefreshTokenRequest.builder()
                                        .refreshToken(login.refreshToken())
                                        .build())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));
    }


    @Test
    void should_logout_and_invalidate_refresh_token() throws Exception {

        register();
        AuthResponse login = login();

        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .header("Authorization", "Bearer " + login.accessToken())
                        .content(objectMapper.writeValueAsString(
                                RefreshTokenRequest.builder()
                                        .refreshToken(login.refreshToken())
                                        .build())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(
                                RefreshTokenRequest.builder()
                                        .refreshToken(login.refreshToken())
                                        .build())))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void should_logout_all_sessions() throws Exception {

        register();

        AuthResponse login1 = login();
        AuthResponse login2 = login();

        String accessToken = login1.accessToken();

        mockMvc.perform(post("/auth/logout-all")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(
                                RefreshTokenRequest.builder()
                                        .refreshToken(login1.refreshToken())
                                        .build())))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON.toString())
                        .content(objectMapper.writeValueAsString(
                                RefreshTokenRequest.builder()
                                        .refreshToken(login2.refreshToken())
                                        .build())))
                .andExpect(status().isUnauthorized());
    }
}
