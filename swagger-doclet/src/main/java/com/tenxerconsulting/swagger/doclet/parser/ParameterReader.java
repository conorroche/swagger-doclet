package com.tenxerconsulting.swagger.doclet.parser;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.sun.javadoc.*;
import com.tenxerconsulting.swagger.doclet.DocletOptions;
import com.tenxerconsulting.swagger.doclet.model.ModelWrapper;
import com.tenxerconsulting.swagger.doclet.model.PropertyWrapper;
import com.tenxerconsulting.swagger.doclet.parser.ParserHelper.NumericTypeFilter;
import com.tenxerconsulting.swagger.doclet.translator.Translator;
import com.tenxerconsulting.swagger.doclet.translator.Translator.OptionalName;
import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.RefModel;
import io.swagger.models.parameters.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.*;

/**
 * The ParameterReader represents a utility class that supports reading api parameters
 *
 * @author conor.roche
 * @version $Id$
 */
public class ParameterReader {

    private static final String JAX_RS_PATH_PARAM = "javax.ws.rs.PathParam";
    private static final String JAX_RS_PATH = "javax.ws.rs.Path";

    private static final Pattern PARAM_PATTERN = Pattern.compile("\\{[^}]+\\}");

    private final DocletOptions options;
    private final Collection<ClassDoc> allClasses;
    private final Translator translator;

    private List<String> paramNames;
    private ClassDoc classDoc;

    /**
     * This creates a PathParameterReader
     *
     * @param options    the doclet options
     * @param allClasses All classes for looking up types
     */
    public ParameterReader(DocletOptions options, Collection<ClassDoc> allClasses) {
        this.options = options;
        this.translator = options == null ? null : options.getTranslator();
        this.allClasses = allClasses;
    }

    /**
     * This reads a class storing any path parameters from the class level
     *
     * @param classDoc The class to read
     */
    public void readClass(ClassDoc classDoc) {
        // reset paramNames
        if (this.paramNames == null) {
            this.paramNames = new ArrayList<String>();
        } else {
            this.paramNames.clear();
        }

        this.classDoc = classDoc;
        String classLevelPath = ParserHelper.getInheritableClassLevelAnnotationValue(this.classDoc, this.options, JAX_RS_PATH, "value");
        if (classLevelPath != null) {
            // extract names/regex patterns of all params from the path
            addPathParams(classLevelPath, this.paramNames);
        }

    }

    void addPathParams(String path, Collection<String> addToCollection) {
        Matcher m = PARAM_PATTERN.matcher(path);
        while (m.find()) {
            int start = m.start();
            int end = m.end();
            if (start < end - 1) {
                String withBraces = path.substring(start, end);
                String withoutBraces = withBraces.substring(1, withBraces.length() - 1).trim();
                int colonPos = withBraces.indexOf(':');
                String name = colonPos == -1 ? withoutBraces : withoutBraces.substring(0, colonPos - 1);
                addToCollection.add(name);
            }
        }
    }

    /**
     * This gets the names of parameters in the class level @Path expression
     *
     * @return the list of parameter names reference in the class level expression
     */
    public List<String> getClassLevelParamNames() {
        return this.paramNames;
    }

    /**
     * This reads a list of class parameters. For a class parameter to be returned it must
     * a) be a variable in the path of the class or one of its methods and b) have a PathParam
     * annotation either on a field or constructor
     *
     * @param models The model set to add any new encountered models to
     * @return A list of class level api parameters
     */
    public List<Parameter> readClassLevelParameters(Set<ModelWrapper> models) {

        List<Parameter> params = new ArrayList<>();

        // add field path params
        FieldDoc[] fields = this.classDoc.fields(false);
        if (fields != null) {
            for (FieldDoc field : fields) {
                AnnotationParser p = new AnnotationParser(field, this.options);
                if (p.isAnnotatedBy(JAX_RS_PATH_PARAM)) {
                    String paramName = p.getAnnotationValue(JAX_RS_PATH_PARAM, "value");
                    if (paramName == null || paramName.isEmpty()) {
                        paramName = field.name();
                    }

                    // if this path param is one of the param names of the class path then add it
                    if (this.paramNames.contains(paramName)) {
                        params.add(buildClassFieldApiParam(field));
                    }
                }
            }
        }

        // add constructor path params
        ConstructorDoc[] constructors = this.classDoc.constructors(false);
        for (ConstructorDoc constructor : constructors) {
            com.sun.javadoc.Parameter[] parameters = constructor.parameters();
            for (com.sun.javadoc.Parameter param : parameters) {
                AnnotationParser p = new AnnotationParser(param, this.options);
                if (p.isAnnotatedBy(JAX_RS_PATH_PARAM)) {
                    String paramName = p.getAnnotationValue(JAX_RS_PATH_PARAM, "value");
                    if (paramName == null || paramName.isEmpty()) {
                        paramName = param.name();
                    }

                    // if this path param is one of the param names of the class path then add it
                    if (this.paramNames.contains(paramName)) {

                        Set<String> rawParamNames = ParserHelper.getParamNames(constructor);
                        Set<String> allParamNames = new HashSet<String>(rawParamNames);

                        params.addAll(buildApiParams(constructor, param, false, allParamNames, models));
                    }
                }
            }
        }

        return params;
    }

