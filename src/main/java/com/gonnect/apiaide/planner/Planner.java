package com.gonnect.apiaide.planner;

import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Planner generates plans to fulfill user queries using a conversational LLM.
 * It maintains conversation context in an embedding store.
 */
@Service
public class Planner {

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
    public String generatePlan(String input) {
        List<String> context = getContext();

        String prompt = context + "\n" + input;

        String response = chain.execute(prompt);

        addToContext(response);

        return response;
    }


    private List<String> getContext() {
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(null, 1000);

        // Extract just the embeddings
        List<Embedding> embeddings = matches.stream()
                .map(EmbeddingMatch::embedding)
                .collect(Collectors.toList());

        return store.addAll(embeddings);
    }

    private void addToContext(String text) {
        Embedding embedding = embeddingModel.embed(text).content();
        store.add(embedding);
    }

}