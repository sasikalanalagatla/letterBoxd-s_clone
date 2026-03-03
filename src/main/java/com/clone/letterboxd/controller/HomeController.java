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

        log.debug("GET / requested, page(batch)={}", page);
        User currentUser = (User) session.getAttribute("loggedInUser");

        // remember which batch the user has loaded (1 == first 60 films, 2 == 120, etc.)
        model.addAttribute("currentPage",     page);
        model.addAttribute("currentUser",     currentUser);

        // pass filter values back to the view
        model.addAttribute("filterYear",      filterYear);
        model.addAttribute("filterGenre",     filterGenre);
        model.addAttribute("filterLang",      filterLang);
        model.addAttribute("filterMinRating", filterMinRating);
        model.addAttribute("sort",            sort);

        // figure out how many API pages we need to fetch to satisfy the
        // requested batch of 60 items per click.  TMDB returns 20 items per page.
        final int BATCH_SIZE = 60;
        final int ITEMS_PER_API_PAGE = 20;
        int neededItems = page * BATCH_SIZE;
        int neededApiPages = (neededItems + ITEMS_PER_API_PAGE - 1) / ITEMS_PER_API_PAGE;

        List<MovieCardDto> aggregated = new java.util.ArrayList<>();
        int totalPagesFromApi = Integer.MAX_VALUE;
        int totalResults = 0;

        try {
            for (int apiPage = 1; apiPage <= neededApiPages && apiPage <= totalPagesFromApi; apiPage++) {
                Map<String, Object> response;
                boolean usingFilters = !filterYear.isBlank() || !filterGenre.isBlank()
                        || !filterLang.isBlank() || !filterMinRating.isBlank() || !"default".equals(sort);

                if (usingFilters) {
                    // sortBy for discover
                    String sortBy = mapSortToTmdb(sort);
                    Double minRatingVal = filterMinRating.isBlank() ? null : Double.parseDouble(filterMinRating);
                    // convert the genre name to TMDB genre id if possible
                    String genreParam = MovieMapper.lookupGenreIdByName(filterGenre);
                    response = tmdbService.discoverMovies(apiPage,
                            filterYear, genreParam, filterLang, minRatingVal, sortBy);
                } else {
                    // no filters -> show weekly trending (popular this week) rather than generic popular
                    response = tmdbService.getTrendingMovies("week", apiPage);
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

                // enrich with counts
                for (MovieCardDto m : pageMovies) {
                    if (m.getId() != null) {
                        long reviewCount = reviewRepository.countByMovieId(m.getId());
                        long likeCount   = likeRepository.countDirectMovieLikes(m.getId());
                        m.setReviewCount(reviewCount);
                        m.setLikeCount(likeCount);
                    }
                }

                aggregated.addAll(pageMovies);
            }

            // build global dropdown sets rather than depending on the tiny batch of
            // movies we happen to have loaded.  This ensures filters reflect the entire
            // TMDB catalogue.
            List<String> allYears = java.time.Year.now().atDay(1).getYear() != 0
                    ? java.util.stream.IntStream.rangeClosed(1900, java.time.Year.now().getValue())
                        .mapToObj(Integer::toString)
                        .collect(Collectors.toList())
                    : java.util.Collections.emptyList();
            Collections.reverse(allYears); // most recent first

            Map<Integer, String> genreMap = tmdbService.getGenreMap();
            List<String> allGenres = new java.util.ArrayList<>(genreMap.values());
            Collections.sort(allGenres);

            List<String> allLangs = tmdbService.getLanguages();
            Collections.sort(allLangs);

            model.addAttribute("allYears",  allYears);
            model.addAttribute("allGenres", allGenres);
            model.addAttribute("allLangs",  allLangs);

            // when filters have been applied we already asked TMDB for a filtered set so
            // there is nothing more to do here; otherwise the data is just the trending
            // results and no additional filtering is necessary.
            // only show up to the number items requested by the client (batch * 60)
            if (aggregated.size() > neededItems) {
                aggregated = aggregated.subList(0, neededItems);
            }
            model.addAttribute("movies", aggregated);
            model.addAttribute("totalCount", totalResults);

        } catch (Exception e) {
            log.error("failed to load popular/trending movies", e);
            model.addAttribute("error", "Failed to load movies: " + e.getMessage());
            model.addAttribute("movies",    Collections.emptyList());
            model.addAttribute("allYears",  Collections.emptyList());
            model.addAttribute("allGenres", Collections.emptyList());
            model.addAttribute("allLangs",  Collections.emptyList());
            model.addAttribute("totalCount", 0);
        }

        return "index";
    }

    /**
     * Map the simple sort keys used by the UI into TMDB discover sort_by values.
     */
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

}