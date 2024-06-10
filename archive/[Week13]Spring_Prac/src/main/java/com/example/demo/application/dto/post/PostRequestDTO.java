package com.example.demo.application.dto.post;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
public class PostRequestDTO {
    @NotNull
    private String title;
    @NotNull
    private String content;
}
