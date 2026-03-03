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

import java.util.Collections;
import java.util.Comparator;
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
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(value = "year",      required = false, defaultValue = "") String filterYear,
                       @RequestParam(value = "genre",     required = false, defaultValue = "") String filterGenre,
                       @RequestParam(value = "lang",      required = false, defaultValue = "") String filterLang,
                       @RequestParam(value = "minRating", required = false, defaultValue = "") String filterMinRating,
                       @RequestParam(value = "sort",      required = false, defaultValue = "default") String sort) {

        log.debug("GET / requested, page={}", page);
        User currentUser = (User) session.getAttribute("loggedInUser");

        // pass filter values back to the view
        model.addAttribute("filterYear",      filterYear);
        model.addAttribute("filterGenre",     filterGenre);
        model.addAttribute("filterLang",      filterLang);
        model.addAttribute("filterMinRating", filterMinRating);
        model.addAttribute("sort",            sort);
        model.addAttribute("currentPage",     page);
        model.addAttribute("currentUser",     currentUser);

        try {
            Map<String, Object> popularResponse = tmdbService.getPopularMovies(page);

            if (popularResponse == null || !popularResponse.containsKey("results")) {
                model.addAttribute("error", "No movies returned from TMDB");
                model.addAttribute("movies", Collections.emptyList());
                model.addAttribute("allYears",  Collections.emptyList());
                model.addAttribute("allGenres", Collections.emptyList());
                model.addAttribute("allLangs",  Collections.emptyList());
                model.addAttribute("totalPages", 1);
                return "index";
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) popularResponse.get("results");

            // Map all movies first (needed to build full dropdown lists)
            List<MovieCardDto> allMovies = results.stream()
                    .map(movieMapper::toMovieCardDto)
                    .filter(dto -> dto != null)
                    .collect(Collectors.toList());

            // Enrich with local review/like counts
            for (MovieCardDto m : allMovies) {
                if (m.getId() != null) {
                    long reviewCount = reviewRepository.countByMovieId(m.getId());
                    long likeCount   = likeRepository.countDirectMovieLikes(m.getId());
                    m.setReviewCount(reviewCount);
                    m.setLikeCount(likeCount);
                }
            }

            // Build dropdown option sets from the full page
            List<String> allYears = allMovies.stream()
                    .map(MovieCardDto::getYear)
                    .filter(y -> y != null && !y.isBlank())
                    .distinct().sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());

            List<String> allGenres = allMovies.stream()
                    .filter(f -> f.getGenreNames() != null)
                    .flatMap(f -> f.getGenreNames().stream())
                    .filter(g -> g != null && !g.isBlank())
                    .distinct().sorted()
                    .collect(Collectors.toList());

            List<String> allLangs = allMovies.stream()
                    .map(MovieCardDto::getOriginalLanguage)
                    .filter(l -> l != null && !l.isBlank())
                    .distinct().sorted()
                    .collect(Collectors.toList());

            model.addAttribute("allYears",  allYears);
            model.addAttribute("allGenres", allGenres);
            model.addAttribute("allLangs",  allLangs);

            // Apply filters
            double minRating = filterMinRating.isBlank() ? 0.0 : Double.parseDouble(filterMinRating);

            List<MovieCardDto> movies = allMovies.stream()
                    .filter(f -> filterYear.isBlank()  || filterYear.equals(f.getYear()))
                    .filter(f -> filterGenre.isBlank() || (f.getGenreNames() != null && f.getGenreNames().contains(filterGenre)))
                    .filter(f -> filterLang.isBlank()  || filterLang.equals(f.getOriginalLanguage()))
                    .filter(f -> minRating <= 0        || (f.getVoteAverage() != null && f.getVoteAverage() >= minRating))
                    .collect(Collectors.toList());

            // Apply sort
            Comparator<MovieCardDto> comparator = null;
            switch (sort) {
                case "az":          comparator = Comparator.comparing(f -> f.getTitle() != null ? f.getTitle() : ""); break;
                case "za":          comparator = Comparator.comparing((MovieCardDto f) -> f.getTitle() != null ? f.getTitle() : "").reversed(); break;
                case "year_asc":    comparator = Comparator.comparing(f -> f.getYear() != null ? f.getYear() : ""); break;
                case "year_desc":   comparator = Comparator.comparing((MovieCardDto f) -> f.getYear() != null ? f.getYear() : "").reversed(); break;
                case "rating_asc":  comparator = Comparator.comparing(f -> f.getVoteAverage() != null ? f.getVoteAverage() : 0.0); break;
                case "rating_desc": comparator = Comparator.comparing((MovieCardDto f) -> f.getVoteAverage() != null ? f.getVoteAverage() : 0.0).reversed(); break;
            }
            if (comparator != null) movies.sort(comparator);

            model.addAttribute("movies", movies);

            Number totalPagesNum = (Number) popularResponse.get("total_pages");
            model.addAttribute("totalPages", totalPagesNum != null ? totalPagesNum.intValue() : 1);

        } catch (Exception e) {
            log.error("failed to load popular movies", e);
            model.addAttribute("error", "Failed to load movies: " + e.getMessage());
            model.addAttribute("movies",    Collections.emptyList());
            model.addAttribute("allYears",  Collections.emptyList());
            model.addAttribute("allGenres", Collections.emptyList());
            model.addAttribute("allLangs",  Collections.emptyList());
            model.addAttribute("totalPages", 1);
        }

        return "index";
    }
}