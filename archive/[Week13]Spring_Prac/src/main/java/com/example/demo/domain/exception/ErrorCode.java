package com.example.demo.domain.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Set;

@Getter
public enum ErrorCode {
    // Common
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류", Set.of()),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "잘못된 입력입니다.",
            Set.of(MethodArgumentNotValidException.class, ConstraintViolationException.class)),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.",
            Set.of(HttpRequestMethodNotSupportedException.class)),

    USERNAME_EXIST(HttpStatus.BAD_REQUEST, "중복된 username 입니다.", Set.of(UsernameExistException.class)),
    USERROLE_NOTFOUND(HttpStatus.NOT_FOUND, "회원의 Role이 존재하지 않습니다.", Set.of(UserRoleNotFoundException.class)),
    USER_NOTFOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다.", Set.of(UsernameNotFoundException.class)),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.", Set.of(PasswordNotMatchException.class)),
    NOT_AUTHOR(HttpStatus.FORBIDDEN, "작성자가 아닙니다.", Set.of(NotAuthorException.class)),
    NOTFOUND_ARTICLE(HttpStatus.NOT_FOUND, "존재하지 않는 게시물입니다.", Set.of(PostNotFoundException.class)),
    NOTFOUND_COMMENT(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다.", Set.of(CommentNotFoundException.class)),
    TOKEN_INVALID(HttpStatus.BAD_REQUEST, "토큰이 유효하지 않습니다.", Set.of(InvalidJwtException.class)),
    TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "토큰이 만료됐습니다.", Set.of(ExpiredJwtException.class));

    private final HttpStatusCode status;
    private final String code;
    private final String message;
    private final Set<Class<? extends Exception>> exceptions;

    ErrorCode(HttpStatusCode status, String code, String message, Set<Class<? extends Exception>> exceptions) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.exceptions = exceptions;
    }

    ErrorCode(HttpStatusCode status, String message, Set<Class<? extends Exception>> exceptions) {
        this.status = status;
        this.code = String.valueOf(status.value());
        this.message = message;
        this.exceptions = exceptions;
    }
    }