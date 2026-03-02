package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.mapper.MovieMapper;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@Slf4j
public class HomeController {

    private final TmdbService tmdbService;
    private final MovieMapper movieMapper;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;

    public HomeController(TmdbService tmdbService,
                          MovieMapper movieMapper,
                          com.clone.letterboxd.repository.ReviewRepository reviewRepository,
                          com.clone.letterboxd.repository.LikeRepository likeRepository) {
        this.tmdbService = tmdbService;
        this.movieMapper = movieMapper;
        this.reviewRepository = reviewRepository;
        this.likeRepository = likeRepository;
    }


    @GetMapping("/")
    public String home(Model model,
                       HttpSession session,
                       @RequestParam(defaultValue = "1") int page) {

        log.debug("GET / requested, page={}", page);
        User currentUser = (User) session.getAttribute("loggedInUser");
        if (currentUser == null) {
            log.trace("no user in session");
        } else {
            log.debug("currentUser == {}", currentUser.getUsername());
        }

        try {
            Map<String, Object> popularResponse = tmdbService.getPopularMovies(page);

            if (popularResponse == null || !popularResponse.containsKey("results")) {
                model.addAttribute("error", "No movies returned from TMDB");
                return "index";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) popularResponse.get("results");

            List<MovieCardDto> movies = results.stream()
                    .map(movieMapper::toMovieCardDto)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            // enrich with local review/like counts
            for (MovieCardDto m : movies) {
                if (m.getId() != null) {
                    long reviewCount = reviewRepository.countByMovieId(m.getId());
                    long likeCount = likeRepository.countDirectMovieLikes(m.getId());
                    m.setReviewCount(reviewCount);
                    m.setLikeCount(likeCount);
                }
            }

            model.addAttribute("movies", movies);
            model.addAttribute("currentPage", page);

            Number totalPagesNum = (Number) popularResponse.get("total_pages");
            model.addAttribute("totalPages", totalPagesNum != null ? totalPagesNum.intValue() : 1);

            model.addAttribute("currentUser", currentUser);

        } catch (Exception e) {
            log.error("failed to load popular movies", e);
            model.addAttribute("error", "Failed to load movies: " + e.getMessage());
            // make sure template has required pagination attributes even on error
            model.addAttribute("movies", java.util.Collections.emptyList());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", 1);
            model.addAttribute("currentUser", currentUser);
        }

        return "index";
    }
}