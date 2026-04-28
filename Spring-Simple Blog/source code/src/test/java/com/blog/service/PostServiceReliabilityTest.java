package com.blog.service;

import com.blog.repository.PostJpaRepository;
import com.blog.repository.PostRepository;
import com.blog.vo.Post;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ============================================================
 *  WHITE-BOX RELIABILITY TESTING — PostService
 *  Fokus : Failure Rate & Recovery Capability
 *  Framework : JUnit 5 + Mockito
 * ============================================================
 *
 *  Cakupan Branch (sesuai tabel skenario WB-01 s/d WB-08 + WB-13):
 *
 *  savePost()   : result!=null (WB-01) | result==null (WB-02)
 *  deletePost() : id ada (WB-03)       | id tidak ada (WB-04)
 *  updatePost() : id tidak ada (WB-05) | title≠∅ & content=∅ (WB-06)
 *                 title=∅ & content≠∅ (WB-07) | keduanya≠∅ (WB-08)
 *  getPost()    : id ada               | id tidak ada
 *  getPosts()   : list terisi          | list kosong
 *  searchPost() : query cocok          | query tidak cocok
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostService — Reliability Test (White-Box)")
class PostServiceReliabilityTest {

    @Mock
    private PostJpaRepository jpaRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private PostService postService;

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
    //  1. savePost()
    // ================================================================
    @Nested
    @DisplayName("1. savePost()")
    class SavePostTests {

        /**
         * WB-01 — Branch: result != null → isSuccess = true
         * Failure Rate: sistem berhasil menyimpan data valid
         */
        @Test
        @DisplayName("WB-01 | save() return non-null → harus return true")
        void wb01_savePost_returnNonNull_shouldReturnTrue() {
            Post post = buildPost(null, "Judul Test", "Isi Konten");
            when(jpaRepository.save(post)).thenReturn(post);

            boolean result = postService.savePost(post);

            assertTrue(result, "[WB-01] savePost harus return TRUE ketika repository berhasil menyimpan");
            verify(jpaRepository, times(1)).save(post);
        }

        /**
         * WB-02 — Branch: result == null → isSuccess = false
         * Failure Rate: sistem mendeteksi kegagalan simpan
         */
        @Test
        @DisplayName("WB-02 | save() return null → harus return false")
        void wb02_savePost_returnNull_shouldReturnFalse() {
            Post post = buildPost(null, "Judul Test", "Isi Konten");
            when(jpaRepository.save(post)).thenReturn(null);

            boolean result = postService.savePost(post);

            assertFalse(result, "[WB-02] savePost harus return FALSE ketika repository mengembalikan null");
        }
    }

    // ================================================================
    //  2. deletePost()
    // ================================================================
    @Nested
    @DisplayName("2. deletePost()")
    class DeletePostTests {

        /**
         * WB-03 — Branch: result != null → deleteById() dipanggil → return true
         * Failure Rate: penghapusan data yang ada berjalan normal
         */
        @Test
        @DisplayName("WB-03 | ID ditemukan → deleteById() dipanggil → return true")
        void wb03_deletePost_idExists_shouldDeleteAndReturnTrue() {
            Long id = 1L;
            Post existing = buildPost(id, "Post Ada", "Konten ada");
            when(jpaRepository.findOneById(id)).thenReturn(existing);
            doNothing().when(jpaRepository).deleteById(id);

            boolean result = postService.deletePost(id);

            assertTrue(result, "[WB-03] deletePost harus return TRUE ketika ID ditemukan");
            verify(jpaRepository, times(1)).deleteById(id);
        }

        /**
         * WB-04 — Branch: result == null → early return false
         * Recovery Capability: sistem tidak crash saat ID tidak ada
         */
        @Test
        @DisplayName("WB-04 | ID tidak ditemukan → deleteById() TIDAK dipanggil → return false")
        void wb04_deletePost_idNotExists_shouldReturnFalseWithoutDelete() {
            Long id = 99999L;
            when(jpaRepository.findOneById(id)).thenReturn(null);

            boolean result = postService.deletePost(id);

            assertFalse(result, "[WB-04] deletePost harus return FALSE ketika ID tidak ditemukan");
            verify(jpaRepository, never()).deleteById(any());
        }
    }

    // ================================================================
    //  3. updatePost()
    // ================================================================
    @Nested
    @DisplayName("3. updatePost()")
    class UpdatePostTests {

        /**
         * WB-05 — Branch: findOneById() null → early return false
         * Recovery Capability: update pada ID tidak ada tidak merusak sistem
         */
        @Test
        @DisplayName("WB-05 | ID tidak ditemukan → return false tanpa update")
        void wb05_updatePost_idNotExists_shouldReturnFalse() {
            Post post = buildPost(99999L, "Judul Baru", "Konten Baru");
            when(jpaRepository.findOneById(99999L)).thenReturn(null);

            boolean result = postService.updatePost(post);

            assertFalse(result, "[WB-05] updatePost harus return FALSE ketika ID tidak ada");
            verify(jpaRepository, never()).save(any());
        }

        /**
         * WB-06 — Branch: title≠empty=true, content=empty=false
         * Failure Rate: partial update hanya pada title
         */
        @Test
        @DisplayName("WB-06 | Title terisi, Content kosong → hanya title yang diupdate")
        void wb06_updatePost_onlyTitleFilled_shouldUpdateTitleOnly() {
            Long id = 1L;
            Post existing = buildPost(id, "Judul Lama", "Konten Lama");
            Post updateReq = buildPost(id, "Judul Baru", ""); // content kosong

            when(jpaRepository.findOneById(id)).thenReturn(existing);
            when(jpaRepository.save(existing)).thenReturn(existing);

            boolean result = postService.updatePost(updateReq);

            assertTrue(result, "[WB-06] updatePost harus return TRUE");
            assertEquals("Judul Baru", existing.getTitle(),
                    "[WB-06] Title harus berubah menjadi 'Judul Baru'");
            assertEquals("Konten Lama", existing.getContent(),
                    "[WB-06] Content tidak boleh berubah karena input content kosong");
        }

