package org.devanalyzer.devport.repository;

import org.devanalyzer.devport.model.AnalysisResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AnalysisResultRepository extends JpaRepository<AnalysisResult, UUID> {

    List<AnalysisResult> findByGithubUsernameIgnoreCase(String githubUsername);

    List<AnalysisResult> findByGithubUsernameIgnoreCaseOrderByAnalyzedAtDesc(String githubUsername);
}
