package com.blog.vo;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name = "post")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Wajib IDENTITY di Spring Boot 3
    private Long id;

    @Column(name = "post_user") // Wajib diubah karena "user" dilarang di MySQL
    private String user;

    private String title;
    private String content;

    @Temporal(TemporalType.TIMESTAMP)
    private Date regDate;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updtDate;

    // --- FITUR OTOMATIS MENGISI TANGGAL ---
    @PrePersist
    protected void onCreate() {
        this.regDate = new Date();
        this.updtDate = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updtDate = new Date();
    }

    // --- GETTER & SETTER ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Date getRegDate() { return regDate; }
    public void setRegDate(Date regDate) { this.regDate = regDate; }

    public Date getUpdtDate() { return updtDate; }
    public void setUpdtDate(Date updtDate) { this.updtDate = updtDate; }
}