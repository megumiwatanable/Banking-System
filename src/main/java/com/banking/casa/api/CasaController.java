package com.banking.casa.api;

import com.banking.casa.api.dto.*;
import com.banking.casa.application.CasaService;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/casa")
public class CasaController {
  private final CasaService service;

  public CasaController(CasaService service) {
    this.service = service;
  }

  @GetMapping("/me")
  public ResponseEntity<CasaEnrollmentResponse> current() {
    return ResponseEntity.ok(service.current());
  }

  @PostMapping("/enroll")
  public ResponseEntity<CasaEnrollmentResponse> enroll(
      @Valid @RequestBody CasaEnrollmentRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.enroll(request));
  }
}
