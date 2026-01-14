package com.thebiggestdata.ingestion.application;

import com.google.gson.Gson;
import com.thebiggestdata.ingestion.infrastructure.port.DocumentStatusProvider;
import com.thebiggestdata.ingestion.infrastructure.port.DownloadDocumentProvider;
import com.thebiggestdata.ingestion.infrastructure.port.ListDocumentsProvider;
import io.javalin.http.Context;
import java.util.Map;

public class DocumentProviderController {
    private final DownloadDocumentProvider ingestDocService;
    private final ListDocumentsProvider listDocsService;
    private final DocumentStatusProvider docStatusService;
    private static final Gson gson = new Gson();

    public DocumentProviderController(DownloadDocumentProvider ingestDocService, ListDocumentsProvider listDocsService, DocumentStatusProvider docStatusService) {
        this.ingestDocService = ingestDocService;
        this.listDocsService = listDocsService;
        this.docStatusService = docStatusService;
    }

    public void ingestDoc(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = ingestDocService.ingest(bookId);
        ctx.result(gson.toJson(result));
    }

    public void listAllDocs(Context ctx) {
        Map<String, Object> result = listDocsService.list();
        ctx.result(gson.toJson(result));
    }

    public void status(Context ctx) {
        int bookId = Integer.parseInt(ctx.pathParam("book_id"));
        Map<String, Object> result = docStatusService.status(bookId);
        ctx.result(gson.toJson(result));
    }
}
