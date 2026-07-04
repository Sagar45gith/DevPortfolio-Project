package org.devanalyzer.devport.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "analysis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "github_username", nullable = false)
    private String githubUsername;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "PROCESSING";

    @Column(name = "overall_score")
    private Integer overallScore;

    @Column(name = "repo_count")
    private Integer repoCount;

    @Column(name = "repo_count_score")
    private Integer repoCountScore;

    @Column(name = "commit_frequency_score")
    private Integer commitFrequencyScore;

    @Column(name = "language_diversity_score")
    private Integer languageDiversityScore;

    @Column(name = "project_documentation_score")
    private Integer projectDocumentationScore;

    @Column(name = "resume_match_score")
    private Integer resumeMatchScore;

    @Column(name = "profile_completeness_score")
    private Integer profileCompletenessScore;

    @Column(name = "stars_recognition_score")
    private Integer starsRecognitionScore;


    @Column(name = "suggestions", columnDefinition = "TEXT")
    private String suggestions;

    @Column(name = "analyzed_at", nullable = false, updatable = false)
    private LocalDateTime analyzedAt;

    @PrePersist
    protected void onCreate() {
        this.analyzedAt = LocalDateTime.now();
    }
}
