package org.devanalyzer.devport.controller;

import org.devanalyzer.devport.dto.ResumeData;
import org.devanalyzer.devport.model.AnalysisResult;
import org.devanalyzer.devport.repository.AnalysisResultRepository;
import org.devanalyzer.devport.service.ProfileOrchestrationService;
import org.devanalyzer.devport.service.ResumeParserService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/analyze")
public class AnalysisController {

    private final ResumeParserService resumeParserService;
    private final ProfileOrchestrationService orchestrationService;
    private final AnalysisResultRepository analysisResultRepository;

    public AnalysisController(ResumeParserService resumeParserService,
                              ProfileOrchestrationService orchestrationService,
                              AnalysisResultRepository analysisResultRepository) {
        this.resumeParserService = resumeParserService;
        this.orchestrationService = orchestrationService;
        this.analysisResultRepository = analysisResultRepository;
    }

    // ------------------------------------------------------------------ //
    //  POST /api/analyze — kick off a full profile analysis               //
    // ------------------------------------------------------------------ //

    /**
     * Accepts a GitHub username and a PDF resume, creates a PROCESSING
     * record, extracts the resume synchronously (to avoid stream-closed
     * errors on the async thread), then hands off to the async
     * orchestration pipeline.
     *
     * @return the analysis ID and status ("PROCESSING")
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> startAnalysis(
            @RequestParam("githubUsername") String githubUsername,
            @RequestParam("resume") MultipartFile resume) throws IOException {

        // ── Validation ──────────────────────────────────────────────────
        if (githubUsername == null || githubUsername.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "githubUsername is required"));
        }
        if (resume.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Resume file is empty"));
        }

        String contentType = resume.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are accepted"));
        }

        // ── 1. Synchronous: persist a PROCESSING record ────────────────
        AnalysisResult record = AnalysisResult.builder()
                .githubUsername(githubUsername)
                .status("PROCESSING")
                .build();
        record = analysisResultRepository.save(record);

        // ── 2. Synchronous: parse resume (avoids stream-closed on async) 
        ResumeData resumeData = resumeParserService.parseResume(resume);

        // ── 3. Async: hand off to the orchestration service ─────────────
        orchestrationService.processProfileAnalysis(
                githubUsername, resumeData, record.getId());

        // ── 4. Return immediately ───────────────────────────────────────
        return ResponseEntity.ok(Map.of(
                "id", record.getId().toString(),
                "status", "PROCESSING"
        ));
    }

    // ------------------------------------------------------------------ //
    //  GET /api/analyze/{id} — poll analysis status / results             //
    // ------------------------------------------------------------------ //

    /**
     * Returns the complete {@link AnalysisResult} for the given ID so the
     * frontend can poll until status is COMPLETED or FAILED.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAnalysisResult(@PathVariable UUID id) {
        return analysisResultRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ------------------------------------------------------------------ //
    //  POST /api/analyze/resume — standalone resume parse (existing)       //
    // ------------------------------------------------------------------ //

    /**
     * Accepts a PDF resume upload, parses its content, and returns a
     * {@link ResumeData} containing the extracted text, detected skills,
     * and detected programming languages.
     */
    @PostMapping(value = "/resume", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Uploaded file is empty"));
        }

        // Check file size limit (5MB)
        long maxFileSize = 5 * 1024 * 1024; // 5MB in bytes
        if (file.getSize() > maxFileSize) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File size exceeds 5MB limit. Maximum allowed size is 5MB"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Only PDF files are accepted"));
        }

        ResumeData resumeData = resumeParserService.parseResume(file);

        return ResponseEntity.ok(resumeData);
    }
}
