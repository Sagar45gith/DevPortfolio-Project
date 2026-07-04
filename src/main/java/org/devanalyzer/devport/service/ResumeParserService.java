package org.devanalyzer.devport.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.devanalyzer.devport.dto.ResumeData;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ResumeParserService {

    /**
     * All keywords we scan for in a resume. Ordered so that multi-word /
     * longer tokens are checked first (e.g. "spring boot" before "spring",
     * "javascript" before "java") — although the regex word-boundary approach
     * handles overlap correctly regardless of order.
     */
    private static final List<String> SKILL_KEYWORDS = List.of(
            "spring boot", "spring", "spring mvc", "spring security",
            "node.js", "express.js",
            "javascript", "typescript", "react", "angular", "vue",
            "html", "css", "bootstrap", "tailwind", "graphql",
            "java", "python", "c++", "c#", "go", "golang","c/c++",
            "kotlin", "scala", "php", "ruby", "rust", "swift",
            "objective-c", "dart", "perl", "matlab",
            "sql", "postgresql", "mysql", "oracle", "mongodb", "redis",
            "elasticsearch", "hibernate", "jpa", "junit", "mockito",
            "rest api", "rest apis", "microservices", "oauth2", "jwt", "api",
            "docker", "kubernetes", "terraform", "ansible", "nginx",
            "aws", "azure", "gcp", "git", "github", "gitlab",
            "maven", "gradle", "jenkins", "ci/cd", "linux", "jira",
            "agile", "scrum", "kafka", "rabbitmq", "thymeleaf",
            "bootstrap", "tailwind css", "android", "flutter", "powershell",
            "bash", "shell scripting", ".net", "asp.net","aws","html","css"
    );

    /**
     * Subset of SKILL_KEYWORDS that are programming languages.
     */
    private static final Set<String> PROGRAMMING_LANGUAGES = Set.of(
            "java", "python", "c++", "c#", "javascript", "typescript",
            "sql", "go", "golang", "kotlin", "scala", "php", "ruby",
            "rust", "swift", "objective-c", "dart", "perl", "matlab",
            "cobol", "fortran", "elixir", "clojure", "haskell",
            "bash", "powershell","c/c++","r programming", "r studio", "r-studio","c programming"
    );

    /**
     * Parses a PDF resume and returns a {@link ResumeData} containing the
     * cleaned text, detected skills, and detected programming languages.
     *
     * @param file the uploaded PDF file
     * @return a fully populated {@link ResumeData}
     * @throws IOException if the file cannot be read or parsed
     */
    public ResumeData parseResume(MultipartFile file) throws IOException {
        String text = extractTextFromPdf(file);
        List<String> skills = extractSkills(text);
        List<String> languages = extractProgrammingLanguages(skills);
        return new ResumeData(text, skills, languages);
    }

    /**
     * Extracts all text from a PDF file, normalizes whitespace, and returns
     * the cleaned lowercase content.
     *
     * @param file the uploaded PDF file
     * @return cleaned, lowercase plain-text content of the PDF
     * @throws IOException if the file cannot be read or parsed
     */
    public String extractTextFromPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            return cleanText(rawText);
        }
    }

    /**
     * Scans the lowercased text for predefined skill keywords using
     * word-boundary regex to avoid partial matches (e.g. "java" will NOT
     * match inside "javascript").
     *
     * @param text the cleaned resume text (should already be lowercase)
     * @return list of matched skill keywords
     */
    public List<String> extractSkills(String text) {
        String lowerText = text.toLowerCase();
        List<String> matched = new ArrayList<>();

        for (String keyword : SKILL_KEYWORDS) {
            // Quote the keyword so special chars like "+" and "." are literal,
            // then wrap with \b word boundaries.
            // For "c++", \b around the quoted pattern won't work because "+"
            // is not a word character. Use a lookaround approach instead.
            Pattern pattern = buildKeywordPattern(keyword);
            if (pattern.matcher(lowerText).find()) {
                matched.add(keyword);
            }
        }

        return matched;
    }

    /**
     * Filters the detected skills list and returns only the entries that
     * are recognised programming languages.
     *
     * @param detectedSkills the full list of detected skills
     * @return a filtered list containing only programming languages
     */
    public List<String> extractProgrammingLanguages(List<String> detectedSkills) {
        return detectedSkills.stream()
                .filter(PROGRAMMING_LANGUAGES::contains)
                .toList();
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Converts text to lowercase and collapses all consecutive whitespace
     * (spaces, tabs, newlines) into single spaces.
     */
    private String cleanText(String text) {
        return text.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Builds a compiled {@link Pattern} that matches the given keyword as a
     * whole "word". For purely alphanumeric keywords we use {@code \b}
     * word-boundary anchors. For keywords containing non-word characters
     * (e.g. "c++", "node.js") we use lookaround assertions that check for
     * a non-alphanumeric character (or start/end of string) on each side.
     */
    private Pattern buildKeywordPattern(String keyword) {
        String quoted = Pattern.quote(keyword);
        boolean hasNonWordChar = !keyword.matches("[\\w\\s]+");

        if (hasNonWordChar) {
            // Lookaround: not preceded/followed by a word character
            return Pattern.compile("(?<![\\w])" + quoted + "(?![\\w])");
        }
        return Pattern.compile("\\b" + quoted + "\\b");
    }
}
