package com.gonnect.apiaide.tmdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

@Service
public class QueryLoaderService {

    @Value("classpath:datasets/tmdb.json")
    private final ResourceLoader resourceLoader;

    public QueryLoaderService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String loadTmdbQueries() {
        try {
            Resource resource = resourceLoader.getResource("classpath:datasets/tmdb.json");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        } catch (IOException e) {
            throw new RuntimeException("Error loading TMDB queries from file", e);
        }
    }
}
