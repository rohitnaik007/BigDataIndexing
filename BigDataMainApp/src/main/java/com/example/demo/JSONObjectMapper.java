package com.example.demo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.example.demo.Service.RedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.github.fge.jackson.JsonLoader;

@Component
public class JSONObjectMapper {
	
@Autowired
RedisService redisService;

@Autowired
private RedisTemplate< String, Object > template;

public static HashMap<Integer, ArrayList<String>> jsonMap = new HashMap<Integer, ArrayList<String>>();

	 public  void traverse(JsonNode node, int level) {
	        if (node.getNodeType() == JsonNodeType.ARRAY) {
	            traverseArray(node, level);
	        } else if (node.getNodeType() == JsonNodeType.OBJECT) {
	            traverseObject(node, level);
	        } else {
	           throw new RuntimeException("Not yet implemented");
	        }
	        
	       
	    }

	    private  void traverseObject(JsonNode node, int level) {
	        node.fieldNames().forEachRemaining((String fieldName) -> {
	            JsonNode childNode = node.get(fieldName);
	            printNode(childNode, fieldName, level);
	            //for nested object or arrays
	            if (traversable(childNode)) {
	                traverse(childNode, level + 1);
	            }
	        });
	    }

	    private  void traverseArray(JsonNode node, int level) {
	        for (JsonNode jsonArrayNode : node) {
	            printNode(jsonArrayNode, "arrayElement", level);
	            if (traversable(jsonArrayNode)) {
	                traverse(jsonArrayNode, level + 1);
	            }
	        }
	    }

	    private  boolean traversable(JsonNode node) {
	        return node.getNodeType() == JsonNodeType.OBJECT ||
	                node.getNodeType() == JsonNodeType.ARRAY;
	    }

	    public void printNode(JsonNode node, String keyName, int level) {
	    	
	    	
	    	
	        if (traversable(node)) {
	            //System.out.printf("%" + (level * 4 - 3) + "s|-- %s=%s type=%s%n",
	             //       "", keyName, node.toString(), node.getNodeType());
	            //System.out.print(node.get("objectId"));
	            List<String> values = new ArrayList<String>();
	            ListOperations<String, Object> listOps = template.opsForList();
	           // Iterator<String> it = node.fieldNames();
	            
	            Iterator<Map.Entry<String, JsonNode>> ygPropIter = node.fields();

	            while (ygPropIter.hasNext()) {
	              Entry<String,JsonNode> field = ygPropIter.next();
	              values.add(field.getKey() +":"+ field.getValue());
	            }           
	           
	            if(keyName.equalsIgnoreCase("arrayElement"))
	            {
	            	System.out.println(node.findParent(keyName));
	            }
	            System.out.println(level + " : " +keyName +":"+ values.toString());
			
	            if( null!=node && null !=node.get("objectId"))
	            {
	            	//System.out.println(node.get("objectId").toString()+ " "+ values.toString());
	            	//jsonMap.put(node.get("objectId").toString(),(ArrayList<String>) values);
	            	//jsonMap.put(level,(ArrayList<String>) values);
	            	//redisService.setValue("ObjectID_"+node.get("objectId").toString(), values.toString());
	            	
	            	 if(!jsonMap.containsKey(level))
	 	            {
	            		 jsonMap.put(level,(ArrayList<String>) values);
	 	            }
	            	 else {
	 	            
	 	            jsonMap.get(level).addAll(values);
	 	            }
	            	
		           
		            
	            	
	            }
	          
	            

	        } else {
	            Object value = null;
	            if (node.isTextual()) {
	                value = node.textValue();
	            } else if (node.isNumber()) {
	                value = node.numberValue();
	            }
	           // jsonMap.getOrDefault(level, new ArrayList<String>()).add(value.toString()
	            System.out.println(level + " : " +keyName +":"+ value.toString());
	            if(jsonMap.containsKey(level))
	            {
	            	jsonMap.get(level).add(keyName +":"+ value.toString());
	            }
	            else {
	            ArrayList<String> a1 = new ArrayList<String>();
	            a1.add(keyName +":"+ value.toString());
	            jsonMap.put(level,a1);
	            }
	           // System.out.print(node.findParent(value.toString()));
	            
	            //todo add more types
	          //  System.out.printf("%" + (level * 4 - 3) + "s|-- %s=%s type=%s%n",
	                  //  "", keyName, value, node.getNodeType());
	        }
	    }
	
	    public static JsonNode nodeFromString(String string) {

	        if (string == null) return null;
	        if (string.isEmpty()) string = "{}";
	        try {
	            return JsonLoader.fromString(string);
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        return null;
	    }
	
	
}
