package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.MovieDetailDto;
import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.mapper.MovieMapper;
import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.DiaryEntryRepository;
import com.clone.letterboxd.service.LikeService;
import com.clone.letterboxd.service.ReviewService;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.Optional;

@Controller
public class MovieController {
    private static final Logger log = LoggerFactory.getLogger(MovieController.class);

    private final TmdbService tmdbService;
    private final MovieMapper movieMapper;
    private final DiaryEntryRepository diaryEntryRepository;
    private final ReviewService reviewService;
    private final LikeService likeService;

    public MovieController(TmdbService tmdbService,
                           MovieMapper movieMapper,
                           DiaryEntryRepository diaryEntryRepository,
                           ReviewService reviewService,
                           LikeService likeService) {
        this.tmdbService = tmdbService;
        this.movieMapper = movieMapper;
        this.diaryEntryRepository = diaryEntryRepository;
        this.reviewService = reviewService;
        this.likeService = likeService;
    }

    @GetMapping("/movies/{id}")
    public String details(@PathVariable Long id, Model model, HttpSession session) {
        log.info("Entering MovieController.details for movie {}", id);
        try {
            Map<String, Object> movieData = tmdbService.getMovieDetails(id);
            if (movieData == null) {
                model.addAttribute("error", "Movie not found");
                return "movie-detail";
            }

            User currentUser = (User) session.getAttribute("loggedInUser");
            DiaryEntry userDiary = null;
            boolean inWatchlist = false;

            if (currentUser != null) {
                Optional<DiaryEntry> opt = diaryEntryRepository.findByUserAndMovieId(currentUser, id);
                if (opt.isPresent()) {
                    userDiary = opt.get();
                }
            }

            Long diaryCount = diaryEntryRepository.countByMovieId(id);
            Long reviewCount = reviewService.countReviewsByMovieId(id);
            Long likeCount = likeService.countDirectMovieLikes(id);
            Double avgRating = diaryEntryRepository.averageRatingByMovieId(id);

            MovieDetailDto dto = MovieMapper.toMovieDetailDto(
                    movieData,
                    userDiary,
                    inWatchlist,
                    diaryCount,
                    reviewCount,
                    likeCount,
                    avgRating
            );
            model.addAttribute("movie", dto);

            // always include reviews list (it may be empty) so fragment can safely access it
            model.addAttribute("reviews",
                    reviewService.getDisplayDtosForMovie(id, currentUser));

            // prepare a blank review form for logged‑in users (template guards visibility)
            com.clone.letterboxd.dto.ReviewFormDto form = new com.clone.letterboxd.dto.ReviewFormDto();
            form.setMovieId(id);
            model.addAttribute("reviewForm", form);
        } catch (Exception e) {
            log.error("Error loading movie details {}", id, e);
            model.addAttribute("error", "Failed to load movie: " + e.getMessage());
        }
        log.debug("Exiting MovieController.details for movie {}", id);
        return "movie-detail";
    }

    @GetMapping("/movies/{id}/reviews")
    public String reviewsForMovie(@PathVariable Long id, Model model, HttpSession session) {
        log.debug("Fetching movie review fragment for movie {}", id);
        User viewer = (User) session.getAttribute("loggedInUser");
        model.addAttribute("reviews", reviewService.getDisplayDtosForMovie(id, viewer));
        // Add a minimal movie object with just the ID for the template
        java.util.Map<String, Object> movie = new java.util.HashMap<>();
        movie.put("id", id);
        model.addAttribute("movie", movie);
        return "fragments/movie-reviews :: reviewList";
    }

    @PostMapping("/movies/{id}/reviews")
    public String createReview(@PathVariable Long id,
                               @Valid ReviewFormDto reviewForm,
                               org.springframework.validation.BindingResult bindingResult,
                               Model model,
                               HttpSession session,
                               jakarta.servlet.http.HttpServletRequest request) {
        log.info("Saving new review for movie {}", id);
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }

        // ensure movieId is set correctly from path
        reviewForm.setMovieId(id);

        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        if (bindingResult.hasErrors()) {
            if (!ajax) {
                // full-page fallback: just reload movie detail so validation errors can be shown there
                return "redirect:/movies/" + id;
            }
            model.addAttribute("reviews", reviewService.getDisplayDtosForMovie(id, current));
            return "fragments/movie-reviews :: reviewList";
        }

        try {
            reviewService.createReview(reviewForm, current, id);
            if (!ajax) {
                return "redirect:/movies/" + id;
            }
            model.addAttribute("reviews", reviewService.getDisplayDtosForMovie(id, current));
            return "fragments/movie-reviews :: reviewList";
        } catch (Exception e) {
            log.error("Failed to create review for movie {}", id, e);
            if (!ajax) {
                return "redirect:/movies/" + id;
            }
            model.addAttribute("reviews", reviewService.getDisplayDtosForMovie(id, current));
            return "fragments/movie-reviews :: reviewList";
        }
    }
}
