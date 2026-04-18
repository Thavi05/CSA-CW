package uk.ac.westminster.smartcampusapi.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDiscovery() {

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("apiVersion", "1.0");
        info.put("title", "Smart Campus Sensor & Room Management API");
        info.put("contact", "admin@smartcampus.westminster.ac.uk");
        info.put("description",
                "RESTful API for managing university campus rooms and IoT sensors");

        // HATEOAS-style resource links
        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("rooms", "/api/v1/rooms");
        resources.put("sensors", "/api/v1/sensors");
        info.put("resources", resources);

        return Response.ok(info).build();
    }
}