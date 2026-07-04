package org.devanalyzer.devport.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeParserServiceTest {

    private final ResumeParserService resumeParserService = new ResumeParserService();

    @Test
    void extractSkills_detectsCommonLanguagesAndTools() {
        String resumeText = """
                Senior Software Engineer with experience in Java, Kotlin, Spring Boot,
                Spring Security, React, TypeScript, Node.js, Express.js, REST APIs,
                Docker, Kubernetes, PostgreSQL, MongoDB, Redis, AWS, GitHub, Jenkins,
                CI/CD, and Terraform.
                """;

        List<String> skills = resumeParserService.extractSkills(resumeText);

        assertTrue(skills.contains("java"));
        assertTrue(skills.contains("kotlin"));
        assertTrue(skills.contains("spring boot"));
        assertTrue(skills.contains("spring security"));
        assertTrue(skills.contains("react"));
        assertTrue(skills.contains("typescript"));
        assertTrue(skills.contains("node.js"));
        assertTrue(skills.contains("express.js"));
        assertTrue(skills.contains("rest apis"));
        assertTrue(skills.contains("docker"));
        assertTrue(skills.contains("kubernetes"));
        assertTrue(skills.contains("postgresql"));
        assertTrue(skills.contains("mongodb"));
        assertTrue(skills.contains("redis"));
        assertTrue(skills.contains("aws"));
        assertTrue(skills.contains("github"));
        assertTrue(skills.contains("jenkins"));
        assertTrue(skills.contains("ci/cd"));
        assertTrue(skills.contains("terraform"));
        assertTrue(skills.contains("spring"));
    }

    @Test
    void extractProgrammingLanguages_filtersOnlyLanguages() {
        List<String> detectedSkills = List.of(
                "java", "spring boot", "kotlin", "docker", "typescript", "go", "bash", "aws", "postgresql"
        );

        List<String> languages = resumeParserService.extractProgrammingLanguages(detectedSkills);

        assertEquals(List.of("java", "kotlin", "typescript", "go", "bash"), languages);
    }
}
