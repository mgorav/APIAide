package com.gonnect.apiaide.oas;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiProcessorTest {

    @Test
    void testReduceOpenApiSpec() {
        // Using text blocks for YAML input
        String yamlSpec = """
                paths:
                  /users:
                    get:
                      parameters:
                        - $ref: '#/components/parameters/userIdParam'
                components:
                  parameters:
                    userIdParam:
                      name: user_id
                      in: query
                      required: true
                      schema:
                        type: integer""";

        // Expected ReducedOpenAPISpec based on the provided scenario
        ReducedOpenAPISpec expectedReducedSpec = new ReducedOpenAPISpec(
                new ArrayList<>(),
                "", // Empty string or provide a non-null value
                List.of(
                        Map.of(
                                "name", "GET /users",
                                "description", "", // Empty string or provide a non-null value
                                "docs", Map.of(
                                        "parameters", List.of(
                                                Map.of(
                                                        "name", "user_id",
                                                        "in", "query",
                                                        "required", true,
                                                        "schema", Map.of("type", "integer")
                                                )
                                        ),
                                        "responses", new HashMap<>()
                                )
                        )
                )
        );

        // Call the reduceOpenApiSpec method
        ReducedOpenAPISpec actualReducedSpec = OpenApiProcessor.parseYamlOpenApiSpec(yamlSpec, true, true, true);

        // Use assertAll to check each field independently
        assertAll("ReducedOpenAPISpec",
                () -> assertEquals(expectedReducedSpec.getServers(), actualReducedSpec.getServers()),
                () -> assertEquals(expectedReducedSpec.getDescription(), actualReducedSpec.getDescription()),
                () -> assertParametersEqual(expectedReducedSpec.getEndpoints(), actualReducedSpec.getEndpoints())
        );
    }

    private void assertParametersEqual(List<Map<String, Object>> expected, List<Map<String, Object>> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            Map<String, Object> expectedEndpoint = expected.get(i);
            Map<String, Object> actualEndpoint = actual.get(i);

            assertEquals(expectedEndpoint.get("name"), actualEndpoint.get("name"));
            assertEquals(expectedEndpoint.get("description"), actualEndpoint.get("description"));

            Map<String, Object> expectedDocs = (Map<String, Object>) expectedEndpoint.get("docs");
            Map<String, Object> actualDocs = (Map<String, Object>) actualEndpoint.get("docs");

            assertParametersListEqual(
                    (List<Map<String, Object>>) expectedDocs.get("parameters"),
                    (List<Map<String, Object>>) actualDocs.get("parameters")
            );
        }
    }

    private void assertParametersListEqual(List<Map<String, Object>> expected, List<Map<String, Object>> actual) {
        assertEquals(expected.size(), actual.size());

        for (int i = 0; i < expected.size(); i++) {
            Map<String, Object> expectedParameter = expected.get(i);
            Map<String, Object> actualParameter = actual.get(i);

            assertEquals(expectedParameter.get("name"), actualParameter.get("name"));
            assertEquals(expectedParameter.get("in"), actualParameter.get("in"));
            assertEquals(expectedParameter.get("required"), actualParameter.get("required"));

            Map<String, Object> expectedSchema = (Map<String, Object>) expectedParameter.get("schema");
            Map<String, Object> actualSchema = (Map<String, Object>) actualParameter.get("schema");

            assertEquals(expectedSchema.get("type"), actualSchema.get("type"));
        }
    }
}
