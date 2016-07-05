package com.tenxerconsulting.swagger.doclet.parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationValue;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.ProgramElementDoc;
import com.sun.tools.javadoc.ParameterizedTypeImpl;
import com.tenxerconsulting.swagger.doclet.DocletOptions;

/**
 * The AnnotationParser represents a utility class for reading values from annotations
 * @version $Id$
 */
public class AnnotationParser {

	private final AnnotationDesc[] annotations;
	private final DocletOptions options;

	/**
	 * This creates an AnnotationParser for a method/field
	 * @param element The method/field javadoc item
	 * @param options The doclet options
	 */
	public AnnotationParser(ProgramElementDoc element, DocletOptions options) {
		this.annotations = element.annotations();
		this.options = options;
	}

	/**
	 * This creates an AnnotationParser for a parameter
	 * @param parameter The parameter javadoc item
	 * @param options The doclet options
	 */
	public AnnotationParser(Parameter parameter, DocletOptions options) {
		this.annotations = parameter.annotations();
		this.options = options;
	}

	/**
	 * This gets the value of the first annotation with the given FQN and attribute named key
	 * @param qualifiedAnnotationTypes The FQN of the annotations to get the value of
	 * @param keys The name of the attribute(s) of the annotation to get the value of
	 * @return The value of the first annotation with the given FQN and attribute named key or null if none matched
	 */
	public String getAnnotationValue(Collection<String> qualifiedAnnotationTypes, String... keys) {
		for (String qualifiedAnnotationType : qualifiedAnnotationTypes) {
			AnnotationDesc annotation = getAnnotation(qualifiedAnnotationType, false);
			if (annotation != null) {
				String value = getAnnotationValue(annotation, keys);
				if (value != null) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * This gets the value of the annotation with the given FQN and attribute named key
	 * @param qualifiedAnnotationType The FQN of the annotation to get the value of
	 * @param keys The name of the attribute(s) of the annotation to get the value of
	 * @return The value of the given named attribute of the given annotation or null if it was not present
	 */
	public String getAnnotationValue(String qualifiedAnnotationType, String... keys) {
		AnnotationDesc annotation = getAnnotation(qualifiedAnnotationType, false);
		if (annotation == null) {
			return null;
		}
		return getAnnotationValue(annotation, keys);
	}

	private String getAnnotationValue(AnnotationDesc annotation, String... keys) {
		for (AnnotationDesc.ElementValuePair evp : annotation.elementValues()) {
			for (String key : keys) {
				if (evp.element().name().equals(key)) {
					String val = evp.value().value().toString();
					val = val.trim();
					return this.options.replaceVars(val);
				}
			}
		}
		return null;
	}

	private ClassDoc getAnnotationClassDocValue(AnnotationDesc annotation, String key) {
		for (AnnotationDesc.ElementValuePair evp : annotation.elementValues()) {
			if (evp.element().name().equals(key)) {
				Object val = evp.value().value();
				if(val instanceof ClassDoc) {
					return (ClassDoc) val;
				}
				if(val instanceof ParameterizedTypeImpl) {
					ParameterizedTypeImpl pVal = (ParameterizedTypeImpl) val;
					return pVal.asClassDoc();
				}
			}
		}
		return null;
	}

	/**
	 * This gets the values of an annotation as class docs
	 * @param qualifiedAnnotationType The FQN of the annotation
	 * @param key The field name of the annotation to get
	 * @return The values or null if none were found
	 */
	public ClassDoc[] getAnnotationClassDocValues(String qualifiedAnnotationType, String key) {
		AnnotationDesc annotation = getAnnotation(qualifiedAnnotationType, false);
		if (annotation == null) {
			return null;
		}
		for (AnnotationDesc.ElementValuePair evp : annotation.elementValues()) {
			if (evp.element().name().equals(key)) {
				Object val = evp.value().value();
				AnnotationValue[] vals = (AnnotationValue[]) val;
				if (vals != null && vals.length > 0) {
					ClassDoc[] res = new ClassDoc[vals.length];
					int i = 0;
					for (AnnotationValue annotationVal : vals) {
						ClassDoc classDoc = (ClassDoc) annotationVal.value();
						res[i++] = classDoc;
					}
					return res;
				}
			}
		}
		return null;
	}

	/**
	 * This gets the values of an annotation as strings
	 * @param qualifiedAnnotationType The FQN of the annotation
	 * @param key The field name of the annotation to get
	 * @return The values or null if none were found
	 */
	public String[] getAnnotationValues(String qualifiedAnnotationType, String key) {
		AnnotationDesc annotation = getAnnotation(qualifiedAnnotationType, false);
		if (annotation == null) {
			return null;
		}
		for (AnnotationDesc.ElementValuePair evp : annotation.elementValues()) {
			if (evp.element().name().equals(key)) {
				Object val = evp.value().value();
				AnnotationValue[] vals = (AnnotationValue[]) val;
				if (vals != null && vals.length > 0) {
					String[] res = new String[vals.length];
					int i = 0;
					for (AnnotationValue annotationVal : vals) {
						String str = annotationVal.value().toString().trim();
						str = this.options.replaceVars(str);
						res[i] = str;
						i++;
					}
					return res;
				}
			}
		}
		return null;
	}

	/**
	 * This gets a list of string values from an annotations field that is itself an array of annotations
	 * @param qualifiedAnnotationType The fqn of the annotation
	 * @param key The key of the annotation field that is the array of annotations
	 * @param subKey The key inside each of the annotations in the array that we want to get the value of
	 * @return A list of string of values
	 */
	public List<ClassDoc> getAnnotationArrayTypes(String qualifiedAnnotationType, String key, String subKey) {
		AnnotationDesc annotation = getAnnotation(qualifiedAnnotationType, false);
		if (annotation == null) {
			return null;
		}
		// we expect a single item which is an array of sub annotations
		for (AnnotationDesc.ElementValuePair evp : annotation.elementValues()) {
			if (evp.element().name().equals(key)) {
				Object val = evp.value().value();
				AnnotationValue[] vals = (AnnotationValue[]) val;
				List<ClassDoc> res = new ArrayList<ClassDoc>();
				for (AnnotationValue annotationVal : vals) {
					AnnotationDesc subAnnotation = (AnnotationDesc) annotationVal.value();
					ClassDoc classDoc = getAnnotationClassDocValue(subAnnotation, "value");
					if (classDoc != null) {
						res.add(classDoc);
					}
				}
				if (res.isEmpty()) {
					return null;
				}
				return res;
			}
		}
		return null;
	}

	/**
	 * This gets whether this is annotated by the given annotation
	 * @param qualifiedAnnotationType The annotation type to check for
	 * @return True if this is annotated by the given annotation
	 */
	public boolean isAnnotatedBy(String qualifiedAnnotationType) {
		return getAnnotation(qualifiedAnnotationType, false) != null;
	}

	/**
	 * This gets whether this is annotated by an annotation that starts with the given prefix
	 * @param qualifiedAnnotationTypePrefix The annotation type prefix to check for
	 * @return True if this is annotated by an annotation that starts with the given prefix
	 */
	public boolean isAnnotatedByPrefix(String qualifiedAnnotationTypePrefix) {
		return getAnnotation(qualifiedAnnotationTypePrefix, true) != null;
	}

	private AnnotationDesc getAnnotation(String qualifiedAnnotationType, boolean startsWith) {
		AnnotationDesc found = null;
		for (AnnotationDesc annotation : this.annotations) {
			try {
				if (startsWith) {
					if (annotation.annotationType().qualifiedTypeName().indexOf(qualifiedAnnotationType) > -1) {
						found = annotation;
						break;
					}
				} else if (annotation.annotationType().qualifiedTypeName().equals(qualifiedAnnotationType)) {
					found = annotation;
					break;
				}
			} catch (RuntimeException e) {
				System.err.println(annotation + " has invalid javadoc: " + e.getClass() + ": " + e.getMessage());
			}
		}
		return found;
	}

}
