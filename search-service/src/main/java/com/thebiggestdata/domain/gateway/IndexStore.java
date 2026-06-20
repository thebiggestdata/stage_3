package com.thebiggestdata.domain.gateway;

import java.util.Set;

public interface IndexStore {
	Set<String> getDocuments(String term);
}