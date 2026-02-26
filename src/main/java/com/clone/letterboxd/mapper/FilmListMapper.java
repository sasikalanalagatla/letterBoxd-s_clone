package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.FilmListDetailDto;
import com.clone.letterboxd.dto.FilmListFormDto;
import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.enums.Visibility;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.User;
import org.springframework.stereotype.Component;

@Component
public class FilmListMapper {

    public static FilmListDetailDto toDetailDto(FilmList list) {
        if (list == null) return null;

        FilmListDetailDto dto = new FilmListDetailDto();
        dto.setId(list.getId());
        dto.setName(list.getName());
        dto.setDescription(list.getDescription());
        dto.setRanked(list.getRanked());
        dto.setIsWatchlist(list.getIsWatchlist());
        dto.setVisibility(list.getVisibility());

        // entries, likeCount, commentCount, currentUserLiked → usually set in service

        return dto;
    }

    public static FilmListSummaryDto toSummaryDto(FilmList list) {
        if (list == null) return null;

        FilmListSummaryDto dto = new FilmListSummaryDto();
        dto.setId(list.getId());
        dto.setName(list.getName());
        dto.setRanked(list.getRanked());
        dto.setIsWatchlist(list.getIsWatchlist());
        dto.setVisibility(list.getVisibility());

        if (list.getDescription() != null) {
            String desc = list.getDescription();
            dto.setDescriptionExcerpt(desc.length() > 120 ? desc.substring(0, 117) + "..." : desc);
        }

        dto.setEntryCount(list.getEntries() != null ? list.getEntries().size() : 0);

        // previewPosterPaths, likeCount, commentCount, currentUserLiked → service layer

        return dto;
    }

    public static FilmList toEntity(FilmListFormDto dto, User user) {
        if (dto == null) return null;

        FilmList list = new FilmList();
        list.setUser(user);
        list.setName(dto.getName());
        list.setDescription(dto.getDescription());
        list.setRanked(dto.getRanked() != null ? dto.getRanked() : false);
        list.setIsWatchlist(dto.getIsWatchlist() != null ? dto.getIsWatchlist() : false);
        list.setVisibility(dto.getVisibility() != null ? dto.getVisibility() : Visibility.PUBLIC);

        return list;
    }

    public static void updateEntity(FilmList list, FilmListFormDto dto) {
        if (dto == null || list == null) return;

        if (dto.getName() != null) list.setName(dto.getName());
        if (dto.getDescription() != null) list.setDescription(dto.getDescription());
        if (dto.getRanked() != null) list.setRanked(dto.getRanked());
        if (dto.getIsWatchlist() != null) list.setIsWatchlist(dto.getIsWatchlist());
        if (dto.getVisibility() != null) list.setVisibility(dto.getVisibility());
    }
}