package com.example.demo.application.dto.auth;

import java.util.Date;

public record AuthResponse(String jwt, String username, String expiresAt) {

}