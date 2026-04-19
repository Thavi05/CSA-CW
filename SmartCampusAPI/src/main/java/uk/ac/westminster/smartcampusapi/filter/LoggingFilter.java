package uk.ac.westminster.smartcampusapi.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
public class LoggingFilter
        implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER =
            Logger.getLogger(LoggingFilter.class.getName());

    // ─────────────────────────────────────────────────────────────
    // Runs BEFORE the request reaches your resource method
    // Logs what came IN
    // ─────────────────────────────────────────────────────────────
    @Override
    public void filter(ContainerRequestContext requestContext)
            throws IOException {

        LOGGER.info("--- Incoming Request ---");
        LOGGER.info("Method : " + requestContext.getMethod());
        LOGGER.info("URI    : " + requestContext.getUriInfo().getAbsolutePath());
    }

    // ─────────────────────────────────────────────────────────────
    // Runs AFTER your resource method sends a response
    // Logs what went OUT
    // ─────────────────────────────────────────────────────────────
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext)
            throws IOException {

        LOGGER.info("--- Outgoing Response ---");
        LOGGER.info("Status : " + responseContext.getStatus());
    }
}