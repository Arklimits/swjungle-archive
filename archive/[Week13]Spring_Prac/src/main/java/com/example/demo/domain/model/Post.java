package com.example.demo.domain.model;

import jakarta.persistence.*;
import lombok.NoArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Getter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private String title;
    private String content;
    private String author;
    private String createdAt;
    private String modifiedAt;

    @OneToMany(mappedBy = "postId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();

    public Post(String title, String content, String author, String date) {
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = date;
        this.modifiedAt = date;
    }

    public void editPost(String title, String content, String date){
        this.title = title;
        this.content = content;
        this.modifiedAt = date;
    }
}
