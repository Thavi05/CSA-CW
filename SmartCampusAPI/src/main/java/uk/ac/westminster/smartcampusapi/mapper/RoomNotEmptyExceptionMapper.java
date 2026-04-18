package uk.ac.westminster.smartcampusapi.mapper;

import uk.ac.westminster.smartcampusapi.exception.RoomNotEmptyException;
import uk.ac.westminster.smartcampusapi.model.ErrorMessage;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RoomNotEmptyExceptionMapper
        implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException e) {
        ErrorMessage error = new ErrorMessage(
            e.getMessage(),
            409,
            "https://smartcampus.westminster.ac.uk/api/docs/errors#409"
        );
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(error)
                .build();
    }
}