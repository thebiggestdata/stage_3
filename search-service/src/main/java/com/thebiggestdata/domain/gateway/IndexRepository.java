package com.thebiggestdata.domain.gateway;

import java.util.Set;

public interface IndexRepository {
	Set<String> getDocuments(String term);
}