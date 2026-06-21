package com.thebiggestdata.indexing.application.usecases.indexingservice;

import com.thebiggestdata.indexing.infrastructure.adapters.hazelcast.MetadataParser;
import com.thebiggestdata.indexing.infrastructure.ports.BookContentReaderPort;
import com.thebiggestdata.indexing.infrastructure.ports.IndexBookPort;
import com.thebiggestdata.indexing.infrastructure.ports.InvertedIndexPort;
import com.thebiggestdata.indexing.infrastructure.ports.MetadataStorePort;
import com.thebiggestdata.indexing.model.BookContent;
import com.thebiggestdata.indexing.model.BookMetadata;
import com.thebiggestdata.indexing.model.IndexedTerm;
import com.thebiggestdata.indexing.model.IndexingResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class IndexBookUseCase implements IndexBookPort {

    private static final Logger log = LoggerFactory.getLogger(IndexBookUseCase.class);

    private final BookContentReaderPort bookReader;
    private final InvertedIndexPort invertedIndex;
    private final MetadataStorePort metadataStore;
    private final TermFrequencyAnalyzer termFrequencyAnalyzer;
    private final MetadataParser metadataParser;

    public IndexBookUseCase(BookContentReaderPort bookReader, InvertedIndexPort invertedIndex, MetadataStorePort metadataStore, TermFrequencyAnalyzer termFrequencyAnalyzer, MetadataParser metadataParser) {
        this.bookReader = bookReader;
        this.invertedIndex = invertedIndex;
        this.metadataStore = metadataStore;
        this.termFrequencyAnalyzer = termFrequencyAnalyzer;
        this.metadataParser = metadataParser;
    }

    @Override
    public IndexingResult index(int bookId) {
        log.info("Start indexing book with id {}", bookId);
        if (!invertedIndex.markAsIndexed(bookId)) {
            log.info("Book {} has already been indexed", bookId);
            return IndexingResult.alreadyIndexed(bookId);
        }

        try {
            BookContent bookContent = bookReader.getBook(bookId);
            Map<String, Long> frequencies = termFrequencyAnalyzer.analyze(bookContent.body());
            int totalTokens = termFrequencyAnalyzer.countTotalTokens(frequencies);

            List<IndexedTerm> entries = frequencies.entrySet().stream()
                    .map(entry -> new IndexedTerm(entry.getKey(), String.valueOf(bookId), entry.getValue()))
                    .toList();

            invertedIndex.addEntries(entries);
            invertedIndex.pushEntries();

            BookMetadata metadata = metadataParser.parseFromHeader(bookContent.header());

            metadataStore.saveMetadata(bookId, metadata);
            invertedIndex.saveTokens(totalTokens);

            return IndexingResult.indexed(bookId, frequencies.size(), totalTokens);
         } catch (Exception e) {
            log.error("Error indexing book with id {}: {}", bookId, e.getMessage());
            return IndexingResult.failed(bookId, e.getMessage());
        }
    }
}
