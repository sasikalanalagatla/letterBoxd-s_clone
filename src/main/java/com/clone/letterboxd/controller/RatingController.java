package com.clone.letterboxd.controller;

import com.clone.letterboxd.enums.Visibility;
import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.DiaryEntryRepository;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import com.clone.letterboxd.service.TmdbService;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class RatingController {

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;
    private final TmdbService tmdbService;

    public RatingController(DiaryEntryRepository diaryEntryRepository,
                            UserRepository userRepository,
                            TmdbService tmdbService) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.userRepository = userRepository;
        this.tmdbService = tmdbService;
    }

    @PostMapping("/movies/{movieId}/rate")
    @Transactional
    public String rateMovie(@PathVariable Long movieId,
                            @RequestParam Double rating,
                            HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return "redirect:/auth/login";
        }
        User user = userOpt.get();
        // disallow rating before release
        if (!tmdbService.isMovieReleased(movieId)) {
            return "redirect:/movies/" + movieId;
        }

        DiaryEntry entry = diaryEntryRepository
                .findByUserAndMovieId(user, movieId)
                .orElseGet(() -> {
                    DiaryEntry d = new DiaryEntry();
                    d.setUser(user);
                    d.setMovieId(movieId);
                    d.setLiked(false);
                    d.setVisibility(Visibility.PUBLIC);
                    // we do not set watch date here; the user can update later
                    d.setCreatedAt(LocalDateTime.now());
                    return d;
                });

        entry.setRating(rating);
        entry.setUpdatedAt(LocalDateTime.now());
        diaryEntryRepository.save(entry);

        return "redirect:/movies/" + movieId;
    }
}
