package com.gonnect.apiaide.apiexecution;

import lombok.Data;

import java.util.Map;

@Data
public class APIExecutionRequest {

    private String background;
    private String plan;
    private String scenario; // TMDB, Spotify etc
    private Map<String, String> parameters; // Additional parameters

    public APIExecutionRequest() {
        this.parameters = Map.of();
    }

    public String getQuery() {
        // Return the value for the "query" field
        return parameters.get("query");
    }

    public String getApiDescription() {
        // Return the value for the "apiDescription" field
        return parameters.get("apiDescription");
    }

    public String getApiPath() {
        // Return the value for the "apiPath" field
        return parameters.get("apiPath");
    }

    public String getApiParam() {
        // Return the value for the "apiParam" field
        return parameters.get("apiParam");
    }

    public String getResponseDescription() {
        // Return the value for the "responseDescription" field
        return parameters.get("responseDescription");
    }

}
