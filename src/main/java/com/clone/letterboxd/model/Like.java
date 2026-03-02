package com.clone.letterboxd.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes")
@Data
public class Like {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "diary_entry_id")
    private DiaryEntry diaryEntry;

    @ManyToOne
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne
    @JoinColumn(name = "film_list_id")
    private FilmList filmList;

    // slug for featured lists (not stored in film_lists table; helps avoid FK violations)
    @Column(name = "featured_list_slug")
    private String featuredListSlug;

    // direct movie likes (not tied to diary or review)
    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}