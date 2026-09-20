package com.banking.hold.api;

import com.banking.hold.api.dto.HoldRequest;
import com.banking.hold.api.dto.HoldResponse;
import com.banking.hold.application.BalanceHoldService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/holds")
public class BalanceHoldController {
  private final BalanceHoldService balanceHoldService;

  public BalanceHoldController(BalanceHoldService balanceHoldService) {
    this.balanceHoldService = balanceHoldService;
  }

  @GetMapping
  public List<HoldResponse> list() {
    return balanceHoldService.list();
  }

  @PostMapping
  public ResponseEntity<HoldResponse> hold(@Valid @RequestBody HoldRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(balanceHoldService.hold(request));
  }

  @PostMapping("/{holdId}/release")
  public HoldResponse release(@PathVariable Long holdId) {
    return balanceHoldService.release(holdId);
  }

  @PostMapping("/{holdId}/capture")
  public HoldResponse capture(@PathVariable Long holdId) {
    return balanceHoldService.capture(holdId);
  }
}
