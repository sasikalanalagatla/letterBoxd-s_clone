package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.FilmListEntryDto;
import com.clone.letterboxd.dto.FilmListFormDto;
import com.clone.letterboxd.dto.FilmListDetailDto;
import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.mapper.FilmListMapper;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.service.FeaturedListService;
import com.clone.letterboxd.service.ReviewService;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.clone.letterboxd.model.FilmListEntry;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/lists")
public class ListController {

    private static final Logger log = LoggerFactory.getLogger(ListController.class);

    private final FilmListRepository filmListRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final LikeRepository likeRepository;
    private final FilmListMapper filmListMapper;
    private final TmdbService tmdbService;
    private final ReviewService reviewService;
    private final FeaturedListService featuredListService;
    private static final String SESSION_MOVIE_IDS      = "pendingMovieIds";
    private static final String SESSION_SEARCH_RESULTS = "movieSearchResults";
    private static final String SESSION_DRAFT          = "listDraft";

    public ListController(FilmListRepository filmListRepository,
                          UserRepository userRepository,
                          ReviewRepository reviewRepository,
                          LikeRepository likeRepository,
                          FilmListMapper filmListMapper,
                          TmdbService tmdbService,
                          ReviewService reviewService,
                          FeaturedListService featuredListService) {
        this.filmListRepository = filmListRepository;
        this.userRepository     = userRepository;
        this.reviewRepository   = reviewRepository;
        this.likeRepository     = likeRepository;
        this.filmListMapper     = filmListMapper;
        this.tmdbService        = tmdbService;
        this.reviewService      = reviewService;
        this.featuredListService = featuredListService;
    }

    @GetMapping
    public String browseLists(
            @RequestParam(value = "allFeatured", required = false, defaultValue = "false") boolean allFeatured,
            HttpSession session,
            Model model) {
        // featured lists. show a preview by default, or all if requested
        List<FeaturedListService.FeaturedListItem> allFeaturedItems =
            featuredListService.getAllFeaturedLists();
        boolean showAllLink = !allFeatured && allFeaturedItems.size() > 6;
        List<FeaturedListService.FeaturedListItem> featuredSource = allFeatured
                ? allFeaturedItems
                : allFeaturedItems.stream().limit(6).toList();
        List<FilmListSummaryDto> featuredLists = featuredSource.stream()
            .map(item -> {
                FilmListSummaryDto dto = featuredListService.toSummaryDto(item);
                // Fetch and add movie posters
                enrichWithMoviePosters(dto, item);
                // Fetch actual like count from database
                long actualLikes = likeRepository.countByFeaturedListSlug(item.getSlug());
                dto.setLikeCount((int) actualLikes);
                return dto;
            })
            .toList();
        
        model.addAttribute("featuredLists", featuredLists);
        model.addAttribute("totalFeaturedCount", allFeaturedItems.size());
        model.addAttribute("showAllLink", showAllLink);
        
        // popular this week (reuse existing repository method)
        List<FilmList> popular = filmListRepository
                .findMostPopularLists(PageRequest.of(0, 6)).getContent();
        model.addAttribute("popularThisWeek", buildSummaryList(popular));
        
        // TODO: recently liked section - for now mirror popular lists
        model.addAttribute("recentlyLiked", buildSummaryList(popular));

        // fetch raw entities from repository
        List<FilmList> curated = filmListRepository
                .findByNameContainingIgnoreCase("Top 500");
        // reuse same 'popular' list for the normal popularLists section

        // convert to view-friendly DTOs and enrich with TMDB metadata
        model.addAttribute("curatedLists", buildSummaryList(curated));
        model.addAttribute("popularLists", buildSummaryList(popular));

        // if the user is logged in, include their own lists as summaries
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId != null) {
            Optional<User> userOpt = userRepository.findById(userId);
            userOpt.ifPresent(user -> {
                List<FilmList> myLists = filmListRepository.findByUser(user);
                model.addAttribute("myLists", buildSummaryList(myLists));
            });
        }

