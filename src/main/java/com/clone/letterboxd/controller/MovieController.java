package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.dto.MovieDetailDto;
import com.clone.letterboxd.dto.ReviewDisplayDto;
import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.mapper.MovieMapper;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.Review;
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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
            // try to fetch watch providers (where to watch)
            try {
                Map<String, Object> providers = tmdbService.getWatchProviders(id);
                String watchText = null;
                if (providers != null && providers.containsKey("results")) {
                    Object resultsObj = providers.get("results");
                    if (resultsObj instanceof java.util.Map) {
                        Object us = ((java.util.Map<?, ?>) resultsObj).get("US");
                        if (us instanceof java.util.Map) {
                            java.util.Set<?> keys = ((java.util.Map<?, ?>) us).keySet();
                            java.util.List<String> names = new java.util.ArrayList<>();
                            for (Object k : keys) {
                                Object listObj = ((java.util.Map<?, ?>) us).get(k);
                                if (listObj instanceof java.util.List) {
                                    for (Object item : (java.util.List<?>) listObj) {
                                        if (item instanceof java.util.Map) {
                                            Object pname = ((java.util.Map<?, ?>) item).get("provider_name");
                                            if (pname != null) {
                                                String pn = pname.toString();
                                                if (!names.contains(pn)) {
                                                    names.add(pn);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            if (!names.isEmpty()) {
                                watchText = String.join(", ", names);
                            }
                        }
                    }
                }
                if (watchText == null) {
                    watchText = "Theatrical";
                }
                dto.setWhereToWatch(watchText);
            } catch (Exception e) {
                log.debug("Failed to load providers for {}", id, e);
                if (dto.getWhereToWatch() == null) {
                    dto.setWhereToWatch("Theatrical");
                }
            }

            model.addAttribute("movie", dto);

            List<ReviewDisplayDto> allReviews =
                    reviewService.getDisplayDtosForMovie(id, currentUser);
            model.addAttribute("reviews", allReviews);
            model.addAttribute("allReviewsSize", allReviews.size());
            model.addAttribute("popularReviews",
                    reviewService.getTopReviewsForMovie(id, currentUser, 6));

            com.clone.letterboxd.dto.ReviewFormDto form = new com.clone.letterboxd.dto.ReviewFormDto();
            form.setMovieId(id);
            model.addAttribute("reviewForm", form);

            // fetch related movies
            try {
                Map<String, Object> similar = tmdbService.getSimilarMovies(id);
                if (similar != null && similar.containsKey("results")) {
                    List<MovieCardDto> related = ((List<?>) similar.get("results")).stream()
                            .filter(Objects::nonNull)
                            .map(item -> movieMapper.toMovieCardDto(item))
                            .limit(6)
                            .collect(Collectors.toList());
                    model.addAttribute("relatedMovies", related);
                    Number total = (Number) similar.getOrDefault("total_results", 0);
                    model.addAttribute("relatedTotal", total.intValue());
                }
            } catch (Exception e) {
                log.debug("Failed to load similar movies for {}", id, e);
            }
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
        // return top few reviews instead of full list so detail page can show only popular ones
        model.addAttribute("reviews", reviewService.getTopReviewsForMovie(id, viewer, 6));
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

        reviewForm.setMovieId(id);

        boolean ajax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));

        // only allow reviews once the movie has actually been released
        if (!tmdbService.isMovieReleased(id)) {
            log.info("Attempted review on unreleased movie {} by user {}", id, current.getId());
            if (!ajax) {
                return "redirect:/movies/" + id;
            }
            model.addAttribute("reviews", reviewService.getTopReviewsForMovie(id, current, 6));
            return "fragments/movie-reviews :: reviewList";
        }

        if (bindingResult.hasErrors()) {
            if (!ajax) {
                return "redirect:/movies/" + id;
            }
            model.addAttribute("reviews", reviewService.getTopReviewsForMovie(id, current, 6));
            return "fragments/movie-reviews :: reviewList";
        }

        try {
            Review created = reviewService.createReview(reviewForm, current, id);
            if (!ajax) {
                return "redirect:/movies/" + id;
            }
            List<ReviewDisplayDto> popular = reviewService.getTopReviewsForMovie(id, current, 6);
            ReviewDisplayDto createdDto = ReviewMapper.toDisplayDto(created);
            createdDto.setAuthor(UserMapper.toSummaryDto(current));
            createdDto.setLikeCount(0);
            createdDto.setCommentCount(0);
            createdDto.setCurrentUserLiked(false);
            // remove any existing instance and add at front
            popular.removeIf(r -> r.getId().equals(createdDto.getId()));
            popular.add(0, createdDto);
            if (popular.size() > 6) {
                popular = popular.subList(0, 6);
            }
            model.addAttribute("reviews", popular);
            return "fragments/movie-reviews :: reviewList";
        } catch (Exception e) {
            log.error("Failed to create review for movie {}", id, e);
            if (!ajax) {
                return "redirect:/movies/" + id;
            }
            model.addAttribute("reviews", reviewService.getTopReviewsForMovie(id, current, 6));
            return "fragments/movie-reviews :: reviewList";
        }
    }
}
