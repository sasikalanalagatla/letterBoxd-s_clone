package com.clone.letterboxd.repository;

import com.clone.letterboxd.model.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {
    @Query("select count(l) from Like l where l.review.movieId = :movieId")
    long countByReviewMovieId(@Param("movieId") Long movieId);

    @Query("select count(l) from Like l where l.diaryEntry.movieId = :movieId")
    long countByDiaryMovieId(@Param("movieId") Long movieId);

    // count DIRECT likes on a movie (not through reviews or diary)
    @Query("select count(l) from Like l where l.movieId = :movieId and l.review is null and l.diaryEntry is null")
    long countDirectMovieLikes(@Param("movieId") Long movieId);

    // count likes belonging to a specific review (used when rendering review lists)
    @Query("select count(l) from Like l where l.review.id = :reviewId")
    long countByReviewId(@Param("reviewId") Long reviewId);

    // check if a particular user has liked a review
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    // movie direct likes
    long countByMovieId(Long movieId);
    boolean existsByMovieIdAndUserId(Long movieId, Long userId);
    void deleteByMovieIdAndUserId(Long movieId, Long userId);

    // find review like by user and review
    java.util.Optional<Like> findByReviewIdAndUserId(Long reviewId, Long userId);
}
