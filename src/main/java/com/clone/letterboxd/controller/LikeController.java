package com.clone.letterboxd.controller;

import com.clone.letterboxd.model.Like;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class LikeController {
    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final TmdbService tmdbService;

    public LikeController(LikeRepository likeRepository, ReviewRepository reviewRepository, UserRepository userRepository, TmdbService tmdbService) {
        this.likeRepository = likeRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.tmdbService = tmdbService;
    }

    @PostMapping("/movies/{movieId}/like")
    @Transactional
    public String toggleMovieLike(@PathVariable Long movieId, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        // don't allow liking unreleased movie
        if (!tmdbService.isMovieReleased(movieId)) {
            return "redirect:/movies/" + movieId;
        }
        if (likeRepository.existsByMovieIdAndUserId(movieId, userId)) {
            likeRepository.deleteByMovieIdAndUserId(movieId, userId);
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setMovieId(movieId);
            likeRepository.save(like);
        }
        return "redirect:/movies/" + movieId;
    }

    @PostMapping("/reviews/{reviewId}/like")
    @Transactional
    public String toggleReviewLike(@PathVariable Long reviewId, HttpSession session,
                                   @RequestParam(required = false) String redirectTo) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return "redirect:/";
        }
        if (likeRepository.existsByReviewIdAndUserId(reviewId, userId)) {
            Optional<Like> existing = likeRepository.findByReviewIdAndUserId(reviewId, userId);
            if (existing.isPresent()) {
                likeRepository.delete(existing.get());
            }
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setReview(review);
            likeRepository.save(like);
        }
        
        // Redirect to specified location or default to movie page
        if (redirectTo != null && !redirectTo.isEmpty()) {
            return "redirect:" + redirectTo;
        }
        return "redirect:/movies/" + review.getMovieId();
    }

    @PostMapping("/featured/{slug}/like")
    @Transactional
    public String toggleFeaturedListLike(@PathVariable String slug, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        // use slug field instead of fake numeric id
        if (likeRepository.existsByFeaturedListSlugAndUserId(slug, userId)) {
            likeRepository.deleteByFeaturedListSlugAndUserId(slug, userId);
        } else {
            Like like = new Like();
            like.setUser(user);
            like.setFeaturedListSlug(slug);
            likeRepository.save(like);
        }
        
        return "redirect:/lists/featured/" + slug;
    }
}
