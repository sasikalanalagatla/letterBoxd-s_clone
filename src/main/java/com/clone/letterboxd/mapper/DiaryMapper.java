package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.DiaryEntryDisplayDto;
import com.clone.letterboxd.dto.DiaryEntryFormDto;
import com.clone.letterboxd.enums.Visibility;
import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DiaryMapper {

    public static DiaryEntryDisplayDto toDisplayDto(DiaryEntry entity, User user) {
        if (entity == null) return null;

        DiaryEntryDisplayDto dto = new DiaryEntryDisplayDto();
        dto.setId(entity.getId());
        dto.setMovieId(entity.getMovieId());
        dto.setWatchDate(entity.getWatchDate());
        dto.setRating(entity.getRating());
        dto.setReviewText(entity.getReviewText());
        dto.setLiked(entity.getLiked());
        dto.setVisibility(entity.getVisibility());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        // These are usually enriched later in service
        // dto.setMovieTitle(...);
        // dto.setMoviePosterPath(...);
        // dto.setMovieYear(...);
        // dto.setRatingDisplay(...);
        // dto.setUser(...);
        // dto.setLikeCount(...);
        // dto.setCommentCount(...);
        // dto.setCurrentUserLiked(...);

        return dto;
    }

    public static DiaryEntry toEntity(DiaryEntryFormDto dto, User user) {
        if (dto == null) return null;

        DiaryEntry entity = new DiaryEntry();
        entity.setUser(user);
        entity.setMovieId(dto.getMovieId());
        entity.setWatchDate(dto.getWatchDate());
        entity.setRating(dto.getRating());
        entity.setReviewText(dto.getReviewText());
        entity.setLiked(dto.getLiked() != null ? dto.getLiked() : false);
        entity.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : Visibility.PUBLIC);

        // createdAt / updatedAt are set by default in entity
        return entity;
    }

    public static void updateEntity(DiaryEntry entity, DiaryEntryFormDto dto) {
        if (dto == null || entity == null) return;

        if (dto.getMovieId() != null) entity.setMovieId(dto.getMovieId());
        if (dto.getWatchDate() != null) entity.setWatchDate(dto.getWatchDate());
        if (dto.getRating() != null) entity.setRating(dto.getRating());
        if (dto.getReviewText() != null) entity.setReviewText(dto.getReviewText());
        if (dto.getLiked() != null) entity.setLiked(dto.getLiked());
        if (dto.getVisibility() != null) entity.setVisibility(dto.getVisibility());

        entity.setUpdatedAt(LocalDateTime.now());
    }
}