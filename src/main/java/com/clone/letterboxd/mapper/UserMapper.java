package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.UserProfileDto;
import com.clone.letterboxd.dto.UserRegistrationDto;
import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.dto.UserUpdateDto;
import com.clone.letterboxd.model.User;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "following", ignore = true)
    @Mapping(target = "followers", ignore = true)
    User toEntity(UserRegistrationDto dto);

    UserSummaryDto toSummaryDto(User user);

    List<UserSummaryDto> toSummaryDtoList(List<User> users);

    @Mapping(target = "email", conditionExpression = "java(isOwnProfile ? user.getEmail() : null)")
    UserProfileDto toProfileDto(User user, @Context boolean isOwnProfile);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(UserUpdateDto dto, @MappingTarget User entity);
}