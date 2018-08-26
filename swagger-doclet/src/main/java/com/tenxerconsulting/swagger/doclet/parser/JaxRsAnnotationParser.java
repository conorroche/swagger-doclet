package com.tenxerconsulting.swagger.doclet.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.google.common.base.Strings;
import com.google.common.io.ByteStreams;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.Type;
import com.tenxerconsulting.swagger.doclet.DocletOptions;
import com.tenxerconsulting.swagger.doclet.Recorder;
import com.tenxerconsulting.swagger.doclet.ServiceDoclet;
import com.tenxerconsulting.swagger.doclet.model.Api;
import com.tenxerconsulting.swagger.doclet.model.ApiDeclaration;
import com.tenxerconsulting.swagger.doclet.model.HttpMethod;
import com.tenxerconsulting.swagger.doclet.model.Operation;
import com.tenxerconsulting.swagger.doclet.model.ResourceListing;
import com.tenxerconsulting.swagger.doclet.model.ResourceListingAPI;

@SuppressWarnings("javadoc")
public class JaxRsAnnotationParser {

	// swagger 1.1 spec see https://groups.google.com/forum/#!topic/swagger-swaggersocket/mHdR9u0utH4
	// diffs between 1.1 and 1.2 see https://github.com/wordnik/swagger-spec/wiki/1.2-transition
	private static final String SWAGGER_VERSION = "1.2";

	private static final String SWAGGER_UI_VERSION = "2.1.0";

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

			List<ApiDeclaration> declarations = null;

			// build up set of subresources
			// do simple parsing to find sub resource classes
			// these are ones referenced in the return types of methods
			// which have a path but no http method
			Map<Type, ClassDoc> subResourceClasses = new HashMap<Type, ClassDoc>();
			for (ClassDoc classDoc : docletClasses) {
				ClassDoc currentClassDoc = classDoc;
				while (currentClassDoc != null) {

					for (MethodDoc method : currentClassDoc.methods()) {
						// if the method has @Path but no Http method then its an entry point to a sub resource
						if (!ParserHelper.resolveMethodPath(method, this.options).isEmpty() && HttpMethod.fromMethod(method) == null) {
							String responseTypeTag = ParserHelper.responseTypeTagOf(method, this.options);
							ClassDoc subResourceClassDoc = responseTypeTag == null ? classCache.findByType(method.returnType()) : classCache.findByName(responseTypeTag);
							if (subResourceClassDoc != null) {
								if (this.options.isLogDebug()) {
									System.out.println("Adding return type as sub resource class : " + subResourceClassDoc.name() + " for method "
											+ method.name() + " of referencing class " + currentClassDoc.name());
								}
								subResourceClasses.put(subResourceClassDoc, subResourceClassDoc);
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
			Map<String, ApiDeclaration> resourceToDeclaration = new HashMap<String, ApiDeclaration>();
			for (ClassDoc classDoc : docletClasses) {
				CrossClassApiParser classParser = new CrossClassApiParser(this.options, classDoc, docletClasses, subResourceClasses, typeClasses,
						SWAGGER_VERSION, this.options.getApiVersion(), this.options.getApiBasePath());
				classParser.parse(resourceToDeclaration);
			}
			Collection<ApiDeclaration> declarationColl = resourceToDeclaration.values();

			if (this.options.isLogDebug()) {
				System.out.println("After parse phase api declarations are: ");
				for (ApiDeclaration apiDec : declarationColl) {
					System.out.println("Api Dec: base path " + apiDec.getBasePath() + ", res path: " + apiDec.getResourcePath());
					for (Api api : apiDec.getApis()) {
						System.out.println("Api path:" + api.getPath());
						for (Operation op : api.getOperations()) {
							System.out.println("Api nick name:" + op.getNickname() + " method " + op.getMethod());
						}
					}
				}
			}

			// add any extra declarations
			if (this.options.getExtraApiDeclarations() != null && !this.options.getExtraApiDeclarations().isEmpty()) {
				declarationColl = new ArrayList<ApiDeclaration>(declarationColl);
				declarationColl.addAll(this.options.getExtraApiDeclarations());
			}

			// set root path on any empty resources
			for (ApiDeclaration api : declarationColl) {
				if (api.getResourcePath() == null || api.getResourcePath().isEmpty() || api.getResourcePath().equals("/")) {
					api.setResourcePath(this.options.getResourceRootPath());
				}
			}

			// merge the api declarations
			declarationColl = new ApiDeclarationMerger(SWAGGER_VERSION, this.options.getApiVersion(), this.options.getApiBasePath()).merge(declarationColl);

			// clear any empty models
			for (ApiDeclaration api : declarationColl) {
				if (api.getModels() != null && api.getModels().isEmpty()) {
					api.setModels(null);
				}
			}

			declarations = new ArrayList<ApiDeclaration>(declarationColl);

			// sort the api declarations if needed
			if (this.options.isSortResourcesByPriority()) {

				Collections.sort(declarations, new Comparator<ApiDeclaration>() {

					public int compare(ApiDeclaration dec1, ApiDeclaration dec2) {
						return Integer.compare(dec1.getPriority(), dec2.getPriority());
					}

				});

			} else if (this.options.isSortResourcesByPath()) {
				Collections.sort(declarations, new Comparator<ApiDeclaration>() {

					public int compare(ApiDeclaration dec1, ApiDeclaration dec2) {
						if (dec1 == null || dec1.getResourcePath() == null) {
							return 1;
						}
						if (dec2 == null || dec2.getResourcePath() == null) {
							return -1;
						}
						return dec1.getResourcePath().compareTo(dec2.getResourcePath());
					}

				});
			}

			// sort apis of each declaration
			if (this.options.isSortApisByPath()) {
				for (ApiDeclaration dec : declarations) {
					if (dec.getApis() != null) {
						Collections.sort(dec.getApis(), new Comparator<Api>() {

							public int compare(Api o1, Api o2) {
								if (o1 == null || o1.getPath() == null) {
									return -1;
								}
								return o1.getPath().compareTo(o2.getPath());
							}
						});
					}
				}
			}

			writeApis(declarations);
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

	private void writeApis(Collection<ApiDeclaration> apis) throws IOException {

		List<ResourceListingAPI> resources = new LinkedList<ResourceListingAPI>();
		File outputDirectory = this.options.getOutputDirectory();
		Recorder recorder = this.options.getRecorder();
		for (ApiDeclaration api : apis) {
			String resourcePath = api.getResourcePath();
			if (!Strings.isNullOrEmpty(resourcePath)) {
				// make sure the filename for the resource is valid
				String resourceFile = ParserHelper.generateResourceFilename(resourcePath);
				resources.add(new ResourceListingAPI("/" + resourceFile + ".{format}", api.getDescription()));
				File apiFile = new File(outputDirectory, resourceFile + ".json");
				recorder.record(apiFile, api);
			}
		}

		// write out json for the resource listing
		ResourceListing listing = new ResourceListing(SWAGGER_VERSION, this.options.getApiVersion(), this.options.getDocBasePath(), resources,
				this.options.getApiAuthorizations(), this.options.getApiInfo());
		File docFile = new File(outputDirectory, "service.json");
		recorder.record(docFile, listing);

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
