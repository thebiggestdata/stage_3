package com.thebiggestdata.indexer.application.usecase;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexer.domain.model.IngestionEvent;
import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.model.TokenStream;
import com.thebiggestdata.indexer.domain.port.*;
import com.thebiggestdata.indexer.domain.service.PostingBuilder;
import com.thebiggestdata.indexer.domain.service.TokenizerPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.List;

public class IndexBookUseCase {
    private static final Logger logger = LoggerFactory.getLogger(IndexBookUseCase.class);

    private final HazelcastInstance hazelcast;
    private final IngestedEventConsumerPort eventConsumer;
    private final DatalakePathResolverPort pathResolver;
    private final DatalakeReadPort datalakeReader;
    private final TokenizerPipeline tokenizerPipeline;
    private final PostingBuilder postingBuilder;
    private final InvertedIndexWriterPort indexWriter;

    public IndexBookUseCase(
            HazelcastInstance hazelcast,
            IngestedEventConsumerPort eventConsumer,
            DatalakePathResolverPort pathResolver,
            DatalakeReadPort datalakeReader,
            TokenizerPipeline tokenizerPipeline,
            PostingBuilder postingBuilder,
            InvertedIndexWriterPort indexWriter
    ) {
        this.hazelcast = hazelcast;
        this.eventConsumer = eventConsumer;
        this.pathResolver = pathResolver;
        this.datalakeReader = datalakeReader;
        this.tokenizerPipeline = tokenizerPipeline;
        this.postingBuilder = postingBuilder;
        this.indexWriter = indexWriter;
    }

    public void run() {
        IngestionEvent event = eventConsumer.consumeBookId();
        int bookId = event.bookId();
        try {
            IMap<Integer, Boolean> processedDocs = hazelcast.getMap("processed-documents");
            if (processedDocs.containsKey(bookId)) {
                logger.info("Document {} already processed, skipping", bookId);
                event.acknowledge();
                return;
            }
            logger.info("Processing document ID: {}", bookId);
            String filePath = pathResolver.resolve(bookId);
            RawDocument rawDoc = datalakeReader.read(bookId, filePath);
            TokenStream tokenStream = tokenizerPipeline.process(rawDoc);
            Map<String, List<Integer>> postings = postingBuilder.build(tokenStream);
            indexWriter.write(postings);
            processedDocs.put(bookId, true);
            event.acknowledge();
            logger.info("Successfully indexed document ID: {} with {} unique tokens", bookId, postings.size());
        } catch (Exception e) {
            logger.error("Failed to index document ID: {}", bookId, e);
            event.reject();
        }
    }
}

