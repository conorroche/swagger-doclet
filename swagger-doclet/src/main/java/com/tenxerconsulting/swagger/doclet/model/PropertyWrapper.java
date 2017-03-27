package com.tenxerconsulting.swagger.doclet.model;


import io.swagger.models.properties.Property;

/**
 * @author RJ Ewing
 */
public final class PropertyWrapper {
    private String rawFieldName;
    private String paramCategory;

    private Property property;

    public PropertyWrapper(String rawFieldName, String paramCategory, Property property) {
        this.rawFieldName = rawFieldName;
        this.paramCategory = paramCategory;
        this.property = property;
    }

    /**
     * This gets the raw field name the property came from
     * @return the raw field name the property came from
     */
    public String getRawFieldName() {
        return rawFieldName;
    }

    /**
     * This gets category of parameter of the field, only applicable to composite parameter fields
     * @return the category of parameter of the field, only applicable to composite parameter fields
     */
    public String getParamCategory() {
        return paramCategory;
    }

    public Property getProperty() {
        return property;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PropertyWrapper)) return false;

        PropertyWrapper that = (PropertyWrapper) o;

        if (getRawFieldName() != null ? !getRawFieldName().equals(that.getRawFieldName()) : that.getRawFieldName() != null)
            return false;
        if (getParamCategory() != null ? !getParamCategory().equals(that.getParamCategory()) : that.getParamCategory() != null)
            return false;
        return getProperty() != null ? getProperty().equals(that.getProperty()) : that.getProperty() == null;
    }

    @Override
    public int hashCode() {
        int result = getRawFieldName() != null ? getRawFieldName().hashCode() : 0;
        result = 31 * result + (getParamCategory() != null ? getParamCategory().hashCode() : 0);
        result = 31 * result + (getProperty() != null ? getProperty().hashCode() : 0);
        return result;
    }
}
