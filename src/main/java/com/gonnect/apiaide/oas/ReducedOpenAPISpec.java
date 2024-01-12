package com.gonnect.apiaide.oas;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class ReducedOpenAPISpec {
    public List<Map<String, Object>> servers;
    public String description;
    public List<Map<String, Object>> endpoints;
}