package com.gonnect.apiaide.tmdb;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class QueryLoaderServiceTest {

    @Test
    void loadTMDBQueries() {
        // Given
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        QueryLoaderService queryLoaderService = new QueryLoaderService(resourceLoader);
        queryLoaderService.setResource(resourceLoader.getResource("classpath:datasets/tmdb.json"));

        // When
        String tmdbQueries = queryLoaderService.loadTMDBQueries();

        // Then
        assertNotNull(tmdbQueries);
    }
}

