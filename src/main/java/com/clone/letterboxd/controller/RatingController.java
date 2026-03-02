package com.clone.letterboxd.controller;

import com.clone.letterboxd.enums.Visibility;
import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.DiaryEntryRepository;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class RatingController {

    private final DiaryEntryRepository diaryEntryRepository;
    private final UserRepository userRepository;

    public RatingController(DiaryEntryRepository diaryEntryRepository,
                            UserRepository userRepository) {
        this.diaryEntryRepository = diaryEntryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create or update a diary entry rating for the current user/movie.
     * The form posts a numeric value (0-10 or 1-5) in the <code>rating</code>
     * parameter. A diary entry is either created or updated accordingly.
     *
     * This endpoint behaves exactly like LikeController: it always redirects
     * back to the movie page so the JS handler can decide whether to reload the
     * full page or update a fragment.
     */
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
