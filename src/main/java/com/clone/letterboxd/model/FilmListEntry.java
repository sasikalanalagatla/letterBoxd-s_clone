package com.clone.letterboxd.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "film_list_movies")
@Data
public class FilmListEntry {
    @Id
    @ManyToOne
    @JoinColumn(name = "list_id")
    private FilmList list;

    @Id
    @Column(name = "movie_id")
    private Long movieId;

    private Integer rank;
    private String note;

    @Column(name = "added_at")
    private LocalDateTime addedAt = LocalDateTime.now();
}