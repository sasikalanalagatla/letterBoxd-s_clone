package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.FilmListEntryDto;
import com.clone.letterboxd.dto.FilmListFormDto;
import com.clone.letterboxd.dto.FilmListDetailDto;
import com.clone.letterboxd.mapper.FilmListMapper;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.repository.LikeRepository;
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
    private static final String SESSION_MOVIE_IDS      = "pendingMovieIds";
    private static final String SESSION_SEARCH_RESULTS = "movieSearchResults";
    private static final String SESSION_DRAFT          = "listDraft";

    public ListController(FilmListRepository filmListRepository,
                          UserRepository userRepository,
                          ReviewRepository reviewRepository,
                          LikeRepository likeRepository,
                          FilmListMapper filmListMapper,
                          TmdbService tmdbService) {
        this.filmListRepository = filmListRepository;
        this.userRepository     = userRepository;
        this.reviewRepository   = reviewRepository;
        this.likeRepository     = likeRepository;
        this.filmListMapper     = filmListMapper;
        this.tmdbService        = tmdbService;
    }

    @GetMapping
    public String browseLists(HttpSession session, Model model) {
        // fetch raw entities from repository
        List<FilmList> curated = filmListRepository
                .findByNameContainingIgnoreCase("Top 500");
        List<FilmList> popular = filmListRepository
                .findMostPopularLists(PageRequest.of(0, 6)).getContent();

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

        // enrich each entry with movie metadata from TMDB
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
            } catch (Exception e) {
                log.warn("TMDB search failed for query={}", query, e);
            }
            // attach review/like counts to each result map
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
            // clear any old search results so page reloads cleanly
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

            // attach any movies the user added during the draft phase
            List<Map<String, Object>> pending = getPendingMovies(session);
            int rankCounter = 1;
            for (Map<String, Object> movie : pending) {
                FilmListEntry entry = new FilmListEntry();
                entry.setList(newList);
                entry.setMovieId((Long) movie.get("id"));
                entry.setRank(rankCounter++);
                // note and other fields could be added later
                newList.getEntries().add(entry);
            }

            FilmList savedList = filmListRepository.save(newList);

            // clear session state now that the list is persisted
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

        // convert existing entries so the template can render them and allow removal
        if (filmList.getEntries() != null) {
            List<FilmListEntryDto> entryDtos = filmList.getEntries().stream()
                    .map(FilmListMapper::toEntryDto)
                    .toList();

            // enrich entries with basic metadata from TMDB
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

        // forward any search results back to the page so the user can
        // perform additional lookups without losing the list they are
        // editing.  reuses the same session key as the create flow.
        model.addAttribute("searchResults", getSearchResults(session));

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
