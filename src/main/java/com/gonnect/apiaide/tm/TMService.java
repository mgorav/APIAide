package com.gonnect.apiaide.tm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonnect.apiaide.oas.OpenApiProcessor;
import com.gonnect.apiaide.oas.ReducedOpenAPISpec;
import com.gonnect.apiaide.orchestrator.APIOrchestration;
import com.gonnect.apiaide.orchestrator.QueryInput;
import com.gonnect.apiaide.request.RequestWrapper;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class TMService {

    private static final Logger log = LoggerFactory.getLogger(TMService.class);

    @Autowired
    private TMLoaderService loaderService;
    @Autowired
    private OpenApiProcessor openApiProcessor;
    @Autowired
    private RequestWrapper requestWrapper;
    @Autowired
    private APIOrchestration apiOrchestration;

    @SneakyThrows
    public void run() {

        ObjectMapper mapper = new ObjectMapper();
        String rawTmdbApiSpecStr = loaderService.loadOAS();

        JsonNode rawTmdbApiSpec = mapper.readTree(rawTmdbApiSpecStr);

        ReducedOpenAPISpec reducedSpec = openApiProcessor.reduceOpenApiSpec(
                rawTmdbApiSpec,
                false,   // dereference
                true,    // onlyRequired
                true     // mergeAllOf
        );

        List<String> queries = mapper.readValue(loaderService.loadTMDBQueries(), List.class);


        for (int idx = 0; idx < queries.size(); idx++) {
            Map<String, String> queryAndSolution = Map.class.cast(queries.get(idx));
            String query = queryAndSolution.get("query");
            log.info("#".repeat(20) + " Query-" + (idx + 1) + " " + "#".repeat(20));
            log.info(query);

            apiOrchestration.run(
                    QueryInput.builder()
                            .query(query)
                            .scenario("tmdb")
                            .apiSpec(reducedSpec)
                            .build()
            );

        }


    }
}
