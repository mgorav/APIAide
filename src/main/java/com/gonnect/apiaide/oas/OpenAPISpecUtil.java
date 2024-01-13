package com.gonnect.apiaide.oas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonnect.apiaide.tm.TMService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
public class OpenAPISpecUtil {

    private static final Logger log = LoggerFactory.getLogger(OpenAPISpecUtil.class);

    private final ObjectMapper mapper;
    private final List<Map<String, Object>> endpoints;

    public List<String> getEndpoints() {
        return endpoints.stream()
                .map(endpointMap -> (String) endpointMap.get("name"))
                .collect(Collectors.toList());
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
        String method = elements[0].trim();
        String path = elements[1].trim();

        return endpoints.stream()
                .peek(endpointMap -> log.debug("Checking: " + endpointMap.get("name")))
                .filter(endpointMap -> path.equals(endpointMap.get("name")))
                .findFirst()
                .map(endpointMap -> {
                    String result = (String) endpointMap.get(method);
                    log.debug(("Found result: " + result));
                    return result;
                })
                .orElse(null);

    }

    public void addEndpoint(String path, String method, String operation) {
        endpoints.add(Collections.singletonMap("name", path + " " + method));
    }

    public void removeEndpoint(String path, String method) {
        endpoints.removeIf(endpointMap -> (path + " " + method).equals(endpointMap.get("name")));
    }
}
