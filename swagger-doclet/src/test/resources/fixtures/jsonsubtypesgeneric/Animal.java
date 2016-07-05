package fixtures.jsonsubtypesgeneric;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@SuppressWarnings("javadoc")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({ @Type(value = Cat1.class, name = "cat1"), @Type(value = Dog1.class, name = "dog1") })
public abstract class Animal<T> {

	public String name;

}
