package com.gonnect.apiaide.config;


import com.gonnect.apiaide.prompts.PlannerPrompts;
import dev.langchain4j.chain.ConversationalRetrievalChain;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import dev.langchain4j.retriever.EmbeddingStoreRetriever;
import dev.langchain4j.retriever.Retriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.python.util.PythonInterpreter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Properties;

import static com.gonnect.apiaide.prompts.APISelectorPrompts.API_SELECTOR_PROMPT;
import static com.gonnect.apiaide.prompts.CallerPrompts.CALLER_PROMPT;
import static com.gonnect.apiaide.prompts.ParsingPrompts.*;
import static com.gonnect.apiaide.prompts.PlannerPrompts.PLANNER_PROMPT;
import static dev.langchain4j.model.input.PromptTemplate.from;
import static dev.langchain4j.model.openai.OpenAiModelName.GPT_3_5_TURBO;

@Configuration
public class Configurer {


    static {
        Properties props = new Properties();
        props.setProperty("python.import.site", "false");
        PythonInterpreter.initialize(props, null, new String[0]);
    }

    /**
     * JSR-223 Scripting Engine
     *
     * @return PythonInterpreter
     */
    @Bean
    public PythonInterpreter interpreter() {

        PythonInterpreter interpreter = new PythonInterpreter();

        interpreter.exec("import sys");
        interpreter.exec("sys.path.append('/usr/local/Cellar/jython/2.7.3/Lib')");


        return interpreter;
    }

    @Bean("chain")
    ConversationalRetrievalChain conversationalRetrievalChain(ChatLanguageModel chatModel,
                                                              Retriever<TextSegment> retriever) {

        return ConversationalRetrievalChain.builder()
                .chatLanguageModel(chatModel)
                .retriever(retriever)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(50000))
                .build();
    }


    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    Retriever<TextSegment> fetch(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using
        int maxResultsRetrieved = 1;
        double minScore = 0.6;

        return EmbeddingStoreRetriever.from(embeddingStore, embeddingModel, maxResultsRetrieved, minScore);
    }


    /**
     * Initializes and populates the embedding store with ICL examples.
     *
     * @param model the embedding model to use
     * @return the populated embedding store
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel model) {

        // Create empty store
        EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

        // Ingest ICL examples
        ingestExamples(model, store);

        return store;
    }

    /**
     * Ingests the ICL examples into the provided store.
     * <p>
     * The process followed is:
     * <p>
     * 1. Extract text of all ICL examples
     * 2. Convert into a Document for processing
     * 3. Split document into text segments
     * 4. Generate embeddings for each segment
     * 5. Add embeddings mapped to original segments in the store
     * <p>
     * This allows the ICL examples to be used for semantic search during query planning.
     *
     * @param model the embedding model
     * @param store the embedding store to populate
     */
    private void ingestExamples(EmbeddingModel model, EmbeddingStore<TextSegment> store) {

        // 1. Construct single document with all ICL text
        String text = String.join("\n", PlannerPrompts.ICL_EXAMPLES.values());
        Document document = new Document(text);

        // 2. Split document
        DocumentSplitter splitter = DocumentSplitters.recursive(100, 0, new OpenAiTokenizer(GPT_3_5_TURBO));
        List<TextSegment> segments = splitter.split(document);

        // 3. Generate embeddings for segments
        List<Embedding> embeddings = model.embedAll(segments).content();

        model.embedAll(segments);

        // 4. Add to store
        store.addAll(embeddings, segments);

    }

}
