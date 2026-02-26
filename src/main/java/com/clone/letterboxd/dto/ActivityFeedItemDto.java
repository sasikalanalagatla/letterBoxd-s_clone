package com.clone.letterboxd.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ActivityFeedItemDto {
    private String type;
    private LocalDateTime timestamp;
    private UserSummaryDto user;
    private Long targetId;
    private String targetTitle;
    private String excerpt;
    private String link;
}