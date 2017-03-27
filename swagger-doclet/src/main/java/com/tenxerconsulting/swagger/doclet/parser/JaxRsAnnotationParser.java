package com.tenxerconsulting.swagger.doclet.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.io.ByteStreams;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;

import com.tenxerconsulting.swagger.doclet.model.*;
import com.tenxerconsulting.swagger.doclet.model.HttpMethod;
import com.tenxerconsulting.swagger.doclet.DocletOptions;
import com.tenxerconsulting.swagger.doclet.Recorder;
import com.tenxerconsulting.swagger.doclet.ServiceDoclet;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Scheme;
import io.swagger.models.Swagger;

@SuppressWarnings("javadoc")
public class JaxRsAnnotationParser {

    // swagger 1.1 spec see https://groups.google.com/forum/#!topic/swagger-swaggersocket/mHdR9u0utH4
    // diffs between 1.1 and 1.2 see https://github.com/wordnik/swagger-spec/wiki/1.2-transition
    private static final String SWAGGER_VERSION = "2.0";

    private static final String SWAGGER_UI_VERSION = "2.2.10";

    private final DocletOptions options;
    private final RootDoc rootDoc;

    private static final <T> void addIfNotNull(Collection<T> collection, T item) {
        if (item != null) {
            collection.add(item);
        }
    }

    public JaxRsAnnotationParser(DocletOptions options, RootDoc rootDoc) {
        this.options = options;
        this.rootDoc = rootDoc;
    }

    public boolean run() {
        try {

            // setup additional classes needed for processing, generally these are java ones such as java.lang.String
            // adding them here allows them to be used in @outputType
            Collection<ClassDoc> typeClasses = new ArrayList<ClassDoc>();
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.String.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Integer.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Boolean.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Float.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Double.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Character.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Long.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.lang.Byte.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.Date.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.Calendar.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.Map.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.Collection.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.Set.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.List.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.math.BigInteger.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.math.BigDecimal.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.util.UUID.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.DayOfWeek.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.Duration.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.Instant.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.LocalDate.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.LocalDateTime.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.Month.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.MonthDay.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.OffsetDateTime.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.OffsetTime.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.Period.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.Year.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.YearMonth.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.ZoneId.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.ZoneOffset.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.time.ZonedDateTime.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.net.URI.class.getName()));
            addIfNotNull(typeClasses, this.rootDoc.classNamed(java.net.URL.class.getName()));

            // filter the classes to process
            Collection<ClassDoc> docletClasses = new ArrayList<ClassDoc>();
            for (ClassDoc classDoc : this.rootDoc.classes()) {

                // see if excluded via its FQN
                boolean excludeResource = false;
                if (this.options.getExcludeResourcePrefixes() != null && !this.options.getExcludeResourcePrefixes().isEmpty()) {
                    for (String prefix : this.options.getExcludeResourcePrefixes()) {
                        String className = classDoc.qualifiedName();
                        if (className.startsWith(prefix)) {
                            excludeResource = true;
                            break;
                        }
                    }
                }

                // see if the inclusion filter is set and if so this resource must match this
                if (!excludeResource && this.options.getIncludeResourcePrefixes() != null && !this.options.getIncludeResourcePrefixes().isEmpty()) {
                    boolean matched = false;
                    for (String prefix : this.options.getIncludeResourcePrefixes()) {
                        String className = classDoc.qualifiedName();
                        if (className.startsWith(prefix)) {
                            matched = true;
                            break;
                        }
                    }
                    excludeResource = !matched;
                }

                if (excludeResource) {
                    continue;
                }

                // see if deprecated
                if (this.options.isExcludeDeprecatedResourceClasses() && ParserHelper.isDeprecated(classDoc, this.options)) {
                    continue;
                }

                // see if excluded via a tag or annotation
                if (ParserHelper.hasTag(classDoc, this.options.getExcludeClassTags())) {
                    continue;
                }
                if (ParserHelper.hasAnnotation(classDoc, this.options.getExcludeClassAnnotations(), this.options)) {
                    continue;
                }

                docletClasses.add(classDoc);
            }

            ClassDocCache classCache = new ClassDocCache(docletClasses);
            io.swagger.models.Swagger swagger = new Swagger();
            swagger.setSwagger(SWAGGER_VERSION);

            List<TagWrapper> tags = new ArrayList<>();
            LinkedHashMap<String, Path> declarations = null;

