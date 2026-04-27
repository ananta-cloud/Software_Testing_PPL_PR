package com.blog.vo;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;

import jakarta.servlet.http.HttpServletResponse;
import java.sql.ResultSet;
import java.util.Date;

import com.blog.service.*;
import com.blog.repository.*;
import com.blog.controller.*;
import com.blog.mapper.PostMapper;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ReliabilityWhiteBoxTest {

    @Mock private PostJpaRepository postJpaRepository;
    @Mock private CommentJpaRepository commentJpaRepository;
    @Mock private HttpServletResponse response;
    
    @InjectMocks private PostService postService;
    @InjectMocks private CommentService commentService;
    @InjectMocks private PostController postController;

    // ============================================================
    // 1. TESTING ENTITY & LIFECYCLE (Post.java)
    // ============================================================

    @Test
    void testPostLifecycle() {
        Post post = new Post();
        
        // [WB-POST-01] Jalur @PrePersist (onCreate)
        post.onCreate(); 
        assertNotNull(post.getRegDate(), "regDate harus terisi otomatis");
        assertNotNull(post.getUpdtDate(), "updtDate harus terisi otomatis");

        // [WB-POST-02] Jalur @PreUpdate (onUpdate)
        post.onUpdate();
        assertNotNull(post.getUpdtDate(), "updtDate harus diperbarui");
    }

    // ============================================================
    // 2. TESTING SERVICE LAYER (Logic Branching)
    // ============================================================

    @Test
    void testPostServiceLogic() {
        Post post = new Post();

        // [WB-SVC-01] savePost: Jalur False (result == null)
        when(postJpaRepository.save(any(Post.class))).thenReturn(null);
        assertFalse(postService.savePost(post), "isSuccess harus false jika save return null");

        // [WB-SVC-02] savePost: Jalur True (result != null)
        when(postJpaRepository.save(any(Post.class))).thenReturn(post);
        assertTrue(postService.savePost(post), "isSuccess harus true jika save berhasil");

        // [WB-SVC-03] deletePost: Jalur ID tidak ditemukan (result == null)
        when(postJpaRepository.findOneById(1L)).thenReturn(null);
        assertFalse(postService.deletePost(1L), "Harus return false jika ID tidak ada");

        // [WB-SVC-04] deletePost: Jalur ID ditemukan (result != null)
        when(postJpaRepository.findOneById(1L)).thenReturn(post);
        assertTrue(postService.deletePost(1L), "Harus return true jika delete berhasil");
    }

    @Test
    void testCommentServiceLogic() {
        Comment comment = new Comment();

        // [WB-SVC-05] saveComment: Jalur False (result == null)
        when(commentJpaRepository.save(any(Comment.class))).thenReturn(null);
        assertFalse(commentService.saveComment(comment), "isSuccess harus false jika save comment null");

        // [WB-SVC-06] deleteComment: Jalur ID tidak ditemukan
        when(commentJpaRepository.findOneById(10L)).thenReturn(null);
        assertFalse(commentService.deleteComment(10L), "Harus return false jika ID comment tidak ada");
    }

    // ============================================================
    // 3. TESTING CONTROLLER LAYER (Exception Handling)
    // ============================================================

    @Test
    void testPostControllerErrorHandling() {
        // [WB-CTRL-01] deletePost: Jalur Catch Exception (Try-Catch)
        when(postService.deletePost(1L)).thenThrow(new RuntimeException("Database Error"));

        Object resultObj = postController.deletePost(response, 1L);
        Result result = (Result) resultObj;

        assertEquals(500, result.getResult(), "Status harus 500 saat terjadi exception");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // ============================================================
    // 4. TESTING MAPPER LAYER (ResultSet Mapping)
    // ============================================================

    @Test
    void testPostMapperDataMapping() throws Exception {
        // [WB-MAP-01] mapRow: Jalur Pemetaan Kolom SQL ke Objek
        ResultSet rs = mock(ResultSet.class);
        PostMapper mapper = new PostMapper();

        when(rs.getLong("id")).thenReturn(100L);
        when(rs.getString("user")).thenReturn("Pras");
        when(rs.getString("title")).thenReturn("White Box Test");

        Post result = mapper.mapRow(rs, 1);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Pras", result.getUser());
    }
}