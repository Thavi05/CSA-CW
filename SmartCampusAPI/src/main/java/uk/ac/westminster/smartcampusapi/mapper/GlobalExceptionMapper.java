package uk.ac.westminster.smartcampusapi.mapper;

import uk.ac.westminster.smartcampusapi.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER =
            Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable t) {

        // Log the full details SERVER-SIDE so sysadmins can diagnose it
        // This is the correct place for technical details — NOT in the response
        LOGGER.severe("Unexpected error caught by global safety net:");
        LOGGER.severe("Type   : " + t.getClass().getName());
        LOGGER.severe("Message: " + t.getMessage());

        // Return a clean, generic message to the CLIENT
        // No stack traces, no internal class names, no framework details
        ErrorMessage error = new ErrorMessage(
            "An unexpected internal error occurred. " +
            "Please contact support if this problem persists.",
            500,
            "https://smartcampus.westminster.ac.uk/api/docs/errors#500"
        );

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}