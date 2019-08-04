package com.example.demo.Service;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

public interface RedisService {

	public void addLink(String userId, String value);
	
	public Object getValue( final String key );
	
	public void setValue( final String key, final String value );

	public void setObjectValue( final String key, final Object value );

	public void traverseInput(JsonNode rootNode);

	public void populateNestedData(JsonNode node, Set<String> childIdSet);

	public boolean deleteValue(String id);
}
