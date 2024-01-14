package com.gonnect.apiaide.orchestrator;

import com.gonnect.apiaide.apiexecution.APIExecution;
import com.gonnect.apiaide.apiexecution.APIExecutionRequest;
import com.gonnect.apiaide.apiselector.APISelector;
import com.gonnect.apiaide.apiselector.APISelectorRequestInput;
import com.gonnect.apiaide.apiselector.HistoryTuple;
import com.gonnect.apiaide.oas.ReducedOpenAPISpec;
import com.gonnect.apiaide.parser.ParserRequestInput;
import com.gonnect.apiaide.parser.ResponseParser;
import com.gonnect.apiaide.planner.Planner;
import com.gonnect.apiaide.prompts.APISelectorPrompts;
import com.gonnect.apiaide.prompts.CallerPrompts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Map.of;

/**
 * The {@code APIOrchestration} class orchestrates the execution of API calls based on user queries using a conversational approach.
 * It integrates the Planner, APISelector, Caller, and ResponseParser components to generate API calling plans,
 * execute API calls, and parse responses iteratively until a stopping condition is met.
 * The class is marked as a Spring service for component scanning and dependency injection.
 */
@Service
public class APIOrchestration {

    @Autowired
    private Planner planner;

    @Autowired
    private APISelector apiSelector;

    @Autowired
    private APIExecution caller;

    @Autowired
    private ResponseParser responseParser;

    /**
     * Executes the orchestration of API calls based on the provided user query.
     *
     * @param input The input containing the user query and scenario details.
     * @return Formatted output of the API calling plan and execution results.
     */
    public String run(QueryInput input) {
        String background = "";
        List<Map<String, String>> exampleHistory = new ArrayList<>();

        String examplePlan = planner.run(of(
                "query", input.getQuery(),
                "history", exampleHistory
        )).get("result");

        int iterations = 0;
        double elapsedTime = 0.0;

        // Instance variable
        List<HistoryTuple> history = new ArrayList<>();

        HistoryTuple lastHistory = null;

        while (shouldContinue(iterations, elapsedTime)) {
            long t1 = System.currentTimeMillis();

            // Create temporary history with just the latest step
            int historySize = exampleHistory.size();
            List<Map<String, String>> tmpExampleHistory = new ArrayList<>();
            if (historySize > 0) {
                tmpExampleHistory.add(exampleHistory.get(historySize - 1));

                // Pass temporary history to planner
                examplePlan = planner.run(Map.of(
                        "input", input.getQuery(),
                        "history", tmpExampleHistory
                )).get("result");
            }


            APISelectorRequestInput apiSelectorInput = APISelectorRequestInput.builder()
                    .plan(examplePlan)
                    .history(history)
                    .lastHistory(lastHistory)
                    .apiSpec(input.getApiSpec()).build();

            String apiPlan = apiSelector.run(apiSelectorInput, input.getScenario());

            lastHistory = getLatestHistoryTuple(history);
            apiSelectorInput.setLastHistory(lastHistory);


            APIExecutionRequest executionRequest = buildExecutionRequest(input, apiPlan, background, input.getApiSpec());

            Map<String, String> executionResult = caller.run(executionRequest, exampleHistory);

            ParserRequestInput parserInput = buildParserInput(input, executionResult.get("conversation"));

            String parsedResult = responseParser.parse(parserInput).get("output");

            // Add latest step to full history
            exampleHistory.add(Map.of(
                    "plan", examplePlan,
                    "execution_res", parsedResult
            ));

            background += parsedResult + "\n";

            examplePlan = planner.run(of(
                    "input", input.getQuery(),
                    "history", exampleHistory
            )).get("result");

            long t2 = System.currentTimeMillis();

            iterations++;
            elapsedTime += (t2 - t1) / 1000.0;
        }

        return formatOutput(examplePlan);
    }

    private HistoryTuple getLatestHistoryTuple(List<HistoryTuple> history) {
        if (history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }

    private void addToHistory(String plan, String apiCall, String result, List<HistoryTuple> history) {

        history.add(new HistoryTuple(plan, apiCall, result));
    }

    /**
     * Checks whether the orchestration should continue based on the number of iterations and elapsed time.
     *
     * @param iterations  The current number of iterations.
     * @param elapsedTime The elapsed time in seconds.
     * @return {@code true} if the orchestration should continue; otherwise, {@code false}.
     */
    private boolean shouldContinue(int iterations, double elapsedTime) {
        return (iterations < 15 && elapsedTime < 60);
    }

    /**
     * Formats the output text for presentation.
     *
     * @param text The text to be formatted.
     * @return Formatted output enclosed in triple backticks.
     */
    private String formatOutput(String text) {
        return "```\n" + text + "\n```";
    }

    /**
     * Builds the API execution request with the provided input, API plan, and background information.
     *
     * @param input      The input containing scenario and parameters.
     * @param apiPlan    The API calling plan.
     * @param background The background information.
     * @return The constructed API execution request.
     */
    private APIExecutionRequest buildExecutionRequest(QueryInput input,
                                                      String apiPlan,
                                                      String background,
                                                      ReducedOpenAPISpec apiSpec) {
        APIExecutionRequest request = new APIExecutionRequest();
        request.setBackground(background);
        request.setPlan(apiPlan);
        request.setScenario(input.getScenario());
        request.setParameters(input.getParameters());
        request.setApiSpec(apiSpec);
        return request;
    }

    /**
     * Builds the parser input with the provided input and conversation details.
     *
     * @param input        The input containing query and parameters.
     * @param conversation The conversation text to include in parsing.
     * @return The constructed parser input.
     */
    private ParserRequestInput buildParserInput(QueryInput input, String conversation) {
        Map<String, String> params = input.getParameters();
        String apiDescription = params.get("apiDescription");
        String apiParam = params.get("apiParam");
        String apiPath = params.get("apiPath");
        String responseDescription = params.get("responseDescription");

        return ParserRequestInput.builder()
                .query(input.getQuery())
                .apiDescription(apiDescription)
                .apiParam(apiParam)
                .apiPath(apiPath)
                .responseDescription(responseDescription)
                .json(conversation)
                .build();
    }

    /**
     * Checks whether the provided API plan indicates the need to continue the planning process.
     *
     * @param plan The API plan to be checked.
     * @return {@code true} if planning should continue; otherwise, {@code false}.
     */
    private boolean shouldContinuePlan(String plan) {
        return Pattern.compile("Continue").matcher(plan).find();
    }
}
