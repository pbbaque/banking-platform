package com.pbbaque.banking.identity.controller;

import com.pbbaque.banking.identity.dto.LoginRequest;
import com.pbbaque.banking.identity.dto.LoginResponse;
import com.pbbaque.banking.identity.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST controller responsible for authentication operations. */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final UserService userService;

  public AuthController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(userService.authenticate(request));
  }
}
