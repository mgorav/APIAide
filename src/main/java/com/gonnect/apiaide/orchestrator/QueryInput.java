package com.gonnect.apiaide.orchestrator;

import com.gonnect.apiaide.oas.ReducedOpenAPISpec;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
public class QueryInput {

    private String query;

    private String scenario; // TMDB, Spotify etc

    private ReducedOpenAPISpec apiSpec;

    private Map<String, String> parameters; // Additional parameters

    public QueryInput() {
        this.parameters = new HashMap<>();
    }
}
