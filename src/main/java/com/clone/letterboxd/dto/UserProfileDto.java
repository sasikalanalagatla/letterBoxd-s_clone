package com.clone.letterboxd.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileDto {

    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String bio;
    private String avatarUrl;
    private String location;
    private LocalDateTime joinedAt;
    private Long filmsWatchedCount;
    private Double averageRating;
    private Integer reviewCount;
    private Integer listCount;
    private Integer followingCount;
    private Integer followersCount;
    private Boolean isOwnProfile;
    private Boolean isFollowing;
    private Boolean isBlocked;
}