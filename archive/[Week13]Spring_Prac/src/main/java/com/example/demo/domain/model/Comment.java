package com.example.demo.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String content;
    private String author;
    private String createdAt;
    private String modifiedAt;
    private Long postId;

    public Comment(String content, String author, String date, Long postId) {
        this.content = content;
        this.author = author;
        this.createdAt = date;
        this.modifiedAt = date;
        this.postId = postId;
    }

    public void editComment(String content, String modifiedAt) {
        this.content = content;
        this.modifiedAt = modifiedAt;
    }
}
