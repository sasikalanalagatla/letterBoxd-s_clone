package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.dto.MovieDetailDto;
import com.clone.letterboxd.model.DiaryEntry;
import org.springframework.stereotype.Component;

@Component
public class MovieMapper {

    public static MovieDetailDto toMovieDetailDto(
            Object tmdbMovie,
            DiaryEntry usersDiaryEntry,
            boolean inWatchlist,
            Long diaryCount,
            Long reviewCount,
            Long likeCount,
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
        
        // genres
        if (tmdbMovie instanceof java.util.Map) {
            Object genresObj = ((java.util.Map<?, ?>) tmdbMovie).get("genres");
            if (genresObj instanceof java.util.List) {
                java.util.List<String> names = new java.util.ArrayList<>();
                for (Object g : (java.util.List<?>) genresObj) {
                    if (g instanceof java.util.Map) {
                        Object name = ((java.util.Map<?, ?>) g).get("name");
                        if (name != null) names.add(name.toString());
                    }
                }
                dto.setGenres(names);
            }
        }
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
        dto.setLikeCount(likeCount);
        dto.setAverageLetterboxdRating(averageLetterboxdRating);

        if (tmdbMovie instanceof java.util.Map) {
            Object creditsObj = ((java.util.Map<?, ?>) tmdbMovie).get("credits");
            if (creditsObj instanceof java.util.Map) {
                Object castObj = ((java.util.Map<?, ?>) creditsObj).get("cast");
                if (castObj instanceof java.util.List) {
                    java.util.List<String> castNames = new java.util.ArrayList<>();
                    int count = 0;
                    for (Object c : (java.util.List<?>) castObj) {
                        if (c instanceof java.util.Map) {
                            Object name = ((java.util.Map<?, ?>) c).get("name");
                            if (name != null) {
                                castNames.add(name.toString());
                                if (++count >= 8) break; 
                            }
                        }
                    }
                    dto.setCast(castNames);
                    dto.setMainCast(castNames.size() > 4 ? castNames.subList(0,4) : castNames);
                }

                Object crewObj = ((java.util.Map<?, ?>) creditsObj).get("crew");
                if (crewObj instanceof java.util.List) {
                    java.util.List<String> crewNames = new java.util.ArrayList<>();
                    java.util.List<String> directors = new java.util.ArrayList<>();
                    for (Object c : (java.util.List<?>) crewObj) {
                        if (c instanceof java.util.Map) {
                            Object job = ((java.util.Map<?, ?>) c).get("job");
                            Object name = ((java.util.Map<?, ?>) c).get("name");
                            if (name != null) crewNames.add(name.toString());
                            if (job != null && "Director".equals(job.toString())) {
                                directors.add(name.toString());
                            }
                        }
                    }
                    dto.setCrew(crewNames);
                    dto.setDirectors(directors);
                }
            }
        }

        if (getString(tmdbMovie, "release_date") != null) {
            dto.setReleaseType("Theatrical");
        }
        String lang = getString(tmdbMovie, "original_language");
        if (lang != null) {
            dto.setLanguage(lang);
        }

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