package com.blog.controller;

import com.blog.service.PostService;
import com.blog.vo.Post;
import com.blog.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 *  WHITE-BOX RELIABILITY TESTING — PostController
 *  Fokus : Recovery Capability (Exception Handling)
 *  Framework : JUnit 5 + Mockito
 * ============================================================
 *
 *  Cakupan Branch (sesuai tabel skenario WB-13, WB-14, WB-15):
 *
 *  savePost()   : isSuccess=true | isSuccess=false | exception (WB-13)
 *  deletePost() : isSuccess=true | isSuccess=false | exception (WB-14)
 *  modifyPost() : isSuccess=true | isSuccess=false | exception (WB-15)
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostController — Reliability Test (White-Box, Exception Handling)")
class PostControllerReliabilityTest {

    @Mock
    private PostService postService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private PostController postController;

    // ----------------------------------------------------------------
    //  Helper
    // ----------------------------------------------------------------
    private Post buildPost(Long id, String title, String content) {
        Post p = new Post();
        p.setId(id);
        p.setUser("tester");
        p.setTitle(title);
        p.setContent(content);
        p.setRegDate(new Date());
        p.setUpdtDate(new Date());
        return p;
    }

    // ================================================================
    //  1. savePost() — WB-13
    // ================================================================
    @Nested
    @DisplayName("1. savePost() — WB-13")
    class SavePostControllerTests {

        @Test
        @DisplayName("WB-13a | Service return true → Result 200 Success")
        void wb13a_savePost_serviceSuccess_shouldReturn200() {
            Post post = buildPost(null, "Judul", "Konten");
            when(postService.savePost(post)).thenReturn(true);

            Object result = postController.savePost(response, post);

            assertInstanceOf(Result.class, result);
            Result r = (Result) result;
            assertEquals(200, r.getResult(), "[WB-13a] HTTP result harus 200");
            assertEquals("Success", r.getMessage());
        }

        @Test
        @DisplayName("WB-13b | Service return false → Result 500 Fail + setStatus(500)")
        void wb13b_savePost_serviceFail_shouldReturn500() {
            Post post = buildPost(null, "Judul", "Konten");
            when(postService.savePost(post)).thenReturn(false);

            Object result = postController.savePost(response, post);

            assertInstanceOf(Result.class, result);
            Result r = (Result) result;
            assertEquals(500, r.getResult(), "[WB-13b] HTTP result harus 500");
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        /**
         * WB-13 — Catch block aktif saat service lempar exception
         * Recovery Capability: controller harus menangkap exception,
         * log error, dan return 500 tanpa crash
         */
        @Test
        @DisplayName("WB-13c | Service lempar Exception → catch block aktif → Result 500 + setStatus(500)")
        void wb13c_savePost_serviceThrowsException_shouldCatchAndReturn500() {
            Post post = buildPost(null, "Judul", "Konten");
            when(postService.savePost(post)).thenThrow(new RuntimeException("DB Connection Error"));

            Object result = postController.savePost(response, post);

            assertInstanceOf(Result.class, result);
            Result r = (Result) result;
            assertEquals(500, r.getResult(), "[WB-13c] Result harus 500 saat exception");
            assertTrue(r.getMessage().contains("DB Connection Error"),
                    "[WB-13c] Pesan error harus mengandung pesan exception");
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ================================================================
    //  2. deletePost() — WB-14
    // ================================================================
    @Nested
    @DisplayName("2. deletePost() — WB-14")
    class DeletePostControllerTests {

        @Test
        @DisplayName("WB-14a | Service return true → Result 200 Success")
        void wb14a_deletePost_serviceSuccess_shouldReturn200() {
            Long id = 1L;
            when(postService.deletePost(id)).thenReturn(true);

            Object result = postController.deletePost(response, id);

            assertInstanceOf(Result.class, result);
            assertEquals(200, ((Result) result).getResult(), "[WB-14a] Harus 200");
        }

        @Test
        @DisplayName("WB-14b | Service return false → Result 500 Fail + setStatus(500)")
        void wb14b_deletePost_serviceFail_shouldReturn500() {
            Long id = 99999L;
            when(postService.deletePost(id)).thenReturn(false);

            Object result = postController.deletePost(response, id);

            assertInstanceOf(Result.class, result);
            assertEquals(500, ((Result) result).getResult(), "[WB-14b] Harus 500");
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        /**
         * WB-14 — Catch block aktif saat deletePost() lempar exception
         * Recovery Capability: controller tidak crash
         */
        @Test
        @DisplayName("WB-14c | Service lempar Exception → catch block aktif → Result 500")
        void wb14c_deletePost_serviceThrowsException_shouldCatchAndReturn500() {
            Long id = 1L;
            when(postService.deletePost(id)).thenThrow(new RuntimeException("Timeout DB"));

            Object result = postController.deletePost(response, id);

            assertInstanceOf(Result.class, result);
            Result r = (Result) result;
            assertEquals(500, r.getResult(), "[WB-14c] Harus 500 saat exception");
            assertTrue(r.getMessage().contains("Timeout DB"),
                    "[WB-14c] Message harus mengandung pesan exception");
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // ================================================================
    //  3. modifyPost() — WB-15
    // ================================================================
    @Nested
    @DisplayName("3. modifyPost() — WB-15")
    class ModifyPostControllerTests {

        @Test
        @DisplayName("WB-15a | Service return true → Result 200 Success")
        void wb15a_modifyPost_serviceSuccess_shouldReturn200() {
            Post post = buildPost(1L, "Judul Baru", "Konten Baru");
            when(postService.updatePost(post)).thenReturn(true);

            Object result = postController.modifyPost(response, post);

            assertInstanceOf(Result.class, result);
            assertEquals(200, ((Result) result).getResult(), "[WB-15a] Harus 200");
        }

        @Test
        @DisplayName("WB-15b | Service return false → Result 500 Fail + setStatus(500)")
        void wb15b_modifyPost_serviceFail_shouldReturn500() {
            Post post = buildPost(99999L, "Judul Baru", "Konten Baru");
            when(postService.updatePost(post)).thenReturn(false);

            Object result = postController.modifyPost(response, post);

            assertInstanceOf(Result.class, result);
            assertEquals(500, ((Result) result).getResult(), "[WB-15b] Harus 500");
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        /**
         * WB-15 — Catch block aktif saat updatePost() lempar exception
         * Recovery Capability: controller tidak crash, response tetap terkontrol
         */
        @Test
        @DisplayName("WB-15c | Service lempar Exception → catch block aktif → Result 500")
        void wb15c_modifyPost_serviceThrowsException_shouldCatchAndReturn500() {
            Post post = buildPost(1L, "Judul", "Konten");
            when(postService.updatePost(post)).thenThrow(new RuntimeException("Constraint Violation"));

            Object result = postController.modifyPost(response, post);

            assertInstanceOf(Result.class, result);
            Result r = (Result) result;
            assertEquals(500, r.getResult(), "[WB-15c] Harus 500 saat exception");
            assertTrue(r.getMessage().contains("Constraint Violation"),
                    "[WB-15c] Message harus mengandung pesan exception");
            verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}