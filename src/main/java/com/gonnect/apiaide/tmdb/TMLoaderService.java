package com.gonnect.apiaide.tmdb;

import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Service
@Setter
public class TMLoaderService {
    private final ResourceLoader resourceLoader;

    @Value("classpath:datasets/tmdb.json")
    private Resource tmdbResource;

    @Value("classpath:oas/tmdb_oas.json")
    private Resource oasResource;

    public TMLoaderService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String loadTMDBQueries() {
        return loadResourceContent(tmdbResource, "TMDB queries");
    }

    public String loadOAS() {
        return loadResourceContent(oasResource, "OpenAPI Specification for TMDB");
    }

    private String loadResourceContent(Resource resource, String description) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new RuntimeException("Error loading " + description + " from file", e);
        }
    }
}
