package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.DiaryEntryDisplayDto;
import com.clone.letterboxd.dto.DiaryEntryFormDto;
import com.clone.letterboxd.model.DiaryEntry;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class, MovieMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DiaryMapper {

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    DiaryEntry toEntity(DiaryEntryFormDto dto);

    @Mapping(target = "ratingDisplay", expression = "java(formatRating(entry.getRating()))")
    @Mapping(target = "movieTitle", ignore = true)        // set from TMDB
    @Mapping(target = "moviePosterPath", ignore = true)   // set from TMDB
    @Mapping(target = "movieYear", ignore = true)         // set from TMDB
    DiaryEntryDisplayDto toDisplayDto(DiaryEntry entry);

    List<DiaryEntryDisplayDto> toDisplayDtos(List<DiaryEntry> entries);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromForm(DiaryEntryFormDto dto, @MappingTarget DiaryEntry entity);

    // Helpers
    default String formatRating(Double rating) {
        if (rating == null || rating <= 0) return "";
        int full = rating.intValue();
        boolean half = rating - full >= 0.5;
        return "★".repeat(full) + (half ? "½" : "");
    }
}
