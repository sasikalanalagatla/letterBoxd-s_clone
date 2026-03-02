package com.clone.letterboxd.repository;

import com.clone.letterboxd.model.Review;
import com.clone.letterboxd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    long countByMovieId(Long movieId);
    java.util.List<Review> findByMovieId(Long movieId);

    java.util.List<Review> findByUser(User user);

    long countByUser(User user);
}
