package com.clone.letterboxd.dto;

import lombok.Data;

@Data
public class MovieCardDto {

    private Long id;
    private String title;
    private String originalTitle;
    private String posterPath;
    private String backdropPath;
    private String overview;
    private String releaseDate;
    private Integer year;
    private Double voteAverage;
    private Integer voteCount;

    private Double userRating;
    private Boolean inDiary;
    private Boolean inWatchlist;

    public String getPosterUrl() {
        return posterPath != null
                ? "https://image.tmdb.org/t/p/w500" + posterPath
                : "/images/no-poster.png";
    }

    public String getPosterUrlSmall() {
        return posterPath != null
                ? "https://image.tmdb.org/t/p/w342" + posterPath
                : "/images/no-poster.png";
    }

    public String getYear() {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        return releaseDate.substring(0, 4);
    }
}