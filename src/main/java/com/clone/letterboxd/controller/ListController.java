package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.FilmListFormDto;
import com.clone.letterboxd.dto.FilmListDetailDto;
import com.clone.letterboxd.mapper.FilmListMapper;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/lists")
public class ListController {

    private final FilmListRepository filmListRepository;
    private final UserRepository userRepository;
    private final FilmListMapper filmListMapper;
    private final TmdbService tmdbService;
    private static final String SESSION_MOVIE_IDS      = "pendingMovieIds";
    private static final String SESSION_SEARCH_RESULTS = "movieSearchResults";
    private static final String SESSION_DRAFT          = "listDraft";

    public ListController(FilmListRepository filmListRepository,
                          UserRepository userRepository,
                          FilmListMapper filmListMapper,
                          TmdbService tmdbService) {
        this.filmListRepository = filmListRepository;
        this.userRepository     = userRepository;
        this.filmListMapper     = filmListMapper;
        this.tmdbService        = tmdbService;
    }

    @GetMapping
    public String browseLists(Model model) {
        List<FilmList> curatedLists = filmListRepository
                .findByNameContainingIgnoreCase("Top 500");
        List<FilmList> popularLists = filmListRepository
                .findMostPopularLists(PageRequest.of(0, 6)).getContent();

        model.addAttribute("curatedLists", curatedLists);
        model.addAttribute("popularLists", popularLists);
        return "lists";
    }

    @GetMapping("/{listId}")
    public String viewList(@PathVariable Long listId, Model model) {
        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        if (listOptional.isEmpty()) return "redirect:/lists";

        FilmList filmList = listOptional.get();
        FilmListDetailDto listDetail = filmListMapper.toDetailDto(filmList);

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
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) return "redirect:/auth/login";

        FilmListFormDto draft = new FilmListFormDto();
        draft.setName(name);
        draft.setDescription(description);
        draft.setRanked(ranked != null ? ranked : false);
        draft.setVisibility(visibility != null
                ? com.clone.letterboxd.enums.Visibility.valueOf(visibility)
                : com.clone.letterboxd.enums.Visibility.PUBLIC);
        session.setAttribute(SESSION_DRAFT, draft);

        if (query != null && query.trim().length() >= 2) {
            Map<String, Object> response = tmdbService.searchMovies(query.trim(), 1);
            List<Map<String, Object>> results =
                    (List<Map<String, Object>>) response.getOrDefault("results", List.of());
            session.setAttribute(SESSION_SEARCH_RESULTS, results);
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
            HttpSession session) {

        if (session.getAttribute("loggedInUserId") == null) return "redirect:/auth/login";

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
    public String clearSearch(HttpSession session) {
        session.removeAttribute(SESSION_SEARCH_RESULTS);
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
        formDto.setName(filmList.getName());
        formDto.setDescription(filmList.getDescription());
        formDto.setRanked(filmList.getRanked());
        formDto.setVisibility(filmList.getVisibility());

        model.addAttribute("list",   formDto);
        model.addAttribute("listId", listId);
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

    @GetMapping("/user/{username}")
    public String userLists(@PathVariable String username, Model model) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) return "redirect:/lists";

        model.addAttribute("username", username);
        model.addAttribute("lists", filmListRepository.findByUser(userOptional.get()));
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
