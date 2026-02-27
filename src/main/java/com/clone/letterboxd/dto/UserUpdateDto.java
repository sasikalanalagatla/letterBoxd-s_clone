package com.clone.letterboxd.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDto {

    @Size(max = 50, message = "Display name too long")
    private String displayName;
    @Size(max = 500, message = "Bio is too long")
    private String bio;
    private String avatarUrl;
    private String location;
}