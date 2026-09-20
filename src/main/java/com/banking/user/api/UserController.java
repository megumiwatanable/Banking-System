package com.banking.user.api;

import com.banking.shared.ratelimit.RateLimit;
import com.banking.user.api.dto.ChangePasswordRequest;
import com.banking.user.api.dto.LoginRequest;
import com.banking.user.api.dto.LoginResponse;
import com.banking.user.api.dto.RegisterRequest;
import com.banking.user.api.dto.UpdateProfileRequest;
import com.banking.user.api.dto.UserResponse;
import com.banking.user.application.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @RateLimit(capacity = 5, refillTokens = 5, refillPeriodSeconds = 60)
  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
    UserResponse response = userService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @RateLimit(capacity = 10, refillTokens = 10, refillPeriodSeconds = 60)
  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    LoginResponse response = userService.login(request);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> getProfile() {
    UserResponse response = userService.getCurrentUser();
    return ResponseEntity.ok(response);
  }

  @PutMapping("/me")
  public ResponseEntity<UserResponse> updateProfile(
      @Valid @RequestBody UpdateProfileRequest request) {
    UserResponse response = userService.updateProfile(request);
    return ResponseEntity.ok(response);
  }

  @RateLimit(capacity = 30, refillTokens = 30, refillPeriodSeconds = 60)
  @PutMapping("/password")
  public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
    userService.changePassword(request);
    return ResponseEntity.noContent().build();
  }
}
