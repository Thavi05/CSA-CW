package uk.ac.westminster.smartcampusapi.resource;

import uk.ac.westminster.smartcampusapi.exception.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampusapi.model.Sensor;
import uk.ac.westminster.smartcampusapi.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Path("/sensors")
public class SensorResource {

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/sensors              → all sensors
    // GET /api/v1/sensors?type=CO2     → filtered by type
    // ─────────────────────────────────────────────────────────────
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSensors(@QueryParam("type") String type) {

        List<Sensor> result = new ArrayList<>(DataStore.sensors.values());

        // If ?type= was provided, filter the list (case-insensitive)
        if (type != null && !type.trim().isEmpty()) {
            result = result.stream()
                    .filter(s -> s.getType().equalsIgnoreCase(type))
                    .collect(Collectors.toList());
        }

        return Response.ok(result).build();
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/sensors/{sensorId}  → one sensor by ID
    // ─────────────────────────────────────────────────────────────
    @GET
    @Path("/{sensorId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSensorById(@PathParam("sensorId") String sensorId) {

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildMessage("Sensor not found: " + sensorId))
                    .build();
        }

        return Response.ok(sensor).build();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/sensors  → register a new sensor
    // Validates that the roomId actually exists before saving
    // ─────────────────────────────────────────────────────────────
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerSensor(Sensor sensor, @Context UriInfo uriInfo) {

        // Basic input validation
        if (sensor.getId() == null || sensor.getId().trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(buildMessage("Sensor ID is required."))
                    .build();
        }

        // Duplicate sensor check
        if (DataStore.sensors.containsKey(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(buildMessage("A sensor with ID '" + sensor.getId() + "' already exists."))
                    .build();
        }

        // *** KEY VALIDATION: roomId must reference an existing room ***
        // If it doesn't exist, throw 422 Unprocessable Entity
        if (sensor.getRoomId() == null || !DataStore.rooms.containsKey(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Cannot register sensor: Room with ID '" + sensor.getRoomId() +
                "' does not exist. Please create the room first."
            );
        }

        // Save the sensor
        DataStore.sensors.put(sensor.getId(), sensor);

        // Initialise an empty readings list for this sensor
        DataStore.readings.put(sensor.getId(), new ArrayList<>());

        // Link sensor ID into the parent room's sensorIds list
        DataStore.rooms.get(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        // Sub-resource locator — delegates /sensors/{id}/readings to SensorReadingResource
        URI location = uriInfo.getAbsolutePathBuilder()
                              .path(sensor.getId())
                              .build();

        return Response.created(location).entity(sensor).build();  // 201 Created
    }

    // ─────────────────────────────────────────────────────────────
    // Sub-resource locator for /sensors/{sensorId}/readings
    // This delegates to SensorReadingResource (Part 4)
    // ─────────────────────────────────────────────────────────────
    @Path("/{sensorId}/readings")
    public SensorReadingResource getReadingsResource(
            @PathParam("sensorId") String sensorId) {
        return new SensorReadingResource(sensorId);
    }

    // ─────────────────────────────────────────────────────────────
    // Helper: simple JSON message body
    // ─────────────────────────────────────────────────────────────
    private Map<String, String> buildMessage(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        return body;
    }
}