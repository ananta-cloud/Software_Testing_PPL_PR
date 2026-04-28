package com.blog.service;

import com.blog.repository.CommentJpaRepository;
import com.blog.vo.Comment;
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
 *  WHITE-BOX RELIABILITY TESTING — CommentService
 *  Fokus : Failure Rate & Recovery Capability
 *  Framework : JUnit 5 + Mockito
 * ============================================================
 *
 *  Cakupan Branch (sesuai tabel skenario WB-09 s/d WB-12):
 *
 *  saveComment()   : result!=null (WB-09) | result==null (WB-10)
 *  deleteComment() : id ada (WB-11)       | id tidak ada (WB-12)
 *  getCommentList(): data ada             | data kosong
 *  searchComment() : query cocok          | query tidak cocok
 *  getComment()    : id ada               | id tidak ada
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentService — Reliability Test (White-Box)")
class CommentServiceReliabilityTest {

    @Mock
    private CommentJpaRepository commentJpaRepository;

    @InjectMocks
    private CommentService commentService;

    // ----------------------------------------------------------------
    //  Helper: membuat Comment dummy
    // ----------------------------------------------------------------
    private Comment buildComment(Long id, Long postId, String user, String text) {
        Comment c = new Comment(postId, user, text);
        c.setId(id);
        return c;
    }

    // ================================================================
    //  1. saveComment()
    // ================================================================
    @Nested
    @DisplayName("1. saveComment()")
    class SaveCommentTests {

        /**
         * WB-09 — Branch: result != null → isSuccess = true
         * Failure Rate: komentar berhasil disimpan
         */
        @Test
        @DisplayName("WB-09 | save() return non-null → harus return true")
        void wb09_saveComment_returnNonNull_shouldReturnTrue() {
            Comment comment = buildComment(null, 1L, "user1", "Komentar bagus!");
            when(commentJpaRepository.save(comment)).thenReturn(comment);

            boolean result = commentService.saveComment(comment);

            assertTrue(result, "[WB-09] saveComment harus return TRUE ketika save berhasil");
            verify(commentJpaRepository, times(1)).save(comment);
        }

        /**
         * WB-10 — Branch: result == null → isSuccess = false
         * Failure Rate: sistem mendeteksi kegagalan simpan komentar
         */
        @Test
        @DisplayName("WB-10 | save() return null → harus return false")
        void wb10_saveComment_returnNull_shouldReturnFalse() {
            Comment comment = buildComment(null, 1L, "user1", "Komentar gagal");
            when(commentJpaRepository.save(comment)).thenReturn(null);

            boolean result = commentService.saveComment(comment);

            assertFalse(result, "[WB-10] saveComment harus return FALSE ketika save mengembalikan null");
        }
    }

    // ================================================================
    //  2. deleteComment()
    // ================================================================
    @Nested
    @DisplayName("2. deleteComment()")
    class DeleteCommentTests {

        /**
         * WB-11 — Branch: result != null → deleteById() → return true
         * Failure Rate: penghapusan komentar yang ada berjalan normal
         */
        @Test
        @DisplayName("WB-11 | ID ditemukan → deleteById() dipanggil → return true")
        void wb11_deleteComment_idExists_shouldDeleteAndReturnTrue() {
            Long id = 1L;
            Comment existing = buildComment(id, 1L, "user1", "Komentar ada");
            when(commentJpaRepository.findOneById(id)).thenReturn(existing);
            doNothing().when(commentJpaRepository).deleteById(id);

            boolean result = commentService.deleteComment(id);

            assertTrue(result, "[WB-11] deleteComment harus return TRUE ketika ID ditemukan");
            verify(commentJpaRepository, times(1)).deleteById(id);
        }

        /**
         * WB-12 — Branch: result == null → early return false
         * Recovery Capability: sistem tidak crash saat komentar tidak ada
         */
        @Test
        @DisplayName("WB-12 | ID tidak ditemukan → deleteById() TIDAK dipanggil → return false")
        void wb12_deleteComment_idNotExists_shouldReturnFalseWithoutDelete() {
            Long id = 99999L;
            when(commentJpaRepository.findOneById(id)).thenReturn(null);

            boolean result = commentService.deleteComment(id);

            assertFalse(result, "[WB-12] deleteComment harus return FALSE ketika ID tidak ditemukan");
            verify(commentJpaRepository, never()).deleteById(any());
        }
    }

