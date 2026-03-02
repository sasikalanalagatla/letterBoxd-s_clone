package com.clone.letterboxd.service;

import com.clone.letterboxd.repository.LikeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LikeService {
    private static final Logger log = LoggerFactory.getLogger(LikeService.class);

    private final LikeRepository likeRepository;

    public LikeService(LikeRepository likeRepository) {
        this.likeRepository = likeRepository;
    }

    public long countDirectMovieLikes(Long movieId) {
        log.debug("Counting direct likes for movie {}", movieId);
        return likeRepository.countDirectMovieLikes(movieId);
    }

    public long countByReviewId(Long reviewId) {
        log.debug("Counting likes for review {}", reviewId);
        return likeRepository.countByReviewId(reviewId);
    }

    public boolean existsByReviewAndUser(Long reviewId, Long userId) {
        log.trace("Checking if user {} liked review {}", userId, reviewId);
        return likeRepository.existsByReviewIdAndUserId(reviewId, userId);
    }

    public long countByMovieId(Long movieId) {
        log.debug("Counting likes for movie {} (any)", movieId);
        return likeRepository.countByMovieId(movieId);
    }

    public boolean existsByMovieAndUser(Long movieId, Long userId) {
        log.trace("Checking if user {} liked movie {}", userId, movieId);
        return likeRepository.existsByMovieIdAndUserId(movieId, userId);
    }

    public void deleteByMovieAndUser(Long movieId, Long userId) {
        log.info("Deleting movie-like entry for user {} movie {}", userId, movieId);
        likeRepository.deleteByMovieIdAndUserId(movieId, userId);
    }
}
