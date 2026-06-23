package com.thebiggestdata.search.infrastructure.ports;

import com.thebiggestdata.search.model.IndexGeneration;

import java.util.Map;

public interface IndexStore {

	Map<Integer, Integer> find(IndexGeneration generation, String term);
}
