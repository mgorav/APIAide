package com.gonnect.apiaide.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonnect.apiaide.python.PythonExecutionService;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static com.gonnect.apiaide.parser.ParsingConstants.MAX_OUTPUT_LENGTH;
import static com.gonnect.apiaide.prompts.ParsingPrompts.*;
import static dev.langchain4j.model.input.PromptTemplate.from;
import static java.util.Map.of;
import static java.util.Optional.ofNullable;


/**
 * {@code ResponseParser} is a component responsible for parsing API responses based on user queries.
 * It employs various parsing strategies, including code templates and a Large Language Model (LLM).
 * The class is designed to be flexible, allowing different parsing chains for different strategies.
 * <p>
 * The parsing process is orchestrated by the {@link #parse(ParserRequestInput)} } method,
 * which sequentially tries different strategies, such as code parsing based on JSON schema, code parsing based on JSON response snippet,
 * and direct parsing using the LLM. Additionally, it handles post-processing of the output if its length exceeds a specified maximum.
 * <p>
 * The class utilizes prompt templates and conversational chains to facilitate communication with the LLM.
 * It includes methods for generating Python code, executing Python code, tracking intermediate steps, encoding input JSON, and post-processing.
 * <p>
 * This class is part of a larger system for conversational API interaction and is intended to be extensible and adaptable to different use cases.
 * <p>
 * Usage Example:
 * <pre>
 * {@code
 * ResponseParser responseParser = new ResponseParser(pythonExecutionService);
 * RequestInput requestInput = new RequestInput("user query", "api description", "api path", "api param", "response description", "json");
 * Map<String, String> parsedOutput = responseParser.parse(requestInput, codeParsingSchemaChain, codeParsingResponseChain, llmParsingChain, postprocessChain);
 * String output = parsedOutput.get(ResponseParser.OUTPUT_KEY);
 * }
 * </pre>
 */
@Service
public class ResponseParser {

    private final Logger logger = LoggerFactory.getLogger(ResponseParser.class);
    private final PythonExecutionService pythonService;
    private final ConversationalRetrievalChain chain;

    /**
     * Constructs a ResponseParser with the specified PythonExecutionService.
     *
     * @param pythonService The PythonExecutionService to use for executing Python code.
     * @param chain         Chain for parsing API response based on JSON schema and query.
     *                      Chain for directly parsing and summarizing response using LLM.
     *                      Chain for post-processing truncated output if needed
     */
    public ResponseParser(PythonExecutionService pythonService,
                          ConversationalRetrievalChain chain) {
        this.pythonService = pythonService;
        this.chain = chain;

    }

    /**
     * Orchestrates parsing an API response based on the provided query.
     * Tries different strategies.
     *
     * @param input The RequestInput containing query and API information..
     * @return A Map containing the parsed output with the key as OUTPUT_KEY.
     */
    public Map<String, String> parse(ParserRequestInput input) {


        // Try different parsing strategies
        String output = tryCodeTemplate(input, codeParsingSchemaTemplate)
                .orElse(tryCodeTemplate(input, codeParsingResponseTemplate)
                        .orElse(tryLLMParsing(input, llmParsingTemplate)));

        // Post-process if output length exceeds maximum allowed
        if (output.length() > MAX_OUTPUT_LENGTH) {
            postProcess(output, from(POSTPROCESS_TEMPLATE));
        }

        return of(ParsingConstants.OUTPUT_KEY, output);
    }

    /**
     * Simplifies the provided JSON by truncating if necessary.
     *
     * @param json The JSON to simplify.
     * @return The simplified JSON.
     */
    private String simplifyJson(String json) {
        if (json.length() > ParsingConstants.MAX_JSON_LENGTH_2) {
            return json.substring(0, ParsingConstants.MAX_JSON_LENGTH_2);
        } else if (json.length() > ParsingConstants.MAX_JSON_LENGTH_1) {
            return json.substring(0, ParsingConstants.MAX_JSON_LENGTH_1);
        }
        return json;
    }

    /**
     * Attempts to execute Python code based on a code template.
     *
     * @param input    The RequestInput containing query and API information.
     * @param template The PromptTemplate for generating Python code.
     * @return An optional output string if successful, otherwise empty.
     */
    private Optional<String> tryCodeTemplate(ParserRequestInput input, PromptTemplate template) {
        String code = generateCode(input, template);
        String encodedJson = encodeInput(input.getJson());
        String output = executePythonCode(code, encodedJson);
        if (output != null) {
            trackIntermediateStep(code, output);
        }
        return ofNullable(output);
    }

    /**
     * Attempts to parse an API response using LLM.
     *
     * @param input              The RequestInput containing query and API information.
     * @param llmParsingTemplate The PromptTemplate for LLM parsing.
     * @return The parsed output based on LLM.
     */
    private String tryLLMParsing(ParserRequestInput input, PromptTemplate llmParsingTemplate) {
        return chain.execute(llmParsingTemplate.apply(input).text());
    }

    /**
     * Performs post-processing on the output if needed.
     *
     * @param output              The output to post-process.
     * @param postprocessTemplate The PromptTemplate for post-processing.
     */
    private void postProcess(String output, PromptTemplate postprocessTemplate) {
        chain.execute(postprocessTemplate.apply(output).text());
    }

    /**
     * Generates Python code based on a template and input information.
     *
     * @param input    The RequestInput containing query and API information.
     * @param template The PromptTemplate for generating Python code.
     * @return The generated Python code.
     */
    private String generateCode(ParserRequestInput input, PromptTemplate template) {
        Prompt prompt = template.apply(of("query", input.getQuery(), "json", input.getJson(),
                "api_path", input.getApiPath(), "api_description", input.getApiDescription(),
                "api_param", input.getApiParam(), "response_description", input.getResponseDescription()));
        return chain.execute(prompt.text());
    }

    /**
     * Executes Python code and returns the output.
     *
     * @param code The Python code to execute.
     * @param json The encoded JSON input for the Python code.
     * @return The output of the Python code.
     */
    @SneakyThrows
    private String executePythonCode(String code, String json) {
        return pythonService.execute(code, of("data", new ObjectMapper().readValue(json, Map.class))).toString();
    }

    /**
     * Tracks an intermediate step with the executed code and output.
     *
     * @param code   The executed Python code.
     * @param output The output of the code.
     */
    private void trackIntermediateStep(String code, String output) {
        logger.info("Tried code: {}, output: {}", code, output);
    }

    /**
     * Encodes the input JSON into Base64 format.
     *
     * @param input The JSON input to encode.
     * @return The Base64-encoded input.
     */
    private String encodeInput(String input) {
        return Base64.encodeBase64String(input.getBytes());
    }
}

