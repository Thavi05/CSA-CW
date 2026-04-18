package uk.ac.westminster.smartcampusapi.model;

public class SensorReading {

    private String id;        // unique reading ID (UUID)
    private long timestamp;   // epoch time in milliseconds
    private double value;     // the actual measured value

    // Default constructor — required by Jackson
    public SensorReading() {}

    // Getters and setters
    public String getId()              { return id; }
    public void setId(String id)       { this.id = id; }

    public long getTimestamp()         { return timestamp; }
    public void setTimestamp(long t)   { this.timestamp = t; }

    public double getValue()           { return value; }
    public void setValue(double value) { this.value = value; }
}