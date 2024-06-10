package com.example.demo.application.dto.comment;

import com.example.demo.domain.model.Comment;

import java.util.List;

public record CommentListDTO(List<Comment> comments) {
}