    /**
     * This builds an Api parameter from a method or constructor parameter
     *
     * @param method            The method or constructor
     * @param parameter         The parameter to build the api parameter for
     * @param consumesMultipart whether the method consumes multipart
     * @param allParamNames     A list of all parameter names on the method
     * @param models            The model set to add any new encountered models to
     * @return The list of parameters (can be multiple if bean params are encountered)
     */
    public List<Parameter> buildApiParams(ExecutableMemberDoc method, com.sun.javadoc.Parameter parameter, boolean consumesMultipart, Set<String> allParamNames,
                                                                       Set<ModelWrapper> models) {

        // TODO cache these constructor/method level params so they are not reprocessed for each parameter

        // read required and optional params
        Set<String> optionalParams = ParserHelper.getMatchingParams(method, allParamNames, this.options.getOptionalParamsTags(),
                this.options.getOptionalParamAnnotations(), this.options);

        Set<String> requiredParams = ParserHelper.getMatchingParams(method, allParamNames, this.options.getRequiredParamsTags(),
                this.options.getRequiredParamAnnotations(), this.options);

        // read csv params
        List<String> csvParams = ParserHelper.getCsvParams(method, allParamNames, this.options.getCsvParamsTags(), this.options);

        // read formats
        Map<String, String> paramFormats = ParserHelper.getMethodParamNameValuePairs(method, allParamNames, this.options.getParamsFormatTags(), this.options);

        // read min and max values of params
        Map<String, String> paramMinVals = ParserHelper.getParameterValues(method, allParamNames, this.options.getParamsMinValueTags(),
                this.options.getParamMinValueAnnotations(), new NumericTypeFilter(this.options), this.options, new String[]{"value", "min"});
        Map<String, String> paramMaxVals = ParserHelper.getParameterValues(method, allParamNames, this.options.getParamsMaxValueTags(),
                this.options.getParamMaxValueAnnotations(), new NumericTypeFilter(this.options), this.options, new String[]{"value", "max"});

        // filter min/max params so they

        // read default values of params
        Map<String, String> paramDefaultVals = ParserHelper.getMethodParamNameValuePairs(method, allParamNames, this.options.getParamsDefaultValueTags(),
                this.options);

        // read allowable values of params
        Map<String, List<String>> paramAllowableVals = ParserHelper.getMethodParamNameValueLists(method, allParamNames,
                this.options.getParamsAllowableValuesTags(), this.options);

        // read override names of params
        Map<String, String> paramNames = ParserHelper.getMethodParamNameValuePairs(method, allParamNames, this.options.getParamsNameTags(), this.options);

        Type paramType = getParamType(parameter.type());
        String paramCategory = ParserHelper.paramTypeOf(consumesMultipart, parameter, this.options);
        String paramName = parameter.name();

        List<Parameter> res = new ArrayList<>();

        // see if its a special composite type e.g. @BeanParam
        if ("composite".equals(paramCategory)) {

            ApiModelParser modelParser = new ApiModelParser(this.options, this.translator, paramType, consumesMultipart, true);
            Set<ModelWrapper> compositeModels = modelParser.parse();
            String rootModelId = modelParser.getRootModelId();
            for (ModelWrapper modelWrapper : compositeModels) {
                io.swagger.models.Model model = modelWrapper.getModel();

                if (model.getReference().equals(rootModelId)) {
					for (Map.Entry<String, PropertyWrapper> entry : modelWrapper.getProperties().entrySet()) {
                        io.swagger.models.properties.Property property = entry.getValue().getProperty();
                        String rawFieldName = entry.getValue().getRawFieldName();

                        res.add(getParameter(property, paramCategory, rawFieldName, csvParams));
                    }
                }
            }

            return res;
        }

        ClassDoc[] viewClasses = ParserHelper.getInheritableJsonViews(method, parameter, this.options);

        // look for a custom input type for body params
        if ("body".equals(paramCategory)) {
            String customParamType = ParserHelper.getInheritableTagValue(method, this.options.getInputTypeTags(), this.options);
            paramType = readCustomParamType(customParamType, paramType, models, viewClasses);
        }

        OptionalName paramTypeFormat = this.translator
                .parameterTypeName(consumesMultipart, parameter, paramType, this.options.isUseFullModelIds(), viewClasses);
        String typeName = paramTypeFormat.value();
        String format = paramTypeFormat.getFormat();

        // overide format if possible
        if (format == null) {
            format = paramFormats.get(paramName);
        }

        Boolean allowMultiple = null;
        List<String> allowableValues = null;
        String itemsRef = null;
        String itemsType = null;
        String itemsFormat = null;
        List<String> itemsAllowableValues = null;
        Boolean uniqueItems = null;
        String minimum = null;
        String maximum = null;
        String defaultVal = null;

        // set to form param type if data type is File
        if ("File".equals(typeName)) {
            paramCategory = "form";
        } else {

            Type containerOf = ParserHelper.getContainerType(paramType, null, this.allClasses);

            if (this.options.isParseModels()) {
                Type modelType = containerOf == null ? paramType : containerOf;
                models.addAll(new ApiModelParser(this.options, this.translator, modelType, viewClasses, this.allClasses).parse());
            }

            // set enum values
            // a) if param type is enum build based on enum values
            ClassDoc typeClassDoc = parameter.type().asClassDoc();
            allowableValues = ParserHelper.getAllowableValues(typeClassDoc);
            if (allowableValues == null) {
                // b) if the method has a javadoc tag for allowable values use that
                allowableValues = paramAllowableVals.get(paramName);
            }

            if (allowableValues != null && !allowableValues.isEmpty()) {
                typeName = "string";
            }

            // set whether its a csv param
            allowMultiple = getAllowMultiple(paramCategory, paramName, csvParams);

            // get min and max param values
            minimum = paramMinVals.get(paramName);
            maximum = paramMaxVals.get(paramName);

            String validationContext = " for the method: " + method.name() + " parameter: " + paramName;

            // verify min max are numbers
            ParserHelper.verifyNumericValue(validationContext + " min value.", typeName, format, minimum);
            ParserHelper.verifyNumericValue(validationContext + " max value.", typeName, format, maximum);

            // get a default value, prioritize the jaxrs annotation
            // otherwise look for the javadoc tag
            defaultVal = ParserHelper.getDefaultValue(parameter, this.options);
            if (defaultVal == null) {
                defaultVal = paramDefaultVals.get(paramName);
            }

            // verify default vs min, max and by itself
            if (defaultVal != null) {
                if (minimum == null && maximum == null) {
                    // just validate the default
                    ParserHelper.verifyValue(validationContext + " default value.", typeName, format, defaultVal);
                }
                // if min/max then default is validated as part of comparison
                if (minimum != null) {
                    int comparison = ParserHelper.compareNumericValues(validationContext + " min value.", typeName, format, defaultVal, minimum);
                    if (comparison < 0) {
                        throw new IllegalStateException("Invalid value for the default value of the method: " + method.name() + " parameter: " + paramName
                                + " it should be >= the minimum: " + minimum);
                    }
                }
                if (maximum != null) {
                    int comparison = ParserHelper.compareNumericValues(validationContext + " max value.", typeName, format, defaultVal, maximum);
                    if (comparison > 0) {
                        throw new IllegalStateException("Invalid value for the default value of the method: " + method.name() + " parameter: " + paramName
                                + " it should be <= the maximum: " + maximum);
                    }
                }

                // if boolean then make lowercase
                if ("boolean".equalsIgnoreCase(typeName)) {
                    defaultVal = defaultVal.toLowerCase();
                }
            }

            // if enum and default value check it matches the enum values
            if (allowableValues != null && defaultVal != null && !allowableValues.contains(defaultVal)) {
                throw new IllegalStateException("Invalid value: " + defaultVal + " for the default value of the method: " + method.name() + " parameter: "
                        + paramName + " it should be one of: " + allowableValues);
            }

            // set collection related fields
            // TODO: consider supporting parameterized collections as api parameters...
            if (containerOf != null) {
                itemsAllowableValues = ParserHelper.getAllowableValues(containerOf.asClassDoc());
                if (itemsAllowableValues != null) {
                    itemsType = "string";
                } else {
                    OptionalName oName = this.translator.typeName(containerOf, this.options.isUseFullModelIds(), viewClasses);
                    if (ParserHelper.isPrimitive(containerOf, this.options)) {
                        itemsType = oName.value();
                        itemsFormat = oName.getFormat();
                    } else {
                        itemsRef = oName.value();
                    }
                }
            }

            if (typeName.equals("array")) {
                if (ParserHelper.isSet(paramType.qualifiedTypeName())) {
                    uniqueItems = Boolean.TRUE;
                }
            }
        }

        // get whether required
        Boolean required = getRequired(paramCategory, paramName, typeName, optionalParams, requiredParams);

        // get the parameter name to use for the documentation
        String renderedParamName = ParserHelper.paramNameOf(parameter, paramNames, this.options.getParameterNameAnnotations(), this.options);

        // get description
        String description = this.options.replaceVars(commentForParameter(method, parameter));

        // build parameter
        Parameter param = buildParameter(paramCategory, renderedParamName, required, allowMultiple, typeName, format, description, itemsRef, itemsType,
                itemsFormat, itemsAllowableValues, uniqueItems, allowableValues, minimum, maximum, defaultVal);

        res.add(param);

        return res;
    }

