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
import java.util.Comparator;
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
    public String search(
            @RequestParam(value = "q",         required = false, defaultValue = "") String query,
            @RequestParam(value = "year",      required = false, defaultValue = "") String filterYear,
            @RequestParam(value = "genre",     required = false, defaultValue = "") String filterGenre,
            @RequestParam(value = "lang",      required = false, defaultValue = "") String filterLang,
            @RequestParam(value = "minRating", required = false, defaultValue = "") String filterMinRating,
            @RequestParam(value = "sort",      required = false, defaultValue = "default") String sort,
            Model model,
            HttpSession session) {

        String q = query.trim();
        model.addAttribute("query", q);

        // Pass active filter values back to the view for pre-selecting dropdowns
        model.addAttribute("filterYear",      filterYear);
        model.addAttribute("filterGenre",     filterGenre);
        model.addAttribute("filterLang",      filterLang);
        model.addAttribute("filterMinRating", filterMinRating);
        model.addAttribute("sort",            sort);

        if (q.isEmpty()) {
            model.addAttribute("films",        Collections.emptyList());
            model.addAttribute("profiles",     Collections.emptyList());
            model.addAttribute("lists",        Collections.emptyList());
            model.addAttribute("allYears",     Collections.emptyList());
            model.addAttribute("allGenres",    Collections.emptyList());
            model.addAttribute("allLangs",     Collections.emptyList());
            return "search-results";
        }

        // ── FILMS ──────────────────────────────────────────────────────────────
        List<MovieCardDto> allFilms = Collections.emptyList();
        try {
            Map<String, Object> response = tmdbService.searchMovies(q, 1);
            if (response != null && response.containsKey("results")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                allFilms = results.stream()
                        .limit(40)                         // fetch more so filters have data to work with
                        .map(movieMapper::toMovieCardDto)
                        .filter(dto -> dto != null)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Film search failed for query={}", q, e);
        }

        // Build distinct dropdown option sets from the FULL result set
        List<String> allYears = allFilms.stream()
                .map(MovieCardDto::getYear)
                .filter(y -> y != null && !y.isBlank())
                .distinct().sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        List<String> allGenres = allFilms.stream()
                .filter(f -> f.getGenreNames() != null)
                .flatMap(f -> f.getGenreNames().stream())
                .filter(g -> g != null && !g.isBlank())
                .distinct().sorted()
                .collect(Collectors.toList());

        List<String> allLangs = allFilms.stream()
                .map(MovieCardDto::getOriginalLanguage)
                .filter(l -> l != null && !l.isBlank())
                .distinct().sorted()
                .collect(Collectors.toList());

        model.addAttribute("allYears",  allYears);
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("allLangs",  allLangs);

        // Apply filters in Java
        double minRating = filterMinRating.isBlank() ? 0.0 : Double.parseDouble(filterMinRating);

        List<MovieCardDto> films = allFilms.stream()
                .filter(f -> filterYear.isBlank()  || filterYear.equals(f.getYear()))
                .filter(f -> filterGenre.isBlank() || (f.getGenreNames() != null && f.getGenreNames().contains(filterGenre)))
                .filter(f -> filterLang.isBlank()  || filterLang.equals(f.getOriginalLanguage()))
                .filter(f -> minRating <= 0        || (f.getVoteAverage() != null && f.getVoteAverage() >= minRating))
                .collect(Collectors.toList());

        // Apply sort in Java
        Comparator<MovieCardDto> comparator = null;
        switch (sort) {
            case "az":          comparator = Comparator.comparing(f -> f.getTitle() != null ? f.getTitle() : ""); break;
            case "za":          comparator = Comparator.comparing((MovieCardDto f) -> f.getTitle() != null ? f.getTitle() : "").reversed(); break;
            case "year_asc":    comparator = Comparator.comparing(f -> f.getYear() != null ? f.getYear() : ""); break;
            case "year_desc":   comparator = Comparator.comparing((MovieCardDto f) -> f.getYear() != null ? f.getYear() : "").reversed(); break;
            case "rating_asc":  comparator = Comparator.comparing(f -> f.getVoteAverage() != null ? f.getVoteAverage() : 0.0); break;
            case "rating_desc": comparator = Comparator.comparing((MovieCardDto f) -> f.getVoteAverage() != null ? f.getVoteAverage() : 0.0).reversed(); break;
        }
        if (comparator != null) films.sort(comparator);

        model.addAttribute("films", films);

        // ── PROFILES ───────────────────────────────────────────────────────────
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

        // ── LISTS ──────────────────────────────────────────────────────────────
        List<FilmListSummaryDto> lists = Collections.emptyList();
        try {
            List<FilmList> byName = filmListRepository.findByNameContainingIgnoreCase(q);

            List<Long> matchedMovieIds = allFilms.stream()
                    .filter(f -> f.getId() != null)
                    .map(MovieCardDto::getId)
                    .collect(Collectors.toList());

            List<FilmList> byFilm = matchedMovieIds.isEmpty()
                    ? Collections.emptyList()
                    : filmListRepository.findByMovieIds(matchedMovieIds);

            Map<Long, FilmList> merged = new java.util.LinkedHashMap<>();
            for (FilmList fl : byName) merged.put(fl.getId(), fl);
            for (FilmList fl : byFilm) merged.putIfAbsent(fl.getId(), fl);

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
