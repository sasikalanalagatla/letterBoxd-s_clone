package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.util.List;

@Data
public class FilmListDetailDto {
    private Long id;
    private String name;
    private String description;
    private Boolean ranked;
    private Boolean isWatchlist;
    private Visibility visibility;
    private UserSummaryDto owner;
    private List<FilmListEntryDto> entries;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean currentUserLiked;
}
