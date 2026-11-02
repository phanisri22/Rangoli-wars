package com.rangoliwars.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    private ResultActions send(String path, String username, String password) throws Exception {
        String body = mapper.writeValueAsString(new Creds(username, password));
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    record Creds(String username, String password) {
    }

    @Test
    void registerReturnsTokenAndLowercasesName() throws Exception {
        send("/api/auth/register", "Asha_1", "secret123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("asha_1"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void duplicateUsernameIsConflict() throws Exception {
        send("/api/auth/register", "ravi", "secret123").andExpect(status().isCreated());
        send("/api/auth/register", "RAVI", "other123")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("That username is already taken"));
    }

    @Test
    void invalidInputIsRejectedWithMessage() throws Exception {
        send("/api/auth/register", "ab", "secret123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Username must be 3-20 letters, digits or _"));
        send("/api/auth/register", "meera", "123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Password must be 6-72 characters"));
    }

    @Test
    void loginChecksPassword() throws Exception {
        send("/api/auth/register", "dev", "secret123").andExpect(status().isCreated());
        send("/api/auth/login", "dev", "wrongpass")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        send("/api/auth/login", "nobody", "secret123").andExpect(status().isUnauthorized());
        send("/api/auth/login", "DEV", "secret123").andExpect(status().isOk());
    }

    @Test
    void meNeedsValidToken() throws Exception {
        String json = send("/api/auth/register", "kiran", "secret123").andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(json).get("token").asText();

        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer bad.token.here"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("kiran"))
                .andExpect(jsonPath("$.role").value("PLAYER"));
    }

    @Test
    void malformedBodyIsBadRequest() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest());
    }
}
