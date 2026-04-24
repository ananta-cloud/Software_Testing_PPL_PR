package com.blog.controller;

import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.blog.service.PostService;
import com.blog.vo.Post;
import com.blog.vo.Result;

@RestController
public class PostController {
    Logger log = LoggerFactory.getLogger(this.getClass());
    
    @Autowired
    PostService postService;
    
    @GetMapping("/post")
    public Post getPost(@RequestParam("id") Long id) {
        return postService.getPost(id);
    }
    
    @GetMapping("/posts")
    public List<Post> getPosts() {
        return postService.getPosts();
    }
    
    @GetMapping("/posts/updtdate/asc")
    public List<Post> getPostsOrderByUpdtAsc() {
        return postService.getPostsOrderByUpdtAsc();
    }
    
    @GetMapping("/posts/regdate/desc")
    public List<Post> getPostsOrderByRegDesc() {
        return postService.getPostsOrderByRegDesc();
    }
    
    @GetMapping("/posts/search/title")
    public List<Post> searchByTitle(@RequestParam("query") String query) {
        return postService.searchPostByTitle(query);
    }
    
    @GetMapping("/posts/search/content")
    public List<Post> searchByContent(@RequestParam("query") String query) {
        return postService.searchPostByContent(query);
    }
    
    @PostMapping("/post")
    public Object savePost(HttpServletResponse response, @RequestBody Post postParam)  {        
        try {
            // LANGSUNG SIMPAN postParam. Tidak perlu repot membuat 'new Post()' lagi.
            boolean isSuccess = postService.savePost(postParam);
            
            if(isSuccess) {
                return new Result(200, "Success");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new Result(500, "Fail");
            }
        } catch (Exception e) {
            // JURUS ANDALAN: Jika gagal, paksa error-nya tercetak di Terminal!
            log.error(" ERROR SAAT MENYIMPAN POSTINGAN : ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new Result(500, "Error: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/post")
    public Object deletePost(HttpServletResponse response, @RequestParam("id") Long id)  {
        try {
            boolean isSuccess = postService.deletePost(id);
            log.info("Mengahapus id ::: " + id);
            
            if(isSuccess) {
                return new Result(200, "Success");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new Result(500, "Fail");
            }
        } catch (Exception e) {
            log.error(" ERROR SAAT MENGHAPUS POSTINGAN : ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new Result(500, "Error: " + e.getMessage());
        }
    }
    
    @PutMapping("/post")
    public Object modifyPost(HttpServletResponse response, @RequestBody Post postParam)  {      
        try {
            // LANGSUNG UPDATE menggunakan postParam
            boolean isSuccess = postService.updatePost(postParam);
                    
            if(isSuccess) {
                return new Result(200, "Success");
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return new Result(500, "Fail");
            }
        } catch (Exception e) {
            log.error("ERROR SAAT MENGUPDATE POSTINGAN : ", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return new Result(500, "Error: " + e.getMessage());
        }
    }
}