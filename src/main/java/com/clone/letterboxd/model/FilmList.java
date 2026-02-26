package com.clone.letterboxd.model;

import com.clone.letterboxd.enums.Visibility;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "film_lists")
@Data
public class FilmList {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Boolean ranked = false;

    @Column(name = "is_watchlist")
    private Boolean isWatchlist = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Visibility visibility = Visibility.PUBLIC;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FilmListEntry> entries = new ArrayList<>();
}