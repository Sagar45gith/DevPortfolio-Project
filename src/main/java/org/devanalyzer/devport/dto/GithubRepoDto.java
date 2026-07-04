package org.devanalyzer.devport.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GithubRepoDto {

    private String name;

    private String language;

    @JsonProperty("stargazers_count")
    private Integer stargazerCount;

    @JsonProperty("forks_count")
    private Integer forksCount;

    private String description;

    @JsonProperty("updated_at")
    private String updatedAt;
}
