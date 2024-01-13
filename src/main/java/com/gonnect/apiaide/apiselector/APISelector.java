package com.gonnect.apiaide.apiselector;

import com.gonnect.apiaide.oas.OpenAPISpecUtil;
import com.gonnect.apiaide.oas.ReducedOpenAPISpec;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gonnect.apiaide.prompts.APISelectorPrompts.API_SELECTOR_PROMPT;
import static com.gonnect.apiaide.prompts.PlannerPrompts.ICL_EXAMPLES;
import static dev.langchain4j.model.input.PromptTemplate.from;
import static java.util.Map.of;

@Service
public class APISelector {

    private final ConversationalRetrievalChain apiSelectorChain;
    private final Map<String, String> iclExamples;

    public APISelector(ConversationalRetrievalChain apiSelectorChain) {
        this.apiSelectorChain = apiSelectorChain;
        this.iclExamples = Map.copyOf(ICL_EXAMPLES);
    }

    public String run(APISelectorRequestInput input, String scenario) {
        Prompt prompt = buildPrompt(input, scenario);
        String output = apiSelectorChain.execute(prompt.text());

        while (!apiIsValid(input, output)) {
            String invalidMessage = output + "\nInvalid API. Please try again.";
            output = apiSelectorChain.execute(invalidMessage);
        }

        return formatOutput(output);
    }

    private String generateEndpointsInfo(ReducedOpenAPISpec reducedOpenAPISpec) {
        OpenAPISpecUtil openAPISpec = OpenAPISpecUtil.builder()
                .endpoints(reducedOpenAPISpec.getEndpoints())
                .build();

        return openAPISpec.getEndpoints().stream()
                .map(e -> e + " " + openAPISpec.getOperation(e))
                .collect(Collectors.joining(" "));
    }


    private String generateICLExamples(String scenario) {
        return iclExamples.getOrDefault(scenario, "");
    }

        private Prompt buildPrompt(APISelectorRequestInput input, String scenario) {
        String endpoints = generateEndpointsInfo(input.getApiSpec());
        String background = input.getBackground() == null ? "" : input.getBackground();
        String plan = input.getPlan() == null ? "" : input.getPlan();
        String iclExamples = generateICLExamples(scenario);
        PromptTemplate template = from(API_SELECTOR_PROMPT);

        return template.apply(of(
                "endpoints", endpoints,
                "background", background,
                "plan", plan,
                "agent_scratchpad", "",
                "icl_examples", iclExamples
        ));
    }


    private boolean apiIsValid(APISelectorRequestInput apiSpec, String apiPlan) {
        return OpenAPISpecUtil.builder()
                .endpoints(apiSpec.getApiSpec().getEndpoints())
                .build()
                .getOperation(parseAPIPlan(apiPlan)) != null;
    }

    private String parseAPIPlan(String text) {
        Pattern pattern = Pattern.compile("API calling \\d+: (.*)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String formatOutput(String plan) {
        plan = plan.replaceAll("API calling \\d+:", "\n\nStep $0:");
        plan = plan.replaceAll("\\{\\{(.*?)\\}\\}", "[[$1]]");
        return plan;
    }
}
