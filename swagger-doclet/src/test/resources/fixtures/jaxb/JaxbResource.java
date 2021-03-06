package fixtures.jaxb;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/jaxb")
@SuppressWarnings("javadoc")
public class JaxbResource {
	
	@GET
	public ResponseModel get() {
		return new ResponseModel();
	}

	@POST
	public Response create(PayloadModel payload) {
		return Response.ok().build();
	}
	
	@GET
	@Path("/user")
	public User getUser() {
		return new User();
	}
}
