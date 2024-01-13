package com.gonnect.apiaide.oas;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class to process OpenAPI specifications.
 * Provides methods to:
 * - Parse YAML OpenAPI specs
 * - Dereference JSON references
 * - Merge "allOf" properties
 * - Reduce specs to simplified form
 * Reduced form contains:
 * - servers
 * - description
 * - Required endpoints only
 * - Required parameters only
 * - requestBody
 * - 200 response only
 * This helps focus retrieval on essential parts of the spec.
 */
public class OpenApiProcessor {
    /**
     * Parses a YAML OpenAPI spec and reduces it to simplified form.
     * <p>
     * Algorithm:
     * 1. Load the YAML spec into a Map using SnakeYAML
     * 2. Extract the paths and for each path, extract operations like GET, POST etc.
     * 3. Convert operations for each path to a Map with keys:
     * name: HTTP method + route
     * description: operation description
     * docs: operation details
     * 4. Add all operations Map to endpoints List
     * 5. If dereference=true, dereference $refs in docs by passing full spec
     * 6. If mergeAllOf=true, merge all "allOf" properties
     * 7. Reduce each endpoint docs:
     * - Only include required parameters
     * - Only include requestBody
     * - Only include 200 response
     * 8. Create reduced spec with:
     * servers
     * description
     * endpoints
     * 9. Return the ReducedOpenAPISpec
     *
     * @param yamlString   YAML OpenAPI spec string
     * @param dereference  True to dereference $refs
     * @param onlyRequired True to only include required params
     * @param mergeAllOf   True to merge allOf properties
     * @return Reduced OpenAPI spec
     */
    public static ReducedOpenAPISpec parseYamlOpenApiSpec(String yamlString, boolean dereference, boolean onlyRequired, boolean mergeAllOf) {
        Yaml yaml = new Yaml();
        Map<String, Object> spec = yaml.load(yamlString);
        return reduceOpenApiSpec(spec, dereference, onlyRequired, mergeAllOf);
    }

    public static ReducedOpenAPISpec parseYamlOpenApiSpec(InputStream yamlInputStream, boolean dereference, boolean onlyRequired, boolean mergeAllOf) {
        Yaml yaml = new Yaml();
        Map<String, Object> spec = yaml.load(yamlInputStream);
        return reduceOpenApiSpec(spec, dereference, onlyRequired, mergeAllOf);
    }


    public static ReducedOpenAPISpec reduceOpenApiSpec(Map<String, Object> spec, boolean dereference, boolean onlyRequired, boolean mergeAllOf) {
        List<Map<String, Object>> endpoints = ((Map<String, Object>) spec.get("paths")).entrySet().stream()
                .flatMap(pathEntry -> {
                    String route = pathEntry.getKey();
                    Map<String, Object> operation = (Map<String, Object>) pathEntry.getValue();
                    return operation.entrySet().stream()
                            .filter(operationEntry -> List.of("get", "post", "patch", "delete", "put").contains(operationEntry.getKey()))
                            .map(operationEntry -> {
                                String operationName = operationEntry.getKey();
                                Map<String, Object> docs = (Map<String, Object>) operationEntry.getValue();

                                Map<String, Object> endpointMap = new HashMap<>();
                                endpointMap.put("name", operationName.toUpperCase() + " " + route);
                                endpointMap.put("description", docs.containsKey("description") ? (String) docs.get("description") : "");
                                endpointMap.put("docs", docs);

                                return endpointMap;
                            });
                })
                .collect(Collectors.toList());

        if (dereference) {
            endpoints = RefDereferencer.dereferenceEndpoints(endpoints, spec);
        }

        if (mergeAllOf) {
            endpoints = AllOfMerger.mergeAllOfEndpoints(endpoints);
        }

        endpoints = RefDereferencer.reduceEndpoints(endpoints, onlyRequired);

        return new ReducedOpenAPISpec(
                spec.containsKey("servers") ? (List<Map<String, Object>>) spec.get("servers") : new ArrayList<>(),
                spec.containsKey("description") && spec.get("description") != null ? (String) spec.get("description") : "",
                endpoints
        );
    }

    /**
     * Dereferences any JSON reference ($ref) in the specObj using the fullSpec.
     * <p>
     * It traverses the specObj recursively.
     * If a $ref is found, it is dereferenced by retrieving the reference path from fullSpec.
     * If an array is found, each element is dereferenced recursively.
     * If an object is found, each field is dereferenced recursively.
     *
     * @param specObj  OpenAPI spec extract (possibly with $refs)
     * @param fullSpec Complete OpenAPI spec
     * @return specObj with all $refs dereferenced by retrieving definition from fullSpec
     */
    public static Object dereferenceRefs(Map<String, Object> specObj, Map<String, Object> fullSpec) {
        return RefDereferencer.dereferenceRefsHelper(specObj, fullSpec);
    }

    /**
     * Merges all "allOf" properties in the given object.
     * <p>
     * It traverses the object recursively.
     * If "allOf" is found, merges all schemas in the array.
     * If an array is found, merges each element recursively.
     * If a map is found, merges it recursively.
     *
     * @param obj OpenAPI object (possibly with allOf properties)
     * @return Object with all "allOf" properties merged
     */
    public static Object mergeAllOfProperties(Object obj) {
        return AllOfMerger.mergeAllOfPropertiesHelper(obj);
    }


}
