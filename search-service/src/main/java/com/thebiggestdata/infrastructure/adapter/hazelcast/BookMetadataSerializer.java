package com.thebiggestdata.infrastructure.adapter.hazelcast;

import com.thebiggestdata.domain.entity.BookInfo;
import com.hazelcast.nio.serialization.compact.CompactReader;
import com.hazelcast.nio.serialization.compact.CompactSerializer;
import com.hazelcast.nio.serialization.compact.CompactWriter;

public class BookMetadataSerializer implements CompactSerializer<BookInfo> {

	@Override
	public BookInfo read(CompactReader reader) {
		String title = reader.readString("title");
		String author = reader.readString("author");
		String language = reader.readString("language");
		Integer year = reader.readNullableInt32("year");
		return new BookInfo(title, author, language, year);
	}

	@Override
	public void write(CompactWriter writer, BookInfo bookMetadata) {
		writer.writeString("title", bookMetadata.title());
		writer.writeString("author", bookMetadata.author());
		writer.writeString("language", bookMetadata.language());
		writer.writeNullableInt32("year", bookMetadata.year());

	}

	@Override
	public String getTypeName() {
		return "BookMetadata";
	}

	@Override
	public Class<BookInfo> getCompactClass() {
		return BookInfo.class;
	}
}