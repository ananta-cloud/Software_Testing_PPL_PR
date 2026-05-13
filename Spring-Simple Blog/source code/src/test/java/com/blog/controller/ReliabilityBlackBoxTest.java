package com.blog.controller;

import com.blog.service.PostService;
import com.blog.vo.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ============================================================
 *  BLACK-BOX RELIABILITY TESTING — PostController & API Endpoint
 *  Level Pengujian : Reliability (ISO 25010)
 *  Framework       : JUnit 5 + Spring MockMvc
 * ============================================================
 *
 *  Cakupan Skenario (sesuai tabel REL-POS-* & REL-NEG-*):
 *
 *  Availability     : REL-POS-02 — Ketersediaan Seluruh Endpoint
 *  Maturity         : REL-POS-03 — Konsistensi Operasi Read/Write (100x)
 *  Maturity         : REL-POS-04 — Pengambilan Data Identik (GET berulang)
 *  Fault Tolerance  : REL-NEG-01 — Pencarian ID Tidak Valid (99999)
 *  Fault Tolerance  : REL-NEG-02 — Manipulasi Tipe Data Parameter (huruf)
 *  Fault Tolerance  : REL-NEG-03 — Metode HTTP Salah (POST ke GET endpoint)
 *  Fault Tolerance  : REL-NEG-04 — Payload JSON Super Besar (>10MB)
 *  Fault Tolerance  : REL-NEG-05 — Format JSON Rusak / Malformed
 *
 *  Catatan:
 *  REL-POS-01 (Uptime 48 jam)  → manual / monitoring tool (Uptime Robot, etc.)
 *  REL-POS-05 (Navigasi cepat) → manual / Selenium/Cypress (UI browser test)
 * ============================================================
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(PostController.class)
@DisplayName("PostController — Reliability Test (Black-Box)")
class ReliabilityBlackboxTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ----------------------------------------------------------------
    //  Helper: membuat Post dummy
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
    //  REL-POS-02 — Availability
    //  Modul/Fitur : API Endpoint
    //  Pendekatan  : Blackbox
    //  Sifat       : Positif
    // ================================================================
    @Nested
    @DisplayName("REL-POS-02 | Ketersediaan Seluruh Endpoint")
    class AvailabilityEndpointTests {

        /**
         * REL-POS-02 — GET /posts harus return HTTP 200
         * Sistem konsisten mengembalikan HTTP 200 OK untuk endpoint valid.
         */
        @Test
        @DisplayName("REL-POS-02a | GET /posts → HTTP 200 OK")
        void relPos02a_getPosts_shouldReturn200() throws Exception {
            when(postService.getPosts()).thenReturn(List.of(buildPost(1L, "Post A", "Konten A")));

            mockMvc.perform(get("/posts"))
                    .andExpect(status().isOk());

            verify(postService, times(1)).getPosts();
        }

        /**
         * REL-POS-02b — GET /post?id=1 harus return HTTP 200
         * Endpoint detail post tersedia dan merespons dengan benar.
         */
        @Test
        @DisplayName("REL-POS-02b | GET /post?id=1 → HTTP 200 OK")
        void relPos02b_getPostById_shouldReturn200() throws Exception {
            when(postService.getPost(1L)).thenReturn(buildPost(1L, "Post A", "Konten A"));

            mockMvc.perform(get("/post").param("id", "1"))
                    .andExpect(status().isOk());
        }
    }

    // ================================================================
    //  REL-POS-03 — Maturity
    //  Modul/Fitur : PostService (via Controller)
    //  Pendekatan  : Blackbox
    //  Sifat       : Positif
    // ================================================================
    @Nested
    @DisplayName("REL-POS-03 | Konsistensi Operasi Read/Write (100x)")
    class ReadWriteConsistencyTests {

        /**
         * REL-POS-03 — Buat, baca, hapus post sebanyak 100 kali.
         * Basis data tidak terkunci dan seluruh data diproses tanpa anomali.
         * (Diverifikasi via mock: tidak ada exception, semua iterasi berhasil)
         */
        @Test
        @DisplayName("REL-POS-03 | Create-Read-Delete 100x → tidak ada anomali")
        void relPos03_crudRepeated100Times_shouldNotFail() throws Exception {
            when(postService.savePost(any(Post.class))).thenReturn(true);
            when(postService.getPost(anyLong())).thenReturn(buildPost(1L, "Post Loop", "Konten Loop"));
            when(postService.deletePost(anyLong())).thenReturn(true);

            Post postBody = buildPost(null, "Post Loop", "Konten Loop");
            String postJson = objectMapper.writeValueAsString(postBody);

            for (int i = 1; i <= 100; i++) {
                // CREATE
                mockMvc.perform(post("/post")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(postJson))
                        .andExpect(status().isOk());

                // READ
                mockMvc.perform(get("/post").param("id", "1"))
                        .andExpect(status().isOk());

                // DELETE
                mockMvc.perform(delete("/post").param("id", "1"))
                        .andExpect(status().isOk());
            }

            // Verifikasi: masing-masing operasi dipanggil tepat 100 kali
            verify(postService, times(100)).savePost(any(Post.class));
            verify(postService, times(100)).getPost(1L);
            verify(postService, times(100)).deletePost(1L);
        }
    }

    // ================================================================
    //  REL-POS-04 — Maturity
    //  Modul/Fitur : API Endpoint
    //  Pendekatan  : Blackbox
    //  Sifat       : Positif
    // ================================================================
    @Nested
    @DisplayName("REL-POS-04 | Pengambilan Data Identik (GET berulang)")
    class IdempotentGetTests {

        /**
         * REL-POS-04 — Lakukan GET /post?id=1 berkali-kali.
         * Sistem mengembalikan struktur JSON yang sama persis setiap saat.
         */
        @Test
        @DisplayName("REL-POS-04 | GET /post?id=1 berulang → JSON identik setiap saat")
        void relPos04_repeatedGet_shouldReturnIdenticalJson() throws Exception {
            Post expected = buildPost(1L, "Post Stabil", "Konten Stabil");
            when(postService.getPost(1L)).thenReturn(expected);

            String firstResponse = mockMvc.perform(get("/post").param("id", "1"))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            // Panggil 9x lagi, bandingkan setiap response dengan yang pertama
            for (int i = 2; i <= 10; i++) {
                String nthResponse = mockMvc.perform(get("/post").param("id", "1"))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

                Assertions.assertEquals(firstResponse, nthResponse,
                        "[REL-POS-04] Respons ke-" + i + " berbeda dari respons pertama");
            }
        }
    }

    // ================================================================
    //  REL-NEG-01 — Fault Tolerance
    //  Modul/Fitur : PostController
    //  Pendekatan  : Blackbox
    //  Sifat       : Negatif
    // ================================================================
    @Nested
    @DisplayName("REL-NEG-01 | Pencarian ID Tidak Valid (99999)")
    class InvalidIdTests {

        /**
         * REL-NEG-01 — Kirim request GET /post?id=99999.
         * Sistem tidak throw NullPointerException; harus tetap merespons normal.
         * Catatan: PostController tidak ada null-check → service return null
         * → Jackson serialisasi null jadi "null" body, status tetap 200.
         * Test ini memverifikasi sistem tidak crash (tidak 500).
         */
        @Test
        @DisplayName("REL-NEG-01 | GET /post?id=99999 → tidak NullPointerException / tidak 500")
        void relNeg01_invalidId_shouldNotThrowNPE() throws Exception {
            when(postService.getPost(99999L)).thenReturn(null);

            mockMvc.perform(get("/post").param("id", "99999"))
                    .andExpect(result ->
                            Assertions.assertNotEquals(500,
                                    result.getResponse().getStatus(),
                                    "[REL-NEG-01] Sistem tidak boleh return 500 / throw NPE untuk ID tidak ada"));
        }
    }

    // ================================================================
    //  REL-NEG-02 — Fault Tolerance
    //  Modul/Fitur : PostController
    //  Pendekatan  : Blackbox
    //  Sifat       : Negatif
    // ================================================================
    @Nested
    @DisplayName("REL-NEG-02 | Manipulasi Tipe Data Parameter (huruf)")
    class InvalidParamTypeTests {

        /**
         * REL-NEG-02 — Kirim request GET /post?id=abc (huruf).
         * Spring otomatis reject non-numerik untuk @RequestParam Long → HTTP 400.
         * Sistem tidak crash dan merespons dengan anggun.
         */
        @Test
        @DisplayName("REL-NEG-02 | GET /post?id=abc → HTTP 400 Bad Request")
        void relNeg02_stringParamForLong_shouldReturn400() throws Exception {
            mockMvc.perform(get("/post").param("id", "abc"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================================================
    //  REL-NEG-03 — Fault Tolerance
    //  Modul/Fitur : API Endpoint
    //  Pendekatan  : Blackbox
    //  Sifat       : Negatif
    // ================================================================
    @Nested
    @DisplayName("REL-NEG-03 | Metode HTTP Salah")
    class WrongHttpMethodTests {

        /**
         * REL-NEG-03 — Tembak endpoint GET /posts menggunakan POST.
         * Sistem mengembalikan HTTP 405 Method Not Allowed dengan anggun.
         */
        @Test
        @DisplayName("REL-NEG-03 | POST ke /posts (seharusnya GET) → HTTP 405 Method Not Allowed")
        void relNeg03_wrongMethod_shouldReturn405() throws Exception {
            mockMvc.perform(post("/posts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isMethodNotAllowed());
        }

        /**
         * REL-NEG-03b — DELETE ke /posts (tidak ada handler DELETE /posts).
         * Sistem tetap mengembalikan 405, tidak crash.
         */
        @Test
        @DisplayName("REL-NEG-03b | DELETE ke /posts → HTTP 405 Method Not Allowed")
        void relNeg03b_deleteToGetEndpoint_shouldReturn405() throws Exception {
            mockMvc.perform(delete("/posts"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ================================================================
    //  REL-NEG-04 — Fault Tolerance
    //  Modul/Fitur : API Endpoint
    //  Pendekatan  : Blackbox
    //  Sifat       : Negatif
    // ================================================================
    @Nested
    @DisplayName("REL-NEG-04 | Payload JSON Super Besar (>10MB)")
    class LargePayloadTests {

        /**
         * REL-NEG-04 — Kirim JSON konten berukuran > 10MB.
         * Sistem menolak dengan error 413 Request Entity Too Large.
         *
         * Catatan: Agar test ini PASS, tambahkan di application.properties:
         *   spring.servlet.multipart.max-request-size=10MB
         *   spring.servlet.multipart.max-file-size=10MB
         *   server.tomcat.max-http-form-post-size=10MB
         * Tanpa konfigurasi tersebut, Spring default tidak membatasi ukuran
         * dan request akan diterima (200) — yang merupakan bug konfigurasi.
         */
        @Test
        @DisplayName("REL-NEG-04 | POST /post dengan body >10MB → HTTP 413 atau ditolak server")
        void relNeg04_largePayload_shouldBeRejected() throws Exception {
            // Buat string konten ~11MB
            String largeContent = "A".repeat(11 * 1024 * 1024);

            Post largePost = new Post();
            largePost.setTitle("Judul Besar");
            largePost.setContent(largeContent);
            largePost.setUser("tester");

            String largeJson = objectMapper.writeValueAsString(largePost);

            int status = mockMvc.perform(post("/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(largeJson))
                    .andReturn()
                    .getResponse()
                    .getStatus();

            // Sistem HARUS menolak: 400, 413, atau 500 (bukan 200)
            Assertions.assertNotEquals(200, status,
                    "[REL-NEG-04] Sistem tidak boleh menerima (200) payload >10MB. " +
                    "Tambahkan konfigurasi max-request-size di application.properties.");
        }
    }

    // ================================================================
    //  REL-NEG-05 — Fault Tolerance
    //  Modul/Fitur : API Endpoint
    //  Pendekatan  : Blackbox
    //  Sifat       : Negatif
    // ================================================================
    @Nested
    @DisplayName("REL-NEG-05 | Format JSON Rusak / Malformed")
    class MalformedJsonTests {

        /**
         * REL-NEG-05 — Kirim payload JSON yang kekurangan kurung kurawal '}'.
         * Sistem gagal mem-parsing dan me-return HTTP 400 Bad Request
         * tanpa merusak memori atau menyebabkan 500.
         */
        @Test
        @DisplayName("REL-NEG-05 | POST /post dengan JSON malformed → HTTP 400 Bad Request")
        void relNeg05_malformedJson_shouldReturn400() throws Exception {
            // JSON tidak lengkap — kurung kurawal penutup hilang
            String malformedJson = "{\"title\": \"Judul Test\", \"content\": \"Isi konten\"";

            mockMvc.perform(post("/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest());
        }

        /**
         * REL-NEG-05b — JSON dengan string tidak tertutup (unclosed string).
         * Sistem tetap return 400, tidak hang atau 500.
         */
        @Test
        @DisplayName("REL-NEG-05b | POST /post dengan string tidak tertutup → HTTP 400")
        void relNeg05b_unclosedStringJson_shouldReturn400() throws Exception {
            String brokenJson = "{\"title\": \"Judul tidak ditutup, \"content\": \"isi\"}";

            mockMvc.perform(post("/post")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(brokenJson))
                    .andExpect(status().isBadRequest());
        }
    }
}