package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserProfileDto;
import com.clone.letterboxd.dto.UserUpdateDto;
import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.mapper.FilmListMapper;
import com.clone.letterboxd.mapper.ReviewMapper;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.FilmList;
import com.clone.letterboxd.model.FilmListEntry;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.repository.UserRepository;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import com.clone.letterboxd.service.TmdbService;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/profile")
@Slf4j
public class ProfileController {

    private final UserRepository userRepository;
    private final FilmListRepository filmListRepository;
    private final ReviewRepository reviewRepository;
    private final UserMapper userMapper;
    private final TmdbService tmdbService;
    private final FilmListMapper filmListMapper;
    private final LikeRepository likeRepository;

    public ProfileController(UserRepository userRepository,
                             FilmListRepository filmListRepository,
                             ReviewRepository reviewRepository,
                             UserMapper userMapper,
                             TmdbService tmdbService, 
                             FilmListMapper filmListMapper,
                             LikeRepository likeRepository) {
        this.userRepository = userRepository;
        this.filmListRepository = filmListRepository;
        this.reviewRepository = reviewRepository;
        this.userMapper = userMapper;
        this.tmdbService = tmdbService;
        this.filmListMapper = filmListMapper;
        this.likeRepository = likeRepository;
    }

    @GetMapping
    public String myProfile(HttpSession session, Model model) {
        log.debug("myProfile request received");
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            log.debug("myProfile redirecting to login");
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        User user = userOptional.get();
        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setIsOwnProfile(true);
        // compute dynamic counts
        profile.setListCount(filmListRepository.findByUser(user).size());
        profile.setReviewCount((int) reviewRepository.countByUser(user));
        profile.setFollowingCount((int) userRepository.countFollowing(userId));
        profile.setLikeCount(likeRepository.findByUserIdOrderByCreatedAtDesc(userId).size());

        // include a few list summaries for display on profile
        List<FilmList> lists = filmListRepository.findByUser(user);
        
        // add reviews written by this user
        var userReviews = reviewRepository.findByUser(user);
        List<com.clone.letterboxd.dto.ReviewDisplayDto> reviewDtos = userReviews.stream()
                .map(r -> {
                    var rd = ReviewMapper.toDisplayDto(r);
                    // fetch movie title/poster from TMDB
                    try {
                        Map<String,Object> movieData = tmdbService.getMovieDetails(r.getMovieId());
                        if (movieData != null) {
                            rd.setMovieTitle((String) movieData.get("title"));
                            String poster = (String) movieData.get("poster_path");
                            if (poster != null) rd.setMoviePosterPath(poster);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load TMDB info for movie {}", r.getMovieId(), e);
                    }
                    return rd;
                }).toList();

        // Show only Top 5 for profile summary
        model.addAttribute("profileLists", buildSummaryList(lists).stream().limit(5).toList());
        model.addAttribute("profileReviews", reviewDtos.stream().limit(5).toList());
        
        // Populate Liked items and truncate
        populateProfileLikes(user, model);
        List<Map<String, Object>> likedMovies = (List<Map<String, Object>>) model.getAttribute("likedMovies");
        List<com.clone.letterboxd.dto.ReviewDisplayDto> likedReviews = (List<com.clone.letterboxd.dto.ReviewDisplayDto>) model.getAttribute("likedReviews");
        model.addAttribute("likedMovies", likedMovies != null ? likedMovies.stream().limit(5).toList() : List.of());
        model.addAttribute("likedReviews", likedReviews != null ? likedReviews : List.of());

        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", true);
        model.addAttribute("activeTab", "reviews");
        
        return "profile";
    }

    @GetMapping("/{username}")
    public String viewProfile(
            @PathVariable String username,
            HttpSession session,
            Model model) {
        log.debug("viewProfile for {}", username);
        Optional<User> userOptional = userRepository.findByUsername(username);
        
        if (userOptional.isEmpty()) {
            log.warn("viewProfile: user {} not found", username);
            model.addAttribute("error", "User not found");
            return "error";
        }

        User user = userOptional.get();
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        
        boolean isOwnProfile = loggedInUserId != null && loggedInUserId.equals(user.getId());
        
        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setIsOwnProfile(isOwnProfile);
        profile.setListCount(filmListRepository.findByUser(user).size());
        profile.setReviewCount((int) reviewRepository.countByUser(user));
        profile.setFollowingCount((int) userRepository.countFollowing(user.getId()));
        profile.setLikeCount(likeRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).size());

        // include a few list summaries
        List<FilmList> lists = filmListRepository.findByUser(user);

        // fetch reviews for public view; they will all belong to this profile
        var userReviews = reviewRepository.findByUser(user);
        List<com.clone.letterboxd.dto.ReviewDisplayDto> reviewDtos = userReviews.stream()
                .map(r -> {
                    var rd = ReviewMapper.toDisplayDto(r);
                    try {
                        Map<String,Object> movieData = tmdbService.getMovieDetails(r.getMovieId());
                        if (movieData != null) {
                            rd.setMovieTitle((String) movieData.get("title"));
                            String poster = (String) movieData.get("poster_path");
                            if (poster != null) rd.setMoviePosterPath(poster);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load TMDB info for movie {}", r.getMovieId(), e);
                    }
                    return rd;
                }).toList();

        // Show only Top 5 for profile summary
        model.addAttribute("profileLists", buildSummaryList(lists).stream().limit(5).toList());
        model.addAttribute("profileReviews", reviewDtos.stream().limit(5).toList());
        
        // Populate Liked items and truncate
        populateProfileLikes(user, model);
        List<Map<String, Object>> likedMovies = (List<Map<String, Object>>) model.getAttribute("likedMovies");
        List<com.clone.letterboxd.dto.ReviewDisplayDto> likedReviews = (List<com.clone.letterboxd.dto.ReviewDisplayDto>) model.getAttribute("likedReviews");
        model.addAttribute("likedMovies", likedMovies != null ? likedMovies.stream().limit(5).toList() : List.of());
        model.addAttribute("likedReviews", likedReviews != null ? likedReviews : List.of());

        // Check if logged-in user is following this profile user
        boolean isFollowing = false;
        if (loggedInUserId != null && !isOwnProfile) {
            isFollowing = userRepository.isFollowing(loggedInUserId, user.getId());
        }
        
        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("activeTab", "reviews");
        
        return "profile";
    }

    @GetMapping("/{username}/network")
    public String showNetwork(
            @PathVariable String username,
            @RequestParam(value = "sortFollowing", required = false, defaultValue = "joined_desc") String sortFollowing,
            @RequestParam(value = "sortFollowers", required = false, defaultValue = "joined_desc") String sortFollowers,
            @RequestParam(value = "tab", required = false, defaultValue = "following") String tab,
            HttpSession session,
            Model model) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            log.warn("showNetwork: username {} not found", username);
            model.addAttribute("error", "User not found");
            return "error";
        }

