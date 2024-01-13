package com.gonnect.apiaide.planner;

import com.gonnect.apiaide.utils.CastUtil;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.gonnect.apiaide.prompts.PlannerPrompts.ICL_EXAMPLES;
import static com.gonnect.apiaide.prompts.PlannerPrompts.PLANNER_PROMPT;
import static com.gonnect.apiaide.utils.CastUtil.castToList;
import static com.gonnect.apiaide.utils.CastUtil.castToMap;
import static java.lang.String.format;
import static java.util.regex.Pattern.compile;

/**
 * Planner generates plans to fulfill user queries using a conversational LLM.
 * It maintains conversation context in an embedding store.
 */
@Service
public class Planner {

    private static AtomicInteger cnt = new AtomicInteger(0);

    final private ConversationalRetrievalChain chain;
    private final EmbeddingStore<TextSegment> store;
    private final EmbeddingModel embeddingModel;

    public Planner(@Qualifier("plannerConversationalChain") ConversationalRetrievalChain chain,
                   EmbeddingStore<TextSegment> store,
                   EmbeddingModel embeddingModel) {
        this.chain = chain;
        this.store = store;
        this.embeddingModel = embeddingModel;
    }

    /**
     * Generates the next plan step for the given user input.
     * Maintains context embeddings in the store.
     */
    public Map<String, String> run(Map<String, ?> inputs) {

        String history = constructScratchpad(castToList(inputs.get("history")));
        String iclExamples = ICL_EXAMPLES.get("tmdb");


        String plannerPrompt = format(PLANNER_PROMPT,
                Map.of(
                        "input", inputs.get("query"),
                        "agent_scratchpad", history,
                        "stop_signals", generateStopSignals(castToMap(inputs))


                ));


        String plannerChainOutput = chain.execute(plannerPrompt);

        return Map.of(
                "result", compile("Plan step \\d+: ")
                        .matcher(plannerChainOutput)
                        .replaceAll("")
                        .trim()
        );
    }


    private String constructScratchpad(List<Map<String, String>> history) {
        if (history.isEmpty()) {
            return "";
        }

        StringBuilder scratchpad = new StringBuilder();
        for (int i = 0; i < history.size(); i++) {
            Map<String, String> step = history.get(i);
            scratchpad.append(format("Plan step %d: %s\n", i + 1, step.get("plan")));
            scratchpad.append(format("API response: %s\n", step.get("execution_res")));
        }
        return scratchpad.toString();
    }

    private String generateStopSignals(Map<String, List<String>> history) {
        StringBuilder stopSignals = new StringBuilder();
        if (history.containsKey("stop_signal")) {
            stopSignals.append(String.format("Stop signal %d: %s\n", cnt.incrementAndGet() + 1, history.get("stop_signal")));
        }

        return stopSignals.toString();
    }


}