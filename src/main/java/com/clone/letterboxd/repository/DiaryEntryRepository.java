package com.clone.letterboxd.repository;

import com.clone.letterboxd.model.DiaryEntry;
import com.clone.letterboxd.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DiaryEntryRepository extends JpaRepository<DiaryEntry, Long> {
    Optional<DiaryEntry> findByUserAndMovieId(User user, Long movieId);
    long countByMovieId(Long movieId);

    @Query("select avg(d.rating) from DiaryEntry d where d.movieId = :movieId")
    Double averageRatingByMovieId(@Param("movieId") Long movieId);
}
