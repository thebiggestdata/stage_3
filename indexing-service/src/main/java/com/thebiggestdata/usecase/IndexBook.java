package com.thebiggestdata.usecase;

import com.thebiggestdata.domain.gateway.BookRepository;
import com.thebiggestdata.domain.gateway.IndexRepository;
import com.thebiggestdata.domain.gateway.IndexingStatusRepository;
import com.thebiggestdata.domain.gateway.MetadataRepository;
import com.thebiggestdata.domain.entity.BookText;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class IndexBook {

    private static final Logger log = LoggerFactory.getLogger(IndexBook.class);

    private final BookRepository bookStore;
    private final IndexRepository indexStore;
    private final MetadataRepository metadataStore;
    private final IndexingStatusRepository statusStore;
    private final TermFrequencyAnalyzer analyzer;

    public IndexBook(BookRepository bookStore, IndexRepository indexStore, MetadataRepository metadataStore, IndexingStatusRepository statusStore, TermFrequencyAnalyzer analyzer) {
        this.bookStore = bookStore;
        this.indexStore = indexStore;
        this.metadataStore = metadataStore;
        this.statusStore = statusStore;
        this.analyzer = analyzer;
    }

    public void execute(int documentId) {
        log.info("Starting local indexing for document: {}", documentId);

        if (!statusStore.markAsIndexed(documentId)) {
            log.info("Document {} already indexed. Skipping.", documentId);
            return;
        }

        try {
            BookText content = bookStore.getBookContent(documentId);

            Map<String, Long> frequencies = analyzer.analyze(content.body());
            int totalTokens = analyzer.countTotalTokens(content.body());

            saveInvertedIndex(documentId, frequencies);
            metadataStore.saveMetadata(documentId, content.header());

            indexStore.saveTokens(totalTokens);
            log.info("Done indexing for document: {}. Token count: {}\n", documentId, totalTokens);
        } catch (Exception e) {
            log.error("Error indexing document {}: {}", documentId, e.getMessage());
            throw new RuntimeException("Failed to index document: " + documentId, e);
        }
    }

    private void saveInvertedIndex(int documentId, Map<String, Long> frequencies) {
        String docIdStr = String.valueOf(documentId);
        frequencies.forEach((term, freq) ->
                indexStore.addEntry(term, docIdStr, freq)
        );
        indexStore.pushEntries();
    }
}