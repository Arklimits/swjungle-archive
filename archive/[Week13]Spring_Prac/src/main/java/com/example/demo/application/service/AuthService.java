package com.example.demo.application.service;

import com.example.demo.application.dto.user.UserInfoDTO;
import com.example.demo.application.dto.user.UserRequestDTO;
import com.example.demo.domain.exception.PasswordNotMatchException;
import com.example.demo.domain.model.User;
import com.example.demo.global.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final JwtUtil jwtUtil;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final ModelMapper modelMapper;

    @Transactional
    public UserInfoDTO login(UserRequestDTO userRequestDTO) {
        String username = userRequestDTO.getUsername();
        String password = userRequestDTO.getPassword();
        User user = userService.findByUsername(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordNotMatchException();
        }

        return modelMapper.map(user, UserInfoDTO.class);
    }
}
