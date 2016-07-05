package fixtures.jsonsubtypesgeneric;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@SuppressWarnings("javadoc")
@Path("/jsonSubTypesResource")
public class JsonSubTypesResource {

	@GET
	@Path("/animal")
	public Animal getAnimal() {
		return null;
	}

}
