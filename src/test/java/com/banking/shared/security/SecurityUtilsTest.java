package com.banking.shared.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.banking.shared.error.UnauthorizedAccessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

  @Mock private Authentication authentication;

  @Mock private SecurityContext securityContext;

  @Test
  void getCurrentUserEmail_shouldReturnEmailWhenAuthenticated() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getName()).thenReturn("test.user@banking.com");

    SecurityContextHolder.setContext(securityContext);

    String email = SecurityUtils.getCurrentUserEmail();

    assertEquals("test.user@banking.com", email);
  }

  @Test
  void getCurrentUserEmail_shouldThrowExceptionWhenNoAuthentication() {
    when(securityContext.getAuthentication()).thenReturn(null);
    SecurityContextHolder.setContext(securityContext);

    assertThrows(
        UnauthorizedAccessException.class,
        () -> {
          SecurityUtils.getCurrentUserEmail();
        });
  }

  @Test
  void getCurrentUserEmail_shouldThrowExceptionWhenNotAuthenticated() {
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.isAuthenticated()).thenReturn(false);

    SecurityContextHolder.setContext(securityContext);

    assertThrows(
        UnauthorizedAccessException.class,
        () -> {
          SecurityUtils.getCurrentUserEmail();
        });
  }
}
