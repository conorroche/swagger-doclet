package com.tenxerconsulting.swagger.doclet;

import java.io.File;
import java.io.IOException;

import com.tenxerconsulting.swagger.doclet.model.ApiDeclaration;
import com.tenxerconsulting.swagger.doclet.model.ResourceListing;
import io.swagger.models.Swagger;

public interface Recorder {

	void record(File file, ResourceListing listing) throws IOException;

	void record(File file, ApiDeclaration declaration) throws IOException;

	void record(File file, Swagger swagger) throws IOException;
}
