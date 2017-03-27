package com.tenxerconsulting.swagger.doclet.model;

import static com.google.common.base.Strings.emptyToNull;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Operation {

	private HttpMethod method;
	private String nickname;

	private String type;
	private String format;
	private PropertyItems items;
	private Boolean uniqueItems;
	private List<String> allowableValues;
	private String minimum;
	private String maximum;
	private String defaultValue;

	private List<ApiParameter> parameters;
	private String summary; // cap at 60 characters for readability in the UI
	private String notes;

	private List<ApiResponseMessage> responseMessages;

	private List<String> consumes;
	private List<String> produces;

	private OperationAuthorizations authorizations;

	private String deprecated;

	@SuppressWarnings("unused")
	private Operation() {
	}

	public Operation(Method method) {
		this.method = method.getMethod();
		this.nickname = emptyToNull(method.getMethodName());
//		this.type = emptyToNull(method.getReturnType());
//		this.format = emptyToNull(method.getReturnTypeFormat());
//		if (method.getReturnTypeItemsRef() != null || method.getReturnTypeItemsType() != null) {
//			this.items = new PropertyItems(method.getReturnTypeItemsRef(), method.getReturnTypeItemsType(), method.getReturnTypeItemsFormat(),
//					method.getReturnTypeItemsAllowableValues());
//
//		}
//		this.uniqueItems = method.getReturnTypeUniqueItems();
//		this.allowableValues = method.getReturnTypeAllowableValues();
//		this.minimum = method.getReturnTypeMinimum();
//		this.maximum = method.getReturnTypeMaximum();
//		this.defaultValue = method.getReturnTypeDefaultValue();
//		this.parameters = method.getParameters().isEmpty() ? null : method.getParameters();
//		this.responseMessages = method.getResponseMessages().isEmpty() ? null : method.getResponseMessages();
//		this.summary = emptyToNull(method.getSummary());
//		this.notes = emptyToNull(method.getNotes());
		this.consumes = method.getConsumes() == null || method.getConsumes().isEmpty() ? null : method.getConsumes();
		this.produces = method.getProduces() == null || method.getProduces().isEmpty() ? null : method.getProduces();
		this.authorizations = method.getAuthorizations();

		if (method.isDeprecated()) {
			this.deprecated = "true";
		}

	}

	public HttpMethod getMethod() {
		return this.method;
	}

	public String getNickname() {
		return this.nickname;
	}

	/**
	 * This gets the type of property
	 * @return the type of property
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * This gets the format
	 * @return the format
	 */
	public String getFormat() {
		return this.format;
	}

	/**
	 * This gets the allowableValues
	 * @return the allowableValues
	 */
	@JsonProperty("enum")
	public List<String> getAllowableValues() {
		return this.allowableValues;
	}

	/**
	 * This gets the uniqueItems
	 * @return the uniqueItems
	 */
	public Boolean getUniqueItems() {
		return this.uniqueItems;
	}

	/**
	 * This gets the items in the collection
	 * @return The items in the collection
	 */
	public PropertyItems getItems() {
		return this.items;
	}

	/**
	 * This gets the minimum value of the property
	 * @return the minimum value of the property
	 */
	public String getMinimum() {
		return this.minimum;
	}

	/**
	 * This gets the maximum value of the property
	 * @return the maximum value of the property
	 */
	public String getMaximum() {
		return this.maximum;
	}

	/**
	 * This gets the defaultValue
	 * @return the defaultValue
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}

	public List<ApiParameter> getParameters() {
		return this.parameters;
	}

	public List<ApiResponseMessage> getResponseMessages() {
		return this.responseMessages;
	}

	public String getSummary() {
		return this.summary;
	}

	public String getNotes() {
		return this.notes;
	}

	/**
	 * This gets the consumes
	 * @return the consumes
	 */
	public List<String> getConsumes() {
		return this.consumes;
	}

	/**
	 * This gets the produces
	 * @return the produces
	 */
	public List<String> getProduces() {
		return this.produces;
	}

	/**
	 * This gets the authorizations
	 * @return the authorizations
	 */
	public OperationAuthorizations getAuthorizations() {
		return this.authorizations;
	}

	/**
	 * This gets the deprecated
	 * @return the deprecated
	 */
	public String getDeprecated() {
		return this.deprecated;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((this.allowableValues == null) ? 0 : this.allowableValues.hashCode());
		result = prime * result + ((this.authorizations == null) ? 0 : this.authorizations.hashCode());
		result = prime * result + ((this.consumes == null) ? 0 : this.consumes.hashCode());
		result = prime * result + ((this.defaultValue == null) ? 0 : this.defaultValue.hashCode());
		result = prime * result + ((this.deprecated == null) ? 0 : this.deprecated.hashCode());
		result = prime * result + ((this.format == null) ? 0 : this.format.hashCode());
		result = prime * result + ((this.items == null) ? 0 : this.items.hashCode());
		result = prime * result + ((this.maximum == null) ? 0 : this.maximum.hashCode());
		result = prime * result + ((this.method == null) ? 0 : this.method.hashCode());
		result = prime * result + ((this.minimum == null) ? 0 : this.minimum.hashCode());
		result = prime * result + ((this.nickname == null) ? 0 : this.nickname.hashCode());
		result = prime * result + ((this.notes == null) ? 0 : this.notes.hashCode());
		result = prime * result + ((this.parameters == null) ? 0 : this.parameters.hashCode());
		result = prime * result + ((this.produces == null) ? 0 : this.produces.hashCode());
		result = prime * result + ((this.responseMessages == null) ? 0 : this.responseMessages.hashCode());
		result = prime * result + ((this.summary == null) ? 0 : this.summary.hashCode());
		result = prime * result + ((this.type == null) ? 0 : this.type.hashCode());
		result = prime * result + ((this.uniqueItems == null) ? 0 : this.uniqueItems.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Operation other = (Operation) obj;
		if (this.allowableValues == null) {
			if (other.allowableValues != null) {
				return false;
			}
		} else if (!this.allowableValues.equals(other.allowableValues)) {
			return false;
		}
		if (this.authorizations == null) {
			if (other.authorizations != null) {
				return false;
			}
		} else if (!this.authorizations.equals(other.authorizations)) {
			return false;
		}
		if (this.consumes == null) {
			if (other.consumes != null) {
				return false;
			}
		} else if (!this.consumes.equals(other.consumes)) {
			return false;
		}
		if (this.defaultValue == null) {
			if (other.defaultValue != null) {
				return false;
			}
		} else if (!this.defaultValue.equals(other.defaultValue)) {
			return false;
		}
		if (this.deprecated == null) {
			if (other.deprecated != null) {
				return false;
			}
		} else if (!this.deprecated.equals(other.deprecated)) {
			return false;
		}
		if (this.format == null) {
			if (other.format != null) {
				return false;
			}
		} else if (!this.format.equals(other.format)) {
			return false;
		}
		if (this.items == null) {
			if (other.items != null) {
				return false;
			}
		} else if (!this.items.equals(other.items)) {
			return false;
		}
		if (this.maximum == null) {
			if (other.maximum != null) {
				return false;
			}
		} else if (!this.maximum.equals(other.maximum)) {
			return false;
		}
		if (this.method != other.method) {
			return false;
		}
		if (this.minimum == null) {
			if (other.minimum != null) {
				return false;
			}
		} else if (!this.minimum.equals(other.minimum)) {
			return false;
		}
		if (this.nickname == null) {
			if (other.nickname != null) {
				return false;
			}
		} else if (!this.nickname.equals(other.nickname)) {
			return false;
		}
		if (this.notes == null) {
			if (other.notes != null) {
				return false;
			}
		} else if (!this.notes.equals(other.notes)) {
			return false;
		}
		if (this.parameters == null) {
			if (other.parameters != null) {
				return false;
			}
		} else if (!this.parameters.equals(other.parameters)) {
			return false;
		}
		if (this.produces == null) {
			if (other.produces != null) {
				return false;
			}
		} else if (!this.produces.equals(other.produces)) {
			return false;
		}
		if (this.responseMessages == null) {
			if (other.responseMessages != null) {
				return false;
			}
		} else if (!this.responseMessages.equals(other.responseMessages)) {
			return false;
		}
		if (this.summary == null) {
			if (other.summary != null) {
				return false;
			}
		} else if (!this.summary.equals(other.summary)) {
			return false;
		}
		if (this.type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!this.type.equals(other.type)) {
			return false;
		}
		if (this.uniqueItems == null) {
			if (other.uniqueItems != null) {
				return false;
			}
		} else if (!this.uniqueItems.equals(other.uniqueItems)) {
			return false;
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Operation [method=" + this.method + ", nickname=" + this.nickname + ", type=" + this.type + ", format=" + this.format + ", items=" + this.items
				+ ", uniqueItems=" + this.uniqueItems + ", allowableValues=" + this.allowableValues + ", minimum=" + this.minimum + ", maximum=" + this.maximum
				+ ", defaultValue=" + this.defaultValue + ", parameters=" + this.parameters + ", summary=" + this.summary + ", notes=" + this.notes
				+ ", responseMessages=" + this.responseMessages + ", consumes=" + this.consumes + ", produces=" + this.produces + ", authorizations="
				+ this.authorizations + ", deprecated=" + this.deprecated + "]";
	}

}