        User user = userOptional.get();
        log.debug("Loading network for user {}", user.getId());
        
        List<User> followers = userRepository.getFollowers(user.getId());
        List<User> following = userRepository.getFollowing(user.getId());
        
        // apply sort for followers
        Comparator<User> followerComp = getComparator(sortFollowers);
        followers.sort(followerComp);
        
        // apply sort for following
        Comparator<User> followingComp = getComparator(sortFollowing);
        following.sort(followingComp);
        
        List<UserSummaryDto> followersDtoList = followers.stream()
                .map(u -> {
                    UserSummaryDto dto = UserMapper.toSummaryDto(u);
                    dto.setFollowersCount(userRepository.countFollowers(u.getId()));
                    dto.setFollowingCount(userRepository.countFollowing(u.getId()));
                    dto.setListCount(filmListRepository.countByUser(u));
                    return dto;
                })
                .toList();
                
        List<UserSummaryDto> followingDtoList = following.stream()
                .map(u -> {
                    UserSummaryDto dto = UserMapper.toSummaryDto(u);
                    dto.setFollowersCount(userRepository.countFollowers(u.getId()));
                    dto.setFollowingCount(userRepository.countFollowing(u.getId()));
                    dto.setListCount(filmListRepository.countByUser(u));
                    return dto;
                })
                .toList();

