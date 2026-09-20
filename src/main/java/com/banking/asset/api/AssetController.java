package com.banking.asset.api;

import com.banking.asset.api.dto.*;
import com.banking.asset.application.AssetService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
  private final AssetService service;

  public AssetController(AssetService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<List<AssetResponse>> list() {
    return ResponseEntity.ok(service.list());
  }

  @PostMapping
  public ResponseEntity<AssetResponse> create(@Valid @RequestBody AssetRequest r) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
  }

  @PutMapping("/{id}")
  public ResponseEntity<AssetResponse> update(
      @PathVariable Long id, @Valid @RequestBody AssetRequest r) {
    return ResponseEntity.ok(service.update(id, r));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
