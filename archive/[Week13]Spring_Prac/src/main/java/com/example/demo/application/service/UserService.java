package com.example.demo.application.service;

import com.example.demo.domain.exception.UsernameExistException;
import com.example.demo.application.dto.user.UserRequestDTO;
import com.example.demo.domain.model.User;
import com.example.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new NoSuchElementException("User not found");
        }

        return userDetails.getUsername();
    }

    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (!(authentication.getPrincipal() instanceof UserDetails userDetails)) {
            throw new NoSuchElementException("User not found");
        }

        if (userDetails.getAuthorities().isEmpty()) {
            throw new NoSuchElementException("User Role not found");
        }

        return authentication.getAuthorities().toString();
    }

    public void saveUser(UserRequestDTO userRequestDTO) {
        if (userRepository.findByUsername(userRequestDTO.getUsername()).isPresent()) {
            throw new UsernameExistException();
        }

        User user = new User(userRequestDTO.getUsername(), passwordEncoder.encode(userRequestDTO.getPassword()), "USER");
        userRepository.save(user);
    }

    public User findByUsername(String username) {

        return userRepository.findByUsername(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public Boolean checkAdministrator() {
        String authority = getCurrentUserRole();

        return authority.equals("[ROLE_ADMIN]");
    }

    public Boolean checkAuthor(String author) {
        String username = getCurrentUsername();

        return username.equals(author);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = findByUsername(username);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole())
                .build();
    }
}
