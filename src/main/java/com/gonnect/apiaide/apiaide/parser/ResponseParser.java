package com.gonnect.apiaide.apiaide.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonnect.apiaide.apiaide.python.PythonExecutionService;
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

import static com.gonnect.apiaide.apiaide.parser.ParsingConstants.*;
import static com.gonnect.apiaide.apiaide.prompts.ParsingPrompts.*;
import static dev.langchain4j.model.input.PromptTemplate.from;
import static java.util.Map.of;
import static java.util.Optional.ofNullable;


/**
 * {@code ResponseParser} is a component responsible for parsing API responses based on user queries.
 * It employs various parsing strategies, including code templates and a Large Language Model (LLM).
 * The class is designed to be flexible, allowing different parsing chains for different strategies.
 * <p>
 * The parsing process is orchestrated by the {@link #parse(ParserRequestInput, ConversationalRetrievalChain, ConversationalRetrievalChain, ConversationalRetrievalChain, ConversationalRetrievalChain)} method,
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

    /**
     * Constructs a ResponseParser with the specified PythonExecutionService.
     *
     * @param pythonService The PythonExecutionService to use for executing Python code.
     */
    public ResponseParser(PythonExecutionService pythonService) {
        this.pythonService = pythonService;
    }

    /**
     * Orchestrates parsing an API response based on the provided query.
     * Tries different strategies.
     *
     * @param input                                  The RequestInput containing query and API information.
     * @param codeParsingSchemaConversationalChain   Chain for parsing API response based on JSON schema and query.
     * @param codeParsingResponseConversationalChain Chain for parsing API response based on JSON response snippet.
     * @param lmParsingConversationalChain           Chain for directly parsing and summarizing response using LLM.
     * @param postprocessConversationalChain         Chain for post-processing truncated output if needed.
     * @return A Map containing the parsed output with the key as OUTPUT_KEY.
     */
    public Map<String, String> parse(ParserRequestInput input, ConversationalRetrievalChain codeParsingSchemaConversationalChain,
                                     ConversationalRetrievalChain codeParsingResponseConversationalChain,
                                     ConversationalRetrievalChain lmParsingConversationalChain,
                                     ConversationalRetrievalChain postprocessConversationalChain) {

        // Prompt templates for generating Python code and parsing responses
        PromptTemplate codeParsingSchemaTemplate = from(CODE_PARSING_SCHEMA_TEMPLATE);
        PromptTemplate codeParsingResponseTemplate = from(CODE_PARSING_RESPONSE_TEMPLATE);
        PromptTemplate llmParsingTemplate = from(LLM_PARSING_TEMPLATE);
        PromptTemplate postprocessTemplate = from(POSTPROCESS_TEMPLATE);

        // Mapping of prompt templates to conversational chains
        Map<PromptTemplate, ConversationalRetrievalChain> llm = of(
                codeParsingSchemaTemplate, codeParsingSchemaConversationalChain,
                codeParsingResponseTemplate, codeParsingResponseConversationalChain,
                llmParsingTemplate, lmParsingConversationalChain,
                postprocessTemplate, postprocessConversationalChain);

        // Try different parsing strategies
        String output = tryCodeTemplate(input, codeParsingSchemaTemplate, llm)
                .orElse(tryCodeTemplate(input, codeParsingResponseTemplate, llm)
                        .orElse(tryLLMParsing(input, llmParsingTemplate, llm)));

        // Post-process if output length exceeds maximum allowed
        if (output.length() > MAX_OUTPUT_LENGTH) {
            postProcess(output, postprocessTemplate, llm);
        }

        return of(OUTPUT_KEY, output);
    }

    /**
     * Simplifies the provided JSON by truncating if necessary.
     *
     * @param json The JSON to simplify.
     * @return The simplified JSON.
     */
    private String simplifyJson(String json) {
        if (json.length() > MAX_JSON_LENGTH_2) {
            return json.substring(0, MAX_JSON_LENGTH_2);
        } else if (json.length() > MAX_JSON_LENGTH_1) {
            return json.substring(0, MAX_JSON_LENGTH_1);
        }
        return json;
    }

    /**
     * Attempts to execute Python code based on a code template.
     *
     * @param input    The RequestInput containing query and API information.
     * @param template The PromptTemplate for generating Python code.
     * @param llm      Mapping of prompt templates to conversational chains.
     * @return An optional output string if successful, otherwise empty.
     */
    private Optional<String> tryCodeTemplate(ParserRequestInput input, PromptTemplate template,
                                             Map<PromptTemplate, ConversationalRetrievalChain> llm) {
        String code = generateCode(input, template, llm);
        String encodedJson = encodeInput(input.getJson());
        String output = executePythonCode(code, encodedJson, llm);
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
     * @param llm                Mapping of prompt templates to conversational chains.
     * @return The parsed output based on LLM.
     */
    private String tryLLMParsing(ParserRequestInput input, PromptTemplate llmParsingTemplate,
                                 Map<PromptTemplate, ConversationalRetrievalChain> llm) {
        return llm.get(llmParsingTemplate).execute(llmParsingTemplate.apply(input).text());
    }

    /**
     * Performs post-processing on the output if needed.
     *
     * @param output              The output to post-process.
     * @param postprocessTemplate The PromptTemplate for post-processing.
     * @param llm                 Mapping of prompt templates to conversational chains.
     */
    private void postProcess(String output, PromptTemplate postprocessTemplate,
                             Map<PromptTemplate, ConversationalRetrievalChain> llm) {
        llm.get(postprocessTemplate).execute(postprocessTemplate.apply(output).text());
    }

    /**
     * Generates Python code based on a template and input information.
     *
     * @param input    The RequestInput containing query and API information.
     * @param template The PromptTemplate for generating Python code.
     * @param llm      Mapping of prompt templates to conversational chains.
     * @return The generated Python code.
     */
    private String generateCode(ParserRequestInput input, PromptTemplate template,
                                Map<PromptTemplate, ConversationalRetrievalChain> llm) {
        Prompt prompt = template.apply(of("query", input.getQuery(), "json", input.getJson(),
                "api_path", input.getApiPath(), "api_description", input.getApiDescription(),
                "api_param", input.getApiParam(), "response_description", input.getResponseDescription()));
        return llm.get(template).execute(prompt.text());
    }

    /**
     * Executes Python code and returns the output.
     *
     * @param code The Python code to execute.
     * @param json The encoded JSON input for the Python code.
     * @param llm  Mapping of prompt templates to conversational chains.
     * @return The output of the Python code.
     */
    @SneakyThrows
    private String executePythonCode(String code, String json,
                                     Map<PromptTemplate, ConversationalRetrievalChain> llm) {
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

