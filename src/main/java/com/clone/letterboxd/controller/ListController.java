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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/lists")
public class ListController {

    private final FilmListRepository filmListRepository;
    private final UserRepository userRepository;
    private final FilmListMapper filmListMapper;
    private final TmdbService tmdbService;

    public ListController(FilmListRepository filmListRepository, 
                        UserRepository userRepository,
                        FilmListMapper filmListMapper,
                        TmdbService tmdbService) {
        this.filmListRepository = filmListRepository;
        this.userRepository = userRepository;
        this.filmListMapper = filmListMapper;
        this.tmdbService = tmdbService;
    }

    // Browse all public lists
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

    // View a specific list
    @GetMapping("/{listId}")
    public String viewList(@PathVariable Long listId, Model model) {
        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        
        if (listOptional.isEmpty()) {
            return "redirect:/lists";
        }

        FilmList filmList = listOptional.get();
        FilmListDetailDto listDetail = filmListMapper.toDetailDto(filmList);
        
        model.addAttribute("list", listDetail);
        model.addAttribute("createdBy", filmList.getUser().getDisplayName());
        
        return "list-detail";
    }

    // Create new list page
    @GetMapping("/create")
    public String createListPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        Object loggedInUser = session.getAttribute("loggedInUser");
        
        if (userId == null || loggedInUser == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("list", new FilmListFormDto());
        return "list-create";
    }

    // Search movies for list creation
    @GetMapping("/search-movie")
    @ResponseBody
    public Map<String, Object> searchMovies(@RequestParam String query) {
        return tmdbService.searchMovies(query, 1);
    }

    // Create new list
    @PostMapping
    public String createList(
            HttpSession session,
            FilmListFormDto listFormDto,
            Model model) {
        
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        try {
            FilmList newList = new FilmList();
            newList.setUser(userOptional.get());
            newList.setName(listFormDto.getName());
            newList.setDescription(listFormDto.getDescription());
            newList.setRanked(listFormDto.getRanked() != null ? listFormDto.getRanked() : false);
            newList.setVisibility(listFormDto.getVisibility());
            newList.setCreatedAt(LocalDateTime.now());
            
            FilmList savedList = filmListRepository.save(newList);
            
            return "redirect:/lists/" + savedList.getId();
            
        } catch (Exception e) {
            model.addAttribute("error", "Error creating list: " + e.getMessage());
            model.addAttribute("list", listFormDto);
            return "list-create";
        }
    }

    // Edit list page
    @GetMapping("/{listId}/edit")
    public String editListPage(
            @PathVariable Long listId,
            HttpSession session,
            Model model) {
        
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        
        if (userOptional.isEmpty() || listOptional.isEmpty()) {
            return "redirect:/lists";
        }

        FilmList filmList = listOptional.get();
        
        // Check if user owns this list
        if (!filmList.getUser().getId().equals(userId)) {
            return "redirect:/lists/" + listId;
        }

        FilmListFormDto formDto = new FilmListFormDto();
        formDto.setName(filmList.getName());
        formDto.setDescription(filmList.getDescription());
        formDto.setRanked(filmList.getRanked());
        formDto.setVisibility(filmList.getVisibility());
        
        model.addAttribute("list", formDto);
        model.addAttribute("listId", listId);
        
        return "list-edit";
    }

    // Update list
    @PostMapping("/{listId}/edit")
    public String updateList(
            @PathVariable Long listId,
            HttpSession session,
            FilmListFormDto listFormDto,
            Model model) {
        
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        
        if (listOptional.isEmpty()) {
            return "redirect:/lists";
        }

        try {
            FilmList filmList = listOptional.get();
            
            // Check if user owns this list
            if (!filmList.getUser().getId().equals(userId)) {
                return "redirect:/lists/" + listId;
            }

            filmList.setName(listFormDto.getName());
            filmList.setDescription(listFormDto.getDescription());
            filmList.setRanked(listFormDto.getRanked());
            filmList.setVisibility(listFormDto.getVisibility());
            
            filmListRepository.save(filmList);
            
            return "redirect:/lists/" + listId;
            
        } catch (Exception e) {
            model.addAttribute("error", "Error updating list: " + e.getMessage());
            model.addAttribute("list", listFormDto);
            model.addAttribute("listId", listId);
            return "list-edit";
        }
    }

    // Delete list
    @PostMapping("/{listId}/delete")
    public String deleteList(
            @PathVariable Long listId,
            HttpSession session) {
        
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<FilmList> listOptional = filmListRepository.findById(listId);
        
        if (listOptional.isPresent()) {
            FilmList filmList = listOptional.get();
            
            // Check if user owns this list
            if (filmList.getUser().getId().equals(userId)) {
                filmListRepository.delete(filmList);
            }
        }
        
        return "redirect:/lists";
    }

    // Browse user's lists
    @GetMapping("/user/{username}")
    public String userLists(@PathVariable String username, Model model) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            return "redirect:/lists";
        }

        List<FilmList> userLists = filmListRepository.findByUser(userOptional.get());
        
        model.addAttribute("username", username);
        model.addAttribute("lists", userLists);
        
        return "user-lists";
    }

    // Curated: Top 500 Narrative Features
    @GetMapping("/curated/top-500")
    public String top500List(Model model) {
        // Get lists matching this curated collection
        List<FilmList> top500Lists = filmListRepository
                .findByNameContainingIgnoreCase("Top 500 Narrative");
        
        model.addAttribute("title", "Top 500 Narrative Feature Films");
        model.addAttribute("description", "The 500 highest-rated narrative feature films on Letterboxd");
        model.addAttribute("lists", top500Lists);
        
        // Initialize empty films list for template compatibility
        model.addAttribute("films", new java.util.ArrayList<>());
        
        return "curated-list";
    }

    // Curated: Most Fans
    @GetMapping("/curated/most-fans")
    public String mostFansList(Model model) {
        // Get lists matching this curated collection
        List<FilmList> mostFansList = filmListRepository
                .findByNameContainingIgnoreCase("Fans");
        
        model.addAttribute("title", "Most Fans on Letterboxd");
        model.addAttribute("description", "Films with the most fans and followers");
        model.addAttribute("lists", mostFansList);
        
        // Initialize empty films list for template compatibility
        model.addAttribute("films", new java.util.ArrayList<>());
        
        return "curated-list";
    }

    // Curated: One Million Watched Club
    @GetMapping("/curated/one-million-watched")
    public String oneMillionWatchedList(Model model) {
        // Get lists matching this curated collection
        List<FilmList> oneMillionWatchedList = filmListRepository
                .findByNameContainingIgnoreCase("One Million");
        
        model.addAttribute("title", "One Million Watched Club");
        model.addAttribute("description", "Films that have been watched over one million times");
        model.addAttribute("lists", oneMillionWatchedList);
        
        // Initialize empty films list for template compatibility
        model.addAttribute("films", new java.util.ArrayList<>());
        
        return "curated-list";
    }
}