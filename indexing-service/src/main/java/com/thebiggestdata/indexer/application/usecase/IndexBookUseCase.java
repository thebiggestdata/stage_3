package com.thebiggestdata.indexer.application.usecase;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.thebiggestdata.indexer.domain.model.IngestionEvent;
import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.model.TokenStream;
import com.thebiggestdata.indexer.domain.port.*;
import com.thebiggestdata.indexer.domain.service.PostingBuilder;
import com.thebiggestdata.indexer.domain.service.TokenizerPipeline;
import java.util.List;
import java.util.Map;

public class IndexBookUseCase {
    private final HazelcastInstance hazelcast;
    private final IngestedEventConsumerPort eventPort;
    private final DatalakePathResolverPort pathResolver;
    private final DatalakeReadPort datalakeReader;
    private final TokenizerPipeline tokenizerPipeline;
    private final PostingBuilder postingBuilder;
    private final InvertedIndexWriterPort indexWriter;

    public IndexBookUseCase(
            HazelcastInstance hazelcast, IngestedEventConsumerPort eventPort,
            DatalakePathResolverPort pathResolver,
            DatalakeReadPort datalakeReader,
            TokenizerPipeline tokenizerPipeline,
            PostingBuilder postingBuilder,
            InvertedIndexWriterPort indexWriter
    ) {
        this.hazelcast = hazelcast;
        this.eventPort = eventPort;
        this.pathResolver = pathResolver;
        this.datalakeReader = datalakeReader;
        this.tokenizerPipeline = tokenizerPipeline;
        this.postingBuilder = postingBuilder;
        this.indexWriter = indexWriter;
    }

    public void run() {
        IngestionEvent event = eventPort.consumeBookId();
        int bookId = event.bookId();
        IMap<Integer, Boolean> processedDocs = hazelcast.getMap("processed-documents");
        processedDocs.lock(bookId);
        try {
            if (processedDocs.containsKey(bookId)) {
                System.out.println("Document " + bookId + " already indexed. Skipping.");
                event.acknowledge();
                return;
            }
        } finally {
            processedDocs.unlock(bookId);
        }
        try {
            String bodyPath = pathResolver.resolveBodyPath(bookId);
            RawDocument doc = datalakeReader.read(bookId, bodyPath);
            TokenStream stream = tokenizerPipeline.process(doc);
            Map<String, List<Integer>> postings = postingBuilder.build(stream);
            indexWriter.write(postings);
            processedDocs.lock(bookId);
            try {processedDocs.put(bookId, true);}
            finally {processedDocs.unlock(bookId);}
            event.acknowledge();
            System.out.println("Document " + bookId + " indexed successfully");
        } catch (Exception e) {
            event.reject();
            System.err.println("Error indexing document " + bookId + ": " + e.getMessage());
            throw e;
        }
    }
}

