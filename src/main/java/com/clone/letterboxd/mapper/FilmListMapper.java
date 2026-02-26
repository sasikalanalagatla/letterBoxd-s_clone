package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.FilmListDetailDto;
import com.clone.letterboxd.dto.FilmListEntryDto;
import com.clone.letterboxd.dto.FilmListFormDto;
import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.FilmListEntry;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface FilmListMapper {

    FilmListSummaryDto toSummaryDto(FilmList list);

    @Mapping(target = "entries", source = "entries")
    FilmListDetailDto toDetailDto(FilmList list);

    List<FilmListDetailDto> toDetailDtos(List<FilmList> lists);

    @Mapping(target = "list", ignore = true)
    @Mapping(target = "addedAt", ignore = true)
    FilmListEntryDto toEntryDto(FilmListEntry entry);

    List<FilmListEntryDto> toEntryDtos(List<FilmListEntry> entries);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    FilmList toEntity(FilmListFormDto dto);
}