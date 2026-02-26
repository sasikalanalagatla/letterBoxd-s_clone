package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.util.List;

@Data
public class FilmListSummaryDto {

    private Long id;
    private String name;
    private String descriptionExcerpt;  // first ~100 chars
    private Boolean ranked;
    private Boolean isWatchlist;
    private Visibility visibility;

    private UserSummaryDto owner;

    // Stats
    private Integer entryCount;
    private Integer likeCount;
    private Integer commentCount;
    private Boolean currentUserLiked;

    // Optional: preview of first few movies
    private List<String> previewPosterPaths;   // first 3–4 posters

    public String getDescriptionExcerpt() {
        if (descriptionExcerpt == null) return "";
        return descriptionExcerpt.length() > 120
                ? descriptionExcerpt.substring(0, 120) + "..."
                : descriptionExcerpt;
    }
}