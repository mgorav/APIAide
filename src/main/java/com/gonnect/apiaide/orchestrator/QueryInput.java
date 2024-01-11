package com.gonnect.apiaide.orchestrator;

import lombok.Data;

import java.util.Map;

@Data
public class QueryInput {

    private String query;

    private String scenario; // TMDB, Spotify etc

    private Map<String, String> parameters; // Additional parameters

}
