package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.dto.MovieDetailDto;
import com.clone.letterboxd.model.DiaryEntry;
import org.mapstruct.*;

import java.util.Map;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MovieMapper {

    @Mapping(target = "posterUrl", expression = "java(buildPosterUrl(posterPath, \"w500\"))")
    @Mapping(target = "year", expression = "java(extractYear(releaseDate))")
    MovieCardDto toMovieCardDto(Map<String, Object> tmdbMovie);

    @Mapping(target = "posterUrl", expression = "java(buildPosterUrl(posterPath, \"w780\"))")
    @Mapping(target = "backdropUrl", expression = "java(buildPosterUrl(backdropPath, \"original\"))")
    @Mapping(target = "year", expression = "java(extractYear(releaseDate))")
    MovieDetailDto toMovieDetailDto(Map<String, Object> tmdbMovie);

    default String buildPosterUrl(String path, String size) {
        if (path == null || path.isBlank()) return null;
        return "https://image.tmdb.org/t/p/" + size + path;
    }

    default String extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        return releaseDate.substring(0, 4);
    }

    @Mapping(target = "userRating", ignore = true)
    @Mapping(target = "watched", ignore = true)
    @Mapping(target = "inWatchlist", ignore = true)
    void enrichWithUserContext(@MappingTarget MovieDetailDto dto, @Context DiaryEntry diaryEntry);
}