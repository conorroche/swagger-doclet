package com.tenxerconsulting.swagger.doclet.apidocs;

import com.sun.javadoc.RootDoc;
import com.tenxerconsulting.swagger.doclet.DocletOptions;
import com.tenxerconsulting.swagger.doclet.Recorder;
import com.tenxerconsulting.swagger.doclet.apidocs.RootDocLoader;
import com.tenxerconsulting.swagger.doclet.model.ApiDeclaration;
import com.tenxerconsulting.swagger.doclet.parser.JaxRsAnnotationParser;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static com.tenxerconsulting.swagger.doclet.apidocs.FixtureLoader.loadFixture;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class Issue8Test {
    private Recorder recorderMock;
    private DocletOptions options;

    @Before
    public void setup() {
        this.recorderMock = mock(Recorder.class);
        this.options = new DocletOptions().setRecorder(this.recorderMock).setIncludeSwaggerUi(false);
    }

    @Test(timeout = 2000)
    public void testStart() throws IOException {
        final RootDoc rootDoc = RootDocLoader.fromPath("src/test/resources", "fixtures.issue8");
        new JaxRsAnnotationParser(this.options, rootDoc).run();

        final ApiDeclaration api = loadFixture("/fixtures/issue8/issue.json", ApiDeclaration.class);
        verify(this.recorderMock).record(any(File.class), eq(api));
    }
}
