package uk.ac.westminster.smartcampusapi.resource;

import uk.ac.westminster.smartcampusapi.exception.SensorUnavailableException;
import uk.ac.westminster.smartcampusapi.model.Sensor;
import uk.ac.westminster.smartcampusapi.model.SensorReading;
import uk.ac.westminster.smartcampusapi.store.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

// No @Path here — the path is set by the locator method in SensorResource
public class SensorReadingResource {

    private final String sensorId;

    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/v1/sensors/{sensorId}/readings
    // Returns the full reading history for this sensor
    // ─────────────────────────────────────────────────────────────
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllReadings() {

        // Check the sensor exists
        if (!DataStore.sensors.containsKey(sensorId)) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildMessage("Sensor not found: " + sensorId))
                    .build();
        }

        List<SensorReading> history =
                DataStore.readings.getOrDefault(sensorId, new ArrayList<>());

        return Response.ok(history).build();
    }

    // ─────────────────────────────────────────────────────────────
    // POST /api/v1/sensors/{sensorId}/readings
    // Appends a new reading + updates parent sensor's currentValue
    // ─────────────────────────────────────────────────────────────
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReading(SensorReading reading) {

        Sensor sensor = DataStore.sensors.get(sensorId);

        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(buildMessage("Sensor not found: " + sensorId))
                    .build();
        }

        // Block readings if sensor is under MAINTENANCE — throws 403
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is currently under MAINTENANCE " +
                "and cannot accept new readings."
            );
        }

        // Auto-generate ID and timestamp
        reading.setId(UUID.randomUUID().toString());
        reading.setTimestamp(System.currentTimeMillis());

        // Store the reading
        DataStore.readings
                .computeIfAbsent(sensorId, k -> new ArrayList<>())
                .add(reading);

        // *** SIDE EFFECT: update parent sensor's currentValue ***
        // This keeps data consistent across the API (rubric requirement)
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }

    // ─────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────
    private Map<String, String> buildMessage(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("message", message);
        return body;
    }
}