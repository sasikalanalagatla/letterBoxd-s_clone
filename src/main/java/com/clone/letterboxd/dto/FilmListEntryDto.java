package com.clone.letterboxd.dto;

import lombok.Data;

@Data
public class FilmListEntryDto {
    private Long movieId;
    private String movieTitle;
    private String posterPath;
    private Integer rank;
    private String note;
}
