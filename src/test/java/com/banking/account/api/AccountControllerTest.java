package com.banking.account.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.banking.account.api.dto.*;
import com.banking.account.application.AccountService;
import com.banking.account.domain.AccountType;
import com.banking.shared.ratelimit.RateLimitInterceptor;
import com.banking.shared.security.JwtAuthenticationFilter;
import com.banking.shared.security.JwtService;
import com.banking.transaction.domain.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AccountControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AccountService accountService;

  @MockitoBean JwtService jwtService;

  @MockitoBean JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean private RateLimitInterceptor rateLimitInterceptor;

  private CreateAccountRequest createAccountRequest;
  private DepositRequest depositRequest;
  private TransferRequest transferRequest;
  private WithdrawRequest withdrawRequest;
  private AccountResponse accountResponse;

  // Constants

  private final BigDecimal BALANCE = BigDecimal.valueOf(100000);
  private final String RECEIVE_ACCOUNT_NO = "1234567890";
  private final String ACCOUNT_NUMBER = "qwert";
  private final Long ID = Long.valueOf(23);
  private final AccountType ACCOUNT_TYPE = AccountType.CURRENT;
  private static final BigDecimal AMOUNT = BigDecimal.valueOf(1000);

  @BeforeEach
  void setUp() throws Exception {
    when(rateLimitInterceptor.preHandle(any(), any(), any())).thenReturn(true);

    createAccountRequest = new CreateAccountRequest(ACCOUNT_TYPE);
    depositRequest = new DepositRequest(AMOUNT);
    transferRequest = new TransferRequest("1000000023", RECEIVE_ACCOUNT_NO, AMOUNT);
    withdrawRequest = new WithdrawRequest(AMOUNT);
    accountResponse = new AccountResponse(ID, ACCOUNT_NUMBER, ACCOUNT_TYPE, BALANCE);
  }

  @Test
  void createAccount_shouldReturnCreated() throws Exception {
    when(accountService.create(createAccountRequest)).thenReturn(accountResponse);

    mockMvc
        .perform(
            post("/api/account/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createAccountRequest)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(ID))
        .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER))
        .andExpect(jsonPath("$.balance").value(BALANCE));
  }

  @Test
  void getAccount_shouldReturnOk() throws Exception {
    when(accountService.getAccountById(ID)).thenReturn(accountResponse);

    mockMvc
        .perform(get("/api/account/{accountId}", ID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(ID))
        .andExpect(jsonPath("$.accountNumber").value(ACCOUNT_NUMBER));
  }

  @Test
  void deposit_shouldReturnOk() throws Exception {
    when(accountService.deposit(ID, depositRequest)).thenReturn(accountResponse);

    mockMvc
        .perform(
            post("/api/account/{accountId}/deposit", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(depositRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.balance").value(BALANCE));
  }

  @Test
  void withdraw_shouldReturnOk() throws Exception {
    when(accountService.withdraw(ID, withdrawRequest)).thenReturn(accountResponse);

    mockMvc
        .perform(
            post("/api/account/{accountId}/withdraw", ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(withdrawRequest)))
        .andExpect(status().isOk());
  }

  @Test
  void transfer_shouldReturnOk() throws Exception {
    when(accountService.transfer(transferRequest)).thenReturn(accountResponse);

    mockMvc
        .perform(
            post("/api/account/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest)))
        .andExpect(status().isOk());
  }

  @Test
  void interbankTransfer_shouldReturnPendingWithoutClaimingSettlement() throws Exception {
    var request =
        new InterbankTransferRequest("1000000023", "VCB", "0123456789", "Nguyen Van B", AMOUNT);
    var response =
        new InterbankTransferResponse(
            42L,
            "1000000023",
            "VCB",
            "0123456789",
            "Nguyen Van B",
            AMOUNT,
            TransactionStatus.PENDING);
    when(accountService.requestInterbankTransfer(request)).thenReturn(response);

    mockMvc
        .perform(
            post("/api/account/transfer/interbank")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andExpect(jsonPath("$.transactionId").value(42));
  }
}
