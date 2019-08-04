package com.example.demo.Controller;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.JSONObjectMapper;
import com.example.demo.JSONValidator;
import com.example.demo.Service.KafkaService;
import com.example.demo.Service.RedisService;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

@RestController
@RequestMapping("/redis")
public class RedisController {
	
	@Autowired
	RedisService redisService;
	
	@Autowired
	JSONObjectMapper jObjectMapper;
	
	@Autowired
	private KafkaService kafkaService;
	
	public static final String ID = "id_";
	
	@GetMapping("/getdata/{key}")
	public String GetData(@PathVariable String key)
	{
		return (String) redisService.getValue(key);
	}
	
	public static void parse(String json) throws IOException  {
	       JsonFactory factory = new JsonFactory();

	       ObjectMapper mapper = new ObjectMapper(factory);
	       JsonNode rootNode = mapper.readTree(json);  

	       Iterator<Map.Entry<String,JsonNode>> fieldsIterator = rootNode.fields();
	       while (fieldsIterator.hasNext()) {

	           Map.Entry<String,JsonNode> field = fieldsIterator.next();
	           
	           System.out.println("Key: " + field.getKey() + "\tValue:" + field.getValue());
	           if(fieldsIterator.hasNext())
	           {
	        	   if(field.getValue().getNodeType() == JsonNodeType.ARRAY)
	        	   {
	        		   if (field.getValue().isArray()) {
	        			    for (final JsonNode objNode : field.getValue()) {
	        			        System.out.println(objNode);
	        			    }
	        			}
	           }}
	       }
	}
	
	
	@RequestMapping(value = "/postdata/{object}", method = RequestMethod.POST)
    public ResponseEntity<String> postValue(@PathVariable String object, HttpEntity<String> input) {

		String planId = "";
		JsonNode rootNode = JSONValidator.validateJSON(input.getBody());
		if(null != rootNode)
		{
			String objectId = rootNode.get("objectId").textValue();
	        planId = ID + rootNode.get("objectType").textValue() + "_" + objectId;
			
			if (redisService.getValue(planId) != null) {
	            return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"resource already exists with Id: " + planId + "\" }");
	        }
			
			redisService.traverseInput(rootNode);
			redisService.setValue(planId, rootNode.toString());
			
			kafkaService.publish(input.getBody(), "index");
			
		}
		else
		{
			 return ResponseEntity.ok().body(" {\"message\": \"JSON not correct\" }");
		}
		
