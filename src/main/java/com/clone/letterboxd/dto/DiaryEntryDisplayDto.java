package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DiaryEntryDisplayDto {

    private Long id;
    private Long movieId;
    private String movieTitle;
    private String moviePosterPath;
    private String movieYear;

    private LocalDate watchDate;
    private Double rating;
    private String ratingDisplay;
    private String reviewText;
    private Boolean liked;
    private Visibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserSummaryDto user;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean currentUserLiked;

    public String getPosterUrl() {
        return moviePosterPath != null
                ? "https://image.tmdb.org/t/p/w500" + moviePosterPath
                : "/images/no-poster.png";
    }
}