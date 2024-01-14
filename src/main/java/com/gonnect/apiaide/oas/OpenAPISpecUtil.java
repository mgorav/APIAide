package com.gonnect.apiaide.oas;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    public record EndpointInfo(String method, String path) {
    }

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

//    public String getOperation(String endpoint) {
//        // Trim endpoint string
//        String trimmedEndpoint = endpoint.trim();
//
//        Optional<EndpointInfo> endpointInfo = parseEndpoint(trimmedEndpoint);
//
//        String[] elements = trimmedEndpoint.split("\\s+", 2);
//
//        // Trim method
//        String method = elements[0].trim();
//
//        if (elements.length > 1) {
//            elements[1] = elements[1].split("\\?", 2)[0];
//        }
//
//        // Trim path
//        String path = elements[1].trim();
//
//        Optional<String> matchedOperation = endpoints.stream()
//                .filter(endpointMap -> {
//                    // Check for null and then trim and make case insensitive comparison
//                    log.info("endpointMap.get(\"name\")=" + endpointMap.get("name"));
//                    String trimmedName = (endpointMap.get("name") != null) ? endpointMap.get("name").toString().trim() : null;
//
//                    String[] trimmedNameSplit = trimmedName.split("\\s+", 2);
//
//                    // Trim method
//                    String trimmedMethod = elements[0].trim();
//
//                    if (trimmedNameSplit.length > 1) {
//                        trimmedNameSplit[1] = trimmedNameSplit[1].split("\\?", 2)[0];
//                    }
//
//                    // Trim path
//                    String trimmedPath = elements[1].trim();
//
//
//                    boolean flag = path.equalsIgnoreCase(trimmedPath);
//
//                    log.info("path.equalsIgnoreCase(trimmedName)=" + flag);
//
//                    return flag;
//                })
//                .findFirst()
//                .map(endpointMap -> {
//                    log.debug("Matched endpoint: " + endpointMap);
//
//                    // Trim operation
//                    Object operationObject = endpointMap.get(method);
//
//                    if (operationObject == null) {
//                        log.warn("No operation docs found for " + method);
//                        return "No docs for this operation";
//                    }
//
//                    String operation = operationObject.toString().trim();
//
//                    log.debug("Found operation: " + operation);
//
//                    return operation;
//                });
//
//        return matchedOperation.orElseGet(() -> {
//            log.error("No matching endpoint found for path: " + path);
//            return null;
//        });
//    }

    public String getOperation(String endpoint) {
        String trimmedEndpoint = endpoint.trim();
        Optional<EndpointInfo> endpointInfo = parseEndpoint(trimmedEndpoint);

        String[] elements = splitEndpoint(trimmedEndpoint);

        String method = elements[0].trim();
        String path = elements[1].trim();

        Optional<String> matchedOperation = findMatchingEndpoint(method, path);

        return matchedOperation.orElseGet(() -> {
            log.error("No matching endpoint found for path: " + path);
            return null;
        });
    }

    private String[] splitEndpoint(String endpoint) {
        String[] elements = endpoint.split("\\s+", 2);

        if (elements.length > 1) {
            elements[1] = elements[1].split("\\?", 2)[0];
        }

        return elements;
    }

    private Optional<String> findMatchingEndpoint(String method, String path) {
        return endpoints.stream()
                .filter(endpointMap -> {
                    String trimmedName = getTrimmedName(endpointMap);

                    boolean flag = path.equalsIgnoreCase(trimmedName);
                    log.info("path.equalsIgnoreCase(trimmedName)=" + flag);

                    return flag;
                })
                .findFirst()
                .map(endpointMap -> {
                    log.debug("Matched endpoint: " + endpointMap);
                    return getOperationFromMap(endpointMap, method);
                });
    }

    private String getTrimmedName(Map<String, Object> endpointMap) {
        String trimmedName = (endpointMap.get("name") != null) ? endpointMap.get("name").toString().trim() : null;
        String[] trimmedNameSplit = trimmedName.split("\\s+", 2);

        if (trimmedNameSplit.length > 1) {
            trimmedNameSplit[1] = trimmedNameSplit[1].split("\\?", 2)[0];
        }

        return trimmedNameSplit[1];
    }

    private String getOperationFromMap(Map<String, Object> endpointMap, String method) {
        Object operationObject = endpointMap.get(method);

        if (operationObject == null) {
            log.warn("No operation docs found for " + method);
            return "No docs for this operation";
        }

        String operation = operationObject.toString().trim();
        log.debug("Found operation: " + operation);

        return operation;
    }


    public void addEndpoint(String path, String method, String operation) {
        endpoints.add(Collections.singletonMap("name", path + " " + method));
    }

    public void removeEndpoint(String path, String method) {
        endpoints.removeIf(endpointMap -> (path + " " + method).equals(endpointMap.get("name")));
    }

    public static Optional<EndpointInfo> parseEndpoint(String trimmedEndpoint) {
        String[] elements = trimmedEndpoint.split("\\s+", 2);

        if (elements.length > 0) {
            String method = elements[0].trim();

            if (elements.length > 1) {
                String path = elements[1].split("\\?", 2)[0].trim();
                return Optional.of(new EndpointInfo(method, path));
            }
        }

        return Optional.empty();
    }

}
