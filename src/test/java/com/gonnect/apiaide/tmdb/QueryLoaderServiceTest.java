package com.gonnect.apiaide.tmdb;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class QueryLoaderServiceTest {

    @Test
    void loadTMDBQueries() {
        // Given
        ResourceLoader resourceLoader = new DefaultResourceLoader();
        QueryLoaderService queryLoaderService = new QueryLoaderService(resourceLoader);

        // When
        String tmdbQueries = queryLoaderService.loadTmdbQueries();

        // Then
        assertNotNull(tmdbQueries);
    }
}