    private Parameter getParameter(io.swagger.models.properties.Property property, String paramCategory, String rawFieldName, List<String> csvParams) {
        Boolean allowMultiple = getAllowMultiple(paramCategory, rawFieldName, csvParams);

        // see if there is a required javadoc tag directly on the bean param field, if so use that
        Boolean required;
        if (property.getRequired()) {
            required = true;
        } else {
            required = getRequired(paramCategory, rawFieldName, property.getType(), null, null);
        }

        String itemsRef = null;// = property.getItems() == null ? null : property.getItems().getRef();
        if (property instanceof RefProperty) {
            itemsRef = ((RefProperty) property).get$ref();
        }
        String itemsType = null; //property.getItems() == null ? null : property.getItems().getType();
        String itemsFormat = null; //property.getItems() == null ? null : property.getItems().getFormat();
        List<String> itemsAllowableValues = null; //property.getItems() == null ? null : property.getItems().getAllowableValues();
        boolean uniqueItems = false;
        if (property instanceof ArrayProperty) {
            ArrayProperty resolved = (ArrayProperty) property;
            itemsType = resolved.getItems().getType();
            itemsFormat = resolved.getItems().getFormat();
            uniqueItems = resolved.getUniqueItems();
            if (resolved.getItems() instanceof StringProperty) {
                itemsAllowableValues = ((StringProperty) resolved.getItems()).getEnum();
            }
        }

        String minimum = null;
        String maximum = null;

        if (property instanceof AbstractNumericProperty) {
            AbstractNumericProperty resolved = (AbstractNumericProperty) property;
            minimum = String.valueOf(resolved.getMinimum());
            maximum = String.valueOf(resolved.getMaximum());
        }

        String defaultValue = null;
        List<String> allowableValues = null;
        if (property instanceof BooleanProperty) {
            BooleanProperty resolved = (BooleanProperty) property;
            defaultValue = String.valueOf(resolved.getDefault());
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof IntegerProperty) {
            IntegerProperty resolved = (IntegerProperty) property;
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof LongProperty) {
            LongProperty resolved = (LongProperty) property;
            defaultValue = String.valueOf(resolved.getDefault());
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof FloatProperty) {
            FloatProperty resolved = (FloatProperty) property;
            defaultValue = String.valueOf(resolved.getDefault());
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof DoubleProperty) {
            DoubleProperty resolved = (DoubleProperty) property;
            defaultValue = String.valueOf(resolved.getDefault());
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof UUIDProperty) {
            UUIDProperty resolved = (UUIDProperty) property;
            defaultValue = String.valueOf(resolved.getDefault());
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof DateProperty) {
            DateProperty resolved = (DateProperty) property;
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof DateTimeProperty) {
            DateTimeProperty resolved = (DateTimeProperty) property;
            allowableValues = resolved.getEnum()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
        } else if (property instanceof StringProperty) {
            StringProperty resolved = (StringProperty) property;
            defaultValue = resolved.getDefault();
            allowableValues = resolved.getEnum();
        }

        return buildParameter(paramCategory, property.getName(), required, allowMultiple, property.getType(),
                property.getFormat(), property.getDescription(), itemsRef, itemsType, itemsFormat, itemsAllowableValues,
                uniqueItems, allowableValues, minimum, maximum, defaultValue);
    }

