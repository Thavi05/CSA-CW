package uk.ac.westminster.smartcampusapi.mapper;

import uk.ac.westminster.smartcampusapi.exception.SensorUnavailableException;
import uk.ac.westminster.smartcampusapi.model.ErrorMessage;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException e) {
        ErrorMessage error = new ErrorMessage(
            e.getMessage(),
            403,
            "https://smartcampus.westminster.ac.uk/api/docs/errors#403"
        );
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}