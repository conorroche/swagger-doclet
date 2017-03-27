package com.tenxerconsulting.swagger.doclet.model;

import io.swagger.models.Tag;

/**
 * @author RJ Ewing
 */
public class TagWrapper {

    private Tag tag;

    private int priority = Integer.MAX_VALUE;

    public TagWrapper(Tag tag, int priority) {
        this.tag = tag;
        this.priority = priority;
    }

    public Tag getTag() {
        return tag;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TagWrapper)) return false;

        TagWrapper that = (TagWrapper) o;

        if (getPriority() != that.getPriority()) return false;
        return getTag() != null ? getTag().equals(that.getTag()) : that.getTag() == null;
    }

    @Override
    public int hashCode() {
        int result = getTag() != null ? getTag().hashCode() : 0;
        result = 31 * result + getPriority();
        return result;
    }
}
