package com.thebiggestdata.indexer.application.usecase;

import com.thebiggestdata.indexer.domain.model.RawDocument;
import com.thebiggestdata.indexer.domain.model.TokenStream;
import com.thebiggestdata.indexer.domain.port.*;
import com.thebiggestdata.indexer.domain.service.PostingBuilder;
import com.thebiggestdata.indexer.domain.service.TokenizerPipeline;

public class IndexBookUseCase {

    private final IngestedEventConsumerPort eventPort;
    private final DatalakePathResolverPort pathResolver;
    private final DatalakeReadPort datalakeReader;
    private final TokenizerPipeline tokenizerPipeline;
    private final PostingBuilder postingBuilder;
    private final InvertedIndexWriterPort indexWriter;

    public IndexBookUseCase(
            IngestedEventConsumerPort eventPort,
            DatalakePathResolverPort pathResolver,
            DatalakeReadPort datalakeReader,
            TokenizerPipeline tokenizerPipeline,
            PostingBuilder postingBuilder,
            InvertedIndexWriterPort indexWriter
    ) {
        this.eventPort = eventPort;
        this.pathResolver = pathResolver;
        this.datalakeReader = datalakeReader;
        this.tokenizerPipeline = tokenizerPipeline;
        this.postingBuilder = postingBuilder;
        this.indexWriter = indexWriter;
    }

    public void run() {

        int bookId = eventPort.consumeBookId();

        String bodyPath = pathResolver.resolveBodyPath(bookId);

        RawDocument doc = datalakeReader.read(bookId, bodyPath);

        TokenStream stream = tokenizerPipeline.process(doc);

        var postings = postingBuilder.build(stream);

        indexWriter.write(postings);
    }
}
