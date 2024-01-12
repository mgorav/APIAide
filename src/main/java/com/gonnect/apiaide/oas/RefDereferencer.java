package com.gonnect.apiaide.oas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RefDereferencer {

    public static Object dereferenceRefsHelper(Object obj, Map<String, Object> fullSpec) {
        if (obj instanceof Map<?, ?>) {
            Map<String, Object> objOut = new HashMap<>();
            Map<String, Object> objMap = (Map<String, Object>) obj;
            for (Map.Entry<String, Object> entry : objMap.entrySet()) {
                String k = entry.getKey();
                Object v = entry.getValue();
                if ("$ref".equals(k)) {
                    return dereferenceRefsHelper(retrieveRefPath((String) v, fullSpec), fullSpec);
                } else if (v instanceof List<?>) {
                    objOut.put(k, dereferenceRefsHelperList((List<Object>) v, fullSpec));
                } else if (v instanceof Map<?, ?>) {
                    objOut.put(k, dereferenceRefsHelper(v, fullSpec));
                } else {
                    objOut.put(k, v);
                }
            }
            return objOut;
        } else if (obj instanceof List<?>) {
            return dereferenceRefsHelperList((List<Object>) obj, fullSpec);
        } else {
            return obj;
        }
    }

    static List<Object> dereferenceRefsHelperList(List<Object> list, Map<String, Object> fullSpec) {
        return list.stream()
                .map(el -> dereferenceRefsHelper(el, fullSpec))
                .collect(Collectors.toList());
    }

    static Map<String, Object> retrieveRefPath(String path, Map<String, Object> fullSpec) {
        String[] components = path.split("/");
        if (!"#".equals(components[0])) {
            throw new RuntimeException("All $refs I've seen so far are uri fragments (start with hash).");
        }
        Map<String, Object> out = fullSpec;
        for (int i = 1; i < components.length; i++) {
            out = (Map<String, Object>) out.get(components[i]);
        }
        return out;
    }

    static List<Map<String, Object>> dereferenceEndpoints(List<Map<String, Object>> endpoints, Map<String, Object> fullSpec) {
        return endpoints.stream()
                .map(endpoint -> Map.of(
                        "name", endpoint.get("name"),
                        "description", endpoint.get("description"),
                        "docs", dereferenceRefsHelper(endpoint.get("docs"), fullSpec)
                ))
                .collect(Collectors.toList());
    }

    static List<Map<String, Object>> reduceEndpoints(List<Map<String, Object>> endpoints, boolean onlyRequired) {
        return endpoints.stream()
                .map(endpoint -> Map.of(
                        "name", endpoint.get("name"),
                        "description", endpoint.get("description"),
                        "docs", reduceEndpointDocs((Map<String, Object>) endpoint.get("docs"), onlyRequired)
                ))
                .collect(Collectors.toList());
    }


    static Map<String, Object> reduceEndpointDocs(Map<String, Object> docs, boolean onlyRequired) {
        Map<String, Object> out = new HashMap<>();

        if(docs.containsKey("description")) {
            out.put("description", docs.get("description"));
        }

        if(docs.containsKey("parameters")) {
            out.put("parameters", reduceRequiredParameters((List<Map<String, Object>>)docs.get("parameters"), onlyRequired));
        }

        if(docs.containsKey("requestBody")) {
            out.put("requestBody", docs.get("requestBody"));
        }

        if(docs.containsKey("responses")) {
            out.put("responses", new HashMap<>());
        }

        return out;
    }

    static List<Map<String, Object>> reduceRequiredParameters(List<Map<String, Object>> parameters, boolean onlyRequired) {
        return parameters.stream()
                .filter(parameter -> !onlyRequired || (boolean) parameter.getOrDefault("required", false))
                .collect(Collectors.toList());
    }
}
