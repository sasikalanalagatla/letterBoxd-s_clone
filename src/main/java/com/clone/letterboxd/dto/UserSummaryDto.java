package com.clone.letterboxd.dto;

import lombok.Data;

@Data
public class UserSummaryDto {

    private Long id;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bioExcerpt;
    private Integer filmsWatchedCount;

    public String getAvatarUrlOrDefault() {
        return avatarUrl != null ? avatarUrl : "/images/default-avatar.png";
    }
}