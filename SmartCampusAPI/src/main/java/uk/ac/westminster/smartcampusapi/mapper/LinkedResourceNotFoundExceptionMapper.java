package uk.ac.westminster.smartcampusapi.mapper;

import uk.ac.westminster.smartcampusapi.exception.LinkedResourceNotFoundException;
import uk.ac.westminster.smartcampusapi.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        ErrorMessage error = new ErrorMessage(
            e.getMessage(),
            422,
            "https://smartcampus.westminster.ac.uk/api/docs/errors#422"
        );
        // 422 Unprocessable Entity - no named constant in Jersey 2.x, use integer directly
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}