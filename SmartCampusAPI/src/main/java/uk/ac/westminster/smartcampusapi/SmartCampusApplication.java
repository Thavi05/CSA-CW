package uk.ac.westminster.smartcampusapi;

import javax.ws.rs.ApplicationPath;
import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("/api/v1")
public class SmartCampusApplication extends ResourceConfig {

    public SmartCampusApplication() {
        // Scans the entire base package for @Path, @Provider, filters etc.
        packages("uk.ac.westminster.smartcampusapi");
    }
}