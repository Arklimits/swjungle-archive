package com.example.demo.global.config;

import com.example.demo.application.service.UserService;
import com.example.demo.component.filter.JwtAuthFilter;
import com.example.demo.component.handler.CustomAccessDeniedHandler;
import com.example.demo.component.handler.CustomAuthenticationEntryPoint;
import com.example.demo.component.handler.GlobalExceptionHandler;
import com.example.demo.global.util.JwtUtil;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RegexRequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
public class SecurityConfig {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final GlobalExceptionHandler globalExceptionHandler;
    private static final String[] AUTH_WHITELIST = {
            "/post/index", "/login", "/register"
    };
    private static final RegexRequestMatcher regexRequestMatcher
            = new RegexRequestMatcher("^/post/[0-9]+$", "GET");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                // 세션 관리 상태 없음 --> Spring Security 가 세션을 생성하거나 사용할 수 없음
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Form Login, Basic Http 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // JwtAuthFilter 를 UserNamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new JwtAuthFilter(userService, jwtUtil), UsernamePasswordAuthenticationFilter.class)
                // Exception Handling Entry Point 변경
                .exceptionHandling((exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                ))

                // 권한 규칙 작성
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(AUTH_WHITELIST).permitAll()
                        .requestMatchers(regexRequestMatcher).permitAll()
                        .anyRequest().authenticated());

        return http.build();
    }
}
