package org.devanalyzer.devport.dto;

import java.util.List;

/**
 * Bundles all GitHub data fetched for a single user into one cacheable unit.
 * Returned by {@code GitHubService.fetchAllGitHubData(username)}.
 *
 * @param userProfile the user's public profile
 * @param repos       all public repositories (up to 100)
 * @param totalCommits total commits across the top-5 repos in the last 6 months
 */
public record GitHubData(
        GithubUserDto userProfile,
        List<GithubRepoDto> repos,
        int totalCommits
) {
}
