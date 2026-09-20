package com.banking.customer.api;

import com.banking.customer.api.dto.*;
import com.banking.customer.application.CustomerProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer")
public class CustomerProfileController {
  private final CustomerProfileService service;

  public CustomerProfileController(CustomerProfileService service) {
    this.service = service;
  }

  @GetMapping("/me")
  public ResponseEntity<CustomerProfileResponse> get() {
    return ResponseEntity.ok(service.getOrCreate());
  }

  @PutMapping("/me")
  public ResponseEntity<CustomerProfileResponse> update(
      @Valid @RequestBody CustomerProfileRequest request) {
    return ResponseEntity.ok(service.update(request));
  }
}
