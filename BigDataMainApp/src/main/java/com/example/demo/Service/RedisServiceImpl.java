package com.example.demo.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.JSONObjectMapper;
import com.example.demo.RedisDAO.RedisDao;
import com.fasterxml.jackson.databind.JsonNode;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class RedisServiceImpl implements RedisService{

	@Autowired
	RedisDao redisDao;
	
	@Autowired
	JSONObjectMapper jobj;
	
	public static final String ID = "id_";

	  public void addLink(String userId, String value) {
	    redisDao.addLink(userId, value);
	  }

	  public Object getValue( final String key ) {
		    return redisDao.getValue(key);
		}

		public void setValue( final String key, final String value ) {
		    redisDao.setValue(key, value);
		}
		
		public void setObjectValue( final String key, final Object value ) {
		    redisDao.setObjectValue(key, value);
		}

		public void traverseInput(JsonNode inputData) {
	        
			inputData.fields().forEachRemaining(entry -> {

				//Check if the field is an array
	            if (entry.getValue().isArray()) 
	            {
	            	ArrayList<JsonNode> innerValues = new ArrayList<JsonNode>();
	                Iterator<JsonNode> iterator = entry.getValue().iterator();
	                while(iterator.hasNext())
	                {
	                    JsonNode en = (JsonNode) iterator.next();
	                    
	                    if (en.isContainerNode()) 
	                    	traverseInput(en);
	                    
	                    innerValues.add(replace(en));
	                    traverseInput(en);
	                }
	                
	                if (!innerValues.isEmpty()) 
	                {
	                    ((ArrayNode) entry.getValue()).removeAll();
	                    innerValues.forEach(s -> {
	                        if (s != null) 
	                        	((ArrayNode) entry.getValue()).add(s);
	                    });
	                }
	            }
	            //Check if the field is an object
	            else if (entry.getValue().isContainerNode()) 
	            {   
	                traverseInput(entry.getValue());
	                replaceWithId(entry);
	            }
	        });
	    }
		
		public void populateNestedData(JsonNode parent, Set<String> childIdSet) {

	        if (parent == null) 
	        	return;
	        
	        while (parent.toString().contains(ID)) 
	        {
	        	parent.fields().forEachRemaining(s -> {
	                if (s.getValue().isArray())
	                {
	                    ArrayList<JsonNode> innerValues = new ArrayList<>();
	                    s.getValue().iterator().forEachRemaining(node -> {
	                        if (node.asText().startsWith((ID)))
	                            innerValues.add(node);
	                        if (node.isContainerNode()) 
	                        	populateNestedData(node, childIdSet);
	                        
	                        node.iterator().forEachRemaining(innerNode -> {
	                            if (innerNode.isContainerNode())
	                                populateNestedData(node, childIdSet);
	                        });
	                    });
	                    
	                    if (!innerValues.isEmpty()) 
	                    {
	                        ((ArrayNode) s.getValue()).removeAll();
	                        innerValues.forEach(innerValue -> {
	                            if (childIdSet != null) childIdSet.add(innerValue.asText());
	                            String value = (String) redisDao.getValue(innerValue.asText());
	                            if (value != null)
	                                ((ArrayNode) s.getValue()).add(jobj.nodeFromString(value));
	                        });
	                    }
	                }
	                
	                String value = s.getValue().asText();

	                if (value.startsWith(ID)) 
	                {
	                    if (childIdSet != null) 
	                    	childIdSet.add(value);
	                    
	                    String val = (String) redisDao.getValue(value);
	                    val = val == null ? "" : val;
	                    JsonNode node = jobj.nodeFromString(val);
	                    s.setValue(node);
	                }
	            });
	        }
	    }

	    private void replaceWithId(Map.Entry<String, JsonNode> entry) 
	    {
	        JsonNode node = replace(entry.getValue());
	        entry.setValue(node);
	    }

	    private JsonNode replace(JsonNode entry) 
	    {
	        ObjectMapper mapper = new ObjectMapper();
	        String value = entry.toString();
	        String id = ID + entry.get("objectType").asText() + "_" + entry.get("objectId").asText();
	        JsonNode node = mapper.valueToTree(id);
	        redisDao.setValue(id, value);
	        return node;
	    }

		@Override
		public boolean deleteValue(String id) {
			
			return redisDao.deleteValue(id);
		}

	
	
}
