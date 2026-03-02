package com.clone.letterboxd.service;

import com.clone.letterboxd.dto.ReviewFormDto;
import com.clone.letterboxd.dto.ReviewDisplayDto;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReviewService {
    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final LikeService likeService;

    public ReviewService(ReviewRepository reviewRepository, LikeService likeService) {
        this.reviewRepository = reviewRepository;
        this.likeService = likeService;
    }

    public Review createReview(ReviewFormDto formDto, User user, Long movieId) {
        log.info("Creating review for movie {} by user {}", movieId, user.getId());
        formDto.setMovieId(movieId);
        Review review = ReviewMapper.toEntity(formDto, user);
        review.setIsDraft(false);
        review.setPublishedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    public Optional<Review> getReviewById(Long id) {
        log.debug("Retrieving review {}", id);
        return reviewRepository.findById(id);
    }

    public List<Review> getReviewsByMovieId(Long movieId) {
        log.trace("Getting reviews for movie {}", movieId);
        return reviewRepository.findByMovieId(movieId);
    }

    /**
     * Convenience for controllers: return display DTOs enriched with author info,
     * like counts, and whether the current user has liked each review.
     */
    public List<ReviewDisplayDto> getDisplayDtosForMovie(Long movieId, User currentUser) {
        log.debug("Fetching display DTOs for movie {} (user {})", movieId,
                  currentUser != null ? currentUser.getId() : null);
        return reviewRepository.findByMovieId(movieId).stream()
                .map(r -> {
                    ReviewDisplayDto rd = ReviewMapper.toDisplayDto(r);
                    rd.setAuthor(UserMapper.toSummaryDto(r.getUser()));
                    rd.setLikeCount((int) likeService.countByReviewId(r.getId()));
                    rd.setCommentCount(0);
                    // always initialize flag to avoid nulls in templates
                    rd.setCurrentUserLiked(false);
                    if (currentUser != null) {
                        rd.setCurrentUserLiked(
                                likeService.existsByReviewAndUser(r.getId(), currentUser.getId()));
                    }
                    return rd;
                })
                .collect(Collectors.toList());
    }

    /**
     * Return the top n reviews by like count for a movie.
     */
    public List<ReviewDisplayDto> getTopReviewsForMovie(Long movieId, User currentUser, int limit) {
        return getDisplayDtosForMovie(movieId, currentUser).stream()
                .sorted((a, b) -> Integer.compare(b.getLikeCount(), a.getLikeCount()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<Review> getReviewsByUserId(Long userId) {
        log.trace("Getting reviews for user {}", userId);
        User lookup = new User();
        lookup.setId(userId);
        return reviewRepository.findByUser(lookup);
    }

    public void updateReview(Review review, ReviewFormDto formDto) {
        log.info("Updating review {}", review.getId());
        ReviewMapper.updateFromDto(review, formDto);
        reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Review review) {
        log.info("Deleting review {}", review.getId());
        likeService.deleteByReviewId(review.getId());
        reviewRepository.delete(review);
    }

    public Long countReviewsByMovieId(Long movieId) {
        return reviewRepository.countByMovieId(movieId);
    }
}
