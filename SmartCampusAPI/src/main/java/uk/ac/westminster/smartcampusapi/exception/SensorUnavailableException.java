package uk.ac.westminster.smartcampusapi.exception;

public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}