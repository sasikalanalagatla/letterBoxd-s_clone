package com.clone.letterboxd.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileDto {

    private Long id;
    private String username;
    private String displayName;
    private String email;               // only visible to owner
    private String bio;
    private String avatarUrl;
    private String location;            // optional

    private LocalDateTime joinedAt;

    // Stats (computed)
    private Long filmsWatchedCount;
    private Double averageRating;       // e.g. 3.7
    private Integer reviewCount;
    private Integer listCount;
    private Integer followingCount;
    private Integer followersCount;

    // For current user viewing someone else's profile
    private Boolean isOwnProfile;
    private Boolean isFollowing;
    private Boolean isBlocked;          // if you implement blocking
}