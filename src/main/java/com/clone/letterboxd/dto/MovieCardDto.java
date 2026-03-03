package com.clone.letterboxd.dto;

import lombok.Data;

import java.util.List;

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
    private List<String> genreNames;   
    private String originalLanguage;   
    private Long reviewCount;
    private Long likeCount;

    public String getPosterUrl() {
        return posterPath != null
                ? "https://image.tmdb.org/t/p/w500" + posterPath
                : "https://placehold.co/500x750/1c2128/667?text=No+Poster";
    }

    public String getPosterUrlSmall() {
        return posterPath != null
                ? "https://image.tmdb.org/t/p/w342" + posterPath
                : "https://placehold.co/342x513/1c2128/667?text=No+Poster";
    }

    public String getYear() {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        return releaseDate.substring(0, 4);
    }
}