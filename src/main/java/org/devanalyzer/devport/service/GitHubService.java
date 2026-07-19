package org.devanalyzer.devport.service;

import org.devanalyzer.devport.dto.GitHubData;
import org.devanalyzer.devport.dto.GithubRepoDto;
import org.devanalyzer.devport.dto.GithubUserDto;
import org.devanalyzer.devport.exception.RateLimitException;
import org.devanalyzer.devport.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class GitHubService {

    private static final Logger log = LoggerFactory.getLogger(GitHubService.class);
    private static final String BASE_URL = "https://api.github.com";

    private final RestTemplate restTemplate;

    public GitHubService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ------------------------------------------------------------------ //
    //  Cached aggregate fetch — single entry point for the orchestrator   //
    // ------------------------------------------------------------------ //

    /**
     * Fetches <em>all</em> GitHub data for a user in one call:
     * profile, repos, and commit activity for the top-5 repos.
     * <p>
     * Cached by username for 15 minutes (see {@code CacheConfig}).
     * If this method body executes, it means the cache was missed.
     *
     * @param username the GitHub handle
     * @return a {@link GitHubData} bundle
     */
    @Cacheable(value = "githubData", key = "#username")
    public GitHubData fetchAllGitHubData(String username) {
        log.info("GitHub data cache MISS for user '{}' — fetching from API", username);

        GithubUserDto userProfile = fetchUserProfile(username);
        List<GithubRepoDto> repos = fetchUserRepos(username);
        if (repos == null) repos = List.of();

        // Commit activity for top-5 repos (by stars)
        List<GithubRepoDto> topRepos = repos.stream()
                .sorted(Comparator.comparingInt(
                        (GithubRepoDto r) -> r.getStargazerCount() != null ? r.getStargazerCount() : 0)
                        .reversed())
                .limit(5)
                .toList();

        int totalCommits = 0;
        for (GithubRepoDto repo : topRepos) {
            totalCommits += fetchCommitActivity(username, repo.getName());
        }

        log.info("GitHub data fetched for '{}': {} repos, {} commits (top 5)",
                username, repos.size(), totalCommits);

        return new GitHubData(userProfile, repos, totalCommits);
    }

    // ------------------------------------------------------------------ //
    //  Individual fetch methods (called internally, NOT cached directly)  //
    // ------------------------------------------------------------------ //

    /**
     * Fetches up to 100 public repositories for the given GitHub user.
     */
    public List<GithubRepoDto> fetchUserRepos(String username) {
        String url = BASE_URL + "/users/{username}/repos?per_page=100";

        try {
            ResponseEntity<List<GithubRepoDto>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {},
                    username
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            handleGitHubError(e, username);
            return List.of();
        }
    }

    /**
     * Fetches the number of commits in a repository for the last 6 months.
     * Uses per_page=1 and reads the total count from the response size heuristic.
     */
    public Integer fetchCommitActivity(String username, String repoName) {
        String sixMonthsAgo = Instant.now().minus(180, ChronoUnit.DAYS).toString();
        String url = BASE_URL + "/repos/{username}/{repoName}/commits?since={since}&per_page=1";

        try {
            ResponseEntity<List<Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {},
                    username,
                    repoName,
                    sixMonthsAgo
            );

            // Parse the Link header for the last page number to get total commit count
            String linkHeader = response.getHeaders().getFirst("Link");
            if (linkHeader != null && linkHeader.contains("rel=\"last\"")) {
                return parseTotalFromLinkHeader(linkHeader);
            }

            // If no Link header, the response body itself is the total
            List<Object> body = response.getBody();
            return (body != null) ? body.size() : 0;

        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.CONFLICT) {
                // 409 CONFLICT = empty repository (no commits)
                return 0;
            }
            handleGitHubError(e, username);
            return 0;
        }
    }

    /**
     * Fetches the public profile for the given GitHub user.
     */
    public GithubUserDto fetchUserProfile(String username) {
        String url = BASE_URL + "/users/{username}";

        try {
            ResponseEntity<GithubUserDto> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {},
                    username
            );
            return response.getBody();
        } catch (HttpClientErrorException e) {
            handleGitHubError(e, username);
            return null;
        }
    }

    /**
     * Central error handler that maps HTTP status codes to custom exceptions.
     */
    private void handleGitHubError(HttpClientErrorException e, String username) {
        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new UserNotFoundException(username);
        }
        if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
            throw new RateLimitException();
        }
        throw e;
    }

    /**
     * Parses the GitHub pagination Link header to extract the total number of items.
     * Example Link header:
     * <https://api.github.com/...?page=42>; rel="last"
     */
    private Integer parseTotalFromLinkHeader(String linkHeader) {
        try {
            String[] links = linkHeader.split(",");
            for (String link : links) {
                if (link.contains("rel=\"last\"")) {
                    int pageIdx = link.indexOf("page=");
                    int endIdx = link.indexOf(">", pageIdx);
                    String pageNumber = link.substring(pageIdx + 5, endIdx);
                    return Integer.parseInt(pageNumber);
                }
            }
        } catch (Exception ignored) {
            // Fallback if parsing fails
        }
        return 1;
    }
}

