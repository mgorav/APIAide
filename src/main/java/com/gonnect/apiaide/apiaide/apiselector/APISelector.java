package com.gonnect.apiaide.apiaide.apiselector;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.model.input.Prompt;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gonnect.apiaide.apiaide.prompts.APISelectorPrompts.API_SELECTOR_PROMPT;
import static dev.langchain4j.model.input.PromptTemplate.from;
import static java.util.Map.of;

/**
 * APISelector is responsible for selecting the sequence of API calls to fulfill a user query.
 * It uses a conversational retrieval chain to have a conversational flow with a language model,
 * prompting it to provide the API calling plan based on the provided input plan, background info,
 * and examples.
 * <p>
 * The key logic includes:
 * <ul>
 * <li> Building the prompt by applying the input variables into the base API selector prompt template
 * <li> Executing the prompt and getting the raw output
 * <li> Validating if the suggested API call is valid based on the OpenAPISpec
 * <li> Re-prompting if invalid, asking the model to provide another API suggestion
 * <li> Parsing out just the raw API calling plan from the full output
 * <li> Formatting the final output - numbering steps, highlighting variables
 * </ul>
 * <p>
 * The APISelector utilizes several external services:
 * <ul>
 * <li> ConversationalRetrievalChain - To have the conversational flow with the LM
 * <li> OpenAPISpec - To validate proposed API calls against the available endpoints
 * <li> iclExamples - Example API calling sequences specific to the scenario (e.g. tmdb)
 * </ul>
 * <p>
 * To use:
 * <ol>
 * <li> Construct APISelector with the required external services
 * <li> Call selectAPIs, passing input plan and background
 * <li> Get back final formatted API calling plan
 * </ol>
 * <p>
 * The output plan then can be executed by something that calls the actual APIs.
 */
@Service
public class APISelector {

    private final ConversationalRetrievalChain apiSelectorChain;
    private final OpenAPISpec openAPISpec;
    @Setter
    private Map<String, String> iclExamples;

    public APISelector(ConversationalRetrievalChain apiSelectorChain, OpenAPISpec openAPISpec) {
        this.apiSelectorChain = apiSelectorChain;
        this.openAPISpec = openAPISpec;
    }

    /**
     * Selects the API calling plan to fulfill the given input plan and background for the specified scenario.
     * Has conversational flow with LM to get valid API plan.
     *
     * @param input    Input plan, background
     * @param scenario API scenario (e.g. tmdb)
     * @return Formatted API calling plan
     */
    public String selectAPIs(APISelectorRequestInput input, String scenario) {
        Prompt prompt = buildPrompt(input, scenario);
        String output = apiSelectorChain.execute(prompt.text());

        while (!apiIsValid(output)) {
            String invalidMessage = output + "\nInvalid API. Please try again.";
            output = apiSelectorChain.execute(invalidMessage);
        }

        return formatOutput(output);
    }

    /**
     * Generates string with endpoints information for prompt.
     *
     * @return Endpoints info string
     */
    private String generateEndpointsInfo() {
        return openAPISpec.getEndpoints().stream()
                .map(e -> e + " " + openAPISpec.getOperation(e))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Gets ICLExamples string for the given scenario.
     * Looks up examples from preconfigured map.
     *
     * @param scenario API scenario
     * @return Examples string
     */
    private String generateICLExamples(String scenario) {
        return iclExamples.getOrDefault(scenario, "");
    }

    /**
     * Builds prompt by applying input variables into selector template for the given scenario.
     *
     * @param input    Input variables
     * @param scenario API scenario
     * @return Constructed prompt
     */
    private Prompt buildPrompt(APISelectorRequestInput input, String scenario) {

        return from(API_SELECTOR_PROMPT).apply(of(
                "endpoints", generateEndpointsInfo(),
                "plan", input.getPlan(),
                "background", input.getBackground(),
                "agent_scratchpad", "",
                "icl_examples", generateICLExamples(scenario)

        ));
    }

    /**
     * Checks if given API plan is valid based on spec.
     *
     * @param apiPlan API calling plan
     * @return True if valid, false otherwise
     */
    private boolean apiIsValid(String apiPlan) {
        return openAPISpec.getOperation(parseAPIPlan(apiPlan)) != null;
    }

    /**
     * Parses out just the raw API calling plan from the full conversational output text.
     * Uses regex to extract text after "API calling" prefix.
     *
     * @param text Full conversational output
     * @return Raw API calling plan string
     */
    private String parseAPIPlan(String text) {
        Pattern pattern = Pattern.compile("API calling \\d+: (.*)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    /**
     * Formats the raw API calling plan for final output.
     * Adds step numbering prefixes.
     * Highlights template variables with brackets.
     *
     * @param plan Raw API calling plan
     * @return Formatted API calling plan
     */
    private String formatOutput(String plan) {
        plan = plan.replaceAll("API calling \\d+:", "\n\nStep $0:");
        plan = plan.replaceAll("\\{\\{(.*?)\\}\\}", "[[$1]]");
        return plan;
    }
}
