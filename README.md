# 🏛️ Smart Campus Sensor & Room Management API

## 1. API Overview

This project is a RESTful API built using JAX-RS (Jersey 2.35) and deployed on Apache Tomcat. It provides a comprehensive backend for managing university campus rooms and IoT sensors as part of the "Smart Campus" initiative.

**Base URL:** `http://localhost:8080/api/v1/`

**Technology Stack:**
- Java 17
- JAX-RS / Jersey 2.35
- Apache Tomcat 9
- Maven (WAR packaging)
- In-memory storage using ConcurrentHashMap

**Resource Hierarchy:**
```
/api/v1/
├── /rooms
│     ├── GET    /             → List all rooms
│     ├── POST   /             → Create a room
│     ├── GET    /{roomId}     → Get room by ID
│     └── DELETE /{roomId}     → Delete room (blocked if sensors exist)
└── /sensors
      ├── GET    /             → List all sensors (supports ?type= filter)
      ├── POST   /             → Register sensor (validates roomId)
      ├── GET    /{sensorId}   → Get sensor by ID
      └── /{sensorId}/readings
            ├── GET  /        → Get reading history
            └── POST /        → Add new reading (updates parent sensor)
```

---