    // ================================================================
    //  3. getCommentList()
    // ================================================================
    @Nested
    @DisplayName("3. getCommentList()")
    class GetCommentListTests {

        @Test
        @DisplayName("PostID valid dengan komentar → return list berisi data (Failure Rate)")
        void getCommentList_dataExists_shouldReturnList() {
            Long postId = 1L;
            List<Comment> mockList = Arrays.asList(
                    buildComment(1L, postId, "user1", "Komentar 1"),
                    buildComment(2L, postId, "user2", "Komentar 2")
            );
            when(commentJpaRepository.findAllByPostIdOrderByRegDateDesc(postId)).thenReturn(mockList);

            List<Comment> result = commentService.getCommentList(postId);

            assertNotNull(result);
            assertEquals(2, result.size(), "Harus return 2 komentar");
        }

        @Test
        @DisplayName("PostID tidak punya komentar → return list kosong (Recovery Capability)")
        void getCommentList_noData_shouldReturnEmptyList() {
            Long postId = 99999L;
            when(commentJpaRepository.findAllByPostIdOrderByRegDateDesc(postId)).thenReturn(List.of());

            List<Comment> result = commentService.getCommentList(postId);

            assertNotNull(result, "Result tidak boleh null");
            assertTrue(result.isEmpty(), "List harus kosong jika tidak ada komentar");
        }
    }

    // ================================================================
    //  4. searchCommentList()
    // ================================================================
    @Nested
    @DisplayName("4. searchCommentList()")
    class SearchCommentTests {

        @Test
        @DisplayName("Query cocok → return list berisi hasil (Failure Rate)")
        void searchComment_queryMatches_shouldReturnResults() {
            Long postId = 1L;
            String query = "bagus";
            List<Comment> mockList = List.of(buildComment(1L, postId, "user1", "Konten bagus!"));
            when(commentJpaRepository.findByPostIdAndCommentContainingOrderByRegDateDesc(postId, query))
                    .thenReturn(mockList);

            List<Comment> result = commentService.searchCommentList(postId, query);

            assertFalse(result.isEmpty(), "Harus ada hasil untuk query yang cocok");
        }

        @Test
        @DisplayName("Query tidak cocok → return list kosong (Recovery Capability)")
        void searchComment_queryNoMatch_shouldReturnEmptyList() {
            Long postId = 1L;
            String query = "zzzznotfound";
            when(commentJpaRepository.findByPostIdAndCommentContainingOrderByRegDateDesc(postId, query))
                    .thenReturn(List.of());

            List<Comment> result = commentService.searchCommentList(postId, query);

            assertNotNull(result);
            assertTrue(result.isEmpty(), "List harus kosong jika query tidak cocok");
        }
    }

    // ================================================================
    //  5. getComment()
    // ================================================================
    @Nested
    @DisplayName("5. getComment()")
    class GetCommentTests {

        @Test
        @DisplayName("ID valid → return Comment object (Failure Rate)")
        void getComment_idExists_shouldReturnComment() {
            Long id = 1L;
            Comment expected = buildComment(id, 1L, "user1", "Komentar test");
            when(commentJpaRepository.findOneById(id)).thenReturn(expected);

            Comment result = commentService.getComment(id);

            assertNotNull(result, "getComment harus return non-null untuk ID yang ada");
            assertEquals(id, result.getId());
        }

        @Test
        @DisplayName("ID tidak ada → return null, tidak crash (Recovery Capability)")
        void getComment_idNotExists_shouldReturnNull() {
            when(commentJpaRepository.findOneById(99999L)).thenReturn(null);

            Comment result = commentService.getComment(99999L);

            assertNull(result, "getComment harus return null untuk ID tidak ada tanpa exception");
        }
    }
}