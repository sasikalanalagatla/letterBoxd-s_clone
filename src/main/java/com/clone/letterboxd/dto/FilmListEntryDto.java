package com.clone.letterboxd.dto;

import lombok.Data;

@Data
public class FilmListEntryDto {
    private Long movieId;
    private String movieTitle;
    private String posterPath;
    private String releaseYear;
    private Double voteAverage;
    private Integer rank;
    private String note;

    // return full URL so templates don’t need to replicate the base path logic
    public String getPosterPath() {
        if (posterPath == null || posterPath.isEmpty()) {
            return null;
        }
        return "https://image.tmdb.org/t/p/w200" + posterPath;
    }
}
