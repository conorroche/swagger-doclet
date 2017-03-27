package com.tenxerconsulting.swagger.doclet.model;

import java.util.List;
import java.util.Map;

import com.tenxerconsulting.swagger.doclet.parser.ParserHelper;
import io.swagger.models.Response;
import io.swagger.models.parameters.Parameter;

public class Method {

	private HttpMethod method;
	private String methodName;
	private List<Parameter> apiParameters;
	private String summary;
	private String description;

	private Map<String, Response> responses;

	private String path;

	private List<String> consumes;
	private List<String> produces;

	private OperationAuthorizations authorizations;

	private boolean deprecated;

	@SuppressWarnings("unused")
	private Method() {
	}

	public Method(HttpMethod method, String methodName, String path, List<Parameter> apiParameters,
				  String summary, String description, Map<String, Response> responses,
				  List<String> consumes, List<String> produces, OperationAuthorizations authorizations, boolean deprecated) {
		this.method = method;
		this.methodName = methodName;
		this.path = ParserHelper.sanitizePath(path);
		this.apiParameters = apiParameters;
		this.summary = summary;
		this.description = description;

		this.responses = responses;

		this.consumes = consumes;
		this.produces = produces;
		this.authorizations = authorizations;
		this.deprecated = deprecated;
	}

	public HttpMethod getMethod() {
		return this.method;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public String getPath() {
		return this.path;
	}

	public List<io.swagger.models.parameters.Parameter> getParameters() {
		return this.apiParameters;
	}

	/**
	 * This gets the summary
	 * @return the summary
	 */
	public String getSummary() {
		return this.summary;
	}

	/**
	 * This gets the description
	 * @return the description
	 */
	public String getDescription() {
		return this.description;
	}

	public Map<String, Response> getResponses() {
		return responses;
	}

	public boolean isSubResource() {
		return this.method == null;
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
	public boolean isDeprecated() {
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
		result = prime * result + ((this.apiParameters == null) ? 0 : this.apiParameters.hashCode());
		result = prime * result + ((this.authorizations == null) ? 0 : this.authorizations.hashCode());
		result = prime * result + ((this.consumes == null) ? 0 : this.consumes.hashCode());
		result = prime * result + (this.deprecated ? 1231 : 1237);
		result = prime * result + ((this.method == null) ? 0 : this.method.hashCode());
		result = prime * result + ((this.methodName == null) ? 0 : this.methodName.hashCode());
		result = prime * result + ((this.description == null) ? 0 : this.description.hashCode());
		result = prime * result + ((this.path == null) ? 0 : this.path.hashCode());
		result = prime * result + ((this.produces == null) ? 0 : this.produces.hashCode());
		result = prime * result + ((this.responses == null) ? 0 : this.responses.hashCode());
		result = prime * result + ((this.summary == null) ? 0 : this.summary.hashCode());
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
		Method other = (Method) obj;
		if (this.apiParameters == null) {
			if (other.apiParameters != null) {
				return false;
			}
		} else if (!this.apiParameters.equals(other.apiParameters)) {
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
		if (this.deprecated != other.deprecated) {
			return false;
		}
		if (this.method != other.method) {
			return false;
		}
		if (this.methodName == null) {
			if (other.methodName != null) {
				return false;
			}
		} else if (!this.methodName.equals(other.methodName)) {
			return false;
		}
		if (this.description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!this.description.equals(other.description)) {
			return false;
		}
		if (this.path == null) {
			if (other.path != null) {
				return false;
			}
		} else if (!this.path.equals(other.path)) {
			return false;
		}
		if (this.produces == null) {
			if (other.produces != null) {
				return false;
			}
		} else if (!this.produces.equals(other.produces)) {
			return false;
		}
		if (this.responses == null) {
			if (other.responses != null) {
				return false;
			}
		} else if (!this.responses.equals(other.responses)) {
			return false;
		}
		if (this.summary == null) {
			if (other.summary != null) {
				return false;
			}
		} else if (!this.summary.equals(other.summary)) {
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
		return "Method [method=" + this.method + ", methodName=" + this.methodName + ", apiParameters=" + this.apiParameters
				+ ", summary=" + this.summary + ", description=" + this.description
				+ ", responses=" + this.responses + ", path=" + this.path + ", consumes=" + this.consumes + ", produces="
				+ this.produces + ", authorizations=" + this.authorizations + ", deprecated=" + this.deprecated + "]";
	}

}