        /**
         * WB-07 — Branch: title=empty=false, content≠empty=true
         * Failure Rate: partial update hanya pada content
         */
        @Test
        @DisplayName("WB-07 | Title kosong, Content terisi → hanya content yang diupdate")
        void wb07_updatePost_onlyContentFilled_shouldUpdateContentOnly() {
            Long id = 1L;
            Post existing = buildPost(id, "Judul Lama", "Konten Lama");
            Post updateReq = buildPost(id, "", "Konten Baru"); // title kosong

            when(jpaRepository.findOneById(id)).thenReturn(existing);
            when(jpaRepository.save(existing)).thenReturn(existing);

            boolean result = postService.updatePost(updateReq);

            assertTrue(result, "[WB-07] updatePost harus return TRUE");
            assertEquals("Judul Lama", existing.getTitle(),
                    "[WB-07] Title tidak boleh berubah karena input title kosong");
            assertEquals("Konten Baru", existing.getContent(),
                    "[WB-07] Content harus berubah menjadi 'Konten Baru'");
        }

        /**
         * WB-08 — Branch: title≠empty=true, content≠empty=true
         * Failure Rate: full update berjalan normal
         */
        @Test
        @DisplayName("WB-08 | Title dan Content keduanya terisi → keduanya diupdate")
        void wb08_updatePost_bothFilled_shouldUpdateBoth() {
            Long id = 1L;
            Post existing = buildPost(id, "Judul Lama", "Konten Lama");
            Post updateReq = buildPost(id, "Judul Baru", "Konten Baru");

            when(jpaRepository.findOneById(id)).thenReturn(existing);
            when(jpaRepository.save(existing)).thenReturn(existing);

            boolean result = postService.updatePost(updateReq);

            assertTrue(result, "[WB-08] updatePost harus return TRUE");
            assertEquals("Judul Baru", existing.getTitle(),
                    "[WB-08] Title harus berubah");
            assertEquals("Konten Baru", existing.getContent(),
                    "[WB-08] Content harus berubah");
        }
    }

    // ================================================================
    //  4. getPost() — tambahan coverage
    // ================================================================
    @Nested
    @DisplayName("4. getPost()")
    class GetPostTests {

        @Test
        @DisplayName("ID valid → return Post object (Failure Rate)")
        void getPost_idExists_shouldReturnPost() {
            Long id = 1L;
            Post expected = buildPost(id, "Judul", "Konten");
            when(jpaRepository.findOneById(id)).thenReturn(expected);

            Post result = postService.getPost(id);

            assertNotNull(result, "getPost harus return non-null untuk ID yang ada");
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("ID tidak ada → return null, sistem tidak crash (Recovery Capability)")
        void getPost_idNotExists_shouldReturnNull() {
            when(jpaRepository.findOneById(99999L)).thenReturn(null);

            Post result = postService.getPost(99999L);

            assertNull(result, "getPost harus return null untuk ID yang tidak ada tanpa exception");
        }
    }

    // ================================================================
    //  5. getPosts() — coverage list
    // ================================================================
    @Nested
    @DisplayName("5. getPosts()")
    class GetPostsTests {

        @Test
        @DisplayName("Data ada → return list berisi posts (Failure Rate)")
        void getPosts_dataExists_shouldReturnNonEmptyList() {
            List<Post> mockList = Arrays.asList(
                    buildPost(1L, "Post A", "Konten A"),
                    buildPost(2L, "Post B", "Konten B")
            );
            when(jpaRepository.findAllByOrderByUpdtDateDesc()).thenReturn(mockList);

            List<Post> result = postService.getPosts();

            assertNotNull(result);
            assertEquals(2, result.size(), "Harus return 2 post");
        }

        @Test
        @DisplayName("Data kosong → return list kosong, tidak null (Recovery Capability)")
        void getPosts_noData_shouldReturnEmptyList() {
            when(jpaRepository.findAllByOrderByUpdtDateDesc()).thenReturn(List.of());

            List<Post> result = postService.getPosts();

            assertNotNull(result, "Result tidak boleh null");
            assertTrue(result.isEmpty(), "List harus kosong");
        }
    }

    // ================================================================
    //  6. searchPostByTitle() — coverage pencarian
    // ================================================================
    @Nested
    @DisplayName("6. searchPostByTitle()")
    class SearchPostTests {

        @Test
        @DisplayName("Query cocok → return list berisi hasil (Failure Rate)")
        void searchByTitle_queryMatches_shouldReturnResults() {
            String query = "java";
            List<Post> mockList = List.of(buildPost(1L, "Belajar Java", "Konten Java"));
            when(jpaRepository.findByTitleContainingOrderByUpdtDateDesc(query)).thenReturn(mockList);

            List<Post> result = postService.searchPostByTitle(query);

            assertFalse(result.isEmpty(), "Harus ada hasil pencarian untuk query yang cocok");
        }

        @Test
        @DisplayName("Query tidak cocok → return list kosong (Recovery Capability)")
        void searchByTitle_queryNoMatch_shouldReturnEmptyList() {
            String query = "zzzznotfound";
            when(jpaRepository.findByTitleContainingOrderByUpdtDateDesc(query)).thenReturn(List.of());

            List<Post> result = postService.searchPostByTitle(query);

            assertNotNull(result);
            assertTrue(result.isEmpty(), "List harus kosong jika tidak ada yang cocok");
        }
    }
}