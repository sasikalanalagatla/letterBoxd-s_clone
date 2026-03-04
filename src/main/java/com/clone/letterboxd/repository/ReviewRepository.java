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

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT r.movieId FROM Review r")
    java.util.List<Long> findAllMovieIds();

    @org.springframework.data.jpa.repository.Query("SELECT r.movieId, COUNT(r.id) FROM Review r WHERE r.movieId IN :movieIds GROUP BY r.movieId")
    java.util.List<Object[]> countByMovieIdIn(@org.springframework.data.repository.query.Param("movieIds") java.util.Collection<Long> movieIds);
}
