package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDisplayDto {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private String moviePosterPath;
    private String title;
    private String body;
    private String bodyExcerpt;
    private Boolean containsSpoilers;
    private Visibility visibility;
    private LocalDateTime publishedAt;
    private UserSummaryDto author;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean currentUserLiked;
}
