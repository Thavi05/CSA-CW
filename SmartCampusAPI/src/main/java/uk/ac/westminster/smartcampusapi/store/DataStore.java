package uk.ac.westminster.smartcampusapi.store;

import uk.ac.westminster.smartcampusapi.model.Room;
import uk.ac.westminster.smartcampusapi.model.Sensor;
import uk.ac.westminster.smartcampusapi.model.SensorReading;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store — acts as our "database" for the entire API.
 * Uses ConcurrentHashMap so multiple simultaneous requests don't corrupt data.
 * All fields are static so there is ONE shared store across the whole application.
 */
public class DataStore {

    // Stores all rooms:   roomId -> Room object
    public static final Map<String, Room> rooms = new ConcurrentHashMap<>();

    // Stores all sensors: sensorId -> Sensor object
    public static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    // Stores reading history: sensorId -> list of SensorReading objects
    public static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();
}