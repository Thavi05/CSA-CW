package uk.ac.westminster.smartcampusapi.exception;

public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}