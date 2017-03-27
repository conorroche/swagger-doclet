package com.tenxerconsulting.swagger.doclet.apidocs;

import static com.tenxerconsulting.swagger.doclet.apidocs.FixtureLoader.loadFixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sun.javadoc.RootDoc;
import com.tenxerconsulting.swagger.doclet.DocletOptions;
import com.tenxerconsulting.swagger.doclet.Recorder;
import com.tenxerconsulting.swagger.doclet.model.ApiDeclaration;
import com.tenxerconsulting.swagger.doclet.model.ResourceListing;
import com.tenxerconsulting.swagger.doclet.parser.JaxRsAnnotationParser;

@SuppressWarnings("javadoc")
public class IncludeApiDeclarationsTest {

	private Recorder recorderMock;
	private DocletOptions options;

	@Before
	public void setup() throws IOException {
		this.recorderMock = mock(Recorder.class);
		this.options = new DocletOptions().setRecorder(this.recorderMock).setIncludeSwaggerUi(false);
		this.options.setSortResourcesByPriority(true);

		final ApiDeclaration api1 = loadFixture("/fixtures/includeapideclarations/includedeclaration1.json", ApiDeclaration.class);
		final ApiDeclaration api2 = loadFixture("/fixtures/includeapideclarations/includedeclaration2.json", ApiDeclaration.class);
		final ApiDeclaration api3 = loadFixture("/fixtures/includeapideclarations/includedeclaration3.json", ApiDeclaration.class);
		List<ApiDeclaration> extraApiDeclarations = new ArrayList<ApiDeclaration>();
		extraApiDeclarations.add(api1);
		extraApiDeclarations.add(api2);
		extraApiDeclarations.add(api3);
//		this.options.setExtraApiDeclarations(extraApiDeclarations);
	}

	@Test
	public void testStart() throws IOException {

		final RootDoc rootDoc = RootDocLoader.fromPath("src/test/resources", "fixtures.includeapideclarations");
		new JaxRsAnnotationParser(this.options, rootDoc).run();

		final ResourceListing expectedListing = loadFixture("/fixtures/includeapideclarations/service.json", ResourceListing.class);
		verify(this.recorderMock).record(any(File.class), eq(expectedListing));

		final ApiDeclaration expectedDeclaration1 = loadFixture("/fixtures/includeapideclarations/foo.json", ApiDeclaration.class);
		verify(this.recorderMock).record(any(File.class), eq(expectedDeclaration1));

		final ApiDeclaration expectedDeclaration2 = loadFixture("/fixtures/includeapideclarations/foo3.json", ApiDeclaration.class);
		expectedDeclaration2.setPriority(5);
		verify(this.recorderMock).record(any(File.class), eq(expectedDeclaration2));
	}

}
