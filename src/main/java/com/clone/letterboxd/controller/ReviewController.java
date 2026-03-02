package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.service.ReviewService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReviewController {
    private static final Logger log = LoggerFactory.getLogger(ReviewController.class);

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final ReviewService reviewService;

    public ReviewController(ReviewRepository reviewRepository, UserRepository userRepository, ReviewService reviewService) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews/{id}/edit")
    public String editReview(@PathVariable Long id, Model model, HttpSession session) {
        log.info("Request to edit review {}", id);
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            log.warn("Review {} not found", id);
            return "redirect:/";
        }
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }
        boolean isOwner = current.getId().equals(review.getUser().getId());
        boolean isAdmin = Boolean.TRUE.equals(current.getIsAdmin());
        if (!isOwner && !isAdmin) {
            log.warn("User {} unauthorized to edit review {}", current.getId(), id);
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
        log.info("Updating review {}", id);
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            log.warn("Review {} not found during update", id);
            return "redirect:/";
        }
        boolean isOwner = current.getId().equals(review.getUser().getId());
        boolean isAdmin = Boolean.TRUE.equals(current.getIsAdmin());
        if (!isOwner && !isAdmin) {
            log.warn("User {} unauthorized to update review {}", current.getId(), id);
            return "redirect:/";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("reviewForm", reviewForm);
            return "review-edit";
        }

        reviewService.updateReview(review, reviewForm);
        return "redirect:/profile/" + review.getUser().getUsername();
    }

    @PostMapping("/reviews/{id}/delete")
    public String deleteReview(@PathVariable Long id,
                               @RequestParam(required = false) Long movieId,
                               HttpSession session) {
        log.info("Deleting review {}", id);
        User current = (User) session.getAttribute("loggedInUser");
        if (current == null) {
            return "redirect:/auth/login";
        }
        Review review = reviewRepository.findById(id).orElse(null);
        if (review == null) {
            log.warn("Review {} not found for deletion", id);
            return "redirect:/";
        }
        boolean isOwner = current.getId().equals(review.getUser().getId());
        boolean isAdmin = Boolean.TRUE.equals(current.getIsAdmin());
        if (!isOwner && !isAdmin) {
            log.warn("User {} unauthorized to delete review {}", current.getId(), id);
            return "redirect:/";
        }
        reviewService.deleteReview(review);
        // if called from a movie page, return to that movie; otherwise go to review owner's profile
        if (movieId != null) {
            return "redirect:/movies/" + movieId;
        }
        String owner = review.getUser().getUsername();
        return "redirect:/profile/" + owner;
    }
}
