package com.pbbaque.banking.identity.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.identity.domain.UserStatus;
import com.pbbaque.banking.identity.dto.CreateUserRequest;
import com.pbbaque.banking.identity.dto.UserResponse;
import com.pbbaque.banking.identity.exception.GlobalExceptionHandler;
import com.pbbaque.banking.identity.exception.UserAlreadyExistsException;
import com.pbbaque.banking.identity.exception.UserNotFoundException;
import com.pbbaque.banking.identity.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldCreateUserAndReturn201() throws Exception {
        UUID id = UUID.randomUUID();

        CreateUserRequest request = new CreateUserRequest(
                "12345678A",
                "test@banking-platform.local",
                "SecurePassword123!");

        UserResponse response = createResponse(
                id,
                request.username(),
                request.email(),
                UserStatus.PENDING);

        when(userService.create(any(CreateUserRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content()
                        .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("12345678A"))
                .andExpect(jsonPath("$.email")
                        .value("test@banking-platform.local"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
        String request = """
                {
                    "username": "",
                    "email": "not-an-email",
                    "password": "short"
                }
                """;

        mockMvc.perform(
                post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void shouldReturn409WhenUserAlreadyExists() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "12345678A",
                "test@banking-platform.local",
                "SecurePassword123!");

        when(userService.create(any(CreateUserRequest.class)))
                .thenThrow(
                        new UserAlreadyExistsException(
                                "A user already exists with username: 12345678A"));

        mockMvc.perform(
                post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "A user already exists with username: 12345678A"));
    }

    @Test
    void shouldGetUserById() throws Exception {
        UUID id = UUID.randomUUID();

        UserResponse response = createResponse(
                id,
                "12345678A",
                "test@banking-platform.local",
                UserStatus.ACTIVE);

        when(userService.getById(id))
                .thenReturn(response);

        mockMvc.perform(
                get("/api/v1/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("12345678A"))
                .andExpect(jsonPath("$.email")
                        .value("test@banking-platform.local"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenUserByIdDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();

        when(userService.getById(id))
                .thenThrow(
                        new UserNotFoundException(
                                "User not found with id: " + id));

        mockMvc.perform(
                get("/api/v1/users/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("User not found with id: " + id));
    }

    @Test
    void shouldReturn400WhenIdIsNotValidUuid() throws Exception {
        mockMvc.perform(
                get("/api/v1/users/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Invalid value for parameter: id"));
    }

    @Test
    void shouldGetUserByUsername() throws Exception {
        UUID id = UUID.randomUUID();

        UserResponse response = createResponse(
                id,
                "12345678A",
                "test@banking-platform.local",
                UserStatus.ACTIVE);

        when(userService.getByUsername("12345678A"))
                .thenReturn(response);

        mockMvc.perform(
                get(
                        "/api/v1/users/by-username/{username}",
                        "12345678A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.username").value("12345678A"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404WhenUsernameDoesNotExist() throws Exception {
        String username = "99999999Z";

        when(userService.getByUsername(username))
                .thenThrow(
                        new UserNotFoundException(
                                "User not found with username: " + username));

        mockMvc.perform(
                get(
                        "/api/v1/users/by-username/{username}",
                        username))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "User not found with username: "
                                        + username));
    }

    private UserResponse createResponse(
            UUID id,
            String username,
            String email,
            UserStatus status) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return new UserResponse(
                id,
                username,
                email,
                status,
                now,
                now,
                null);
    }
}