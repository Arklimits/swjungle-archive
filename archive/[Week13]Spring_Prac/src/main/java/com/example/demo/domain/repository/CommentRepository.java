package com.example.demo.domain.repository;

import com.example.demo.domain.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT comment FROM Comment comment ORDER BY comment.createdAt DESC")
    List<Comment> findByPostIdCommentOrderedByDateDesc(Long postId);

    void deleteByPostId(Long postId);
}
