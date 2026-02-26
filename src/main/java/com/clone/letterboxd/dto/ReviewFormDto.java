package com.clone.letterboxd.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewFormDto {

    private Long id;

    @NotNull(message = "Movie is required")
    private Long movieId;

    @Size(max = 120, message = "Title cannot exceed 120 characters")
    private String title;

    @NotBlank(message = "Review body is required")
    @Size(min = 20, max = 5000, message = "Review should be between 20 and 5000 characters")
    private String body;

    private Boolean containsSpoilers = false;

    private Boolean isDraft = false;

    private Long diaryEntryId;

    private String visibility = "PUBLIC";
}