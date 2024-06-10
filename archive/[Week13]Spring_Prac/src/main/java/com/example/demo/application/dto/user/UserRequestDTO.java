package com.example.demo.application.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class UserRequestDTO {
    @NotNull
    @Size(min = 4, max = 10, message = "유저 이름은 4~10 글자여야 합니다.")
    @Pattern(regexp = "^[a-z0-9]+$", message = "유저 이름은 알파벳 소문자, 숫자로 구성되어야 합니다.")
    private String username;

    @NotNull
    @Size(min = 8, max = 15, message = "비밀번호는 8~15 글자여야 합니다")
    @Pattern(regexp = "^[A-Za-z\\d@$!%*?&]+$", message = "비밀번호는 알파벳 소문자, 대문자, 숫자, 특수문자로 구성되어야 합니다.")
    private String password;
}
