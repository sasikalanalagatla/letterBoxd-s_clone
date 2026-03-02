package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.MovieDetailDto;
import com.clone.letterboxd.dto.ReviewDisplayDto;
import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.mapper.MovieMapper;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.DiaryEntryRepository;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.service.TmdbService;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.Like;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;
import java.util.List;
import java.util.Optional;

@Controller
public class MovieController {
    private static final Logger log = LoggerFactory.getLogger(MovieController.class);

    private final TmdbService tmdbService;
    private final MovieMapper movieMapper;
    private final DiaryEntryRepository diaryEntryRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;
    private final UserRepository userRepository;

    public MovieController(TmdbService tmdbService,
                           MovieMapper movieMapper,
                           DiaryEntryRepository diaryEntryRepository,
                           ReviewRepository reviewRepository,
                           LikeRepository likeRepository,
                           UserRepository userRepository) {
        this.tmdbService = tmdbService;
        this.movieMapper = movieMapper;
        this.diaryEntryRepository = diaryEntryRepository;
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/movies/{id}")
    public String details(@PathVariable Long id, Model model, HttpSession session) {
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
                // TODO: check watchlist membership when watchlist feature working
            }

            Long diaryCount = diaryEntryRepository.countByMovieId(id);
            Long reviewCount = reviewRepository.countByMovieId(id);
            // movie likes = only DIRECT likes on the movie itself (not review or diary likes)
            Long likeCount = likeRepository.countDirectMovieLikes(id);
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

            // prepare an empty review form for submission
            ReviewFormDto reviewForm = new ReviewFormDto();
            reviewForm.setMovieId(id);
            model.addAttribute("reviewForm", reviewForm);

            // load existing reviews
            var existing = reviewRepository.findByMovieId(id);
            List<ReviewDisplayDto> reviewDtos = existing.stream()
                    .map(r -> {
                        ReviewDisplayDto rd = ReviewMapper.toDisplayDto(r);
                        // attach author info
                        rd.setAuthor(UserMapper.toSummaryDto(r.getUser()));
                        // compute like and comment counts
                        rd.setLikeCount((int) likeRepository.countByReviewId(r.getId()));
                        rd.setCommentCount(0);
                        // if user is logged in, check whether they liked this review
                        if (currentUser != null) {
                            rd.setCurrentUserLiked(likeRepository.existsByReviewIdAndUserId(r.getId(), currentUser.getId()));
                        }
                        return rd;
                    })
                    .collect(java.util.stream.Collectors.toList());
            model.addAttribute("reviews", reviewDtos);
        } catch (Exception e) {
            log.error("Error loading movie details {}", id, e);
            model.addAttribute("error", "Failed to load movie: " + e.getMessage());
        }
        return "movie-detail";
    }

    @PostMapping("/movies/{id}/reviews")
    public String submitReview(@PathVariable("id") Long id,
                               @jakarta.validation.Valid ReviewFormDto reviewForm,
                               org.springframework.validation.BindingResult bindingResult,
                               Model model,
                               HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }

        // if validation failed, log and re-display the detail page with errors
        if (bindingResult.hasErrors()) {
            log.debug("review binding errors: {}", bindingResult.getAllErrors());
            // reuse details logic to populate movie and reviews
            details(id, model, session);
            // keep the submitted form (movieId should already be set via binding)
            model.addAttribute("reviewForm", reviewForm);
            return "movie-detail";
        }

        reviewForm.setMovieId(id);
        log.debug("Submitting review form: {}", reviewForm);
        Review review = ReviewMapper.toEntity(reviewForm, user);
        review.setIsDraft(false);
        review.setPublishedAt(java.time.LocalDateTime.now());
        Review saved = reviewRepository.save(review);
        log.debug("Review saved with id {}", saved.getId());
        return "redirect:/movies/" + id;
    }

    @PostMapping("/movies/{id}/like")
    public String toggleMovieLike(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        if (likeRepository.existsByMovieIdAndUserId(id, userId)) {
            likeRepository.deleteByMovieIdAndUserId(id, userId);
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setMovieId(id);
            likeRepository.save(like);
        }
        return "redirect:/movies/" + id;
    }
}
