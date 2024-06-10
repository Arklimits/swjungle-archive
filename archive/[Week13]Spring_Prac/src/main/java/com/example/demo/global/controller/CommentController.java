package com.example.demo.global.controller;

import com.example.demo.application.dto.comment.CommentListDTO;
import com.example.demo.application.dto.comment.CommentRequestDTO;
import com.example.demo.application.dto.comment.CommentResponseDTO;
import com.example.demo.application.service.CommentService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/post/{postId}/comment")
@AllArgsConstructor
public class CommentController {
    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<CommentListDTO> getComments(@PathVariable("postId") Long postId) {
        CommentListDTO commentListDTO = new CommentListDTO(commentService.getCommentsByPostId(postId));

        return new ResponseEntity<>(commentListDTO, HttpStatus.OK);
    }

    @PostMapping("/new")
    public ResponseEntity<CommentResponseDTO> createComment(@PathVariable("postId") Long postId, @RequestBody CommentRequestDTO commentRequestDTO) {

        CommentResponseDTO commentResponseDTO = commentService.saveComment(postId, commentRequestDTO);

        return new ResponseEntity<>(commentResponseDTO, HttpStatus.OK);
    }

    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> getComment(@PathVariable("postId") Long postId, @PathVariable("commentId") Long commentId) {

        return new ResponseEntity<>(commentService.getCommentsById(commentId), HttpStatus.OK);
    }

    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> updateComment(@PathVariable("postId") Long postId, @PathVariable("commentId") Long commentId, @RequestBody CommentRequestDTO commentRequestDTO) {
        CommentResponseDTO commentResponseDTO = commentService.updateComment(commentId, commentRequestDTO);

        return new ResponseEntity<>(commentResponseDTO, HttpStatus.OK);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> deleteComment(@PathVariable("postId") Long postId, @PathVariable("commentId") Long commentId) {
        CommentResponseDTO commentResponseDTO = commentService.deleteComment(commentId);

        return new ResponseEntity<>(commentResponseDTO, HttpStatus.OK);
    }
}
