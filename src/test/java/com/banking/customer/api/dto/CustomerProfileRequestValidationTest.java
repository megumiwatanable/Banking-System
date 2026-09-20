package com.banking.customer.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CustomerProfileRequestValidationTest {
  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void acceptsVietnameseAddressAndInternationalPhoneNumber() {
    CustomerProfileRequest request =
        new CustomerProfileRequest(
            "+84901234567",
            "12 Nguyễn Huệ, Quận 1, Thành phố Hồ Chí Minh",
            LocalDate.of(1990, 1, 1));

    assertThat(validator.validate(request)).isEmpty();
  }

  @Test
  void rejectsPhoneNumberWithPlusOutsideThePrefix() {
    CustomerProfileRequest request =
        new CustomerProfileRequest("090+123456", "Hà Nội", LocalDate.of(1990, 1, 1));

    assertThat(validator.validate(request))
        .anySatisfy(
            violation -> assertThat(violation.getPropertyPath()).hasToString("phoneNumber"));
  }
}
