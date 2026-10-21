package com.pbbaque.banking.customer.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pbbaque.banking.customer.config.SecurityConfig;
import com.pbbaque.banking.customer.domain.CustomerStatus;
import com.pbbaque.banking.customer.domain.DocumentType;
import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.exception.CustomerNotFoundException;
import com.pbbaque.banking.customer.exception.GlobalExceptionHandler;
import com.pbbaque.banking.customer.security.InternalServiceAuthenticationFilter;
import com.pbbaque.banking.customer.service.CustomerService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InternalCustomerController.class)
@Import({
  GlobalExceptionHandler.class,
  SecurityConfig.class,
  InternalServiceAuthenticationFilter.class
})
@TestPropertySource(properties = "security.internal.token=test-internal-token")
class InternalCustomerControllerTest {

  private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Service-Token";

  private static final String INTERNAL_TOKEN = "test-internal-token";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CustomerService customerService;

  private CustomerResponse createResponse(UUID id, UUID identityUserId) {
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    return new CustomerResponse(
        id,
        identityUserId,
        "Test",
        "Customer",
        DocumentType.DNI,
        "12345678A",
        LocalDate.of(1990, 1, 1),
        "600000000",
        "Calle Test 1",
        "48001",
        "Bilbao",
        "ES",
        CustomerStatus.ACTIVE,
        now,
        now);
  }

  @Test
  void shouldGetCustomerByIdentityUserId() throws Exception {
    UUID customerId = UUID.randomUUID();
    UUID identityUserId = UUID.randomUUID();

    CustomerResponse response = createResponse(customerId, identityUserId);

    when(customerService.getByIdentityUserId(identityUserId)).thenReturn(response);

    mockMvc
        .perform(
            get("/api/internal/customers/" + "by-identity-user/{identityUserId}", identityUserId)
                .header(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(customerId.toString()))
        .andExpect(jsonPath("$.identityUserId").value(identityUserId.toString()));
  }

  @Test
  void shouldReturn404WhenCustomerDoesNotExistForIdentityUserId() throws Exception {
    UUID identityUserId = UUID.randomUUID();

    when(customerService.getByIdentityUserId(identityUserId))
        .thenThrow(
            new CustomerNotFoundException(
                "Customer not found for identity user id: " + identityUserId));

    mockMvc
        .perform(
            get("/api/internal/customers/" + "by-identity-user/{identityUserId}", identityUserId)
                .header(INTERNAL_TOKEN_HEADER, INTERNAL_TOKEN))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"));
  }

  @Test
  void shouldReturn401WhenInternalTokenIsMissing() throws Exception {
    UUID identityUserId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/internal/customers/" + "by-identity-user/{identityUserId}", identityUserId))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void shouldReturn401WhenInternalTokenIsInvalid() throws Exception {
    UUID identityUserId = UUID.randomUUID();

    mockMvc
        .perform(
            get("/api/internal/customers/" + "by-identity-user/{identityUserId}", identityUserId)
                .header(INTERNAL_TOKEN_HEADER, "invalid-internal-token"))
        .andExpect(status().isUnauthorized());
  }
}
