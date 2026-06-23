package com.thebiggestdata.ingestion.infrastructure.adapters.hazelcast;

import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;
import com.thebiggestdata.ingestion.model.BookContent;

public final class BookContentSerializer implements CompactSerializer<BookContent> {

    @Override
    public BookContent read(CompactReader reader) {
        return new BookContent(reader.readString("header"), reader.readString("body"));
    }

    @Override
    public void write(CompactWriter writer, BookContent content) {
        writer.writeString("header", content.header());
        writer.writeString("body", content.body());
    }

    @Override
    public String getTypeName() {
        return "BookContent";
    }

    @Override
    public Class<BookContent> getCompactClass() {
        return BookContent.class;
    }
}
