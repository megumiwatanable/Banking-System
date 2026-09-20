package com.banking.user.application;

import com.banking.user.api.dto.ChangePasswordRequest;
import com.banking.user.api.dto.LoginRequest;
import com.banking.user.api.dto.LoginResponse;
import com.banking.user.api.dto.RegisterRequest;
import com.banking.user.api.dto.UpdateProfileRequest;
import com.banking.user.api.dto.UserResponse;

public interface UserService {

  UserResponse register(RegisterRequest request);

  LoginResponse login(LoginRequest request);

  UserResponse getCurrentUser();

  UserResponse updateProfile(UpdateProfileRequest request);

  void changePassword(ChangePasswordRequest request);
}
