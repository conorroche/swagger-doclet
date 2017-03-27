package com.tenxerconsulting.swagger.doclet;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.tenxerconsulting.swagger.doclet.model.ApiDeclaration;
import com.tenxerconsulting.swagger.doclet.model.ResourceListing;
import io.swagger.models.Swagger;

/**
 * The ObjectMapperRecorder represents a mapper for writing swagger objects to files
 */
public class ObjectMapperRecorder implements Recorder {

	final ObjectMapper mapper = new ObjectMapper();

	private void processCsv(String csv, CsvItemProcessor processor) {
		if (csv != null) {
			csv = csv.trim();
			if (csv.length() > 0) {
				String[] nvps = csv.split(",");
				for (String nvp : nvps) {
					nvp = nvp.trim();
					if (nvp.length() > 0) {
						if (nvp.indexOf(":") > -1) {
							String[] nvpParts = nvp.split(":");
							String name = nvpParts[0].trim();
							String valPart = nvpParts[1].trim();
							if (valPart.equalsIgnoreCase("true")) {
								processor.csvItem(name, true);
							} else if (valPart.equalsIgnoreCase("false")) {
								processor.csvItem(name, false);
							}
						}
					}
				}
			}
		}
	}

	interface CsvItemProcessor {

		void csvItem(String name, boolean val);
	}

	/**
	 * This creates a ObjectMapperRecorder
	 * @param serializationFeaturesCsv The CSV of serialization features to enable
	 * @param deserializationFeaturesCsv The CSV of deserialization features to enable
	 * @param defaultTyping The default typing to enable
	 * @param serializationInclusion The serialization inclusion to use e.g. NON_NULL
	 */
	public ObjectMapperRecorder(String serializationFeaturesCsv, String deserializationFeaturesCsv, String defaultTyping, String serializationInclusion) {

		// configure serialization features
		if (serializationFeaturesCsv == null) {
			serializationFeaturesCsv = SerializationFeature.INDENT_OUTPUT.toString() + ":true";
		}
		processCsv(serializationFeaturesCsv, new CsvItemProcessor() {

			public void csvItem(String name, boolean value) {

				for (SerializationFeature feature : SerializationFeature.values()) {
					if (feature.name().equalsIgnoreCase(name)) {
						ObjectMapperRecorder.this.mapper.configure(feature, value);
					}
				}

			}
		});

		// configure deserialization features
		if (deserializationFeaturesCsv != null) {
			processCsv(deserializationFeaturesCsv, new CsvItemProcessor() {

				public void csvItem(String name, boolean value) {

					for (DeserializationFeature feature : DeserializationFeature.values()) {
						if (feature.name().equalsIgnoreCase(name)) {
							ObjectMapperRecorder.this.mapper.configure(feature, value);
						}
					}

				}
			});
		}

		if (defaultTyping != null) {
			this.mapper.enableDefaultTyping(DefaultTyping.valueOf(defaultTyping));
		}

		if (serializationInclusion == null) {
			serializationInclusion = JsonInclude.Include.NON_NULL.toString();
		}
		this.mapper.setSerializationInclusion(JsonInclude.Include.valueOf(serializationInclusion));

	}

	/**
	 * {@inheritDoc}
	 * @see com.tenxerconsulting.swagger.doclet.Recorder#record(java.io.File, com.tenxerconsulting.swagger.doclet.model.ApiDeclaration)
	 */
	public void record(File file, ApiDeclaration declaration) throws IOException {
		this.mapper.writeValue(file, declaration);
	}

	/**
	 * {@inheritDoc}
	 * @see com.tenxerconsulting.swagger.doclet.Recorder#record(java.io.File, com.tenxerconsulting.swagger.doclet.model.ResourceListing)
	 */
	public void record(File file, ResourceListing listing) throws IOException {
		this.mapper.writeValue(file, listing);
	}

	@Override
	public void record(File file, Swagger swagger) throws IOException {
		this.mapper.writeValue(file, swagger);
	}
}
