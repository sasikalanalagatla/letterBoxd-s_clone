package com.clone.letterboxd.repository;

import com.clone.letterboxd.enums.Visibility;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FilmListRepository extends JpaRepository<FilmList, Long> {
    List<FilmList> findByUser(User user);
    List<FilmList> findByUserAndVisibility(User user, Visibility visibility);
    List<FilmList> findByUserAndVisibilityIn(User user, Collection<Visibility> visibilities);
    List<FilmList> findByVisibilityOrderByCreatedAtDesc(Visibility visibility);

    @Query("SELECT fl FROM FilmList fl WHERE fl.visibility = 'PUBLIC' ORDER BY SIZE(fl.entries) DESC")
    Page<FilmList> findMostPopularPublicLists(Pageable pageable);

    @Query("SELECT fl FROM FilmList fl ORDER BY SIZE(fl.entries) DESC")
    Page<FilmList> findMostPopularLists(Pageable pageable);
    
    List<FilmList> findByNameContainingIgnoreCase(String name);
    List<FilmList> findByNameContainingIgnoreCaseAndVisibility(String name, Visibility visibility);

    @Query("SELECT DISTINCT fl FROM FilmList fl JOIN fl.entries e WHERE e.movieId IN :movieIds")
    List<FilmList> findByMovieIds(@Param("movieIds") List<Long> movieIds);

    long countByUser(User user);

    @Query("SELECT DISTINCT e.movieId FROM FilmList fl JOIN fl.entries e")
    List<Long> findAllMovieIdsInLists();
}
