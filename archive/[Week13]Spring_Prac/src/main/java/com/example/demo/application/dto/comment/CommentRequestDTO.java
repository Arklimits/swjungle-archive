package com.example.demo.application.dto.comment;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class CommentRequestDTO {
    @NotNull
    private String content;
}
