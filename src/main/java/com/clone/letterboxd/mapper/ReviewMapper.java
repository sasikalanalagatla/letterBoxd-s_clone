package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.ReviewDisplayDto;
import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.model.Review;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {UserMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ReviewMapper {

    @Mapping(target = "bodyExcerpt", expression = "java(createExcerpt(review.getBody()))")
    @Mapping(target = "movieTitle", ignore = true)
    @Mapping(target = "moviePosterPath", ignore = true)
    ReviewDisplayDto toDisplayDto(Review review);

    List<ReviewDisplayDto> toDisplayDtos(List<Review> reviews);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Review toEntity(ReviewFormDto dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(ReviewFormDto dto, @MappingTarget Review entity);

    default String createExcerpt(String body) {
        if (body == null) return "";
        int max = 220;
        return body.length() > max ? body.substring(0, max) + "..." : body;
    }
}