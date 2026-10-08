package com.pbbaque.banking.customer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbbaque.banking.customer.domain.CustomerStatus;
import com.pbbaque.banking.customer.domain.DocumentType;
import com.pbbaque.banking.customer.dto.CreateCustomerRequest;
import com.pbbaque.banking.customer.dto.CustomerResponse;
import com.pbbaque.banking.customer.dto.UpdateCustomerContactRequest;
import com.pbbaque.banking.customer.exception.CustomerAlreadyExistsException;
import com.pbbaque.banking.customer.exception.CustomerNotFoundException;
import com.pbbaque.banking.customer.exception.GlobalExceptionHandler;
import com.pbbaque.banking.customer.exception.InvalidCustomerStatusTransitionException;
import com.pbbaque.banking.customer.service.CustomerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private CustomerService customerService;

        private CustomerResponse createResponse(
                        UUID id,
                        UUID identityUserId,
                        CustomerStatus status) {
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
                                status,
                                now,
                                now);
        }

        @Test
        void shouldCreateCustomerAndReturn201() throws Exception {
                UUID identityUserId = UUID.randomUUID();
                UUID customerId = UUID.randomUUID();

                CreateCustomerRequest request = new CreateCustomerRequest(
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
                                "ES");

                CustomerResponse response = createResponse(
                                customerId,
                                identityUserId,
                                CustomerStatus.PENDING_VERIFICATION);

                when(customerService.create(any(CreateCustomerRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(
                                post("/api/v1/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(content()
                                                .contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.id").value(customerId.toString()))
                                .andExpect(jsonPath("$.identityUserId")
                                                .value(identityUserId.toString()))
                                .andExpect(jsonPath("$.firstName").value("Test"))
                                .andExpect(jsonPath("$.status")
                                                .value("PENDING_VERIFICATION"));
        }

        @Test
        void shouldReturn400WhenRequestIsInvalid() throws Exception {
                String request = """
                                {
                                    "identityUserId": "22222222-2222-2222-2222-222222222222",
                                    "firstName": "",
                                    "lastName": "Customer",
                                    "documentType": "DNI",
                                    "documentNumber": "12345678A",
                                    "dateOfBirth": "2030-01-01",
                                    "countryCode": "ESP"
                                }
                                """;

                mockMvc.perform(
                                post("/api/v1/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(request))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.error").value("Bad Request"));
        }

        @Test
        void shouldReturn409WhenCustomerAlreadyExists() throws Exception {
                UUID identityUserId = UUID.randomUUID();

                CreateCustomerRequest request = new CreateCustomerRequest(
                                identityUserId,
                                "Test",
                                "Customer",
                                DocumentType.DNI,
                                "12345678A",
                                LocalDate.of(1990, 1, 1),
                                null,
                                null,
                                null,
                                null,
                                "ES");

                when(customerService.create(any(CreateCustomerRequest.class)))
                                .thenThrow(new CustomerAlreadyExistsException(
                                                "A customer already exists with document: DNI 12345678A"));

                mockMvc.perform(
                                post("/api/v1/customers")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"));
        }

        @Test
        void shouldGetCustomerById() throws Exception {
                UUID id = UUID.randomUUID();
                UUID identityUserId = UUID.randomUUID();

                CustomerResponse response = createResponse(
                                id,
                                identityUserId,
                                CustomerStatus.ACTIVE);

                when(customerService.getById(id))
                                .thenReturn(response);

                mockMvc.perform(
                                get("/api/v1/customers/{id}", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void shouldReturn404WhenCustomerDoesNotExist() throws Exception {
                UUID id = UUID.randomUUID();

                when(customerService.getById(id))
                                .thenThrow(new CustomerNotFoundException(
                                                "Customer not found with id: " + id));

                mockMvc.perform(
                                get("/api/v1/customers/{id}", id))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message")
                                                .value("Customer not found with id: " + id));
        }

        @Test
        void shouldReturn400WhenIdIsNotValidUuid() throws Exception {
                mockMvc.perform(
                                get("/api/v1/customers/not-a-uuid"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message")
                                                .value("Invalid value for parameter: id"));
        }

        @Test
        void shouldReturnAllCustomers() throws Exception {
                UUID id1 = UUID.randomUUID();
                UUID id2 = UUID.randomUUID();

                CustomerResponse customer1 = createResponse(
                                id1,
                                UUID.randomUUID(),
                                CustomerStatus.ACTIVE);

                CustomerResponse customer2 = createResponse(
                                id2,
                                UUID.randomUUID(),
                                CustomerStatus.PENDING_VERIFICATION);

                when(customerService.getAll())
                                .thenReturn(List.of(customer1, customer2));

                mockMvc.perform(
                                get("/api/v1/customers"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                                .andExpect(jsonPath("$[1].id").value(id2.toString()));
        }

        @Test
        void shouldUpdateCustomerContactData() throws Exception {
                UUID id = UUID.randomUUID();
                UUID identityUserId = UUID.randomUUID();

                UpdateCustomerContactRequest request = new UpdateCustomerContactRequest(
                                "611111111",
                                "Nueva direccion 10",
                                "48002",
                                "Bilbao",
                                "ES");

                OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

                CustomerResponse response = new CustomerResponse(
                                id,
                                identityUserId,
                                "Test",
                                "Customer",
                                DocumentType.DNI,
                                "12345678A",
                                LocalDate.of(1990, 1, 1),
                                "611111111",
                                "Nueva direccion 10",
                                "48002",
                                "Bilbao",
                                "ES",
                                CustomerStatus.ACTIVE,
                                now,
                                now);

                when(customerService.updateContactData(
                                eq(id),
                                any(UpdateCustomerContactRequest.class))).thenReturn(response);

                mockMvc.perform(
                                put("/api/v1/customers/{id}/contact", id)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.phoneNumber").value("611111111"))
                                .andExpect(jsonPath("$.fiscalAddress")
                                                .value("Nueva direccion 10"))
                                .andExpect(jsonPath("$.postalCode").value("48002"));
        }

        @Test
        void shouldArchiveCustomerAndReturn204() throws Exception {
                UUID id = UUID.randomUUID();

                doNothing()
                                .when(customerService)
                                .archive(id);

                mockMvc.perform(
                                delete("/api/v1/customers/{id}", id))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));
        }

        @Test
        void shouldActivateCustomer() throws Exception {
                UUID id = UUID.randomUUID();

                CustomerResponse response = createResponse(
                                id,
                                UUID.randomUUID(),
                                CustomerStatus.ACTIVE);

                when(customerService.activate(id))
                                .thenReturn(response);

                mockMvc.perform(
                                patch("/api/v1/customers/{id}/activate", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("ACTIVE"));
        }

        @Test
        void shouldBlockCustomer() throws Exception {
                UUID id = UUID.randomUUID();

                CustomerResponse response = createResponse(
                                id,
                                UUID.randomUUID(),
                                CustomerStatus.BLOCKED);

                when(customerService.block(id))
                                .thenReturn(response);

                mockMvc.perform(
                                patch("/api/v1/customers/{id}/block", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("BLOCKED"));
        }

        @Test
        void shouldDeactivateCustomer() throws Exception {
                UUID id = UUID.randomUUID();

                CustomerResponse response = createResponse(
                                id,
                                UUID.randomUUID(),
                                CustomerStatus.INACTIVE);

                when(customerService.deactivate(id))
                                .thenReturn(response);

                mockMvc.perform(
                                patch("/api/v1/customers/{id}/deactivate", id))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(id.toString()))
                                .andExpect(jsonPath("$.status").value("INACTIVE"));
        }

        @Test
        void shouldReturn409WhenStatusTransitionIsInvalid() throws Exception {
                UUID id = UUID.randomUUID();

                when(customerService.activate(id))
                                .thenThrow(
                                                new InvalidCustomerStatusTransitionException(
                                                                CustomerStatus.INACTIVE,
                                                                CustomerStatus.ACTIVE));

                mockMvc.perform(
                                patch("/api/v1/customers/{id}/activate", id))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.error").value("Conflict"))
                                .andExpect(jsonPath("$.message")
                                                .value(
                                                                "Invalid customer status transition from " +
                                                                                "INACTIVE to ACTIVE"));
        }
}