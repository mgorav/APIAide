package com.gonnect.apiaide.apiaide.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gonnect.apiaide.apiaide.prompots.ParsingPrompts;
import com.gonnect.apiaide.apiaide.python.PythonExecutionService;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;

import static dev.langchain4j.model.input.PromptTemplate.from;
import static java.util.Map.of;

/**
 * Parses API JSON responses based on user query.
 * Tries different parsing strategies using an LLM.
 */
@Component
public class ResponseParser {

    private final Logger logger = LoggerFactory.getLogger(ResponseParser.class);

    private final PythonExecutionService pythonService;

    /**
     * Prompt template for generating python code
     * using json schema and query.
     */
    private final PromptTemplate codeParsingSchemaTemplate;

    /**
     * Backup template for code generation
     * using json response snippet.
     */
    private final PromptTemplate codeParsingResponseTemplate;

    /**
     * Template for LLM to directly parse
     * and summarize response.
     */
    private final PromptTemplate llmParsingTemplate;

    /**
     * Template for post-processing
     * truncated output if needed.
     */
    private final PromptTemplate postprocessTemplate;

    private final Map<PromptTemplate, ConversationalRetrievalChain> llm;


    /**
     *
     */
    public ResponseParser(
            @Qualifier("codeParsingSchemaConversationalChain")
            ConversationalRetrievalChain codeParsingSchemaConversationalChain,
            @Qualifier("codeParsingResponseConversationalChain")
            ConversationalRetrievalChain codeParsingResponseConversationalChain,
            @Qualifier("lmParsingConversationalChain")
            ConversationalRetrievalChain lmParsingConversationalChain,
            @Qualifier("postprocessConversationalChain")
            ConversationalRetrievalChain postprocessConversationalChain,
            PythonExecutionService pythonService) {
        this.pythonService = pythonService;


        this.codeParsingSchemaTemplate = from(ParsingPrompts.CODE_PARSING_SCHEMA_TEMPLATE);
        this.codeParsingResponseTemplate = from(ParsingPrompts.CODE_PARSING_RESPONSE_TEMPLATE);
        this.llmParsingTemplate = from(ParsingPrompts.LLM_PARSING_TEMPLATE);
        this.postprocessTemplate = from(ParsingPrompts.POSTPROCESS_TEMPLATE);

        this.llm = Map.of(
                codeParsingSchemaTemplate, codeParsingSchemaConversationalChain,
                codeParsingResponseTemplate, codeParsingResponseConversationalChain,
                llmParsingTemplate, lmParsingConversationalChain,
                postprocessTemplate, postprocessConversationalChain
        );

    }

    /**
     * Orchestrates parsing an API response
     * based on provided query.
     * Tries different strategies.
     */
    public String parse(RequestInput input) {

        String output = tryCodeTemplate(input, codeParsingSchemaTemplate);

        if (output == null) {
            output = tryCodeTemplate(input, codeParsingResponseTemplate);
        }

        if (output == null) {
            output = tryLLMParsing(input);
        }

        return postProcess(output);

    }

    // Helper methods like:

    /**
     * Encodes input string to base64.
     */
    private String encodeInput(String input) {
        return Base64.encodeBase64String(input.getBytes());
    }

    /**
     * Logs intermediate step of tried code
     * and generated output.
     */
    private void trackIntermediateStep(String code, String output) {
        logger.info("Tried code: " + code + ", output: " + output);
    }

    private String generateCode(RequestInput input, PromptTemplate template) {

        Prompt prompt = template.apply(of(
                "query", input.getQuery(),
                "json", input.getJson()
        ));

        return llm.get(template).execute(prompt.text());
    }

    @SneakyThrows
    private String executePythonCode(String code, String json) {

        return pythonService.execute(code,
                        of("data",
                                new ObjectMapper().readValue(json, Map.class)))
                .toString();
    }

    private String tryCodeTemplate(RequestInput input, PromptTemplate template) {

        String code = generateCode(input, template);

        String encodedJson = encodeInput(input.getJson());

        String output = executePythonCode(code, encodedJson);

        if (output != null) {
            trackIntermediateStep(code, output);
        }

        return output;

    }

    private String tryLLMParsing(RequestInput input) {

        return llm.get(llmParsingTemplate).execute(llmParsingTemplate.apply(input).text());

    }

    private String postProcess(String output) {
        return llm.get(postprocessTemplate).execute(postprocessTemplate.apply(output).text());
    }


}