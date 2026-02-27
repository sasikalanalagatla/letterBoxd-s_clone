package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserProfileDto;
import com.clone.letterboxd.dto.UserUpdateDto;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public ProfileController(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @GetMapping
    public String myProfile(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        User user = userOptional.get();
        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setIsOwnProfile(true);
        
        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", true);
        
        return "profile";
    }

    @GetMapping("/{username}")
    public String viewProfile(
            @PathVariable String username,
            HttpSession session,
            Model model) {
        
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            model.addAttribute("error", "User not found");
            return "error";
        }

        User user = userOptional.get();
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        
        boolean isOwnProfile = loggedInUserId != null && loggedInUserId.equals(user.getId());
        
        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setIsOwnProfile(isOwnProfile);
        
        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", isOwnProfile);
        
        return "profile";
    }

    @GetMapping("/edit")
    public String editProfilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        User user = userOptional.get();
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setDisplayName(user.getDisplayName());
        updateDto.setBio(user.getBio());
        updateDto.setAvatarUrl(user.getAvatarUrl());
        
        model.addAttribute("userUpdate", updateDto);
        model.addAttribute("username", user.getUsername());
        
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            HttpSession session,
            UserUpdateDto userUpdate,
            Model model) {
        
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        try {
            User user = userOptional.get();
            
            userMapper.updateFromDto(user, userUpdate);
            
            userRepository.save(user);
            
            model.addAttribute("success", "Profile updated successfully!");
            return "redirect:/profile";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error updating profile: " + e.getMessage());
            model.addAttribute("userUpdate", userUpdate);
            return "profile-edit";
        }
    }
}
