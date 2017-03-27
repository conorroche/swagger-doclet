package com.tenxerconsulting.swagger.doclet.model;

import io.swagger.models.Model;

import java.util.List;
import java.util.Map;

/**
 * @author RJ Ewing
 */
public final class ModelWrapper {

    private Map<String, PropertyWrapper> properties;
    private Model model;

    public ModelWrapper(Model model, Map<String, PropertyWrapper> properties) {
        this.properties = properties;
        this.model = model;
    }

    public Map<String, PropertyWrapper> getProperties() {
        return properties;
    }

    public Model getModel() {
        return model;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModelWrapper)) return false;

        ModelWrapper that = (ModelWrapper) o;

        if (!getProperties().equals(that.getProperties())) return false;
        return getModel().equals(that.getModel());
    }

    @Override
    public int hashCode() {
        int result = getProperties().hashCode();
        result = 31 * result + getModel().hashCode();
        // ModelImpl doesn't include reference in the hashCode, however that is often times the only
        // property we set on a model
        result = 31 * result + (getModel().getReference() != null ? getModel().getReference().hashCode() : 0);
        return result;
    }
}
