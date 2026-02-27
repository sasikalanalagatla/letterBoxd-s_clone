package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.util.List;

@Data
public class FilmListSummaryDto {

    private Long id;
    private String name;
    private String descriptionExcerpt;
    private Boolean ranked;
    private Boolean isWatchlist;
    private Visibility visibility;
    private UserSummaryDto owner;
    private Integer entryCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean currentUserLiked;
    private List<String> previewPosterPaths;

    public String getDescriptionExcerpt() {
        if (descriptionExcerpt == null) return "";
        return descriptionExcerpt.length() > 120
                ? descriptionExcerpt.substring(0, 120) + "..."
                : descriptionExcerpt;
    }
}