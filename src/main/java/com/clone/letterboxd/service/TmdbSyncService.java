package com.clone.letterboxd.service;

import com.clone.letterboxd.repository.DiaryEntryRepository;
import com.clone.letterboxd.repository.FilmListRepository;
import com.clone.letterboxd.repository.LikeRepository;
import com.clone.letterboxd.repository.ReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
@Slf4j
public class TmdbSyncService {

    private final TmdbService tmdbService;
    private final ReviewRepository reviewRepository;
    private final DiaryEntryRepository diaryEntryRepository;
    private final LikeRepository likeRepository;
    private final FilmListRepository filmListRepository;
    private final CacheManager cacheManager;

    public TmdbSyncService(TmdbService tmdbService,
                           ReviewRepository reviewRepository,
                           DiaryEntryRepository diaryEntryRepository,
                           LikeRepository likeRepository,
                           FilmListRepository filmListRepository,
                           CacheManager cacheManager) {
        this.tmdbService = tmdbService;
        this.reviewRepository = reviewRepository;
        this.diaryEntryRepository = diaryEntryRepository;
        this.likeRepository = likeRepository;
        this.filmListRepository = filmListRepository;
        this.cacheManager = cacheManager;
    }

    /**
     * Daily synchronization job that runs everyday at midnight (12 AM).
     * Refreshes metadata for all movies that have user interactions (reviews, likes, diary entries, or in lists).
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void syncFilmsWithTmdb() {
        log.info("Starting daily midnight TMDB synchronization job...");
        
        Set<Long> uniqueMovieIds = new HashSet<>();
        
        try {
            // Collect all unique movie IDs from various repositories
            uniqueMovieIds.addAll(reviewRepository.findAllMovieIds());
            uniqueMovieIds.addAll(diaryEntryRepository.findAllMovieIds());
            uniqueMovieIds.addAll(likeRepository.findAllMovieIds());
            uniqueMovieIds.addAll(filmListRepository.findAllMovieIdsInLists());
            
            log.info("Found {} unique movies with user interactions to synchronize.", uniqueMovieIds.size());
            
            // Clear the movieDetails cache to ensure we get fresh data from TMDB
            Cache movieCache = cacheManager.getCache("movieDetails");
            if (movieCache != null) {
                log.debug("Clearing 'movieDetails' cache for fresh synchronization.");
                movieCache.clear();
            }
            
            int successCount = 0;
            for (Long movieId : uniqueMovieIds) {
                if (movieId != null) {
                    try {
                        // Refresh metadata from TMDB
                        tmdbService.getMovieDetails(movieId);
                        successCount++;
                    } catch (Exception e) {
                        log.error("Failed to sync movie ID {}: {}", movieId, e.getMessage());
                    }
                }
            }
            
            log.info("TMDB synchronization job completed. Successfully refreshed {}/{} movies.", 
                    successCount, uniqueMovieIds.size());
            
        } catch (Exception e) {
            log.error("Fatal error during TMDB synchronization job", e);
        }
    }
}
