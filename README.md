#  Smart Campus Sensor & Room Management API

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

### Step 1 - Clone the Repository
```bash
git clone https://github.com/Thavi05/CSA-CW.git
cd CSA-CW/SmartCampusAPI
```

### Step 2 - Build the Project
```bash
mvn clean package
```
This produces `target/smart-campus-api.war`

### Step 3 - Deploy to Tomcat
Copy the WAR file to your Tomcat webapps folder:
```bash
cp target/smart-campus-api.war /path/to/tomcat/webapps/
```
Or in NetBeans: right-click project → **Run** (auto-deploys to configured Tomcat)

### Step 4 - Start Tomcat
```bash
/path/to/tomcat/bin/startup.sh     # Linux/Mac
/path/to/tomcat/bin/startup.bat    # Windows
```

### Step 5 - Verify the API is Running
```bash
curl http://localhost:8080/api/v1/
```
Expected response: JSON object with API metadata and resource links.

---
## 3. Sample curl Commands

### Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### Get All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### Register a Sensor
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

### Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=Temperature"
```

### Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.5}'
```

### Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/TEMP-001/readings
```

### Attempt to Delete a Room with Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

---
## 4. Conceptual Report — Question Answers

### Part 1.1 — JAX-RS Resource Lifecycle

By default, the JAX-RS runtime operates on a **request-scoped lifecycle**, meaning a brand new instance of each resource class is instantiated for every incoming HTTP request and discarded immediately after the response is sent. This is the direct opposite of a singleton, where one shared instance persists for the entire lifetime of the application. The key architectural consequence of request-scoped lifecycle is that instance variables inside resource classes cannot hold state — any data stored in a field during one request is gone by the next, because a completely fresh object is created each time.

This has a direct impact on how in-memory data must be managed. Since resource instances are transient, all shared data structures must live outside the resource class entirely. In this implementation, a dedicated `DataStore` class holds three `static` fields — one each for rooms, sensors, and reading histories. The `static` keyword ensures there is exactly one copy of each map shared across every instance and every thread for the entire server lifetime, regardless of how many request-scoped resource objects are created and destroyed.

However, because Apache Tomcat handles multiple HTTP requests concurrently on separate threads, a standard `HashMap` would be unsafe — concurrent reads and writes without synchronisation can produce corrupted entries, lost updates, or `ConcurrentModificationException`. The solution applied is `ConcurrentHashMap`, which segments its internal storage to allow multiple threads to operate simultaneously on different segments without blocking each other. This provides both thread safety and high throughput without requiring explicit `synchronized` blocks. This strategy — static shared storage combined with `ConcurrentHashMap` — directly counteracts the stateless nature of request-scoped JAX-RS resources and prevents race conditions under concurrent load.

---

### Part 1.2 - HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) is the principle that API responses should contain navigational links to related resources, allowing a client to drive its own workflow entirely through the responses it receives rather than relying on prior knowledge of the URL structure. It is considered a hallmark of advanced RESTful design because it is the characteristic that brings a REST API closest to how the World Wide Web itself works. A browser user starts at a homepage and navigates entirely through hyperlinks, never needing to memorise or hardcode URLs in advance.

In this implementation, the discovery endpoint at `GET /api/v1/` returns a JSON object containing explicit links to `/api/v1/rooms` and `/api/v1/sensors`. A client developer can start at this single entry point and programmatically discover the entire API surface without consulting any external documentation.

This has significant practical advantages over static documentation. Static documentation becomes outdated the moment a URL is restructured or a new resource is added, and clients built against it break silently. A HATEOAS-compliant API is self-describing and self-updating, the links embedded in responses always reflect the live, current state of the API. Client code written to follow links rather than hardcode paths is inherently more resilient to server-side evolution, reducing the maintenance burden on both API producers and consumers and enabling genuine loose coupling between client and server.

---

### Part 2.1 - Returning IDs vs Full Room Objects

Returning only IDs in a list response minimises bandwidth consumption because each item in the array is a short string rather than a full object. This is advantageous when the client only needs to know which rooms exist or will selectively fetch details for a small subset. However, this design introduces a significant performance liability known as the **N+1 problem**: if the client needs details for all N rooms, it must issue N additional `GET /rooms/{id}` requests after the initial list call. Each of these is a separate HTTP round-trip with its own network latency, connection overhead, and server processing cost, which degrades performance severely at scale.

Returning full room objects in the list response increases the initial payload size proportionally to the number of fields per room and the number of rooms, but completely eliminates all follow-up requests. For a Smart Campus facilities management system where administrators need to view capacity, name, and sensor assignments for all rooms simultaneously, returning full objects in a single response is clearly the more efficient and practical design choice. The bandwidth cost of the larger payload is a one-time, predictable overhead, whereas the N+1 pattern creates unpredictable and compounding latency. For very large datasets, the appropriate solution is pagination — returning full objects in controlled page sizes using `?page=` and `?limit=` query parameters, which preserves the single-request benefit while managing payload size.

---

### Part 2.2 - DELETE Idempotency

In REST, an operation is idempotent if executing it multiple times produces the same server state as executing it once. The DELETE operation in this implementation is effectively idempotent with respect to server state, and the exact behaviour across multiple identical calls can be described precisely.

On the **first call** to `DELETE /api/v1/rooms/LIB-301`, the server verifies the room exists and has no sensors assigned. If both conditions are satisfied, it removes the entry from the `ConcurrentHashMap` and returns `200 OK`. Server state after this call: room `LIB-301` does not exist.

On the **second call** to `DELETE /api/v1/rooms/LIB-301`, the server checks the map and finds no entry for that key. It returns `404 Not Found`. Server state after this call: room `LIB-301` does not exist, identical to the state after the first call.

On every subsequent call, the outcome is identical to the second call: 404, no state change. At no point does a repeated DELETE cause any additional side effect, data corruption, or unintended removal. This satisfies the formal REST definition of idempotency. The response code differs between calls, but idempotency is defined in terms of side effects and state changes, not response codes. This behaviour is critically important in distributed systems where clients may retry requests due to network timeouts. A client that retries a DELETE because it did not receive the first response will not accidentally corrupt the data store.

---
