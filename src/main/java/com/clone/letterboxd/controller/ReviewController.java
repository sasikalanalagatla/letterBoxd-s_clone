package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.model.Like;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ReviewController {
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final LikeRepository likeRepository;

    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository, LikeRepository likeRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.likeRepository = likeRepository;
    }

    @GetMapping("/reviews/{id}/edit")
    public String editReview(@PathVariable Long id, Model model, HttpSession session) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            return "redirect:/";
        }
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }
        boolean isOwner = current.getId().equals(review.getUser().getId());
        boolean isAdmin = Boolean.TRUE.equals(current.getIsAdmin());
        if (!isOwner && !isAdmin) {
            return "redirect:/";
        }

        ReviewFormDto form = new ReviewFormDto();
        form.setId(review.getId());
        form.setMovieId(review.getMovieId());
        form.setBody(review.getBody());
        model.addAttribute("reviewForm", form);
        return "review-edit";
    }

    @PostMapping("/reviews/{id}/edit")
    public String updateReview(@PathVariable Long id,
                               @Valid ReviewFormDto reviewForm,
                               BindingResult bindingResult,
                               Model model,
                               HttpSession session) {
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            return "redirect:/";
        }
        boolean isOwner = current.getId().equals(review.getUser().getId());
        boolean isAdmin = Boolean.TRUE.equals(current.getIsAdmin());
        if (!isOwner && !isAdmin) {
            return "redirect:/";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("reviewForm", reviewForm);
            return "review-edit";
        }

        ReviewMapper.updateFromDto(review, reviewForm);
        reviewRepository.save(review);
        return "redirect:/profile/" + review.getUser().getUsername();
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id, HttpSession session) {
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            return "redirect:/";
        }
        boolean isOwner = current.getId().equals(review.getUser().getId());
        boolean isAdmin = Boolean.TRUE.equals(current.getIsAdmin());
        if (!isOwner && !isAdmin) {
            return "redirect:/";
        }
        String owner = review.getUser().getUsername();
        reviewRepository.delete(review);
        return "redirect:/profile/" + owner;
    }

    @PostMapping("/reviews/{id}/like")
    public String toggleReviewLike(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/auth/login";
        }
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            return "redirect:/";
        }
        if (likeRepository.existsByReviewIdAndUserId(id, userId)) {
            var existing = likeRepository.findByReviewIdAndUserId(id, userId);
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
