package com.banking.citad.api;

import com.banking.citad.api.dto.*;
import com.banking.citad.application.CitadInquiryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/citad/inquiries")
public class CitadInquiryController {
  private final CitadInquiryService service;

  public CitadInquiryController(CitadInquiryService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<CitadInquiryResponse>> list() {
    return ResponseEntity.ok(service.list());
  }

  @GetMapping("/{id}")
  public ResponseEntity<CitadInquiryResponse> get(@PathVariable Long id) {
    return ResponseEntity.ok(service.get(id));
  }

  @PostMapping
  public ResponseEntity<CitadInquiryResponse> create(
      @Valid @RequestBody CreateCitadInquiryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
  }
}