            // build up set of subresources
            // do simple parsing to find sub resource classes
            // these are ones referenced in the return types of methods
            // which have a path but no http method
            List<ClassDoc> subResourceClasses = new ArrayList<>();
            for (ClassDoc classDoc : docletClasses) {
                ClassDoc currentClassDoc = classDoc;
                while (currentClassDoc != null) {

                    for (MethodDoc method : currentClassDoc.methods()) {
                        // if the method has @Path but no Http method then its an entry point to a sub resource
                        if (!ParserHelper.resolveMethodPath(method, this.options).isEmpty() && HttpMethod.fromMethod(method) == null) {
                            ClassDoc subResourceClassDoc = classCache.findByType(method.returnType());
                            // look for a custom return type, this is useful where we return a jaxrs Resource in the method signature
                            // which typically returns a different subResource object
                            if (subResourceClassDoc == null) {
                                Type customType = ParserHelper.readCustomType(method, this.options, docletClasses);
                                subResourceClassDoc = classCache.findByType(customType);
                            }
                            if (subResourceClassDoc != null) {
                                if (this.options.isLogDebug()) {
                                    System.out.println("Adding return type as sub resource class : " + subResourceClassDoc.name() + " for method "
                                            + method.name() + " of referencing class " + currentClassDoc.name());
                                }

                                if (!subResourceClasses.contains(subResourceClassDoc)) {
                                    subResourceClasses.add(subResourceClassDoc);
                                }
                            }
                        }
                    }

                    currentClassDoc = currentClassDoc.superclass();

                    // ignore parent object class
                    if (!ParserHelper.hasAncestor(currentClassDoc)) {
                        break;
                    }
                }
            }

            // parse with the v2 parser that supports endpoints of the same resource being spread across resource files
            Map<String, Path> resourceToDeclaration = new HashMap<>();
            for (ClassDoc classDoc : docletClasses) {
                CrossClassApiParser classParser = new CrossClassApiParser(swagger, this.options, classDoc, docletClasses, subResourceClasses, typeClasses,
                        tags);
                classParser.parse(resourceToDeclaration);
            }

            if (this.options.isLogDebug()) {
                System.out.println("After parse phase api declarations are: ");
                for (Map.Entry<String, Path> entry : resourceToDeclaration.entrySet()) {
                    System.out.println("Api Dec: path " + entry.getKey());
                    for (Map.Entry<io.swagger.models.HttpMethod, io.swagger.models.Operation> opEntry : entry.getValue().getOperationMap().entrySet()) {
                        System.out.println("Api nick name:" + opEntry.getValue().getOperationId() + " method " + opEntry.getKey().toString());
                    }
                }
            }

            // add any extra declarations
//			if (this.options.getExtraApiDeclarations() != null && !this.options.getExtraApiDeclarations().isEmpty()) {
//				declarationColl = new ArrayList<>(declarationColl);
//				declarationColl.addAll(this.options.getExtraApiDeclarations());
//			}

            // set root path on any empty resources
            for (Path path: resourceToDeclaration.values()) {
                for (Operation op: path.getOperations()) {
                    if (op.getTags().size() == 0) {
                        op.addTag(this.options.getResourceRootPath());
                    }
                    if (op.getTags().contains("/")) {
                        op.getTags().remove("/");
                        op.addTag(this.options.getResourceRootPath());
                    }
                }
            }
            List<TagWrapper> tagsToRemove = new ArrayList<>();
            for (TagWrapper tagWrapper: tags) {
                if (tagWrapper.getTag().getName().equals("/")) {
                    tagsToRemove.add(tagWrapper);
                }
            }

            tags.removeAll(tagsToRemove);

            // merge the api declarations
//			declarationColl = new ApiDeclarationMerger(SWAGGER_VERSION, this.options.getApiVersion(), this.options.getApiBasePath()).merge(declarationColl);

            declarations = new LinkedHashMap<>();

            List<TagWrapper> sortedTags = new LinkedList<>(tags);
            List<Map.Entry<String, Path>> sortedDeclarations = new LinkedList<>(resourceToDeclaration.entrySet());
            // sort the api declarations if needed
            if (this.options.isSortResourcesByPriority()) {

                Collections.sort(sortedTags, new Comparator<TagWrapper>() {
                    @Override
                    public int compare(TagWrapper t1, TagWrapper t2) {
                        return Integer.compare(t1.getPriority(), t2.getPriority());
                    }
                });

            } else if (this.options.isSortResourcesByPath()){
                Collections.sort(sortedTags, new Comparator<TagWrapper>() {

                    @Override
                    public int compare(TagWrapper t1, TagWrapper t2) {
                        if (t1 == null || t1.getTag().getName() == null) {
                            return -1;
                        }
                        return t1.getTag().getName().compareTo(t2.getTag().getName());
                    }
                });
            }

            // sort apis of each declaration
            if (this.options.isSortApisByPath()) {
                Collections.sort(sortedDeclarations, new Comparator<Map.Entry<String, Path>>() {
                    @Override
                    public int compare(Map.Entry<String, Path> e1, Map.Entry<String, Path> e2) {
                        return e1.getKey().compareTo(e2.getKey());
                    }
                });
            }