    /**
     * This reads implicit params from the javadoc of the method or class using the @implicitParam tag
     *
     * @param method            The method to read
     * @param consumesMultipart
     * @param models            A set of models to add any custom param type to
     * @return A list of implicit method parameters
     */
    public List<Parameter> readImplicitParameters(ExecutableMemberDoc method, boolean consumesMultipart, Set<ModelWrapper> models) {
        List<Parameter> params = new ArrayList<>();
        // add on any extra parameters defined in the javadoc of the method
        List<String> implicitParamDefs = new ArrayList<>();
        List<String> methodImplicitParamDefs = ParserHelper.getInheritableTagValues(method, this.options.getImplicitParamTags(), this.options);
        if (methodImplicitParamDefs != null) {
            implicitParamDefs.addAll(methodImplicitParamDefs);
        }
        // and also in the class the method is on
        List<String> classImplicitParamDefs = ParserHelper.getInheritableTagValues(method.containingClass(), this.options.getImplicitParamTags(), this.options);
        if (classImplicitParamDefs != null) {
            implicitParamDefs.addAll(classImplicitParamDefs);
        }

        for (String implicitParamDef : implicitParamDefs) {
            Parameter param = buildImplicitApiParam(implicitParamDef, models);
            if (param != null) {
                params.add(param);
            }
        }
        return params;
    }

