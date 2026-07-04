package org.devanalyzer.devport.service;

import org.devanalyzer.devport.dto.GithubRepoDto;
import org.devanalyzer.devport.dto.GithubUserDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Core scoring engine that evaluates a developer's profile across seven
 * dimensions, producing a total score out of 100 and a list of
 * actionable suggestions.
 */
@Service
public class ScoringEngine {

    // ------------------------------------------------------------------ //
    //  1. Repo Count Score  (max 15 pts)                                  //
    // ------------------------------------------------------------------ //

    /**
     * Scores based on the number of public repositories.
     * <ul>
     *   <li>0 repos → 0 pts</li>
     *   <li>1–3 repos → 5 pts</li>
     *   <li>4–7 repos → 10 pts</li>
     *   <li>8+ repos → 15 pts</li>
     * </ul>
     */
    public int calculateRepoCountScore(int repoCount) {
        if (repoCount >= 8) return 15;
        if (repoCount >= 4) return 10;
        if (repoCount >= 1) return 5;
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  2. Commit Frequency Score  (max 20 pts)                            //
    // ------------------------------------------------------------------ //

    /**
     * Scores based on total commits across the top 5 repos in the last
     * 6 months.
     * <ul>
     *   <li>0 commits → 0 pts</li>
     *   <li>1–10 → 5 pts</li>
     *   <li>11–30 → 10 pts</li>
     *   <li>31–60 → 15 pts</li>
     *   <li>60+ → 20 pts</li>
     * </ul>
     */
    public int calculateCommitFrequencyScore(int totalCommits) {
        if (totalCommits > 60) return 20;
        if (totalCommits >= 31) return 15;
        if (totalCommits >= 11) return 10;
        if (totalCommits >= 1) return 5;
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  3. Language Diversity Score  (max 15 pts)                           //
    // ------------------------------------------------------------------ //

    /**
     * Scores based on the number of unique languages used across repos.
     * <ul>
     *   <li>0 languages → 0 pts</li>
     *   <li>1 language → 5 pts</li>
     *   <li>2–3 → 10 pts</li>
     *   <li>4+ → 15 pts</li>
     * </ul>
     */
    public int calculateLanguageDiversityScore(List<GithubRepoDto> repos) {
        long uniqueLanguages = repos.stream()
                .map(GithubRepoDto::getLanguage)
                .filter(lang -> lang != null && !lang.isBlank())
                .distinct()
                .count();

        if (uniqueLanguages >= 4) return 15;
        if (uniqueLanguages >= 2) return 10;
        if (uniqueLanguages == 1) return 5;
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  4. Project Documentation Score  (max 10 pts)                       //
    // ------------------------------------------------------------------ //

    /**
     * Scores based on the percentage of repos that have a non-null,
     * non-blank description.
     * <ul>
     *   <li>&lt; 30% → 0 pts</li>
     *   <li>30–60% → 5 pts</li>
     *   <li>60%+ → 10 pts</li>
     * </ul>
     */
    public int calculateProjectDocumentationScore(List<GithubRepoDto> repos) {
        if (repos.isEmpty()) return 0;

        long withDescription = repos.stream()
                .filter(r -> r.getDescription() != null && !r.getDescription().isBlank())
                .count();

        double percentage = (double) withDescription / repos.size() * 100;

        if (percentage >= 60) return 10;
        if (percentage >= 30) return 5;
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  5. Resume–GitHub Match Score  (max 20 pts)                         //
    // ------------------------------------------------------------------ //

    /**
     * Compares the skills detected on the resume with the languages used
     * in GitHub repos. Each match earns 4 pts, capped at 20.
     */
    public int calculateResumeMatchScore(List<String> resumeSkills,
                                         List<GithubRepoDto> repos) {
        Set<String> repoLanguages = repos.stream()
                .map(GithubRepoDto::getLanguage)
                .filter(lang -> lang != null && !lang.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        int matches = 0;
        for (String skill : resumeSkills) {
            if (repoLanguages.contains(skill.toLowerCase())) {
                matches++;
            }
        }

        return Math.min(matches * 4, 20);
    }

    // ------------------------------------------------------------------ //
    //  6. Profile Completeness Score  (max 10 pts)                        //
    // ------------------------------------------------------------------ //

    /**
     * Scores profile completeness based on:
     * <ul>
     *   <li>Has bio → +5 pts</li>
     *   <li>Has 1+ followers → +3 pts</li>
     *   <li>Has "pinned" repos (publicRepos &ge; 3) → +2 pts</li>
     * </ul>
     */
    public int calculateProfileCompletenessScore(GithubUserDto userProfile) {
        if (userProfile == null) return 0;

        int score = 0;
        if (userProfile.getBio() != null && !userProfile.getBio().isBlank()) {
            score += 5;
        }
        if (userProfile.getFollowers() != null && userProfile.getFollowers() >= 1) {
            score += 3;
        }
        if (userProfile.getPublicRepos() != null && userProfile.getPublicRepos() >= 3) {
            score += 2;
        }
        return score;
    }

    // ------------------------------------------------------------------ //
    //  7. Stars & Recognition Score  (max 10 pts)                         //
    // ------------------------------------------------------------------ //

    /**
     * Scores based on total stargazer count across all repos.
     * <ul>
     *   <li>0 stars → 0 pts</li>
     *   <li>1–5 → 3 pts</li>
     *   <li>6–20 → 7 pts</li>
     *   <li>20+ → 10 pts</li>
     * </ul>
     */
    public int calculateStarsRecognitionScore(List<GithubRepoDto> repos) {
        int totalStars = repos.stream()
                .mapToInt(r -> r.getStargazerCount() != null ? r.getStargazerCount() : 0)
                .sum();

        if (totalStars > 20) return 10;
        if (totalStars >= 6) return 7;
        if (totalStars >= 1) return 3;
        return 0;
    }

    // ------------------------------------------------------------------ //
    //  Suggestions                                                        //
    // ------------------------------------------------------------------ //

    /**
     * Generates actionable improvement suggestions based on individual
     * score weaknesses.
     */
    public List<String> generateSuggestions(int repoCount,
                                            int commitFrequencyScore,
                                            int languageDiversityScore,
                                            int projectDocumentationScore,
                                            int resumeMatchScore) {

        List<String> suggestions = new ArrayList<>();

        if (commitFrequencyScore < 10) {
            suggestions.add("Your commit activity has been low in the last 6 months. "
                    + "Regular commits signal active learning.");
        }
        if (projectDocumentationScore < 5) {
            suggestions.add("Most of your repos lack descriptions. "
                    + "Add descriptions/READMEs to show project context.");
        }
        if (languageDiversityScore < 10) {
            suggestions.add("Consider expanding your language diversity "
                    + "beyond your primary stack.");
        }
        if (resumeMatchScore < 10) {
            suggestions.add("Your resume skills don't align well with your GitHub projects. "
                    + "Build projects that reflect your resume.");
        }
        if (repoCount < 5) {
            suggestions.add("You have fewer than 5 public repositories. "
                    + "More projects demonstrate consistent building.");
        }

        return suggestions;
    }
}
