package com.clone.letterboxd.mapper;

import com.clone.letterboxd.dto.UserProfileDto;
import com.clone.letterboxd.dto.UserRegistrationDto;
import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.dto.UserUpdateDto;
import com.clone.letterboxd.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public static UserSummaryDto toSummaryDto(User user) {
        if (user == null) return null;

        UserSummaryDto dto = new UserSummaryDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setAvatarUrl(normalizeAvatarUrl(user.getAvatarUrl()));

        if (user.getBio() != null) {
            String bio = user.getBio();
            dto.setBioExcerpt(bio.length() > 120 ? bio.substring(0, 117) + "..." : bio);
        }

        return dto;
    }

    public static UserProfileDto toProfileDto(User user) {
        if (user == null) return null;

        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setDisplayName(user.getDisplayName());
        dto.setBio(user.getBio());
        dto.setAvatarUrl(normalizeAvatarUrl(user.getAvatarUrl()));
        dto.setJoinedAt(user.getCreatedAt());
        dto.setIsAdmin(user.getIsAdmin());

        return dto;
    }

    public static User toEntity(UserRegistrationDto dto) {
        if (dto == null) return null;

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setDisplayName(dto.getDisplayName());
        user.setBio(dto.getBio());

        return user;
    }

    public static void updateFromDto(User user, UserUpdateDto dto) {
        if (dto == null || user == null) return;

        if (dto.getDisplayName() != null) user.setDisplayName(dto.getDisplayName());
        if (dto.getBio() != null) user.setBio(dto.getBio());
    }

    private static String normalizeAvatarUrl(String url) {
        if (url == null || url.isEmpty()) return null;
        if (url.startsWith("http") || url.startsWith("/")) return url;
        return "/" + url;
    }
}