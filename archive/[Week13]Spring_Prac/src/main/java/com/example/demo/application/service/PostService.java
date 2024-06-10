package com.example.demo.application.service;

import com.example.demo.application.dto.post.PostRequestDTO;
import com.example.demo.application.dto.post.PostResponseDTO;
import com.example.demo.domain.exception.NotAuthorException;
import com.example.demo.domain.exception.PostNotFoundException;
import com.example.demo.domain.model.Post;
import com.example.demo.domain.repository.CommentRepository;
import com.example.demo.domain.repository.PostRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@AllArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;

    /**
     * 전체 Post 로드
     *
     * @return 수정된 날짜 기준으로 내림차순 정렬된 Posts 리스트
     */
    public List<Post> getAllPosts() {

        return postRepository.findAllPostOrderByDateDesc();
    }

    /**
     * Post 게시
     *
     * @param postRequestDTO Post Request DTO
     * @return PostResponseDTO
     */
    public PostResponseDTO addPost(PostRequestDTO postRequestDTO) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Post post = new Post(postRequestDTO.getTitle(), postRequestDTO.getContent(), userService.getCurrentUsername(), date);

        return new PostResponseDTO(postRepository.save(post));
    }

    /**
     * Post 찾아서 DTO 에 넣기
     *
     * @param id Post ID
     * @return PostResponseDTO
     */
    public PostResponseDTO getPostById(long id) {

        return new PostResponseDTO(findPostById(id));
    }

    /**
     * Post Id로 찾기
     *
     * @param id Post ID
     * @return Post
     */
    public Post findPostById(long id) {

        return postRepository.findById(id).orElseThrow(PostNotFoundException::new);
    }

    /**
     * Post 업데이트
     *
     * @param id             Post ID
     * @param postRequestDTO Original PostResponseDTO
     * @return Updated PostResponseDTO
     */
    public PostResponseDTO updatePost(long id, PostRequestDTO postRequestDTO) {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        Post post = findPostById(id);

        Done:
        {
            if (userService.checkAdministrator())
                break Done;

            if (!userService.checkAuthor(post.getAuthor()))
                throw new NotAuthorException();
        }

        post.editPost(postRequestDTO.getTitle(), postRequestDTO.getContent(), date);

        return new PostResponseDTO(postRepository.save(post));
    }

    /**
     * Post 삭제 및 연결된 Comment 삭제
     *
     * @param id Post ID
     */
    public PostResponseDTO deletePostById(long id) {
        Post post = postRepository.findById(id).orElseThrow(PostNotFoundException::new);

        Done:
        {
            if (userService.checkAdministrator())
                break Done;

            if (!userService.checkAuthor(post.getAuthor()))
                throw new NotAuthorException();
        }

        commentRepository.deleteByPostId(id);
        postRepository.deleteById(id);

        return new PostResponseDTO(post);
    }
}