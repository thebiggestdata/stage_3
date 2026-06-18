package com.thebiggestdata.search.model;

public record BookMetadata(String title, String author, String language, Integer year) {

	public boolean matches(String authorFilter, String languageFilter, Integer yearFilter) {
		if (authorFilter != null && !containsIgnoreCase(this.author, authorFilter)) return false;
		if (languageFilter != null && !containsIgnoreCase(this.language, languageFilter)) return false;
		if (yearFilter != null && !yearFilter.equals(this.year)) return false;
		return true;
	}

	private boolean containsIgnoreCase(String source, String target) {
		return source != null && target != null &&
				source.toLowerCase().contains(target.toLowerCase());
	}
}