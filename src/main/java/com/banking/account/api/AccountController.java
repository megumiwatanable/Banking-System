package com.banking.account.api;

import com.banking.account.api.dto.*;
import com.banking.account.application.AccountService;
import com.banking.shared.ratelimit.RateLimit;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  private final AccountService accountService;

  public AccountController(AccountService accountService) {
    this.accountService = accountService;
  }

  @GetMapping
  public ResponseEntity<List<AccountResponse>> getMyAccounts() {
    return ResponseEntity.ok(accountService.getMyAccounts());
  }

  @PostMapping("/create")
  public ResponseEntity<AccountResponse> createAccount(
      @Valid @RequestBody CreateAccountRequest request) {
    AccountResponse response = accountService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @GetMapping("/{accountId}")
  public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId) {
    AccountResponse response = accountService.getAccountById(accountId);
    return ResponseEntity.ok(response);
  }

  @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
  @PostMapping("/{accountId}/deposit")
  public ResponseEntity<AccountResponse> deposit(
      @PathVariable Long accountId, @Valid @RequestBody DepositRequest request) {
    AccountResponse response = accountService.deposit(accountId, request);
    return ResponseEntity.ok(response);
  }

  @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
  @PostMapping("/{accountId}/withdraw")
  public ResponseEntity<AccountResponse> withDraw(
      @PathVariable Long accountId, @Valid @RequestBody WithdrawRequest request) {
    AccountResponse response = accountService.withdraw(accountId, request);
    return ResponseEntity.ok(response);
  }

  @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
  @PostMapping("/transfer")
  public ResponseEntity<AccountResponse> transfer(@Valid @RequestBody TransferRequest request) {
    AccountResponse response = accountService.transfer(request);
    return ResponseEntity.ok(response);
  }

  @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
  @PostMapping("/transfer/interbank")
  public ResponseEntity<InterbankTransferResponse> requestInterbankTransfer(
      @Valid @RequestBody InterbankTransferRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED)
        .body(accountService.requestInterbankTransfer(request));
  }
}
