package org.devanalyzer.devport.exception;

public class RateLimitException extends RuntimeException {

    public RateLimitException() {
        super("GitHub API rate limit exceeded. Please provide a GITHUB_TOKEN or try again later.");
    }
}
