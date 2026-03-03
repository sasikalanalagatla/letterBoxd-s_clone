package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import lombok.Data;

import java.util.List;

@Data
public class FilmListSummaryDto {

    private Long id;
    private String name;
    private String description;
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
    private List<Long> previewMovieIds;
    private String slug;

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public List<Long> getPreviewMovieIds() {
        return previewMovieIds != null ? previewMovieIds : List.of();
    }

    public void setPreviewMovieIds(List<Long> previewMovieIds) {
        this.previewMovieIds = previewMovieIds;
    }

    public String getDescriptionExcerpt() {
        if (descriptionExcerpt == null) return "";
        return descriptionExcerpt.length() > 120
                ? descriptionExcerpt.substring(0, 120) + "..."
                : descriptionExcerpt;
    }

    // ensure preview paths include full image URL
    public List<String> getPreviewPosterPaths() {
        if (previewPosterPaths == null) return List.of();
        return previewPosterPaths.stream()
                .map(p -> p != null ? "https://image.tmdb.org/t/p/w200" + p : null)
                .toList();
    }
}