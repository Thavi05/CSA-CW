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

## 2. How to Build and Run

### Prerequisites
- Java JDK 17 or above
- Apache Maven 3.6+
- Apache Tomcat 9.0
- NetBeans IDE 27 (or any IDE with Maven support)

### Step 1 — Clone the Repository
```bash
git clone https://github.com/Thavi05/CSA-CW.git
cd CSA-CW/SmartCampusAPI
```

### Step 2 — Build the Project
```bash
mvn clean package
```
This produces `target/smart-campus-api.war`

### Step 3 — Deploy to Tomcat
Copy the WAR file to your Tomcat webapps folder:
```bash
cp target/smart-campus-api.war /path/to/tomcat/webapps/
```
Or in NetBeans: right-click project → **Run** (auto-deploys to configured Tomcat)

### Step 4 — Start Tomcat
```bash
/path/to/tomcat/bin/startup.sh     # Linux/Mac
/path/to/tomcat/bin/startup.bat    # Windows
```

### Step 5 — Verify the API is Running
```bash
curl http://localhost:8080/api/v1/
```
Expected response: JSON object with API metadata and resource links.

---
