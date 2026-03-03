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
    
    private Long followersCount = 0L;
    private Long followingCount = 0L;
    private Long listCount = 0L;
    private Long likeCount = 0L;

    public String getAvatarUrlOrDefault() {
        return avatarUrl != null ? avatarUrl : "/images/default-avatar.png";
    }
}