package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.dto.MovieCardDto;
import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.mapper.FilmListMapper;
import com.clone.letterboxd.mapper.MovieMapper;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.FilmListEntry;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final TmdbService tmdbService;
    private final MovieMapper movieMapper;
    private final UserRepository userRepository;
    private final FilmListRepository filmListRepository;
    private final FilmListMapper filmListMapper;

    public SearchController(TmdbService tmdbService,
                            MovieMapper movieMapper,
                            UserRepository userRepository,
                            FilmListRepository filmListRepository,
                            FilmListMapper filmListMapper) {
        this.tmdbService = tmdbService;
        this.movieMapper = movieMapper;
        this.userRepository = userRepository;
        this.filmListRepository = filmListRepository;
        this.filmListMapper = filmListMapper;
    }

    @GetMapping("/search")
    public String search(@RequestParam(value = "q", required = false, defaultValue = "") String query,
                         Model model,
                         HttpSession session) {

        String q = query.trim();
        model.addAttribute("query", q);

        if (q.isEmpty()) {
            model.addAttribute("films", Collections.emptyList());
            model.addAttribute("profiles", Collections.emptyList());
            model.addAttribute("lists", Collections.emptyList());
            return "search-results";
        }

        List<MovieCardDto> films = Collections.emptyList();
        try {
            Map<String, Object> response = tmdbService.searchMovies(q, 1);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                films = results.stream()
                        .limit(18)
                        .map(movieMapper::toMovieCardDto)
                        .filter(dto -> dto != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Film search failed for query={}", q, e);
        }
        model.addAttribute("films", films);

        List<UserSummaryDto> profiles = Collections.emptyList();
        try {
            List<User> users = userRepository
                    .findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(q, q);
            profiles = users.stream()
                    .map(UserMapper::toSummaryDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Profile search failed for query={}", q, e);
        }
        model.addAttribute("profiles", profiles);

        List<FilmListSummaryDto> lists = Collections.emptyList();
        try {
            List<FilmList> byName = filmListRepository.findByNameContainingIgnoreCase(q);

            List<Long> matchedMovieIds = new ArrayList<>();
            try {
                Map<String, Object> response = tmdbService.searchMovies(q, 1);
                if (response != null && response.containsKey("results")) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                    for (Map<String, Object> res : results) {
                        Object idObj = res.get("id");
                        if (idObj instanceof Number) {
                            matchedMovieIds.add(((Number) idObj).longValue());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Could not fetch TMDB IDs for list-by-film search, query={}", q, e);
            }

            List<FilmList> byFilm = matchedMovieIds.isEmpty()
                    ? Collections.emptyList()
                    : filmListRepository.findByMovieIds(matchedMovieIds);

            Map<Long, FilmList> merged = new java.util.LinkedHashMap<>();
            for (FilmList fl : byName)  merged.put(fl.getId(), fl);
            for (FilmList fl : byFilm)  merged.putIfAbsent(fl.getId(), fl);

            lists = merged.values().stream()
                    .map(this::toSummaryDto)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("List search failed for query={}", q, e);
        }
        model.addAttribute("lists", lists);

        return "search-results";
    }

    private FilmListSummaryDto toSummaryDto(FilmList list) {
        FilmListSummaryDto dto = filmListMapper.toSummaryDto(list);

        if (list.getUser() != null) {
            UserSummaryDto owner = new UserSummaryDto();
            owner.setId(list.getUser().getId());
            owner.setUsername(list.getUser().getUsername());
            owner.setDisplayName(list.getUser().getDisplayName());
            owner.setAvatarUrl(list.getUser().getAvatarUrl());
            dto.setOwner(owner);
        }

        if (list.getEntries() != null && !list.getEntries().isEmpty()) {
            List<String> posters = new ArrayList<>();
            for (FilmListEntry entry : list.getEntries()) {
                if (posters.size() >= 4) break;
                try {
                    Map<String, Object> movieData = tmdbService.getMovieDetails(entry.getMovieId());
                    if (movieData != null) {
                        String poster = (String) movieData.get("poster_path");
                        if (poster != null) posters.add(poster);
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch TMDB preview poster for movie {}", entry.getMovieId(), e);
                }
            }
            dto.setPreviewPosterPaths(posters);
        }

        return dto;
    }
}
