package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class DiaryEntryFormDto {

    private Long id;                    // null when creating

    @NotNull(message = "Movie is required")
    private Long movieId;

    @NotNull(message = "Watch date is required")
    @PastOrPresent(message = "Watch date cannot be in the future")
    private LocalDate watchDate;

    @DecimalMin(value = "0.5", message = "Rating must be at least 0.5")
    @DecimalMax(value = "5.0", message = "Rating cannot exceed 5.0")
    @Digits(integer = 1, fraction = 1, message = "Rating must have at most one decimal place")
    private Double rating;

    @Size(max = 2000, message = "Review text is too long")
    private String reviewText;

    private Boolean liked = false;

    private Visibility visibility = Visibility.PUBLIC;
}