package com.banking.customer.application;

import com.banking.customer.api.dto.CustomerProfileRequest;
import com.banking.customer.api.dto.CustomerProfileResponse;
import com.banking.customer.domain.CustomerProfile;
import com.banking.customer.infrastructure.CustomerProfileRepository;
import com.banking.shared.error.ResourceNotFoundException;
import com.banking.shared.security.SecurityUtils;
import com.banking.user.domain.User;
import com.banking.user.infrastructure.UserRepository;
import java.security.SecureRandom;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerProfileService {
  private static final long CUSTOMER_NUMBER_BASE = 1_000_000_000L;
  private static final long CUSTOMER_NUMBER_RANGE = 9_000_000_000L;

  private final CustomerProfileRepository profileRepository;
  private final UserRepository userRepository;
  private final SecureRandom random = new SecureRandom();

  public CustomerProfileService(
      CustomerProfileRepository profileRepository, UserRepository userRepository) {
    this.profileRepository = profileRepository;
    this.userRepository = userRepository;
  }

  @Transactional
  public CustomerProfileResponse getOrCreate() {
    String email = SecurityUtils.getCurrentUserEmail();
    return profileRepository
        .findByUserEmail(email)
        .map(this::toResponse)
        .orElseGet(
            () -> {
              User user =
                  userRepository
                      .findByEmail(email)
                      .orElseThrow(() -> new ResourceNotFoundException("User not found"));
              CustomerProfile profile = new CustomerProfile();
              profile.setUser(user);
              profile.setCustomerNumber(generateCustomerNumber());
              return toResponse(profileRepository.save(profile));
            });
  }

  @Transactional
  public CustomerProfileResponse update(CustomerProfileRequest request) {
    String email = SecurityUtils.getCurrentUserEmail();
    CustomerProfile profile =
        profileRepository
            .findByUserEmail(email)
            .orElseGet(
                () -> {
                  User user =
                      userRepository
                          .findByEmail(email)
                          .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                  CustomerProfile created = new CustomerProfile();
                  created.setUser(user);
                  created.setCustomerNumber(generateCustomerNumber());
                  return created;
                });
    profile.setPhoneNumber(request.phoneNumber());
    profile.setAddress(request.address());
    profile.setDateOfBirth(request.dateOfBirth());
    return toResponse(profileRepository.save(profile));
  }

  private String generateCustomerNumber() {
    String value;
    do {
      value =
          "CIF" + (CUSTOMER_NUMBER_BASE + Math.floorMod(random.nextLong(), CUSTOMER_NUMBER_RANGE));
    } while (profileRepository.existsByCustomerNumber(value));
    return value;
  }

  private CustomerProfileResponse toResponse(CustomerProfile profile) {
    return new CustomerProfileResponse(
        profile.getId(),
        profile.getCustomerNumber(),
        profile.getUser().getFullName(),
        profile.getUser().getEmail(),
        profile.getPhoneNumber(),
        profile.getAddress(),
        profile.getDateOfBirth(),
        profile.getKycStatus());
  }
}
