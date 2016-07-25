package fixtures.issue8;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path("/issue")
public class Resource {
    @POST
    public Response test(Recursive.A input) {
        return Response.ok().build();
    }
}
