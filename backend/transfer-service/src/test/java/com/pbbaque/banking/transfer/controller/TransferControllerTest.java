package com.pbbaque.banking.transfer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.transfer.domain.Transfer;
import com.pbbaque.banking.transfer.dto.CreateTransferRequest;
import com.pbbaque.banking.transfer.exception.GlobalExceptionHandler;
import com.pbbaque.banking.transfer.exception.TransferNotFoundException;
import com.pbbaque.banking.transfer.service.TransferService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/** Web layer tests for {@link TransferController}. */
@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

  private static final UUID SOURCE_ACCOUNT_ID =
      UUID.fromString("11111111-1111-1111-1111-111111111111");

  private static final UUID DESTINATION_ACCOUNT_ID =
      UUID.fromString("22222222-2222-2222-2222-222222222222");

  private static final UUID TRANSFER_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  private static final String IDEMPOTENCY_KEY = "transfer-key-1";

  @Mock private TransferService transferService;

  @InjectMocks private TransferController transferController;

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  /** Configures the standalone MVC test environment before each test. */
  @BeforeEach
  void setUp() {
    LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
    validator.afterPropertiesSet();

    mockMvc =
        MockMvcBuilders.standaloneSetup(transferController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setValidator(validator)
            .build();

    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();
  }

  @Test
  void shouldCreateTransfer() throws Exception {
    CreateTransferRequest request =
        new CreateTransferRequest(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("100.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("100.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    transfer.complete();

    when(transferService.createTransfer(any(CreateTransferRequest.class))).thenReturn(transfer);

    mockMvc
        .perform(
            post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.sourceAccountId").value(SOURCE_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.destinationAccountId").value(DESTINATION_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.amount").value(100.0000))
        .andExpect(jsonPath("$.currency").value("EUR"))
        .andExpect(jsonPath("$.status").value("COMPLETED"))
        .andExpect(jsonPath("$.idempotencyKey").value(IDEMPOTENCY_KEY));
  }

  @Test
  void shouldReturnTransferById() throws Exception {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("50.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    when(transferService.getById(TRANSFER_ID)).thenReturn(transfer);

    mockMvc
        .perform(get("/api/transfers/{id}", TRANSFER_ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.sourceAccountId").value(SOURCE_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.destinationAccountId").value(DESTINATION_ACCOUNT_ID.toString()))
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void shouldReturnTransferByIdempotencyKey() throws Exception {
    Transfer transfer =
        Transfer.create(
            SOURCE_ACCOUNT_ID,
            DESTINATION_ACCOUNT_ID,
            new BigDecimal("75.0000"),
            "EUR",
            IDEMPOTENCY_KEY);

    when(transferService.getByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(transfer);

    mockMvc
        .perform(get("/api/transfers").param("idempotencyKey", IDEMPOTENCY_KEY))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.idempotencyKey").value(IDEMPOTENCY_KEY))
        .andExpect(jsonPath("$.amount").value(75.0000))
        .andExpect(jsonPath("$.currency").value("EUR"));
  }

  @Test
  void shouldReturnNotFoundWhenTransferDoesNotExist() throws Exception {

    when(transferService.getById(TRANSFER_ID))
        .thenThrow(new TransferNotFoundException(TRANSFER_ID));

    mockMvc
        .perform(get("/api/transfers/{id}", TRANSFER_ID))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.error").value("Not Found"))
        .andExpect(jsonPath("$.message").value("Transfer not found: " + TRANSFER_ID));
  }

  @Test
  void shouldRejectInvalidTransferRequest() throws Exception {
    CreateTransferRequest request =
        new CreateTransferRequest(
            SOURCE_ACCOUNT_ID, DESTINATION_ACCOUNT_ID, BigDecimal.ZERO, "", "");

    mockMvc
        .perform(
            post("/api/transfers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }

  @Test
  void shouldRejectInvalidTransferId() throws Exception {
    mockMvc
        .perform(get("/api/transfers/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.error").value("Bad Request"));
  }
}
