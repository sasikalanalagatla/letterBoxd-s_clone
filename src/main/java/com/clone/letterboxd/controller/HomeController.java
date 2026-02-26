package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.mapper.MovieMapper;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.service.TmdbService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class HomeController {

    private final TmdbService tmdbService;
    private final MovieMapper movieMapper;

    public HomeController(TmdbService tmdbService, MovieMapper movieMapper) {
        this.tmdbService = tmdbService;
        this.movieMapper = movieMapper;
    }

    private User getFakeCurrentUser() {
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setDisplayName("Test User");
        user.setEmail("test@example.com");
        return user;
    }

    @GetMapping("/")
    public String home(Model model,
                       @RequestParam(defaultValue = "1") int page) {

        User currentUser = getFakeCurrentUser();

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

            model.addAttribute("movies", movies);
            model.addAttribute("currentPage", page);

            Number totalPagesNum = (Number) popularResponse.get("total_pages");
            model.addAttribute("totalPages", totalPagesNum != null ? totalPagesNum.intValue() : 1);

            model.addAttribute("currentUser", currentUser);

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load movies: " + e.getMessage());
            // log the real error
            e.printStackTrace();
        }

        return "index";
    }
}