            for (Map.Entry<String, Path> entry : sortedDeclarations) {
                declarations.put(entry.getKey(), entry.getValue());
            }

            for (TagWrapper tag: sortedTags) {
                swagger.addTag(tag.getTag());
            }

            List<Scheme> schemes = new ArrayList<>();
            for (String s : this.options.getSchemes()) {
                schemes.add(Scheme.forValue(s));
            }

            this.options.getApiInfo().setVersion(this.options.getApiVersion());

            swagger.paths(declarations);
            swagger.setSchemes(schemes);
            swagger.setHost(this.options.getHost());
            swagger.setBasePath(this.options.getApiBasePath());
            swagger.setInfo(this.options.getApiInfo());


            writeApis(swagger);
            // Copy swagger-ui into the output directory.
            if (this.options.isIncludeSwaggerUi()) {
                copyUi();
            }
            return true;
        } catch (IOException e) {
            System.err.println("Failed to write api docs, err msg: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void writeApis(Swagger swagger) throws IOException {

        File outputDirectory = this.options.getOutputDirectory();
        Recorder recorder = this.options.getRecorder();

        File docFile = new File(outputDirectory, "service.json");
        recorder.record(docFile, swagger);

    }

    private void copyUi() throws IOException {
        File outputDirectory = this.options.getOutputDirectory();
        if (outputDirectory == null) {
            outputDirectory = new File(".");
        }
        Recorder recorder = this.options.getRecorder();
        String uiPath = this.options.getSwaggerUiPath();

        if (uiPath == null) {
            // default inbuilt zip
            copyZip(recorder, null, outputDirectory);
        } else {
            // zip or dir
            File uiPathFile = new File(uiPath);
            if (uiPathFile.isDirectory()) {
                System.out.println("Using swagger dir from: " + uiPathFile.getAbsolutePath());
                copyDirectory(recorder, uiPathFile, uiPathFile, outputDirectory);
            } else if (!uiPathFile.exists()) {
                File f = new File(".");
                System.out.println("SwaggerDoclet working directory: " + f.getAbsolutePath());
                System.out.println("-swaggerUiPath not set correctly as it did not exist: " + uiPathFile.getAbsolutePath());
                throw new RuntimeException("-swaggerUiPath not set correctly as it did not exist: " + uiPathFile.getAbsolutePath());
            } else {
                copyZip(recorder, uiPathFile, outputDirectory);
            }
        }
    }

    private void copyZip(Recorder recorder, File uiPathFile, File outputDirectory) throws IOException {
        ZipInputStream swaggerZip = null;
        try {
            if (uiPathFile == null) {
                swaggerZip = new ZipInputStream(ServiceDoclet.class.getResourceAsStream("/swagger-ui-" + SWAGGER_UI_VERSION + ".zip"));
                System.out.println("Using default swagger-ui.zip file from SwaggerDoclet jar file");
            } else {
                swaggerZip = new ZipInputStream(new FileInputStream(uiPathFile));
                System.out.println("Using swagger-ui.zip file from: " + uiPathFile.getAbsolutePath());
            }

            ZipEntry entry = swaggerZip.getNextEntry();
            while (entry != null) {
                final File swaggerFile = new File(outputDirectory, entry.getName());
                if (entry.isDirectory()) {
                    if (!swaggerFile.isDirectory() && !swaggerFile.mkdirs()) {
                        throw new RuntimeException("Unable to create directory: " + swaggerFile);
                    }
                } else {

                    FileOutputStream outputStream = null;
                    try {
                        outputStream = new FileOutputStream(swaggerFile);
                        ByteStreams.copy(swaggerZip, outputStream);
                        outputStream.flush();
                    } finally {
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }

                }

                entry = swaggerZip.getNextEntry();
            }

        } finally {
            if (swaggerZip != null) {
                swaggerZip.close();
            }
        }
    }

    private void copyDirectory(Recorder recorder, File uiPathFile, File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                if (!targetLocation.mkdirs()) {
                    throw new IOException("Failed to create the dir: " + targetLocation.getAbsolutePath());
                }
            }

            String[] children = sourceLocation.list();
            if (children != null) {
                for (String element : children) {
                    copyDirectory(recorder, uiPathFile, new File(sourceLocation, element), new File(targetLocation, element));
                }
            }
        } else {

            InputStream in = null;
            OutputStream out = null;
            try {
                in = new FileInputStream(sourceLocation);
                out = new FileOutputStream(targetLocation);
                ByteStreams.copy(in, out);
                out.flush();

            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                }
            }
        }
    }

}
