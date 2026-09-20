package com.banking.beneficiary.api;

import com.banking.beneficiary.api.dto.BeneficiaryRequest;
import com.banking.beneficiary.api.dto.BeneficiaryResponse;
import com.banking.beneficiary.application.BeneficiaryService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/beneficiaries")
public class BeneficiaryController {
  private final BeneficiaryService beneficiaryService;

  public BeneficiaryController(BeneficiaryService beneficiaryService) {
    this.beneficiaryService = beneficiaryService;
  }

  @GetMapping
  public List<BeneficiaryResponse> list() {
    return beneficiaryService.list();
  }

  @PostMapping
  public ResponseEntity<BeneficiaryResponse> create(
      @Valid @RequestBody BeneficiaryRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(beneficiaryService.create(request));
  }

  @PutMapping("/{beneficiaryId}")
  public BeneficiaryResponse update(
      @PathVariable Long beneficiaryId, @Valid @RequestBody BeneficiaryRequest request) {
    return beneficiaryService.update(beneficiaryId, request);
  }

  @DeleteMapping("/{beneficiaryId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long beneficiaryId) {
    beneficiaryService.delete(beneficiaryId);
  }
}
