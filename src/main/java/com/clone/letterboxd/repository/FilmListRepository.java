package com.clone.letterboxd.repository;

import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FilmListRepository extends JpaRepository<FilmList, Long> {
    List<FilmList> findByUser(User user);

    @Query("SELECT fl FROM FilmList fl ORDER BY SIZE(fl.entries) DESC")
    Page<FilmList> findMostPopularLists(Pageable pageable);
    
    List<FilmList> findByNameContainingIgnoreCase(String name);
}
