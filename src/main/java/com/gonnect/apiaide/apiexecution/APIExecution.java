package com.gonnect.apiaide.apiexecution;

import com.gonnect.apiaide.oas.OpenAPISpecUtil;
import com.gonnect.apiaide.parser.ParserRequestInput;
import com.gonnect.apiaide.parser.ResponseParser;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gonnect.apiaide.prompts.CallerPrompts.CALLER_PROMPT;
import static java.util.stream.Collectors.toMap;

/**
 * The Caller class is responsible for executing API calls based on a given plan
 * and parsing the responses using a ConversationalRetrievalChain and a ResponseParser.
 */
@Service
public class APIExecution {

    private final ConversationalRetrievalChain callerChain;
    private final OpenAPISpecUtil openAPISpec;
    private final ResponseParser responseParser;

    /**
     * Constructor for the Caller class.
     *
     * @param callerConversationalChain The ConversationalRetrievalChain used for API execution.
     * @param openAPISpec               The OpenAPISpec providing API specifications.
     * @param responseParser            The ResponseParser used for parsing API responses.
     */
    public APIExecution(ConversationalRetrievalChain callerConversationalChain, OpenAPISpecUtil openAPISpec, ResponseParser responseParser) {
        this.callerChain = callerConversationalChain;
        this.openAPISpec = openAPISpec;
        this.responseParser = responseParser;
    }

    /**
     * Executes API calls based on a given plan and returns the formatted output.
     *
     * @param input                The APIExecutionRequest containing the API plan and details.
     * @param conversationalChains List of conversational chains for parsing responses.
     * @return The formatted output of the API execution result.
     */
    public Map<String, String> executeAPIs(APIExecutionRequest input, List<String> conversationalChains) {
        // Create a prompt template with API documentation
        PromptTemplate template = PromptTemplate.from(CALLER_PROMPT);
        Prompt prompt = template.apply(Map.of("api_docs", generateAPIDocs()));

        // Execute the API calls in a conversational chain
        String conversation = callerChain.execute(prompt.text());
        while (!isExecutionComplete(conversation)) {
            String nextPrompt = getNextPrompt(conversation);
            conversation += callerChain.execute(nextPrompt);
        }

        // Use the ResponseParser to parse the API response
        ParserRequestInput parserRequestInput = createParserRequestInput(input, conversation);
        return conversationalChains.stream()
                .collect(toMap(
                        chain -> chain,
                        chain -> responseParser.parse(parserRequestInput).toString()
                ));


        // Return the formatted output
        //return formatOutput(parsedOutput.get(OUTPUT_KEY));
    }

    /**
     * Generates API documentation based on OpenAPISpec.
     *
     * @return Concatenated API documentation for all endpoints.
     */
    private String generateAPIDocs() {
        return openAPISpec.getEndpoints().stream()
                .map(e -> e + " " + openAPISpec.getOperation(e))
                .collect(Collectors.joining("\n"));
    }

    /**
     * Checks if the API execution is complete by searching for a specific string in the conversation.
     *
     * @param conversation The conversation text to analyze.
     * @return True if the execution is complete; otherwise, false.
     */
    private boolean isExecutionComplete(String conversation) {
        return conversation.contains("Execution Result:");
    }

    /**
     * Identifies the next API action based on the conversation text.
     *
     * @param conversation The conversation text to analyze.
     * @return The identified next API action.
     */
    private String getNextPrompt(String conversation) {
        String nextAction = identifyNextAction(conversation);
        return "Operation: " + nextAction + "\nInput: ";
    }

    /**
     * Formats the conversation text as output for presentation.
     *
     * @param conversation The conversation text to format.
     * @return Formatted output for the conversation.
     */
    private String formatOutput(String conversation) {
        return "```\n" + conversation + "\n```";
    }

    /**
     * Identifies the next API action from the conversation text using regular expressions.
     *
     * @param conversation The conversation text to analyze.
     * @return The identified next API action.
     */
    private String identifyNextAction(String conversation) {
        Pattern pattern = Pattern.compile("Operation: (\\w+)");
        Matcher matcher = pattern.matcher(conversation);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "GET"; // Default if not found
    }

    /**
     * Creates a ParserRequestInput from APIExecutionRequest and conversation details.
     *
     * @param input        The APIExecutionRequest containing details.
     * @param conversation The conversation text to include in parsing.
     * @return The constructed ParserRequestInput.
     */
    private ParserRequestInput createParserRequestInput(APIExecutionRequest input, String conversation) {
        // Customize this method based on your needs
        return new ParserRequestInput(
                input.getQuery(),
                input.getApiDescription(),
                input.getApiPath(),
                input.getApiParam(),
                input.getResponseDescription(),
                conversation // Pass the conversation as JSON for parsing, modify if needed
        );
    }
}

