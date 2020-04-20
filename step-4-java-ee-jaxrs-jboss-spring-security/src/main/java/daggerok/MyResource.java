package daggerok;

import javax.enterprise.context.RequestScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

@Path("v1")
@RequestScoped
@Produces(APPLICATION_JSON)
public class MyResource {

  @GET
  @Path("hello")
  public JsonObject hello() {
    return Json.createObjectBuilder()
               .add("hello", "world!")
               .build();
  }
}
