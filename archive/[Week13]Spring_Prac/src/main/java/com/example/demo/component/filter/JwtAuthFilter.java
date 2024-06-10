package com.example.demo.component.filter;

import com.example.demo.application.service.UserService;
import com.example.demo.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * JWT 토큰 검증 필터 수행
     *
     * @param request     HttpServletRequest
     * @param response    - HttpServletResponse
     * @param filterChain FilterChain
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader("Authorization");
        done:
        {
            // 헤더에 JWT 검사
            if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) break done;

            String token = authorizationHeader.substring(7);

            // JWT 유효성 검증
            if (!jwtUtil.isTokenValid(token)) break done;

            String username = jwtUtil.getUsername(token);
            UserDetails userDetails = userService.loadUserByUsername(username); // 유저와 토큰이 일치할 시 userDetails 생성

            // Username, Password, Role -> Token 생성
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken
                    = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            // 현재 Request 의 Security Context 에 권한 설정
            SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
        }
        filterChain.doFilter(request, response); // 다음 필터로 이동
    }

}