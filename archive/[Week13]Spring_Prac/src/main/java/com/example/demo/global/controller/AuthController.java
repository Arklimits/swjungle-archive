package com.example.demo.global.controller;

import com.example.demo.global.util.JwtUtil;
import com.example.demo.application.dto.auth.AuthResponse;
import com.example.demo.application.dto.user.UserInfoDTO;
import com.example.demo.application.dto.user.UserRequestDTO;
import com.example.demo.application.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> getUserProfile(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        UserInfoDTO userInfoDTO = this.authService.login(userRequestDTO);
        AuthResponse authResponse = jwtUtil.createAccessToken(userInfoDTO);

        return new ResponseEntity<>(authResponse, HttpStatus.OK);
    }
}
