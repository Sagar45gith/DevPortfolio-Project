package org.devanalyzer.devport.service;

import org.devanalyzer.devport.dto.GitHubData;
import org.devanalyzer.devport.dto.GithubRepoDto;
import org.devanalyzer.devport.dto.ResumeData;
import org.devanalyzer.devport.model.AnalysisResult;
import org.devanalyzer.devport.repository.AnalysisResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the full profile-analysis workflow:
 * <ol>
 *   <li>Fetches GitHub data (profile, repos, commit activity) — may be cached</li>
 *   <li>Runs every scoring method via {@link ScoringEngine}</li>
 *   <li>Persists the completed {@link AnalysisResult}</li>
 * </ol>
 * The main entry point {@link #processProfileAnalysis} runs on an async
 * thread so the client receives an immediate "PROCESSING" response.
 * <p>
 * <strong>This method is NOT cached</strong> — scoring always runs fresh
 * because the same GitHub username can be paired with a different resume.
 */
@Service
public class ProfileOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(ProfileOrchestrationService.class);

    private final GitHubService gitHubService;
    private final ScoringEngine scoringEngine;
    private final AnalysisResultRepository analysisResultRepository;

    public ProfileOrchestrationService(GitHubService gitHubService,
                                       ScoringEngine scoringEngine,
                                       AnalysisResultRepository analysisResultRepository) {
        this.gitHubService = gitHubService;
        this.scoringEngine = scoringEngine;
        this.analysisResultRepository = analysisResultRepository;
    }

    /**
     * Runs the full analysis pipeline asynchronously.
     *
     * @param githubUsername the GitHub handle to analyse
     * @param resumeData    pre-parsed resume (text + skills + languages)
     * @param analysisId    the ID of the already-persisted PROCESSING record
     */
    @Async
    public void processProfileAnalysis(String githubUsername,
                                       ResumeData resumeData,
                                       UUID analysisId) {
        try {
            // ── 1. Fetch GitHub data (cached by GitHubService) ──────────
            GitHubData gitHubData = gitHubService.fetchAllGitHubData(githubUsername);

            List<GithubRepoDto> repos = gitHubData.repos();
            int totalCommits = gitHubData.totalCommits();

            log.info("Using GitHub data for '{}': {} repos, {} commits",
                    githubUsername, repos.size(), totalCommits);

            // ── 2. Calculate every score (ALWAYS fresh) ─────────────────
            int repoCountScore       = scoringEngine.calculateRepoCountScore(repos.size());
            int commitFreqScore      = scoringEngine.calculateCommitFrequencyScore(totalCommits);
            int langDiversityScore   = scoringEngine.calculateLanguageDiversityScore(repos);
            int projDocScore         = scoringEngine.calculateProjectDocumentationScore(repos);
            int resumeMatchScore     = scoringEngine.calculateResumeMatchScore(
                    resumeData.detectedSkills(), repos);
            int profileComplScore    = scoringEngine.calculateProfileCompletenessScore(gitHubData.userProfile());
            int starsScore           = scoringEngine.calculateStarsRecognitionScore(repos);

            int overallScore = repoCountScore
                    + commitFreqScore
                    + langDiversityScore
                    + projDocScore
                    + resumeMatchScore
                    + profileComplScore
                    + starsScore;

            // ── 3. Generate suggestions ─────────────────────────────────
            List<String> suggestions = scoringEngine.generateSuggestions(
                    repos.size(),
                    commitFreqScore,
                    langDiversityScore,
                    projDocScore,
                    resumeMatchScore
            );

            // ── 4. Persist results ──────────────────────────────────────
            AnalysisResult result = analysisResultRepository.findById(analysisId)
                    .orElseThrow(() -> new IllegalStateException(
                            "AnalysisResult not found for id: " + analysisId));

            result.setStatus("COMPLETED");
            result.setOverallScore(overallScore);
            result.setRepoCount(repos.size());
            result.setRepoCountScore(repoCountScore);
            result.setCommitFrequencyScore(commitFreqScore);
            result.setLanguageDiversityScore(langDiversityScore);
            result.setProjectDocumentationScore(projDocScore);
            result.setResumeMatchScore(resumeMatchScore);
            result.setProfileCompletenessScore(profileComplScore);
            result.setStarsRecognitionScore(starsScore);
            result.setSuggestions(String.join("||", suggestions));

            analysisResultRepository.save(result);

            log.info("Analysis completed for {} — overall score: {}", githubUsername, overallScore);

        } catch (Exception ex) {
            log.error("Analysis failed for {}: {}", githubUsername, ex.getMessage(), ex);

            analysisResultRepository.findById(analysisId).ifPresent(result -> {
                result.setStatus("FAILED");
                result.setSuggestions("Analysis failed: " + ex.getMessage());
                analysisResultRepository.save(result);
            });
        }
    }
}
