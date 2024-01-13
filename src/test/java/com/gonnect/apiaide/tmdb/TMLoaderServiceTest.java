package com.gonnect.apiaide.tmdb;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TMLoaderServiceTest {

    @Test
    void loadTMDBQueries() throws IOException {
        // Given
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource tmdbResource = resourceLoader.getResource("classpath:datasets/tmdb.json");
        TMLoaderService tmLoaderService = new TMLoaderService(resourceLoader);
        tmLoaderService.setTmdbResource(tmdbResource);

        // When
        String tmdbQueries = tmLoaderService.loadTMDBQueries();

        // Then
        assertNotNull(tmdbQueries);
    }

    @Test
    void loadOAS() throws IOException {
        // Given
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        Resource oasResource = resourceLoader.getResource("classpath:oas/tmdb_oas.json");
        TMLoaderService tmLoaderService = new TMLoaderService(resourceLoader);
        tmLoaderService.setOasResource(oasResource);

        // When
        String oasContent = tmLoaderService.loadOAS();

        // Then
        assertNotNull(oasContent);
    }


}

