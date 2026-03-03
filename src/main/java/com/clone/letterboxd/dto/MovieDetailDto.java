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
    private List<String> cast;
    private List<String> crew;
    private String releaseType;
    private String whereToWatch;
    private Double voteAverage;
    private String language;
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
    private Long likeCount;
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

    /**
     * Returns true if the movie has already been released (release date <= today).
     * This will be used by templates to disable review/rating/likes on unreleased titles.
     */
    public boolean isReleased() {
        if (releaseDate == null || releaseDate.isBlank()) return false;
        try {
            java.time.LocalDate d = java.time.LocalDate.parse(releaseDate);
            return !d.isAfter(java.time.LocalDate.now());
        } catch (java.time.format.DateTimeParseException e) {
            return false;
        }
    }
}