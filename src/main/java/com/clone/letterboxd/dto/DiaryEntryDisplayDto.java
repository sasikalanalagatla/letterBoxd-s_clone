package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiaryEntryDisplayDto {

    private Long id;
    private Long movieId;

    // Enriched from TMDB
    private String movieTitle;
    private String moviePosterPath;
    private String movieYear;

    private LocalDate watchDate;
    private Double rating;
    private String ratingDisplay;       // e.g. "★★★½"

    private String reviewText;          // short note/review
    private Boolean liked;              // heart for the film

    private Visibility visibility;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Social/interaction fields (usually populated in service)
    private UserSummaryDto user;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean currentUserLiked;

    // Convenience method (can be called in Thymeleaf)
    public String getPosterUrl() {
        return moviePosterPath != null
                ? "https://image.tmdb.org/t/p/w500" + moviePosterPath
                : "/images/no-poster.png";
    }
}