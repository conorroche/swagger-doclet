package com.tenxerconsulting.swagger.doclet.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ParameterizedType;
import com.sun.javadoc.Type;

/**
 * The ClassDocCache represents a cache of class doc with helper methods for looking them up via
 * name, type etc
 * @version $Id$
 * @author conor.roche
 */
public class ClassDocCache {

	private static String getTypeName(Type type) {
		String typeName = type.qualifiedTypeName();

		// look for Class<X> way of referencing sub resources
		ParameterizedType pt = type.asParameterizedType();
		if (pt != null && typeName.equals("java.lang.Class")) {
			Type[] typeArgs = pt.typeArguments();
			if (typeArgs != null && typeArgs.length == 1) {
				typeName = typeArgs[0].qualifiedTypeName();
			}
		}

		return typeName;
	}

	private Map<String, ClassDoc> typeNameToClass = new HashMap<>();

	/**
	 * This creates a ClassDocCache
	 * using the given classes
	 * @param classes The classes to add to the cache
	 */
	public ClassDocCache(Collection<ClassDoc> classes) {
		for (ClassDoc classDoc : classes) {
			this.typeNameToClass.put(classDoc.qualifiedTypeName(), classDoc);
		}
	}

	/**
	 * This finds a class doc matching the given type
	 * @param type The type to find a matching class doc for
	 * @return The class doc or null if none matched
	 */
	public ClassDoc findByType(Type type) {
		if (type != null) {
			String typeName = getTypeName(type);
			if (typeName != null) {
				return this.typeNameToClass.get(typeName);
			}
		}
		return null;
	}

}