    private Parameter buildImplicitApiParam(String implicitParamDef, Set<ModelWrapper> models) {
        // format of param def is:
        // name|dataType|paramType|required|defaultValue|minValue|maxValue|allowableValues|allowMultiple|description
        // name, datatype are required; other fields can be left empty
        // allowableValues is a CSV
        String[] parts = implicitParamDef.split("\\|");

        boolean useFqn = this.options.isUseFullModelIds();

        // NOTE for now we are not supporting complex types as implicit params
        // e.g. we don't support arrays, or collections as the main use case is for implicit headers
        // which are just strings. We may add array/collection support at a later stage if necessary

        String paramName = parts[0];
        String dataTypeFqn = parts[1];
        String[] typeFormat = ParserHelper.typeOf(dataTypeFqn, useFqn, this.options);
        String typeName = typeFormat[0];
        String format = typeFormat[1];

        // if its not a primitive add the model
        if (this.options.isParseModels() && !ParserHelper.isPrimitive(typeName, this.options)) {
            Type modelType = ParserHelper.findModel(this.allClasses, dataTypeFqn);
            if (modelType == null) {
                throw new IllegalStateException(
                        "Could not find the source for the parameter "
                                + paramName
                                + " class: "
                                + dataTypeFqn
                                + ". If it is not in the same project as the one you have added the doclet to, "
                                + "for example if it is in a dependent project then you should copy the source to the doclet calling project using the maven-dependency-plugin's unpack goal,"
                                + " and then add it to the source using the build-helper-maven-plugin's add-source goal.");
            }
            models.addAll(new ApiModelParser(this.options, this.translator, modelType, null, this.allClasses).parse());
        }

        String paramCategory = parts[2];
        Boolean required = hasValAtPos(parts, 3) ? Boolean.valueOf(parts[3].trim()) : getRequired(paramCategory, paramName, typeName, null, null);
        String defaultVal = hasValAtPos(parts, 4) ? parts[4].trim() : null;
        String minimum = hasValAtPos(parts, 5) ? parts[5].trim() : null;
        String maximum = hasValAtPos(parts, 6) ? parts[6].trim() : null;
        List<String> allowableValues = null;
        if (hasValAtPos(parts, 7)) {
            String[] vals = parts[7].trim().split(",");
            allowableValues = new ArrayList<>(vals.length);
            for (String val : vals) {
                if (!val.trim().isEmpty()) {
                    allowableValues.add(val.trim());
                }
            }
        }
        Boolean allowMultiple = hasValAtPos(parts, 8) ? Boolean.valueOf(parts[8].trim()) : null;
        String description = hasValAtPos(parts, 9) ? parts[9].trim() : null;

        // collection fields, not supported at present
        String itemsRef = null;
        String itemsType = null;
        String itemsFormat = null;
        List<String> itemsAllowableValues = null;
        Boolean uniqueItems = null;

        // build parameter
        Parameter param = buildParameter(paramCategory, paramName, required, allowMultiple, typeName, format, description, itemsRef, itemsType,
                itemsFormat, itemsAllowableValues, uniqueItems, allowableValues, minimum, maximum, defaultVal);

        return param;
    }

