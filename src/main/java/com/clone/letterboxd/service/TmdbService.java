package com.clone.letterboxd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import java.util.List;
import java.util.Map;

@Service
public class TmdbService {

    private static final Logger log = LoggerFactory.getLogger(TmdbService.class);

    @Value("${tmdb.api.key}")
    private String apiKey;

    @Value("${tmdb.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    @Autowired
    public TmdbService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<String, Object> getPopularMovies(int page) {
        String url = baseUrl + "/movie/popular" +
                "?api_key=" + apiKey +
                "&page=" + page +
                "&language=en-US";

        return executeGetRequest(url);
    }

    /**
     * Return a map of all TMDB genres (id &rarr; name).  Cached so we only hit the
     * API once per application lifetime.
     */
    @org.springframework.cache.annotation.Cacheable("genreMap")
    public Map<Integer, String> getGenreMap() {
        String url = baseUrl + "/genre/movie/list" +
                "?api_key=" + apiKey +
                "&language=en-US";
        Map<String, Object> resp = executeGetRequest(url);
        Map<Integer, String> map = new java.util.HashMap<>();
        if (resp != null && resp.containsKey("genres")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) resp.get("genres");
            for (Map<String, Object> g : list) {
                Number id = (Number) g.get("id");
                String name = (String) g.get("name");
                if (id != null && name != null) {
                    map.put(id.intValue(), name);
                }
            }
        }
        return map;
    }

    /**
     * Return a list of language codes supported by TMDB.
     */
    @org.springframework.cache.annotation.Cacheable("languages")
    public List<String> getLanguages() {
        String url = baseUrl + "/configuration/languages" +
                "?api_key=" + apiKey;
        List<Map<String, Object>> resp = executeListRequest(url);
        List<String> langs = new java.util.ArrayList<>();
        if (resp != null) {
            for (Map<String, Object> entry : resp) {
                Object code = entry.get("iso_639_1");
                if (code != null) {
                    langs.add(code.toString());
                }
            }
        }
        return langs;
    }

public Map<String, Object> discoverMovies(Integer page,                                              String year,
                                              String genre,
                                              String lang,
                                              Double minRating,
                                              String sortBy) {
        StringBuilder url = new StringBuilder(baseUrl + "/discover/movie");
        url.append("?api_key=").append(apiKey);
        if (page != null) {
            url.append("&page=").append(page);
        }
        url.append("&language=en-US");

        if (year != null && !year.isBlank()) {
            url.append("&primary_release_year=").append(year);
        }
        if (genre != null && !genre.isBlank()) {
            url.append("&with_genres=").append(UriUtils.encode(genre, "UTF-8"));
        }
        if (lang != null && !lang.isBlank()) {
            url.append("&with_original_language=").append(lang);
        }
        if (minRating != null && minRating > 0) {
            url.append("&vote_average.gte=").append(minRating);
        }
        if (sortBy != null && !sortBy.isBlank()) {
            url.append("&sort_by=").append(sortBy);
        }

        return executeGetRequest(url.toString());
    }

    public Map<String, Object> getTrendingMovies(String timeWindow, int page) {
        String url = baseUrl + "/trending/movie/" + timeWindow +
                "?api_key=" + apiKey +
                "&page=" + page +
                "&language=en-US";
        return executeGetRequest(url);
    }

    @Cacheable(value = "movieDetails", key = "#movieId")
    public Map<String, Object> getMovieDetails(Long movieId) {
        String url = baseUrl + "/movie/" + movieId +
            "?api_key=" + apiKey +
            "&language=en-US" +
            "&append_to_response=credits,videos,images,external_ids";

        return executeGetRequest(url);
    }

    /**
     * Return true if the given movie has a release date that is on or before today.
     * Useful for gating user actions like reviews/likes/ratings.
     */
    public boolean isMovieReleased(Long movieId) {
        Map<String, Object> details = getMovieDetails(movieId);
        if (details == null) return false;
        Object rdObj = details.get("release_date");
        if (!(rdObj instanceof String)) return false;
        String rd = ((String) rdObj).trim();
        if (rd.isEmpty()) return false;
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(rd);
            return !date.isAfter(java.time.LocalDate.now());
        } catch (java.time.format.DateTimeParseException e) {
            log.debug("Cannot parse release date '{}' for movie {}", rd, movieId);
            return false;
        }
    }

    public Map<String, Object> searchMovies(String query, int page) {
        String url = baseUrl + "/search/movie" +
                "?api_key=" + apiKey +
                "&query=" + UriUtils.encode(query, "UTF-8") +
                "&page=" + page +
                "&include_adult=false" +
                "&language=en-US";

        return executeGetRequest(url);
    }

    public Map<String, Object> searchWithFilters(String query,
                                                 Integer page,
                                                 String year,
                                                 String genre,
                                                 String lang,
                                                 Double minRating,
                                                 String sortBy) {
        StringBuilder url = new StringBuilder(baseUrl + "/discover/movie");
        url.append("?api_key=").append(apiKey);
        if (page != null) {
            url.append("&page=").append(page);
        }
        url.append("&language=en-US");

        if (query != null && !query.isBlank()) {
            url.append("&with_text_query=").append(UriUtils.encode(query, "UTF-8"));
        }
        if (year != null && !year.isBlank()) {
            url.append("&primary_release_year=").append(year);
        }
        if (genre != null && !genre.isBlank()) {
            url.append("&with_genres=").append(UriUtils.encode(genre, "UTF-8"));
        }
        if (lang != null && !lang.isBlank()) {
            url.append("&with_original_language=").append(lang);
        }
        if (minRating != null && minRating > 0) {
            url.append("&vote_average.gte=").append(minRating);
        }
        if (sortBy != null && !sortBy.isBlank()) {
            url.append("&sort_by=").append(sortBy);
        }

        return executeGetRequest(url.toString());
    }

    public Map<String, Object> getWatchProviders(Long movieId) {
        String url = baseUrl + "/movie/" + movieId + "/watch/providers" +
                "?api_key=" + apiKey;
        return executeGetRequest(url);
    }

    public Map<String, Object> getSimilarMovies(Long movieId) {
        String url = baseUrl + "/movie/" + movieId + "/similar" +
                "?api_key=" + apiKey +
                "&language=en-US";
        return executeGetRequest(url);
    }

    private Map<String, Object> executeGetRequest(String url) {
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            Map<String, Object> body = response.getBody();
            if (body == null || body.isEmpty()) {
                log.warn("TMDB returned empty body for URL: {}", url);
                throw new RuntimeException("Empty response from TMDB API");
            }
            return body;

        } catch (HttpClientErrorException e) {
            log.warn("TMDB API error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling TMDB", e);
            // return null so callers can gracefully handle absence of data
            return null;
        }
    }

    private List<Map<String, Object>> executeListRequest(String url) {
        try {
            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> body = response.getBody();
            if (body == null || body.isEmpty()) {
                log.warn("TMDB returned empty list for URL: {}", url);
                throw new RuntimeException("Empty response from TMDB API");
            }
            return body;
        } catch (HttpClientErrorException e) {
            log.warn("TMDB API error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error calling TMDB", e);
            return null;
        }
    }
}