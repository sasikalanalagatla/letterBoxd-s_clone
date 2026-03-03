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

import java.util.*;
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
    public Object search(
            @RequestParam(value = "q",         required = false, defaultValue = "") String query,
            @RequestParam(value = "page",      required = false, defaultValue = "1") int page,
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
            model.addAttribute("totalCount",   0);
            model.addAttribute("currentPage", page);
            return "search-results";
        }

        return performSearch(q, page, filterYear, filterGenre, filterLang, filterMinRating, sort, model);
    }

    private String mapSortToTmdb(String sort) {
        switch (sort) {
            case "az":          return "original_title.asc";
            case "za":          return "original_title.desc";
            case "year_asc":    return "primary_release_date.asc";
            case "year_desc":   return "primary_release_date.desc";
            case "rating_asc":  return "vote_average.asc";
            case "rating_desc": return "vote_average.desc";
            default:             return "popularity.desc";
        }
    }

    private String performSearch(String q, int page, String filterYear, String filterGenre, String filterLang, String filterMinRating, String sort, Model model) {
        final int BATCH_SIZE = 60;
        final int ITEMS_PER_API_PAGE = 20;
        int neededItems = page * BATCH_SIZE;
        int neededApiPages = (neededItems + ITEMS_PER_API_PAGE - 1) / ITEMS_PER_API_PAGE;

        List<MovieCardDto> aggregated = new ArrayList<>();
        int totalPagesFromApi = Integer.MAX_VALUE;
        int totalResults = 0;

        try {
            for (int apiPage = 1; apiPage <= neededApiPages && apiPage <= totalPagesFromApi; apiPage++) {
                Map<String, Object> response;
                boolean usingFilters = !filterYear.isBlank() || !filterGenre.isBlank()
                        || !filterLang.isBlank() || !filterMinRating.isBlank() || !"default".equals(sort);

                String sortBy = mapSortToTmdb(sort);
                Double minRatingVal = filterMinRating.isBlank() ? null : Double.parseDouble(filterMinRating);
                String genreParam = MovieMapper.lookupGenreIdByName(filterGenre);

                if (usingFilters) {
                    response = tmdbService.searchWithFilters(q, apiPage,
                            filterYear, genreParam, filterLang, minRatingVal, sortBy);
                } else {
                    response = tmdbService.searchMovies(q, apiPage);
                }

                if (response == null || !response.containsKey("results")) {
                    break;
                }

                if (apiPage == 1) {
                    Number totalResultsNum = (Number) response.get("total_results");
                    totalResults = totalResultsNum != null ? totalResultsNum.intValue() : 0;
                    Number totalPagesNum = (Number) response.get("total_pages");
                    totalPagesFromApi = totalPagesNum != null ? totalPagesNum.intValue() : 1;
                }

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
                List<MovieCardDto> pageMovies = results.stream()
                        .map(movieMapper::toMovieCardDto)
                        .filter(dto -> dto != null)
                        .collect(Collectors.toList());


                aggregated.addAll(pageMovies);
            }
        } catch (Exception e) {
            log.warn("Film search failed for query={}", q, e);
        }

        // build filter dropdowns from the set of movies we actually fetched for this query
        List<String> allYears = aggregated.stream()
                .map(MovieCardDto::getYear)
                .filter(y -> y != null && !y.isBlank())
                .distinct().sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        List<String> allGenres = aggregated.stream()
                .filter(f -> f.getGenreNames() != null)
                .flatMap(f -> f.getGenreNames().stream())
                .filter(g -> g != null && !g.isBlank())
                .distinct().sorted()
                .collect(Collectors.toList());

        List<String> allLangs = aggregated.stream()
                .map(MovieCardDto::getOriginalLanguage)
                .filter(l -> l != null && !l.isBlank())
                .distinct().sorted()
                .collect(Collectors.toList());

        model.addAttribute("allYears", allYears);
        model.addAttribute("allGenres", allGenres);
        model.addAttribute("allLangs", allLangs);

        // trim to requested batch
        if (aggregated.size() > neededItems) {
            aggregated = aggregated.subList(0, neededItems);
        }
        model.addAttribute("films", aggregated);
        model.addAttribute("totalCount", totalResults);
        model.addAttribute("currentPage", page);

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

            List<Long> matchedMovieIds = aggregated.stream()
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
