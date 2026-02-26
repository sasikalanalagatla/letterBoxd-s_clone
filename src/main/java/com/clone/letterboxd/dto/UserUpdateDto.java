package com.clone.letterboxd.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateDto {

    @Size(max = 50, message = "Display name too long")
    private String displayName;

    @Size(max = 500, message = "Bio is too long")
    private String bio;

    private String avatarUrl;           // URL or we'll handle upload separately

    private String location;

    // Password change would usually be a separate endpoint/form
}