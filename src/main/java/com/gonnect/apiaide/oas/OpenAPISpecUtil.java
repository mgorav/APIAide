package com.gonnect.apiaide.oas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class OpenAPISpecUtil {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Map<String, String>> endpoints = new ConcurrentHashMap<>();


    public List<String> getEndpoints() {
        List<String> result = new ArrayList<>();
        endpoints.forEach((path, operations) ->
                operations.forEach((method, operation) ->
                        result.add(method + " " + path)));
        return result;
    }

    @SneakyThrows
    public String simplifyJson(String json) {
        Object obj = simplifyJson(mapper.readValue(json, Object.class));
        return mapper.writeValueAsString(obj);
    }

    private Object simplifyJson(Object obj) {
        if (obj instanceof Map) {
            return ((Map<String, Object>) obj).entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> simplifyJson(entry.getValue())));
        } else if (obj instanceof List) {
            List<Object> json = (List<Object>) obj;
            return json.size() == 1 ? Collections.singletonList(simplifyJson(json.get(0))) :
                    Arrays.asList(simplifyJson(json.get(0)), simplifyJson(json.get(1)));
        } else {
            return obj;
        }
    }

    @SneakyThrows
    public String fixJsonError(String data) {
        data = data.trim().replace("\"", "").replace("`", "").replace(",", "");
        try {
            mapper.readTree(data);
            return data;
        } catch (JsonProcessingException e) {
            String[] lines = fixJsonFormat(data.split("\\n"));
            return String.join(" ", lines);
        }
    }

    private String[] fixJsonFormat(String[] lines) {
        lines = formatLines(lines);
        return fixCommas(lines);
    }

    private String[] formatLines(String[] lines) {
        return Arrays.stream(lines)
                .map(String::trim)
                .filter(line -> !(line.isEmpty() || line.matches("[\\[\\]{},]") || line.endsWith("[") || line.endsWith("]")))
                .map(line -> line.endsWith(",") || Arrays.asList("]", "}").contains(lines[Arrays.asList(lines).indexOf(line) + 1]) ? line : line + ",")
                .toArray(String[]::new);
    }

    private String[] fixCommas(String[] lines) {
        return Arrays.stream(lines)
                .map(line -> Arrays.asList("]", "}").contains(lines[Arrays.asList(lines).indexOf(line) + 1]) && line.endsWith(",") ? line.substring(0, line.length() - 1) : line)
                .toArray(String[]::new);
    }

    public List<String> getMatchedEndpoints(OpenAPISpecUtil spec, String plan) {
        Pattern pattern = Pattern.compile("\\b(GET|POST|PATCH|DELETE|PUT)\\s+(/\\S*)*");
        List<String> planEndpoints = new ArrayList<>();
        Matcher matcher = pattern.matcher(plan);
        while (matcher.find()) {
            String method = matcher.group(1);
            String route = matcher.group(2).split("\\?")[0];
            planEndpoints.add(method + " " + route);
        }

        List<String> specEndpoints = spec.getEndpoints();

        return planEndpoints.stream()
                .filter(specEndpoints::contains)
                .collect(Collectors.toList());
    }

    public String getOperation(String endpoint) {
        String[] elements = endpoint.split("\\s+");
        String method = elements[0];
        String path = elements[1];

        return endpoints.getOrDefault(path, new ConcurrentHashMap<>()).get(method);
    }

    public void addEndpoint(String path, String method, String operation) {
        endpoints.computeIfAbsent(path, k -> new ConcurrentHashMap<>())
                .put(method, operation);
    }

    public void removeEndpoint(String path, String method) {
        Map<String, String> operations = endpoints.get(path);
        if (operations != null) {
            operations.remove(method);
        }
    }
}