        // build profile dto for header back link/title convenience
        UserProfileDto profile = userMapper.toProfileDto(user);
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        profile.setIsOwnProfile(loggedInUserId != null && loggedInUserId.equals(user.getId()));

        model.addAttribute("followers", followersDtoList);
        model.addAttribute("following", followingDtoList);
        model.addAttribute("listTitle", "Network");
        model.addAttribute("profile", profile);
        model.addAttribute("sortFollowing", sortFollowing);
        model.addAttribute("sortFollowers", sortFollowers);
        model.addAttribute("activeTab", tab);
        model.addAttribute("basePath", "/profile/" + username + "/network");
        
        return "user-list";
    }

    @GetMapping("/{username}/reviews")
    public String showReviews(@PathVariable String username, HttpSession session, Model model) {
        log.debug("showReviews for {}", username);
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) return "redirect:/";
        
        User user = userOptional.get();
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        boolean isOwnProfile = loggedInUserId != null && loggedInUserId.equals(user.getId());

        var userReviews = reviewRepository.findByUser(user);
        List<com.clone.letterboxd.dto.ReviewDisplayDto> reviewDtos = userReviews.stream()
                .map(r -> {
                    var rd = ReviewMapper.toDisplayDto(r);
                    try {
                        Map<String,Object> movieData = tmdbService.getMovieDetails(r.getMovieId());
                        if (movieData != null) {
                            rd.setMovieTitle((String) movieData.get("title"));
                            String poster = (String) movieData.get("poster_path");
                            if (poster != null) rd.setMoviePosterPath(poster);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to load TMDB info for movie {}", r.getMovieId(), e);
                    }
                    return rd;
                }).toList();

        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setIsOwnProfile(isOwnProfile);

        model.addAttribute("profile", profile);
        model.addAttribute("profileReviews", reviewDtos);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("activeTab", "reviews");
        
        return "user-reviews";
    }

    @GetMapping("/{username}/likes")
    public String showLikes(@PathVariable String username, HttpSession session, Model model) {
        log.debug("showLikes for {}", username);
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) return "redirect:/";
        
        User user = userOptional.get();
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        boolean isOwnProfile = loggedInUserId != null && loggedInUserId.equals(user.getId());

        populateProfileLikes(user, model);

        UserProfileDto profile = userMapper.toProfileDto(user);
        profile.setIsOwnProfile(isOwnProfile);

        model.addAttribute("profile", profile);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("activeTab", "likes");

        return "likes";
    }

    @GetMapping("/{username}/lists")
    public String showLists(@PathVariable String username) {
        return "redirect:/lists/user/" + username;
    }

    private void populateProfileLikes(User user, Model model) {
        List<com.clone.letterboxd.model.Like> allLikes = likeRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        List<Map<String, Object>> likedMovies = new ArrayList<>();
        List<com.clone.letterboxd.dto.ReviewDisplayDto> likedReviews = new ArrayList<>();

        for (com.clone.letterboxd.model.Like like : allLikes) {
            if (like.getMovieId() != null && like.getReview() == null && like.getDiaryEntry() == null) {
                try {
                    Map<String, Object> movieData = tmdbService.getMovieDetails(like.getMovieId());
                    if (movieData != null) {
                        likedMovies.add(movieData);
                    }
                } catch (Exception e) {
                    log.warn("Failed to load TMDB info for liked movie {}", like.getMovieId(), e);
                }
            } else if (like.getReview() != null) {
                var rd = ReviewMapper.toDisplayDto(like.getReview());
                try {
                    Map<String, Object> movieData = tmdbService.getMovieDetails(like.getReview().getMovieId());
                    if (movieData != null) {
                        rd.setMovieTitle((String) movieData.get("title"));
                        String poster = (String) movieData.get("poster_path");
                        if (poster != null) rd.setMoviePosterPath(poster);
                    }
                } catch (Exception e) {
                    log.warn("Failed to load TMDB info for movie in liked review {}", like.getReview().getMovieId(), e);
                }
                likedReviews.add(rd);
            }
        }

        model.addAttribute("likedMovies", likedMovies);
        model.addAttribute("likedReviews", likedReviews);
    }

    private Comparator<User> getComparator(String sort) {
        switch (sort) {
            case "username_za":
                return Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER).reversed();
            case "username_az":
                return Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER);
            case "joined_asc":
                return Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "joined_desc":
            default:
                return Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed();
        }
    }

    private List<FilmListSummaryDto> buildSummaryList(List<FilmList> lists) {
        if (lists == null) return List.of();
        return lists.stream().map(this::toSummaryDto).toList();
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

    @GetMapping("/edit")
    public String editProfilePage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        User user = userOptional.get();
        UserUpdateDto updateDto = new UserUpdateDto();
        updateDto.setDisplayName(user.getDisplayName());
        updateDto.setBio(user.getBio());
        // updateDto.setAvatarUrl(user.getAvatarUrl()); // Removed
        
        model.addAttribute("userUpdate", updateDto);
        model.addAttribute("username", user.getUsername());
        model.addAttribute("avatarUrl", UserMapper.getAvatarUrl(user));
        
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(
            HttpSession session,
            UserUpdateDto userUpdate,
            Model model) {
        log.debug("updateProfile called");
        Long userId = (Long) session.getAttribute("loggedInUserId");
        
        if (userId == null) {
            log.debug("updateProfile attempted without login");
            return "redirect:/auth/login";
        }

        Optional<User> userOptional = userRepository.findById(userId);
        
        if (userOptional.isEmpty()) {
            return "redirect:/auth/logout";
        }

        try {
            User user = userOptional.get();
            
            if (userUpdate.getDisplayName() != null) user.setDisplayName(userUpdate.getDisplayName());
            if (userUpdate.getBio() != null) user.setBio(userUpdate.getBio());
            
            if (userUpdate.getAvatarFile() != null && !userUpdate.getAvatarFile().isEmpty()) {
                user.setAvatarBytes(userUpdate.getAvatarFile().getBytes());
                user.setAvatarContentType(userUpdate.getAvatarFile().getContentType());
            } else if (userUpdate.isRemoveAvatar()) {
                user.setAvatarBytes(null);
                user.setAvatarContentType(null);
            }
            
            userRepository.save(user);
            
            model.addAttribute("success", "Profile updated successfully!");
            return "redirect:/profile";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error updating profile: " + e.getMessage());
            model.addAttribute("userUpdate", userUpdate);
            return "profile-edit";
        }
    }

    @PostMapping("/{username}/follow")
    @Transactional
    public String followUser(
            @PathVariable String username,
            HttpSession session) {
        
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        
        if (loggedInUserId == null) {
            return "redirect:/auth/login";
        }
        
        Optional<User> loggedInUserOpt = userRepository.findById(loggedInUserId);
        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        
        if (loggedInUserOpt.isEmpty() || targetUserOpt.isEmpty()) {
            return "redirect:/profile/" + username;
        }
        
        User loggedInUser = loggedInUserOpt.get();
        User targetUser = targetUserOpt.get();
        
        // Can't follow yourself
        if (loggedInUser.getId().equals(targetUser.getId())) {
            log.debug("User {} attempted to follow themself", loggedInUserId);
            return "redirect:/profile/" + username;
        }

        // perform follow by direct insert rather than touching the set
        userRepository.addFollow(loggedInUser.getId(), targetUser.getId());
        log.info("User {} now follows {}", loggedInUserId, targetUser.getId());
        
        return "redirect:/profile/" + username;
    }

    @PostMapping("/{username}/unfollow")
    public String unfollowUser(
            @PathVariable String username,
            HttpSession session) {
        
        Long loggedInUserId = (Long) session.getAttribute("loggedInUserId");
        if (loggedInUserId == null) {
            log.debug("unfollowUser called without login");
            return "redirect:/auth/login";
        }

        Optional<User> targetUserOpt = userRepository.findByUsername(username);
        if (targetUserOpt.isEmpty()) {
            log.warn("unfollowUser: target {} not found", username);
            return "redirect:/profile/" + username;
        }

        User targetUser = targetUserOpt.get();
        userRepository.removeFollow(loggedInUserId, targetUser.getId());
        log.info("User {} unfollowed {}", loggedInUserId, targetUser.getId());
        
        return "redirect:/profile/" + username;
    }
}
