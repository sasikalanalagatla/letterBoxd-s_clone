package com.clone.letterboxd.model;

import com.clone.letterboxd.enums.Visibility;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Review {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "movie_id", nullable = false)
    private Long movieId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "diary_entry_id")
    private DiaryEntry diaryEntry;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    private Boolean containsSpoilers = false;
    private Boolean isDraft = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
