package com.banking.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.banking.shared.error.InvalidCredentialsException;
import com.banking.shared.error.ResourceAlreadyExistsException;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.security.JwtService;
import com.banking.shared.security.SecurityUtils;
import com.banking.user.api.dto.ChangePasswordRequest;
import com.banking.user.api.dto.LoginRequest;
import com.banking.user.api.dto.RegisterRequest;
import com.banking.user.api.dto.UpdateProfileRequest;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

  @Mock private UserRepository userRepository;

  @Mock private PasswordEncoder passwordEncoder;

  @Mock private JwtService jwtService;

  private UserServiceImpl userService;

  private MockedStatic<SecurityUtils> securityUtils;

  @BeforeEach
  void setUp() {
    userService = new UserServiceImpl(userRepository, passwordEncoder, jwtService);
    securityUtils = mockStatic(SecurityUtils.class);
  }

  @AfterEach
  void tearDown() {
    securityUtils.close();
  }

  @Test
  void registerThrowsWhenEmailAlreadyTaken() {
    RegisterRequest request = new RegisterRequest("Jamie Doe", "jamie@bank.com", "pass1234");
    when(userRepository.existsByEmail("jamie@bank.com")).thenReturn(true);

    assertThrows(ResourceAlreadyExistsException.class, () -> userService.register(request));

    verify(userRepository, never()).save(any());
  }

  @Test
  void registerSavesEncodedPassword() {
    RegisterRequest request = new RegisterRequest("Jamie Doe", "jamie@bank.com", "pass1234");
    when(userRepository.existsByEmail("jamie@bank.com")).thenReturn(false);
    when(passwordEncoder.encode("pass1234")).thenReturn("encoded-pass");
    when(userRepository.save(any(User.class)))
        .thenAnswer(
            inv -> {
              User u = inv.getArgument(0);
              u.setId(1L);
              return u;
            });

    var response = userService.register(request);

    assertEquals("jamie@bank.com", response.email());
    verify(passwordEncoder).encode("pass1234");
  }

  @Test
  void registerTranslatesRaceConditionOnUniqueConstraint() {
    RegisterRequest request = new RegisterRequest("Jamie Doe", "jamie@bank.com", "pass1234");
    when(userRepository.existsByEmail("jamie@bank.com")).thenReturn(false);
    when(passwordEncoder.encode(anyString())).thenReturn("encoded-pass");
    when(userRepository.save(any(User.class)))
        .thenThrow(new DataIntegrityViolationException("dup key"));

    assertThrows(ResourceAlreadyExistsException.class, () -> userService.register(request));
  }

  @Test
  void loginThrowsWhenEmailNotFound() {
    LoginRequest request = new LoginRequest("ghost@bank.com", "whatever");
    when(userRepository.findByEmail("ghost@bank.com")).thenReturn(Optional.empty());

    assertThrows(InvalidCredentialsException.class, () -> userService.login(request));
  }

  @Test
  void loginThrowsWhenPasswordDoesNotMatch() {
    User user = new User();
    user.setEmail("jamie@bank.com");
    user.setPassword("encoded-pass");
    LoginRequest request = new LoginRequest("jamie@bank.com", "wrong");
    when(userRepository.findByEmail("jamie@bank.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong", "encoded-pass")).thenReturn(false);

    assertThrows(InvalidCredentialsException.class, () -> userService.login(request));

    verify(jwtService, never()).generateToken(anyString());
  }

  @Test
  void loginReturnsTokenOnSuccess() {
    User user = new User();
    user.setId(3L);
    user.setEmail("jamie@bank.com");
    user.setFullName("Jamie Doe");
    user.setPassword("encoded-pass");
    LoginRequest request = new LoginRequest("jamie@bank.com", "pass1234");
    when(userRepository.findByEmail("jamie@bank.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("pass1234", "encoded-pass")).thenReturn(true);
    when(jwtService.generateToken("jamie@bank.com")).thenReturn("jwt-token");

    var response = userService.login(request);

    assertEquals("jwt-token", response.token());
    assertEquals(3L, response.userId());
  }

  @Test
  void getCurrentUserThrowsWhenSessionUserWasDeleted() {
    securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("ghost@bank.com");
    when(userRepository.findByEmail("ghost@bank.com")).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> userService.getCurrentUser());
  }

  @Test
  void updateProfileThrowsWhenNewEmailBelongsToSomeoneElse() {
    User user = new User();
    user.setId(1L);
    user.setEmail("old@bank.com");
    securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("old@bank.com");
    when(userRepository.findByEmail("old@bank.com")).thenReturn(Optional.of(user));
    when(userRepository.existsByEmail("taken@bank.com")).thenReturn(true);

    UpdateProfileRequest request = new UpdateProfileRequest("Jamie Doe", "taken@bank.com");

    assertThrows(ResourceAlreadyExistsException.class, () -> userService.updateProfile(request));
  }

  @Test
  void updateProfileAllowsKeepingSameEmail() {
    User user = new User();
    user.setId(1L);
    user.setEmail("jamie@bank.com");
    user.setFullName("Old Name");
    securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("jamie@bank.com");
    when(userRepository.findByEmail("jamie@bank.com")).thenReturn(Optional.of(user));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateProfileRequest request = new UpdateProfileRequest("New Name", "jamie@bank.com");
    var response = userService.updateProfile(request);

    assertEquals("New Name", response.fullName());
    verify(userRepository, never()).existsByEmail(anyString());
  }

  @Test
  void changePasswordThrowsWhenCurrentPasswordWrong() {
    User user = new User();
    user.setEmail("jamie@bank.com");
    user.setPassword("encoded-old");
    securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("jamie@bank.com");
    when(userRepository.findByEmail("jamie@bank.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("wrong-current", "encoded-old")).thenReturn(false);

    ChangePasswordRequest request = new ChangePasswordRequest("wrong-current", "newpass123");

    assertThrows(InvalidCredentialsException.class, () -> userService.changePassword(request));

    verify(userRepository, never()).save(any());
  }

  @Test
  void changePasswordUpdatesEncodedPassword() {
    User user = new User();
    user.setEmail("jamie@bank.com");
    user.setPassword("encoded-old");
    securityUtils.when(SecurityUtils::getCurrentUserEmail).thenReturn("jamie@bank.com");
    when(userRepository.findByEmail("jamie@bank.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("current123", "encoded-old")).thenReturn(true);
    when(passwordEncoder.encode("newpass123")).thenReturn("encoded-new");

    ChangePasswordRequest request = new ChangePasswordRequest("current123", "newpass123");
    userService.changePassword(request);

    assertEquals("encoded-new", user.getPassword());
    verify(userRepository).save(user);
  }
}
