package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.dto.MovieDetailDto;
import com.clone.letterboxd.model.DiaryEntry;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public static MovieDetailDto toMovieDetailDto(
            Object tmdbMovie,           // replace with your actual TMDB DTO/record/class
            DiaryEntry usersDiaryEntry, // null if not watched
            boolean inWatchlist,
            Long diaryCount,
            Long reviewCount,
            Double averageLetterboxdRating
    ) {
        MovieDetailDto dto = new MovieDetailDto();

        dto.setId(getLong(tmdbMovie, "id"));
        dto.setTitle(getString(tmdbMovie, "title"));
        dto.setOriginalTitle(getString(tmdbMovie, "original_title"));
        dto.setTagline(getString(tmdbMovie, "tagline"));
        dto.setOverview(getString(tmdbMovie, "overview"));
        dto.setPosterPath(getString(tmdbMovie, "poster_path"));
        dto.setBackdropPath(getString(tmdbMovie, "backdrop_path"));
        dto.setReleaseDate(getString(tmdbMovie, "release_date"));
        dto.setRuntime(getInteger(tmdbMovie, "runtime"));

        dto.setVoteAverage(getDouble(tmdbMovie, "vote_average"));
        dto.setVoteCount(getInteger(tmdbMovie, "vote_count"));

        if (usersDiaryEntry != null) {
            dto.setUserRating(usersDiaryEntry.getRating());
            dto.setWatched(true);
            dto.setWatchDate(usersDiaryEntry.getWatchDate() != null ?
                    usersDiaryEntry.getWatchDate().toString() : null);
            dto.setDiaryEntryId(usersDiaryEntry.getId());
        } else {
            dto.setUserRating(null);
            dto.setWatched(false);
            dto.setWatchDate(null);
            dto.setDiaryEntryId(null);
        }

        dto.setInWatchlist(inWatchlist);

        dto.setDiaryCount(diaryCount);
        dto.setReviewCount(reviewCount);
        dto.setAverageLetterboxdRating(averageLetterboxdRating);

        return dto;
    }

    public MovieCardDto toMovieCardDto(Object tmdbMovie) {
        MovieCardDto dto = new MovieCardDto();

        dto.setId(getLong(tmdbMovie, "id"));
        dto.setTitle(getString(tmdbMovie, "title"));
        dto.setOriginalTitle(getString(tmdbMovie, "original_title"));
        dto.setPosterPath(getString(tmdbMovie, "poster_path"));
        dto.setBackdropPath(getString(tmdbMovie, "backdrop_path"));
        dto.setOverview(getString(tmdbMovie, "overview"));
        dto.setReleaseDate(getString(tmdbMovie, "release_date"));

        dto.setVoteAverage(getDouble(tmdbMovie, "vote_average"));
        dto.setVoteCount(getInteger(tmdbMovie, "vote_count"));

        // User-specific fields – usually set later in service
        dto.setUserRating(null);
        dto.setInDiary(false);
        dto.setInWatchlist(false);

        return dto;
    }

    private static Long getLong(Object obj, String field) {
        if (obj instanceof java.util.Map) {
            Object value = ((java.util.Map<?, ?>) obj).get(field);
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
        }
        return null;
    }

    private static String getString(Object obj, String field) {
        if (obj instanceof java.util.Map) {
            Object value = ((java.util.Map<?, ?>) obj).get(field);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private static Integer getInteger(Object obj, String field) {
        if (obj instanceof java.util.Map) {
            Object value = ((java.util.Map<?, ?>) obj).get(field);
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        }
        return null;
    }

    private static Double getDouble(Object obj, String field) {
        if (obj instanceof java.util.Map) {
            Object value = ((java.util.Map<?, ?>) obj).get(field);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        return null;
    }

}