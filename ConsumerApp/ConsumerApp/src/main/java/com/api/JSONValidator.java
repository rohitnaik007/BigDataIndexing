package com.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

public class JSONValidator {

	public static JsonNode validateJSON(String jsonString)
	{
		final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

		try 
		{
			File initialFile = new File("./input.json");
			InputStream schema = new FileInputStream(initialFile);
			JsonNode schemaNode = new ObjectMapper().readTree(schema);
			final JsonSchema jsonSchema = factory.getJsonSchema(schemaNode);
			JsonNode inputNode = new ObjectMapper().readTree(jsonString);
			ProcessingReport processingReport = jsonSchema.validate(inputNode);
			if(processingReport.isSuccess())
				return inputNode;
		} 
		catch (Exception e) {

			return null;
		}
		return null;
	}
}
