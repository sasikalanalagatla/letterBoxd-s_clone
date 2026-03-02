package com.clone.letterboxd.service;

import com.clone.letterboxd.dto.FilmListSummaryDto;
import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.enums.Visibility;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FeaturedListService {

    /**
     * Provides a complete list of featured lists organized by category.
     * Each featured list includes popular TMDB movie IDs for poster display.
     */
    public List<FeaturedListItem> getAllFeaturedLists() {
        return List.of(
                // ── CLASSIC & RATED LISTS ──
                new FeaturedListItem("IMDb Top 50", "The highest-rated films on IMDb", "🏆", "Classic Collections",
                        Arrays.asList(278L, 238L, 155L, 550L, 680L, 240L, 122L, 389L), 1240),
                new FeaturedListItem("All-Time Greatest Movies", "A curated selection of the greatest films ever made", "⭐", "Classic Collections",
                        Arrays.asList(550L, 680L, 278L, 238L, 240L, 122L, 389L, 155L), 870),
                new FeaturedListItem("Letterboxd Favorites", "Most-liked films on Letterboxd's", "💚", "Classic Collections",
                        Arrays.asList(278L, 238L, 550L, 680L, 389L, 155L, 240L, 122L), 930),
                new FeaturedListItem("Critics' Choice", "Films selected by top critics worldwide", "🎬", "Classic Collections",
                        Arrays.asList(238L, 278L, 680L, 240L, 122L, 155L, 389L, 550L), 760),
                new FeaturedListItem("Cult Classics", "Iconic films with devoted followings", "🎭", "Classic Collections",
                        Arrays.asList(603L, 155L, 680L, 550L, 278L, 240L, 122L, 13L), 510),
                new FeaturedListItem("Underrated Gems", "Hidden masterpieces worth discovering", "💎", "Classic Collections",
                        Arrays.asList(597L, 13L, 122L, 240L, 278L, 603L, 680L, 155L), 620),

                // ── GENRE LISTS ──
                new FeaturedListItem("Best Action Movies", "The most thrilling action films ever made", "💥", "Genre Favorites",
                        Arrays.asList(680L, 278L, 155L, 550L, 603L, 240L, 122L, 389L, 597L, 13L)),
                new FeaturedListItem("Best Romantic Movies", "Timeless tales of love and romance", "💕", "Genre Favorites",
                        Arrays.asList(597L, 13L, 122L, 240L, 278L, 603L, 680L, 155L, 550L, 389L)),
                new FeaturedListItem("Best Horror Movies", "Movies that will give you chills", "👻", "Genre Favorites",
                        Arrays.asList(155L, 550L, 278L, 680L, 240L, 122L, 389L, 603L, 597L, 13L), 340),
                new FeaturedListItem("Best Sci-Fi Movies", "Mind-bending science fiction masterpieces", "🚀", "Genre Favorites",
                        Arrays.asList(240L, 603L, 155L, 278L, 550L, 680L, 389L, 122L, 13L, 597L), 410),
                new FeaturedListItem("Best Comedy Movies", "Laugh-out-loud funny films", "😂", "Genre Favorites",
                        Arrays.asList(278L, 13L, 597L, 122L, 680L, 550L, 240L, 603L, 389L, 155L), 290),
                new FeaturedListItem("Best Thriller Movies", "Edge-of-your-seat thrillers", "🔪", "Genre Favorites",
                        Arrays.asList(550L, 278L, 680L, 238L, 155L, 603L, 389L, 122L, 240L, 597L), 360),

                // ── AWARD WINNERS ──
                new FeaturedListItem("Oscar Best Picture Winners", "Academy Award Best Picture winners", "🏅", "Award Winners",
                        Arrays.asList(238L, 122L, 278L, 240L, 389L, 155L, 597L, 680L, 550L, 603L), 510),
                new FeaturedListItem("Cannes Film Festival Winners", "Palme d'Or and award-winning films from Cannes", "🎪", "Award Winners",
                        Arrays.asList(550L, 680L, 278L, 238L, 155L, 122L, 389L, 240L, 597L, 603L), 430),
                new FeaturedListItem("Golden Globe Best Films", "Golden Globe Award winners", "✨", "Award Winners",
                        Arrays.asList(278L, 680L, 238L, 240L, 550L, 389L, 155L, 122L, 603L, 597L), 365),
                new FeaturedListItem("BAFTA Award Winners", "British Academy Film Award recipients", "🎖️", "Award Winners",
                        Arrays.asList(238L, 278L, 680L, 240L, 122L, 155L, 389L, 550L, 597L, 603L), 295),
                new FeaturedListItem("National Award Winners (India)", "Prestigious Indian national film awards", "🇮🇳", "Award Winners",
                        Arrays.asList(13L, 278L, 238L, 680L, 550L, 122L, 240L, 603L, 389L, 155L), 210),

                // ── REGIONAL & INTERNATIONAL ──
                new FeaturedListItem("Best Indian Movies of All Time", "The finest films from Indian cinema", "🎬", "Regional Cinema",
                        Arrays.asList(13L, 238L, 278L, 680L, 550L, 122L, 240L, 603L, 389L, 155L), 480),
                new FeaturedListItem("Bollywood Classics", "Classic films from Bollywood's golden and modern eras", "🌟", "Regional Cinema",
                        Arrays.asList(13L, 238L, 278L, 680L, 550L, 122L, 240L, 603L, 389L, 155L), 420),
                new FeaturedListItem("Top South Indian Movies", "Great films from Telugu, Tamil, Kannada, and Malayalam cinema", "🎭", "Regional Cinema",
                        Arrays.asList(13L, 238L, 278L, 680L, 550L, 122L, 240L, 603L, 389L, 155L), 360),
                new FeaturedListItem("Best Malayalam Cinema", "Outstanding films from Malayalam cinema", "🎥", "Regional Cinema",
                        Arrays.asList(13L, 238L, 278L, 680L, 550L, 122L, 240L, 603L, 389L, 155L), 310),
                new FeaturedListItem("Top Korean Movies", "Essential Korean films you must watch", "🌶️", "Regional Cinema",
                        Arrays.asList(278L, 238L, 680L, 550L, 122L, 240L, 603L, 389L, 155L, 13L), 290),
                new FeaturedListItem("World Cinema Essentials", "The best films from around the globe", "🌍", "Regional Cinema",
                        Arrays.asList(278L, 680L, 238L, 240L, 550L, 389L, 155L, 122L, 603L, 597L), 250)
        );
    }

    /**
     * Get featured lists limited to a specific count
     */
    public List<FeaturedListItem> getFeaturedListsLimited(int limit) {
        List<FeaturedListItem> all = getAllFeaturedLists();
        return all.stream().limit(limit).toList();
    }

    /**
     * Get featured lists by category
     */
    public Map<String, List<FeaturedListItem>> getFeaturedListsByCategory() {
        Map<String, List<FeaturedListItem>> categorizedLists = new LinkedHashMap<>();
        
        getAllFeaturedLists().forEach(item -> {
            categorizedLists.computeIfAbsent(item.getCategory(), k -> new ArrayList<>())
                    .add(item);
        });
        
        return categorizedLists;
    }

    /**
     * Convert a FeaturedListItem to FilmListSummaryDto for template rendering
     */
    public FilmListSummaryDto toSummaryDto(FeaturedListItem item) {
        FilmListSummaryDto dto = new FilmListSummaryDto();
        dto.setId(0L); // placeholder
        dto.setName(item.getName());
        dto.setSlug(item.getSlug());
        dto.setDescriptionExcerpt(item.getDescription());
        dto.setRanked(false);
        dto.setIsWatchlist(false);
        dto.setVisibility(Visibility.PUBLIC);
        
        // Create owner as "Official Lists"
        UserSummaryDto owner = new UserSummaryDto();
        owner.setUsername("Official Lists");
        owner.setDisplayName("Official Lists");
        dto.setOwner(owner);
        
        dto.setEntryCount(item.getMovieIds().size());
        dto.setLikeCount(item.getLikeCount());
        dto.setCommentCount(0);
        dto.setCurrentUserLiked(false);
        dto.setPreviewPosterPaths(Collections.emptyList()); // Will be populated with poster data later
        
        return dto;
    }

    /**
     * Inner class for holding featured list data
     */
    public static class FeaturedListItem {
        private final String name;
        private final String description;
        private final String emoji;
        private final String category;
        private final List<Long> movieIds;
        private final int likeCount;

        public FeaturedListItem(String name, String description, String emoji, String category) {
            this(name, description, emoji, category, Collections.emptyList(), 0);
        }

        public FeaturedListItem(String name, String description, String emoji, String category, List<Long> movieIds) {
            this(name, description, emoji, category, movieIds, 0);
        }

        public FeaturedListItem(String name, String description, String emoji, String category, List<Long> movieIds, int likeCount) {
            this.name = name;
            this.description = description;
            this.emoji = emoji;
            this.category = category;
            this.movieIds = movieIds != null ? movieIds : Collections.emptyList();
            this.likeCount = likeCount;
        }

        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getEmoji() { return emoji; }
        public String getCategory() { return category; }
        public List<Long> getMovieIds() { return movieIds; }
        public String getSlug() {
            // generate a simple slug from the name
            return name.toLowerCase()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("(^-|-$)", "");
        }

        public int getLikeCount() {
            return likeCount;
        }
    }
}
