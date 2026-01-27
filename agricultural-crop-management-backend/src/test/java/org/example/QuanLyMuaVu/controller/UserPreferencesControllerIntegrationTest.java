package org.example.QuanLyMuaVu.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.QuanLyMuaVu.Entity.Role;
import org.example.QuanLyMuaVu.Entity.User;
import org.example.QuanLyMuaVu.Enums.UserStatus;
import org.example.QuanLyMuaVu.Repository.RoleRepository;
import org.example.QuanLyMuaVu.Repository.UserPreferencesRepository;
import org.example.QuanLyMuaVu.Repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserPreferencesControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserPreferencesRepository userPreferencesRepository;

    private static final String TEST_PASSWORD = "testPassword123";
    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        Role farmerRole = roleRepository.findByCode("FARMER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code("FARMER")
                        .name("Farmer")
                        .description("Farm owner")
                        .build()));

        userA = User.builder()
                .username("prefs_user_a")
                .email("prefs.a@test.local")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(farmerRole)))
                .build();
        userA = userRepository.saveAndFlush(userA);

        userB = User.builder()
                .username("prefs_user_b")
                .email("prefs.b@test.local")
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(farmerRole)))
                .build();
        userB = userRepository.saveAndFlush(userB);
    }

    @Test
    @DisplayName("GET /api/v1/preferences/me returns defaults and auto-creates record")
    void getMyPreferences_DefaultsCreated() throws Exception {
        String token = loginAndGetToken(userA.getEmail(), TEST_PASSWORD);

        mockMvc.perform(get("/api/v1/preferences/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currency", is("VND")))
                .andExpect(jsonPath("$.result.weightUnit", is("KG")))
                .andExpect(jsonPath("$.result.locale", is("vi-VN")));

        userPreferencesRepository.findByUser_Id(userA.getId())
                .orElseThrow();
    }

    @Test
    @DisplayName("PUT /api/v1/preferences/me supports partial update")
    void updateMyPreferences_PartialUpdate() throws Exception {
        String token = loginAndGetToken(userA.getEmail(), TEST_PASSWORD);

        Map<String, Object> payload = Map.of("currency", "USD");

        mockMvc.perform(put("/api/v1/preferences/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currency", is("USD")))
                .andExpect(jsonPath("$.result.weightUnit", is("KG")))
                .andExpect(jsonPath("$.result.locale", is("vi-VN")));
    }

    @Test
    @DisplayName("PUT /api/v1/preferences/me rejects invalid enums")
    void updateMyPreferences_InvalidEnum() throws Exception {
        String token = loginAndGetToken(userA.getEmail(), TEST_PASSWORD);

        Map<String, Object> payload = Map.of("currency", "BAD");

        mockMvc.perform(put("/api/v1/preferences/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("ERR_BAD_REQUEST")));
    }

    @Test
    @DisplayName("User updates do not affect other users")
    void updateMyPreferences_IsolatedPerUser() throws Exception {
        String tokenA = loginAndGetToken(userA.getEmail(), TEST_PASSWORD);
        String tokenB = loginAndGetToken(userB.getEmail(), TEST_PASSWORD);

        Map<String, Object> payload = Map.of("currency", "USD");

        mockMvc.perform(put("/api/v1/preferences/me")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currency", is("USD")));

        mockMvc.perform(get("/api/v1/preferences/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.currency", is("VND")))
                .andExpect(jsonPath("$.result.weightUnit", is("KG")));
    }

    private String loginAndGetToken(String identifier, String password) throws Exception {
        Map<String, Object> loginPayload = Map.of(
                "identifier", identifier,
                "password", password
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        return objectMapper.readTree(responseBody).path("result").path("token").asText();
    }
}
