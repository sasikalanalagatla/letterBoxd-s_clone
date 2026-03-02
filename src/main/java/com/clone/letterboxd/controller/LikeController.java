package com.clone.letterboxd.controller;

import com.clone.letterboxd.model.Like;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Optional;

@Controller
public class LikeController {
    private final LikeRepository likeRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public LikeController(LikeRepository likeRepository, ReviewRepository reviewRepository, UserRepository userRepository) {
        this.likeRepository = likeRepository;
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/movies/{movieId}/like")
    public String toggleMovieLike(@PathVariable Long movieId, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
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
    public String toggleReviewLike(@PathVariable Long reviewId, HttpSession session) {
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
        return "redirect:/movies/" + review.getMovieId();
    }
}
