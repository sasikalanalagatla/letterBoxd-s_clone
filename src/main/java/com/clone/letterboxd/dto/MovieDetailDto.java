package com.clone.letterboxd.dto;

import lombok.Data;

import java.util.List;

@Data
public class MovieDetailDto {

    private Long id;
    private String title;
    private String originalTitle;
    private String tagline;
    private String overview;
    private String posterPath;
    private String backdropPath;
    private String releaseDate;
    private Integer runtime;
    private List<String> genres;
    private Double voteAverage;
    private Integer voteCount;

    private List<String> directors;
    private List<String> mainCast;

    private Double userRating;
    private Boolean watched;
    private Boolean inWatchlist;
    private String watchDate;
    private Long diaryEntryId;

    private Long diaryCount;
    private Long reviewCount;
    private Double averageLetterboxdRating;

    public String getPosterUrl() {
        return posterPath != null
                ? "https://image.tmdb.org/t/p/w780" + posterPath
                : "/images/no-poster.png";
    }

    public String getBackdropUrl() {
        return backdropPath != null
                ? "https://image.tmdb.org/t/p/original" + backdropPath
                : null;
    }

    public String getYear() {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        return releaseDate.substring(0, 4);
    }
}