        return ResponseEntity.ok().body(" {\"message\": \"Created data with key: " + planId + "\" }");
    }
	
	
	@RequestMapping(value = "/getdata/{object}/{key}", method = RequestMethod.GET)
    public ResponseEntity<String> getValue(@PathVariable String object, @PathVariable String key) {

		String internalID = ID + object + "_" + key;
        String value = (String) redisService.getValue(internalID);
        
        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }
        
        try
        {
        	JsonNode node = JSONObjectMapper.nodeFromString(value);		
    		redisService.populateNestedData(node, null);
    		value = node.toString();
    		return ResponseEntity.ok().body(value);
        }
        catch(Exception e)
        {
        	System.out.println(e.getMessage());
        }
        
		return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    }
	
	
	@RequestMapping(value = "/deletedata/{object}/{objectId}", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteObject(@PathVariable("object") String object, @PathVariable("objectId") String objectId) {

        String internalID = ID + object + "_" + objectId;
        String masterObject = (String) redisService.getValue(internalID);
        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        redisService.populateNestedData(JSONObjectMapper.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;
        
        for(String id : childIdSet)
        {
        	deleteSuccess = redisService.deleteValue(id);
        }
        
        kafkaService.publish(objectId, "delete");
        
        if(deleteSuccess)
        	return new ResponseEntity<>(" {\"message\": \"Deleted\" }", HttpStatus.OK);
        
        return new ResponseEntity<>(" {\"message\": \"Nothing to delete\" }", HttpStatus.NOT_FOUND);
    }

	
	@RequestMapping(value = "/putdata/{object}/{key}", method = RequestMethod.PUT)
    public ResponseEntity<String> putValue(@PathVariable String object, HttpEntity<String> input, @PathVariable String key) {
		
		String internalID = ID + object + "_" + key;
        String masterObject = (String) redisService.getValue(internalID);
        
        if (masterObject == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }
		
        Set<String> childIdSet = new HashSet<String>();
        childIdSet.add(internalID);
        redisService.populateNestedData(JSONObjectMapper.nodeFromString(masterObject), childIdSet);
        boolean deleteSuccess = false;
        
        for(String id : childIdSet)
        {
        	deleteSuccess = redisService.deleteValue(id);
        }
        
        if(deleteSuccess)
        {
        	kafkaService.publish(input.getBody(), "index");
        	String planId = "";
    		JsonNode rootNode = JSONValidator.validateJSON(input.getBody());
    		if(null != rootNode)
    		{
    			String objectId = rootNode.get("objectId").textValue();
    	        planId = ID + rootNode.get("objectType").textValue() + "_" + objectId;
    			
    			if (redisService.getValue(planId) != null) {
    	            return ResponseEntity.status(HttpStatus.CONFLICT).body(" {\"message\": \"A resource already exists with the id: " + planId + "\" }");
    	        }
    			
    			redisService.traverseInput(rootNode);
    			redisService.setValue(planId, rootNode.toString());
    		}
    		else
    		{
    			return ResponseEntity.ok().body(" {\"message\": \"Error validating the input data\" }");
    		}
    		  		
    		
            return ResponseEntity.ok().body(" {\"message\": \"Updated data with key: " + planId + "\" }");
        }
        
        return ResponseEntity.ok().body(" {\"message\": \"Error updating the object }");
    }
	
	@RequestMapping(value = "/patchdata/{object}/{key}", method = RequestMethod.PATCH)
    public ResponseEntity<String> patchValue(@PathVariable String object, @PathVariable String key, HttpEntity<String> input) {

		String internalID = ID + object + "_" + key;
        String value = (String) redisService.getValue(internalID);
        
        if (value == null) {
            return new ResponseEntity<String>("{\"message\": \"No Data Found\" }", HttpStatus.NOT_FOUND);
        }
        
        try
        {
        	//Get the old node from redis using the object Id 
        	JsonNode oldNode = JSONObjectMapper.nodeFromString(value);
        	redisService.populateNestedData(oldNode, null);
    		value = oldNode.toString();
    		
    		//Construct the new node from the input body
    		String inputData = input.getBody();
    		JsonNode newNode = JSONObjectMapper.nodeFromString(inputData);
    		
    		ArrayNode planServicesNew = (ArrayNode) newNode.get("linkedPlanServices");
            Set<JsonNode> planServicesSet = new HashSet<>();
            Set<String> objectIds = new HashSet<String>();

            planServicesNew.addAll((ArrayNode) oldNode.get("linkedPlanServices"));

            for(JsonNode node : planServicesNew)
            {
            	Iterator<Entry<String, JsonNode>> sitr = node.fields();
            	while(sitr.hasNext())
            	{
            		Entry<String, JsonNode> val = sitr.next();
            		if(val.getKey().equals("objectId"))
            		{
            			if(!objectIds.contains(val.getValue().toString())) 
            			{
            				planServicesSet.add(node);
            				objectIds.add(val.getValue().toString());
            			}
            		}
            	}
            }
            
            planServicesNew.removeAll();
            
            if (!planServicesSet.isEmpty())
                planServicesSet.forEach(s -> { planServicesNew.add(s); });
            
            redisService.traverseInput(newNode);
            redisService.setValue(internalID, newNode.toString());
            kafkaService.publish(input.getBody(), "index");
            
        }
        catch(Exception e)
        {
        	return new ResponseEntity<>(" {\"message\": \"Invalid Data\" }", HttpStatus.BAD_REQUEST);
        }
		return ResponseEntity.ok().body(" {\"message\": \"Updated data with key: " + internalID + "\" }");
    }
	
	
	
}
