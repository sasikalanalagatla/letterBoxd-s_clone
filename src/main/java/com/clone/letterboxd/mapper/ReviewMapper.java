package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.ReviewDisplayDto;
import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.enums.Visibility;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public static ReviewDisplayDto toDisplayDto(Review review) {
        if (review == null) return null;

        ReviewDisplayDto dto = new ReviewDisplayDto();
        dto.setId(review.getId());
        dto.setMovieId(review.getMovieId());
        dto.setTitle(review.getTitle());
        dto.setBody(review.getBody());
        dto.setContainsSpoilers(review.getContainsSpoilers() != null ? review.getContainsSpoilers() : false);
        dto.setVisibility(review.getVisibility());
        dto.setPublishedAt(review.getPublishedAt());

        // Excerpt - simple truncation
        if (review.getBody() != null) {
            String body = review.getBody();
            dto.setBodyExcerpt(body.length() > 200 ? body.substring(0, 197) + "..." : body);
        }

        // Usually enriched later:
        // dto.setMovieTitle(...)
        // dto.setMoviePosterPath(...)
        // dto.setAuthor(...)
        // dto.setLikeCount(...)
        // dto.setCommentCount(...)
        // dto.setCurrentUserLiked(...)

        return dto;
    }

    public static Review toEntity(ReviewFormDto dto, User user) {
        if (dto == null) return null;

        Review review = new Review();
        review.setUser(user);
        review.setMovieId(dto.getMovieId());
        review.setTitle(dto.getTitle());
        review.setBody(dto.getBody());
        review.setContainsSpoilers(dto.getContainsSpoilers() != null ? dto.getContainsSpoilers() : false);
        review.setIsDraft(dto.getIsDraft() != null ? dto.getIsDraft() : true);
        review.setVisibility(dto.getVisibility() != null ? Visibility.valueOf(dto.getVisibility()) : Visibility.PUBLIC);

        return review;
    }

    public static void updateFromDto(Review review, ReviewFormDto dto) {
        if (dto == null || review == null) return;

        if (dto.getTitle() != null) review.setTitle(dto.getTitle());
        if (dto.getBody() != null) review.setBody(dto.getBody());
        if (dto.getContainsSpoilers() != null) review.setContainsSpoilers(dto.getContainsSpoilers());
        if (dto.getIsDraft() != null) review.setIsDraft(dto.getIsDraft());
        if (dto.getVisibility() != null) review.setVisibility(Visibility.valueOf(dto.getVisibility()));
    }
}