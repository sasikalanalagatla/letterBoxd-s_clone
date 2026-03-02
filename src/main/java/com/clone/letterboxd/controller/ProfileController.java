package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserProfileDto;
import com.clone.letterboxd.dto.UserUpdateDto;
import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.mapper.FilmListMapper;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.FilmListEntry;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/profile")
@Slf4j
public class ProfileController {

    private final UserRepository userRepository;
    private final FilmListRepository filmListRepository;
    private final ReviewRepository reviewRepository;
    private final UserMapper userMapper;
    private final TmdbService tmdbService;
    private final FilmListMapper filmListMapper;

    public ProfileController(UserRepository userRepository,
                             FilmListRepository filmListRepository,
                             ReviewRepository reviewRepository,
                             UserMapper userMapper,
                             TmdbService tmdbService, FilmListMapper filmListMapper) {
        this.userRepository = userRepository;
        this.filmListRepository = filmListRepository;
        this.reviewRepository = reviewRepository;
        this.userMapper = userMapper;
        this.tmdbService = tmdbService;
        this.filmListMapper = filmListMapper;
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
        // compute dynamic counts
        profile.setListCount(filmListRepository.findByUser(user).size());
        profile.setReviewCount((int) reviewRepository.countByUser(user));

        // include a few list summaries for display on profile
        List<FilmList> lists = filmListRepository.findByUser(user);
        model.addAttribute("profileLists", buildSummaryList(lists));

        // add reviews written by this user
        var userReviews = reviewRepository.findByUser(user);
        List<com.clone.letterboxd.dto.ReviewDisplayDto> reviewDtos = userReviews.stream()
                .map(r -> {
                    var rd = ReviewMapper.toDisplayDto(r);
                    // fetch movie title/poster from TMDB
                    try {
                        Map<String,Object> movieData = tmdbService.getMovieDetails(r.getMovieId());
                        if (movieData != null) {
                            rd.setMovieTitle((String) movieData.get("title"));
                            String poster = (String) movieData.get("poster_path");
                            if (poster != null) rd.setMoviePosterPath(poster);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load TMDB info for movie {}", r.getMovieId(), e);
                    }
                    return rd;
                }).toList();
        model.addAttribute("profileReviews", reviewDtos);

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
        profile.setListCount(filmListRepository.findByUser(user).size());
        profile.setReviewCount((int) reviewRepository.countByUser(user));

        // include a few list summaries
        List<FilmList> lists = filmListRepository.findByUser(user);
        model.addAttribute("profileLists", buildSummaryList(lists));

        // fetch reviews for public view; they will all belong to this profile
        var userReviews = reviewRepository.findByUser(user);
        List<com.clone.letterboxd.dto.ReviewDisplayDto> reviewDtos = userReviews.stream()
                .map(r -> {
                    var rd = ReviewMapper.toDisplayDto(r);
                    try {
                        Map<String,Object> movieData = tmdbService.getMovieDetails(r.getMovieId());
                        if (movieData != null) {
                            rd.setMovieTitle((String) movieData.get("title"));
                            String poster = (String) movieData.get("poster_path");
                            if (poster != null) rd.setMoviePosterPath(poster);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load TMDB info for movie {}", r.getMovieId(), e);
                    }
                    return rd;
                }).toList();
        model.addAttribute("profileReviews", reviewDtos);

        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", isOwnProfile);
        
        return "profile";
    }

    // helper methods copied from ListController for consistency
    private List<FilmListSummaryDto> buildSummaryList(List<FilmList> lists) {
        if (lists == null) return List.of();
        return lists.stream().map(this::toSummaryDto).toList();
    }

    private FilmListSummaryDto toSummaryDto(FilmList list) {
        FilmListSummaryDto dto = filmListMapper.toSummaryDto(list);

        if (list.getUser() != null) {
            UserSummaryDto owner = new UserSummaryDto();
            owner.setId(list.getUser().getId());
            owner.setUsername(list.getUser().getUsername());
            owner.setDisplayName(list.getUser().getDisplayName());
            owner.setAvatarUrl(list.getUser().getAvatarUrl());
            dto.setOwner(owner);
        }

        if (list.getEntries() != null && !list.getEntries().isEmpty()) {
            List<String> posters = new ArrayList<>();
            for (FilmListEntry entry : list.getEntries()) {
                if (posters.size() >= 4) break;
                try {
                    Map<String, Object> movieData = tmdbService.getMovieDetails(entry.getMovieId());
                    if (movieData != null) {
                        String poster = (String) movieData.get("poster_path");
                        if (poster != null) posters.add(poster);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch TMDB preview poster for movie {}", entry.getMovieId(), e);
                }
            }
            dto.setPreviewPosterPaths(posters);
        }

        return dto;
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
