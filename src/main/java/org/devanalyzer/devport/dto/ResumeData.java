package org.devanalyzer.devport.dto;

import java.util.List;

/**
 * DTO representing the parsed output of a resume PDF.
 * Contains the cleaned raw text, detected technical skills,
 * and detected programming languages.
 */
public record ResumeData(
        String rawText,
        List<String> detectedSkills,
        List<String> detectedLanguages
) {
}
