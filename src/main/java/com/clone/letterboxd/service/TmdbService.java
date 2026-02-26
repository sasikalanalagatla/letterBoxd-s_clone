package com.clone.letterboxd.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;


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

    public Map<String, Object> getMovieDetails(Long movieId) {
        String url = baseUrl + "/movie/" + movieId +
                "?api_key=" + apiKey +
                "&language=en-US" +
                "&append_to_response=credits,videos,images";

        return executeGetRequest(url);
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
            throw new RuntimeException("TMDB request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error calling TMDB", e);
            throw new RuntimeException("Failed to communicate with TMDB", e);
        }
    }
}