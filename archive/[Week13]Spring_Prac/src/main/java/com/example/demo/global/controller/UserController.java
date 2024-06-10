package com.example.demo.global.controller;

import com.example.demo.application.dto.user.UserRequestDTO;
import com.example.demo.application.service.UserService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserRequestDTO> registerUser(@Valid @RequestBody UserRequestDTO userRequestDTO) {
        userService.saveUser(userRequestDTO);

        return new ResponseEntity<>(userRequestDTO, HttpStatus.OK);
    }
}
