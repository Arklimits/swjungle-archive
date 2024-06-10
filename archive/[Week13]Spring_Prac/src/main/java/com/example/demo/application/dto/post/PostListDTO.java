package com.example.demo.application.dto.post;

import com.example.demo.domain.model.Post;

import java.util.List;

public record PostListDTO(List<Post> posts) {
}