    private boolean hasValAtPos(String[] parts, int pos) {
        if (pos < parts.length) {
            if (parts[pos] != null && !parts[pos].trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Parameter buildClassFieldApiParam(FieldDoc field) {

        Type paramType = field.type();

        OptionalName paramTypeFormat = this.translator.typeName(paramType, this.options.isUseFullModelIds(), null);
        String typeName = paramTypeFormat.value();
        String format = paramTypeFormat.getFormat();

        Boolean allowMultiple = null;
        List<String> allowableValues = null;
        String itemsRef = null;
        String itemsType = null;
        String itemsFormat = null;
        List<String> itemsAllowableValues = null;
        Boolean uniqueItems = null;
        String minimum = null;
        String maximum = null;
        String defaultVal = null;

        Type containerOf = ParserHelper.getContainerType(paramType, null, this.allClasses);

        // set enum values
        ClassDoc typeClassDoc = paramType.asClassDoc();
        allowableValues = ParserHelper.getAllowableValues(typeClassDoc);
        if (allowableValues != null) {
            typeName = "string";
        }

        // TODO set whether its a csv param
        // TODO get min and max param values
        // TODO set default

        // set collection related fields
        // TODO: consider supporting parameterized collections as api parameters...
        if (containerOf != null) {
            itemsAllowableValues = ParserHelper.getAllowableValues(containerOf.asClassDoc());
            if (itemsAllowableValues != null) {
                itemsType = "string";
            } else {
                OptionalName oName = this.translator.typeName(containerOf, this.options.isUseFullModelIds());
                if (ParserHelper.isPrimitive(containerOf, this.options)) {
                    itemsType = oName.value();
                    itemsFormat = oName.getFormat();
                } else {
                    itemsRef = oName.value();
                }
            }
        }

        if (typeName.equals("array")) {
            if (ParserHelper.isSet(paramType.qualifiedTypeName())) {
                uniqueItems = Boolean.TRUE;
            }
        }

        // get whether required
        Boolean required = Boolean.TRUE;

        // get the parameter name to use for the documentation
        // TODO support name overriding
        Map<String, String> overrideParamNames = null;
        String renderedParamName = ParserHelper.fieldParamNameOf(field, overrideParamNames, this.options.getParameterNameAnnotations(), this.options);

        // TODO get description from field
        String description = null;

        // build parameter
        Parameter param = buildParameter("path", renderedParamName, required, allowMultiple, typeName, format, description, itemsRef, itemsType,
                itemsFormat, itemsAllowableValues, uniqueItems, allowableValues, minimum, maximum, defaultVal);

        return param;

    }

    private String commentForParameter(ExecutableMemberDoc method, com.sun.javadoc.Parameter parameter) {
        while (method != null) {
            for (ParamTag tag : method.paramTags()) {
                if (tag.parameterName().equals(parameter.name())) {
                    return tag.parameterComment();
                }
            }
            // didn't find a documentation on this method, maybe we can find it on a method we inherited from.
            method = ParserHelper.getAncestorMethod(method);
        }
        return "";
    }

    private Boolean getAllowMultiple(String paramCategory, String paramName, List<String> csvParams) {
        Boolean allowMultiple = null;
        if ("query".equals(paramCategory) || "path".equals(paramCategory) || "header".equals(paramCategory)) {
            if (csvParams != null && csvParams.contains(paramName)) {
                allowMultiple = Boolean.TRUE;
            }
        }
        return allowMultiple;
    }

    private Boolean getRequired(String paramCategory, String paramName, String typeName, Collection<String> optionalParams, Collection<String> requiredParams) {
        // set whether the parameter is required or not
        Boolean required = null;
        // if its a path param then its required as per swagger spec
        if ("path".equals(paramCategory)) {
            required = Boolean.TRUE;
        }
        // if its in the required list then its required
        else if (requiredParams != null && requiredParams.contains(paramName)) {
            required = Boolean.TRUE;
        }
        // else if its in the optional list its optional
        else if (optionalParams != null && optionalParams.contains(paramName)) {
            // leave as null as this is equivalent to false but doesn't add to the json
        }
        // else if its a body or File param its required
        else if ("body".equals(paramCategory) || ("File".equals(typeName) && "form".equals(paramCategory))) {
            required = Boolean.TRUE;
        }
        // otherwise its optional
        else {
            // leave as null as this is equivalent to false but doesn't add to the json
        }
        return required;
    }

    private Type getParamType(Type type) {
        if (type != null) {
            ParameterizedType pt = type.asParameterizedType();
            if (pt != null) {
                Type[] typeArgs = pt.typeArguments();
                if (typeArgs != null && typeArgs.length > 0) {
                    // if its a generic wrapper type then return the wrapped type
                    if (this.options.getGenericWrapperTypes().contains(type.qualifiedTypeName())) {
                        return typeArgs[0];
                    }
                }
            }
        }
        return type;
    }

    private Type readCustomParamType(String customTypeName, Type defaultType, Set<ModelWrapper> models, ClassDoc[] viewClasses) {
        if (customTypeName != null) {
            // lookup the type from the doclet classes
            Type customType = ParserHelper.findModel(this.allClasses, customTypeName);
            if (customType != null) {
                // also add this custom return type to the models
                if (this.options.isParseModels()) {
                    models.addAll(new ApiModelParser(this.options, this.translator, customType, viewClasses, this.allClasses).parse());
                }
                return customType;
            }
        }
        return defaultType;
    }

    private Parameter buildParameter(String paramCategory, String name, Boolean required, Boolean allowMultiple, String type, String format, String description,
                                                                  String itemsRef, String itemsType, String itemsFormat, List<String> itemsAllowableValues, Boolean uniqueItems, List<String> allowableValues,
                                                                  String minimum, String maximum, String defaultValue) {
        Parameter param;
        switch (paramCategory) {
            case "path":
                param = new PathParameter();
                break;
            case "query":
                param = new QueryParameter();
                break;
            case "header":
                param = new HeaderParameter();
                break;
            case "form":
                param = new FormParameter();
                break;
            default:
                param = new BodyParameter();
                break;
        }

        Property items = ParserHelper.buildItems(itemsRef, itemsType, itemsFormat, itemsAllowableValues, uniqueItems);

        if (param instanceof AbstractSerializableParameter) {
            AbstractSerializableParameter abstractSerializableParameter = (AbstractSerializableParameter) param;
            if (allowMultiple != null && allowMultiple) {
                abstractSerializableParameter.setType("array");

                if (param instanceof FormParameter) {
                    abstractSerializableParameter.setCollectionFormat("multi");
                } else {
                    abstractSerializableParameter.setCollectionFormat("csv");
                }

                Property multiParam = ParserHelper.buildItems(null, type, format, null, null);
                abstractSerializableParameter.setItems(multiParam);
            } else {
                abstractSerializableParameter.setItems(items);
                abstractSerializableParameter.setType(type);
            }

            if (minimum != null) {
                abstractSerializableParameter.setMinimum(new BigDecimal(minimum));
            }
            if (maximum != null) {
                abstractSerializableParameter.setMaximum(new BigDecimal(maximum));
            }
            abstractSerializableParameter.setEnum(allowableValues);
            abstractSerializableParameter.setDefault(defaultValue);
            abstractSerializableParameter.setFormat(format);

        } else {
            Model model;

            if (ParserHelper.isPrimitive(type, this.options)) {
                ModelImpl modelImpl = new ModelImpl()
                        .format(format)
                        .type(type)
                        ._enum(allowableValues);
                if (minimum != null) {
                    modelImpl.minimum(new BigDecimal(minimum));
                }
                if (maximum != null) {
                    modelImpl.maximum(new BigDecimal(maximum));
                }
                modelImpl.setDefaultValue(defaultValue);

                model = modelImpl;
            } else if (ParserHelper.isArray(type) || ParserHelper.isCollection(type)) {
                ArrayModel arrayModel = new ArrayModel();
                arrayModel.setType(type);
                arrayModel.setItems(items);
                model = arrayModel;
            } else {
                model = new RefModel(type);
            }

            ((BodyParameter) param).schema(model);
        }

        if (required != null) {
            param.setRequired(required);
        }
        param.setDescription(description);
        param.setName(name);

        return param;
    }

}