        return "lists";
    }

    @GetMapping("/{listId}")
    public String viewList(@PathVariable Long listId, Model model) {
        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        if (listOptional.isEmpty()) return "redirect:/lists";

        FilmList filmList = listOptional.get();
        FilmListDetailDto listDetail = filmListMapper.toDetailDto(filmList);

        if (listDetail.getEntries() != null) {
            for (FilmListEntryDto entry : listDetail.getEntries()) {
                try {
                    Map<String, Object> movieData = tmdbService.getMovieDetails(entry.getMovieId());
                    if (movieData != null) {
                        entry.setMovieTitle((String) movieData.get("title"));
                        entry.setPosterPath((String) movieData.get("poster_path"));
                        String release = (String) movieData.get("release_date");
                        if (release != null && release.length() >= 4) {
                            entry.setReleaseYear(release.substring(0, 4));
                        }
                        Object vote = movieData.get("vote_average");
                        if (vote instanceof Number) {
                            entry.setVoteAverage(((Number) vote).doubleValue());
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to fetch TMDB details for movie {}", entry.getMovieId(), e);
                }
            }
        }

        model.addAttribute("list", listDetail);
        model.addAttribute("createdBy", filmList.getUser().getDisplayName());
        return "list-detail";
    }

    @GetMapping("/create")
    public String createListPage(HttpSession session, Model model) {
        if (session.getAttribute("loggedInUserId") == null) return "redirect:/auth/login";

        List<Map<String, Object>> pendingMovies = getPendingMovies(session);
        List<Map<String, Object>> searchResults = getSearchResults(session);

        FilmListFormDto draft = (FilmListFormDto) session.getAttribute(SESSION_DRAFT);
        if (draft == null) draft = new FilmListFormDto();

        model.addAttribute("list",          draft);
        model.addAttribute("pendingMovies", pendingMovies);
        model.addAttribute("searchResults", searchResults);
        model.addAttribute("searchQuery", ""); // ensure input is cleared
        return "list-create";
    }

    @PostMapping("/search-movie")
    public String searchMovie(
            @RequestParam String query,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean ranked,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Long listId,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) return "redirect:/auth/login";
        log.debug("searchMovie: query='{}', listId={}", query, listId);

        FilmListFormDto draft = new FilmListFormDto();
        draft.setName(name);
        draft.setDescription(description);
        draft.setRanked(ranked != null ? ranked : false);
        draft.setVisibility(visibility != null
                ? com.clone.letterboxd.enums.Visibility.valueOf(visibility)
                : com.clone.letterboxd.enums.Visibility.PUBLIC);
        session.setAttribute(SESSION_DRAFT, draft);

        if (query != null && query.trim().length() >= 2) {
            List<Map<String, Object>> results = List.of();
            try {
                Map<String, Object> response = tmdbService.searchMovies(query.trim(), 1);
                results = (List<Map<String, Object>>) response.getOrDefault("results", List.of());
                // only keep the first 10 entries to avoid overwhelming the user
                if (results.size() > 10) {
                    results = results.subList(0, 10);
                }
            } catch (Exception e) {
                log.warn("TMDB search failed for query={}", query, e);
            }
            for (Map<String, Object> movie : results) {
                Object idObj = movie.get("id");
                if (idObj instanceof Number) {
                    Long mid = ((Number) idObj).longValue();
                    movie.put("reviewCount", reviewRepository.countByMovieId(mid));
                    long likes = likeRepository.countDirectMovieLikes(mid);
                    movie.put("likeCount", likes);
                }
            }
            session.setAttribute(SESSION_SEARCH_RESULTS, results);
            log.debug("stored {} results in session", results.size());
        } else {
            session.removeAttribute(SESSION_SEARCH_RESULTS);
        }

        if (listId != null) {
            return "redirect:/lists/" + listId + "/edit";
        }
        return "redirect:/lists/create";
    }

    @PostMapping("/add-movie")
    public String addMovie(
            @RequestParam Long movieId,
            @RequestParam String movieTitle,
            @RequestParam(required = false) String posterPath,
            @RequestParam(required = false) String releaseYear,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean ranked,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Long listId,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) return "redirect:/auth/login";

        if (listId != null) {
            // adding to existing persisted list
            Optional<FilmList> listOpt = filmListRepository.findById(listId);
            if (listOpt.isPresent()) {
                FilmList list = listOpt.get();
                // verify ownership
                Long userId = (Long) session.getAttribute("loggedInUserId");
                if (list.getUser().getId().equals(userId)) {
                    int rank = list.getEntries() != null ? list.getEntries().size() + 1 : 1;
                    FilmListEntry entry = new FilmListEntry();
                    entry.setList(list);
                    entry.setMovieId(movieId);
                    entry.setRank(rank);
                    list.getEntries().add(entry);
                    filmListRepository.save(list);
                }
            }
            session.removeAttribute(SESSION_SEARCH_RESULTS);
            return "redirect:/lists/" + listId + "/edit";
        }

        List<Map<String, Object>> pending = getPendingMovies(session);

        boolean alreadyAdded = pending.stream()
                .anyMatch(m -> movieId.equals(m.get("id")));

        if (!alreadyAdded) {
            Map<String, Object> movie = new LinkedHashMap<>();
            movie.put("id",          movieId);
            movie.put("title",       movieTitle);
            movie.put("posterPath",  posterPath);
            movie.put("releaseYear", releaseYear);
            pending.add(movie);
            session.setAttribute(SESSION_MOVIE_IDS, pending);
        }
        session.removeAttribute(SESSION_SEARCH_RESULTS);

        return buildRedirectToCreate(name, description, ranked, visibility);
    }

    @PostMapping("/remove-movie")
    public String removeMovie(
            @RequestParam Long movieId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Boolean ranked,
            @RequestParam(required = false) String visibility,
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) return "redirect:/auth/login";

        List<Map<String, Object>> pending = getPendingMovies(session);
        pending.removeIf(m -> movieId.equals(m.get("id")));
        session.setAttribute(SESSION_MOVIE_IDS, pending);

        return buildRedirectToCreate(name, description, ranked, visibility);
    }

    @PostMapping("/clear-search")
    public String clearSearch(@RequestParam(required = false) Long listId, HttpSession session) {
        session.removeAttribute(SESSION_SEARCH_RESULTS);
        if (listId != null) {
            return "redirect:/lists/" + listId + "/edit";
        }
        return "redirect:/lists/create";
    }

    @PostMapping
    public String createList(
            HttpSession session,
            FilmListFormDto listFormDto,
            Model model) {

        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) return "redirect:/auth/login";

        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) return "redirect:/auth/logout";

        try {
            FilmList newList = new FilmList();
            newList.setUser(userOptional.get());
            newList.setName(listFormDto.getName());
            newList.setDescription(listFormDto.getDescription());
            newList.setRanked(listFormDto.getRanked() != null ? listFormDto.getRanked() : false);
            newList.setVisibility(listFormDto.getVisibility());
            newList.setCreatedAt(LocalDateTime.now());

            List<Map<String, Object>> pending = getPendingMovies(session);
            int rankCounter = 1;
            for (Map<String, Object> movie : pending) {
                FilmListEntry entry = new FilmListEntry();
                entry.setList(newList);
                entry.setMovieId((Long) movie.get("id"));
                entry.setRank(rankCounter++);
                newList.getEntries().add(entry);
            }

            FilmList savedList = filmListRepository.save(newList);

            session.removeAttribute(SESSION_MOVIE_IDS);
            session.removeAttribute(SESSION_SEARCH_RESULTS);

            return "redirect:/lists/" + savedList.getId();

        } catch (Exception e) {
            model.addAttribute("error", "Error creating list: " + e.getMessage());
            model.addAttribute("list", listFormDto);
            model.addAttribute("pendingMovies", getPendingMovies(session));
            model.addAttribute("searchResults", getSearchResults(session));
            return "list-create";
        }
    }

    @GetMapping("/{listId}/edit")
    public String editListPage(@PathVariable Long listId, HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) return "redirect:/auth/login";

        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        if (listOptional.isEmpty()) return "redirect:/lists";

        FilmList filmList = listOptional.get();
        if (!filmList.getUser().getId().equals(userId)) return "redirect:/lists/" + listId;

        FilmListFormDto formDto = new FilmListFormDto();
        formDto.setId(filmList.getId());
        formDto.setName(filmList.getName());
        formDto.setDescription(filmList.getDescription());
        formDto.setRanked(filmList.getRanked());
        formDto.setVisibility(filmList.getVisibility());

        if (filmList.getEntries() != null) {
            List<FilmListEntryDto> entryDtos = filmList.getEntries().stream()
                    .map(FilmListMapper::toEntryDto)
                    .toList();

            for (FilmListEntryDto entry : entryDtos) {
                try {
                    Map<String, Object> movieData = tmdbService.getMovieDetails(entry.getMovieId());
                    if (movieData != null) {
                        entry.setMovieTitle((String) movieData.get("title"));
                        entry.setPosterPath((String) movieData.get("poster_path"));
                        String release = (String) movieData.get("release_date");
                        if (release != null && release.length() >= 4) {
                            entry.setReleaseYear(release.substring(0, 4));
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to enrich entry {} for edit page", entry.getMovieId(), e);
                }
            }
            formDto.setEntries(entryDtos);
        }

        model.addAttribute("list",   formDto);
        model.addAttribute("listId", listId);

        model.addAttribute("searchResults", getSearchResults(session));
        model.addAttribute("searchQuery", "");

        return "list-edit";
    }

    @PostMapping("/{listId}/edit")
    public String updateList(@PathVariable Long listId, HttpSession session,
                             FilmListFormDto listFormDto, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) return "redirect:/auth/login";

        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        if (listOptional.isEmpty()) return "redirect:/lists";

        try {
            FilmList filmList = listOptional.get();
            if (!filmList.getUser().getId().equals(userId)) return "redirect:/lists/" + listId;

            filmList.setName(listFormDto.getName());
            filmList.setDescription(listFormDto.getDescription());
            filmList.setRanked(listFormDto.getRanked());
            filmList.setVisibility(listFormDto.getVisibility());
            filmListRepository.save(filmList);

            return "redirect:/lists/" + listId;

        } catch (Exception e) {
            model.addAttribute("error", "Error updating list: " + e.getMessage());
            model.addAttribute("list",   listFormDto);
            model.addAttribute("listId", listId);
            return "list-edit";
        }
    }

    @PostMapping("/{listId}/delete")
    public String deleteList(@PathVariable Long listId, HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) return "redirect:/auth/login";

        filmListRepository.findById(listId).ifPresent(filmList -> {
            if (filmList.getUser().getId().equals(userId))
                filmListRepository.delete(filmList);
        });
        return "redirect:/lists";
    }

    @PostMapping("/{listId}/entries/{movieId}/remove")
    public String removeEntry(@PathVariable Long listId,
                              @PathVariable Long movieId,
                              HttpSession session) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId == null) return "redirect:/auth/login";

        filmListRepository.findById(listId).ifPresent(list -> {
            if (list.getUser().getId().equals(userId)) {
                list.getEntries().removeIf(e -> movieId.equals(e.getMovieId()));
                filmListRepository.save(list);
            }
        });
        return "redirect:/lists/" + listId + "/edit";
    }

    @GetMapping("/user/{username}")
    public String userLists(@PathVariable String username, Model model) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) return "redirect:/lists";

        List<FilmList> found = filmListRepository.findByUser(userOptional.get());
        model.addAttribute("username", username);
        model.addAttribute("lists", buildSummaryList(found));
        return "user-lists";
    }

    @GetMapping("/featured")
    public String redirectFeatured() {
        // consolidated view lives on /lists; preserve query param semantics
        return "redirect:/lists?allFeatured=true";
    }

    // removed; full featured view now handled by browseLists with allFeatured flag

    @GetMapping("/featured/{slug}")
    public String viewFeaturedList(@PathVariable String slug, HttpSession session, Model model) {
        FeaturedListService.FeaturedListItem item = 
                featuredListService.getAllFeaturedLists().stream()
                    .filter(i -> i.getSlug().equals(slug))
                    .findFirst()
                    .orElse(null);
        if (item == null) {
            return "redirect:/lists?allFeatured=true";
        }
        FilmListSummaryDto dto = featuredListService.toSummaryDto(item);
        enrichWithMoviePosters(dto, item);

        // load reviews for each movie in the list
        com.clone.letterboxd.model.User currentUser = null;
        Long userId = (Long) session.getAttribute("loggedInUserId");
        if (userId != null) {
            currentUser = userRepository.findById(userId).orElse(null);
        }
        Map<Long, List<com.clone.letterboxd.dto.ReviewDisplayDto>> reviewsByMovie = new LinkedHashMap<>();
        Map<Long, String> movieTitles = new LinkedHashMap<>();
        for (Long mid : item.getMovieIds()) {
            List<com.clone.letterboxd.dto.ReviewDisplayDto> reviews =
                    reviewService.getDisplayDtosForMovie(mid, currentUser);
            // only add to map if there are reviews
            if (reviews != null && !reviews.isEmpty()) {
                reviewsByMovie.put(mid, reviews);
            }
            // also fetch title for display
            try {
                Map<String,Object> md = tmdbService.getMovieDetails(mid);
                if (md != null) {
                    movieTitles.put(mid, (String)md.get("title"));
                }
            } catch (Exception e) {
                log.warn("failed to fetch title for movie {}", mid, e);
            }
        }

        // Get featured list likes by slug
        long listLikeCount = likeRepository.countByFeaturedListSlug(slug);
        boolean currentUserLikedList = userId != null && likeRepository.existsByFeaturedListSlugAndUserId(slug, userId);

        model.addAttribute("list", dto);
        model.addAttribute("reviewsByMovie", reviewsByMovie);
        model.addAttribute("movieTitles", movieTitles);
        model.addAttribute("featuredSlug", slug);
        model.addAttribute("listLikeCount", listLikeCount);
        model.addAttribute("currentUserLikedList", currentUserLikedList);
        // indicate login state for template logic
        model.addAttribute("loggedIn", userId != null);
        return "featured-list-detail";
    }

    @GetMapping("/curated/top-500")
    public String top500List(Model model) {
        model.addAttribute("title",       "Top 500 Narrative Feature Films");
        model.addAttribute("description", "The 500 highest-rated narrative feature films on Letterboxd");
        model.addAttribute("lists",       filmListRepository.findByNameContainingIgnoreCase("Top 500 Narrative"));
        model.addAttribute("films",       new ArrayList<>());
        return "curated-list";
    }

    @GetMapping("/curated/most-fans")
    public String mostFansList(Model model) {
        model.addAttribute("title",       "Most Fans on Letterboxd");
        model.addAttribute("description", "Films with the most fans and followers");
        model.addAttribute("lists",       filmListRepository.findByNameContainingIgnoreCase("Fans"));
        model.addAttribute("films",       new ArrayList<>());
        return "curated-list";
    }

    @GetMapping("/curated/one-million-watched")
    public String oneMillionWatchedList(Model model) {
        model.addAttribute("title",       "One Million Watched Club");
        model.addAttribute("description", "Films that have been watched over one million times");
        model.addAttribute("lists",       filmListRepository.findByNameContainingIgnoreCase("One Million"));
        model.addAttribute("films",       new ArrayList<>());
        return "curated-list";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getPendingMovies(HttpSession session) {
        List<Map<String, Object>> pending =
                (List<Map<String, Object>>) session.getAttribute(SESSION_MOVIE_IDS);
        if (pending == null) {
            pending = new ArrayList<>();
            session.setAttribute(SESSION_MOVIE_IDS, pending);
        }
        return pending;
    }

    private List<com.clone.letterboxd.dto.FilmListSummaryDto> buildSummaryList(List<FilmList> lists) {
        if (lists == null) return List.of();
        return lists.stream().map(this::toSummaryDto).toList();
    }

    private void enrichWithMoviePosters(FilmListSummaryDto dto, FeaturedListService.FeaturedListItem item) {
        if (item.getMovieIds() == null || item.getMovieIds().isEmpty()) {
            return;
        }
        
        List<String> posters = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (Long movieId : item.getMovieIds()) {
            try {
                Map<String, Object> movieData = tmdbService.getMovieDetails(movieId);
                if (movieData != null) {
                    String poster = (String) movieData.get("poster_path");
                    if (poster != null) {
                        posters.add(poster);
                        ids.add(movieId);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch TMDB details for featured movie {}", movieId, e);
            }
        }
        dto.setPreviewPosterPaths(posters);
        dto.setPreviewMovieIds(ids);
    }

    private com.clone.letterboxd.dto.FilmListSummaryDto toSummaryDto(FilmList list) {
        com.clone.letterboxd.dto.FilmListSummaryDto dto = filmListMapper.toSummaryDto(list);

        if (list.getUser() != null) {
            com.clone.letterboxd.dto.UserSummaryDto owner = new com.clone.letterboxd.dto.UserSummaryDto();
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

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getSearchResults(HttpSession session) {
        List<Map<String, Object>> results =
                (List<Map<String, Object>>) session.getAttribute(SESSION_SEARCH_RESULTS);
        return results != null ? results : List.of();
    }

    private String buildRedirectToCreate(String name, String description,
                                         Boolean ranked, String visibility) {
        return "redirect:/lists/create";
    }
}
