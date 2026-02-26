package com.clone.letterboxd.dto;

import com.clone.letterboxd.enums.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FilmListFormDto {

    private Long id;

    @NotBlank(message = "List name is required")
    @Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    private String name;

    @Size(max = 2000, message = "Description is too long")
    private String description;

    private Boolean ranked = false;

    private Boolean isWatchlist = false;

    private Visibility visibility = Visibility.PUBLIC;

}