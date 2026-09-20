package com.banking.credit.api;

import com.banking.credit.api.dto.CreditRatingResponse;
import com.banking.credit.application.CreditRatingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/credit-rating")
public class CreditRatingController {
  private final CreditRatingService service;

  public CreditRatingController(CreditRatingService service) {
    this.service = service;
  }

  @GetMapping("/me")
  public ResponseEntity<CreditRatingResponse> current() {
    return ResponseEntity.ok(service.current());
  }

  @PostMapping("/evaluate")
  public ResponseEntity<CreditRatingResponse> evaluate() {
    return ResponseEntity.ok(service.evaluate());
  }
}
