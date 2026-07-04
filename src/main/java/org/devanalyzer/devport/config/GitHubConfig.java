package org.devanalyzer.devport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Configuration
public class GitHubConfig {

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();

        String githubToken = System.getenv("GITHUB_TOKEN");

        if (githubToken != null && !githubToken.isBlank()) {
            ClientHttpRequestInterceptor authInterceptor = (request, body, execution) -> {
                request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + githubToken);
                request.getHeaders().set(HttpHeaders.ACCEPT, "application/vnd.github.v3+json");
                return execution.execute(request, body);
            };
            restTemplate.setInterceptors(List.of(authInterceptor));
        }

        return restTemplate;
    }
}
