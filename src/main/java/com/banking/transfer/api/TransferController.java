package com.banking.transfer.api;

import com.banking.shared.ratelimit.RateLimit;
import com.banking.transfer.api.dto.InternalTransferRequest;
import com.banking.transfer.api.dto.TransferResult;
import com.banking.transfer.application.InternalTransferService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {
  private final InternalTransferService transferService;

  public TransferController(InternalTransferService transferService) {
    this.transferService = transferService;
  }

  @PostMapping("/internal")
  @RateLimit(capacity = 20, refillTokens = 20, refillPeriodSeconds = 60)
  public TransferResult transfer(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody InternalTransferRequest request) {
    return transferService.transfer(idempotencyKey, request);
  }

  @PostMapping("/{transactionId}/reversal")
  public TransferResult reverse(@PathVariable Long transactionId) {
    return transferService.reverse(transactionId);
  }
}
