#  Smart Campus Sensor & Room Management API

## 1. API Overview

This project is a RESTful API built using JAX-RS (Jersey 2.35) and deployed on Apache Tomcat. It provides a comprehensive backend for managing university campus rooms and IoT sensors as part of the "Smart Campus" initiative.

**Base URL:** `http://localhost:8080/SmartCampusAPI/api/v1/`

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

### Step 5 — Verify the API is Running
```bash
curl http://localhost:8080/SmartCampusAPI/api/v1/
```
Expected response: JSON object with API metadata and resource links.
---
## 3. Sample curl Commands

### Verify API is Running
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/
```

### Create a Room
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":50}'
```

### Get All Rooms
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/rooms
```

### Register a Sensor
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

### Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/SmartCampusAPI/api/v1/sensors?type=Temperature"
```

### Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.5}'
```

### Get Reading History
```bash
curl -X GET http://localhost:8080/SmartCampusAPI/api/v1/sensors/TEMP-001/readings
```

### Attempt to Delete Room With Sensors (409 Conflict)
```bash
curl -X DELETE http://localhost:8080/SmartCampusAPI/api/v1/rooms/LIB-301
```
---
## 4. Conceptual Report - Question Answers

### Part 1.1 - JAX-RS Resource Lifecycle

By default, the JAX-RS runtime operates on a **request-scoped lifecycle**, meaning a brand new instance of each resource class is instantiated for every incoming HTTP request and discarded immediately after the response is sent. This is the direct opposite of a singleton, where one shared instance persists for the entire lifetime of the application. The key architectural consequence of request-scoped lifecycle is that instance variables inside resource classes cannot hold state  any data stored in a field during one request is gone by the next, because a completely fresh object is created each time.

This has a direct impact on how in-memory data must be managed. Since resource instances are transient, all shared data structures must live outside the resource class entirely. In this implementation, a dedicated `DataStore` class holds three `static` fields, one each for rooms, sensors, and reading histories. The `static` keyword ensures there is exactly one copy of each map shared across every instance and every thread for the entire server lifetime, regardless of how many request-scoped resource objects are created and destroyed.

However, because Apache Tomcat handles multiple HTTP requests concurrently on separate threads, a standard `HashMap` would be unsafe, concurrent reads and writes without synchronisation can produce corrupted entries, lost updates, or `ConcurrentModificationException`. The solution applied is `ConcurrentHashMap`, which segments its internal storage to allow multiple threads to operate simultaneously on different segments without blocking each other. This provides both thread safety and high throughput without requiring explicit `synchronized` blocks. This strategy, static shared storage combined with `ConcurrentHashMap`, directly counteracts the stateless nature of request-scoped JAX-RS resources and prevents race conditions under concurrent load.

---

### Part 1.2 - HATEOAS

HATEOAS (Hypermedia as the Engine of Application State) is the principle that API responses should contain navigational links to related resources, allowing a client to drive its own workflow entirely through the responses it receives rather than relying on prior knowledge of the URL structure. It is considered a hallmark of advanced RESTful design because it is the characteristic that brings a REST API closest to how the World Wide Web itself works. A browser user starts at a homepage and navigates entirely through hyperlinks, never needing to memorise or hardcode URLs in advance.

In this implementation, the discovery endpoint at `GET /api/v1/` returns a JSON object containing explicit links to `/api/v1/rooms` and `/api/v1/sensors`. A client developer can start at this single entry point and programmatically discover the entire API surface without consulting any external documentation.

This has significant practical advantages over static documentation. Static documentation becomes outdated the moment a URL is restructured or a new resource is added, and clients built against it break silently. A HATEOAS-compliant API is self-describing and self-updating, the links embedded in responses always reflect the live, current state of the API. Client code written to follow links rather than hardcode paths is inherently more resilient to server-side evolution, reducing the maintenance burden on both API producers and consumers and enabling genuine loose coupling between client and server.

---

### Part 2.1 - Returning IDs vs Full Room Objects

Returning only IDs in a list response minimises bandwidth consumption because each item in the array is a short string rather than a full object. This is advantageous when the client only needs to know which rooms exist or will selectively fetch details for a small subset. However, this design introduces a significant performance liability known as the **N+1 problem**: if the client needs details for all N rooms, it must issue N additional `GET /rooms/{id}` requests after the initial list call. Each of these is a separate HTTP round-trip with its own network latency, connection overhead, and server processing cost, which degrades performance severely at scale.

Returning full room objects in the list response increases the initial payload size proportionally to the number of fields per room and the number of rooms, but completely eliminates all follow-up requests. For a Smart Campus facilities management system where administrators need to view capacity, name, and sensor assignments for all rooms simultaneously, returning full objects in a single response is clearly the more efficient and practical design choice. The bandwidth cost of the larger payload is a one-time, predictable overhead, whereas the N+1 pattern creates unpredictable and compounding latency. For very large datasets, the appropriate solution is pagination, returning full objects in controlled page sizes using `?page=` and `?limit=` query parameters, which preserves the single-request benefit while managing payload size.

---

### Part 2.2 - DELETE Idempotency

In REST, an operation is idempotent if executing it multiple times produces the same server state as executing it once. The DELETE operation in this implementation is effectively idempotent with respect to server state, and the exact behaviour across multiple identical calls can be described precisely.

On the **first call** to `DELETE /api/v1/rooms/LIB-301`, the server verifies the room exists and has no sensors assigned. If both conditions are satisfied, it removes the entry from the `ConcurrentHashMap` and returns `200 OK`. Server state after this call: room `LIB-301` does not exist.

On the **second call** to `DELETE /api/v1/rooms/LIB-301`, the server checks the map and finds no entry for that key. It returns `404 Not Found`. Server state after this call: room `LIB-301` does not exist, identical to the state after the first call.

On every subsequent call, the outcome is identical to the second call: 404, no state change. At no point does a repeated DELETE cause any additional side effect, data corruption, or unintended removal. This satisfies the formal REST definition of idempotency. The response code differs between calls, but idempotency is defined in terms of side effects and state changes, not response codes. This behaviour is critically important in distributed systems where clients may retry requests due to network timeouts. A client that retries a DELETE because it did not receive the first response will not accidentally corrupt the data store.

---
### Part 3.1 - @Consumes Media Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation registers a content negotiation contract at the framework level. When a client sends a POST request, the JAX-RS runtime inspects the `Content-Type` header of the incoming request and compares it against the media types declared by all `@Consumes` annotations on the available resource methods.

If the client sends `Content-Type: text/plain` or `Content-Type: application/xml` when the method only declares `application/json`, the JAX-RS runtime finds no matching method and immediately rejects the request with **HTTP 415 Unsupported Media Type** before the request ever reaches the resource method body. No application code is executed. The rejection happens entirely within the framework's request dispatching layer. This is not an application-level exception; it is a content negotiation failure handled by Jersey's `MessageBodyReader` selection mechanism, which scans all registered readers and finds none capable of deserialising the incoming content type into the expected Java object.

This mechanism provides two important guarantees. First, it ensures type safety at the transport boundary. Only correctly formatted JSON payloads can trigger deserialisation into a `Sensor` or `Room` object, preventing malformed input from reaching business logic. Second, it provides a security benefit, clients sending unexpected content types are blocked automatically without any custom validation code and the 415 response signals clearly to the client developer that `Content-Type: application/json` must be set in their request headers.

---

### Part 3.2 - @QueryParam vs Path-Based Filtering

The fundamental distinction between query parameters and path parameters is semantic: a path parameter identifies a specific, discrete resource by its unique identity, while a query parameter modifies or refines what is returned from a collection without implying that a new resource exists.

Consider the two designs side by side. Using a path segment : `GET /api/v1/sensors/type/CO2` implies that `type/CO2` is a named resource in the hierarchy, which violates REST's resource oriented design principle because `CO2` is a filter criterion, not a resource. It also creates a URL collision risk since `{sensorId}` already occupies that path position, requiring additional routing logic to distinguish a fetch-by-ID from a filter-by-type request.

Using a query parameter : `GET /api/v1/sensors?type=CO2` correctly expresses that the client is requesting the `/sensors` collection and applying a refinement. The collection is the resource; the query string is the instruction. This design is also composable. Multiple filters can be combined naturally without changing the URL structure, for example `?type=CO2&status=ACTIVE`. A path-based approach would require deeply nested segments for each additional filter, making URLs brittle and unreadable. Furthermore, `GET /api/v1/sensors` without any parameter naturally returns the full unfiltered collection, which is consistent behaviour requiring no additional endpoint. The `@QueryParam` annotation in JAX-RS exists precisely for this purpose: optional, additive query criteria on collection resources.

---

### Part 4.1 - Sub-Resource Locator Pattern

The Sub-Resource Locator pattern is an architectural technique in JAX-RS where a resource method does not handle an HTTP verb directly, but instead returns an object of another class that JAX-RS then inspects for further `@GET`, `@POST`, and `@DELETE` annotations to handle the remainder of the request. In this implementation, `SensorResource` contains a locator method annotated with `@Path("/{sensorId}/readings")` that returns a new `SensorReadingResource` instance, passing the `sensorId` as a constructor argument to preserve context.

The primary benefit is management of complexity through delegation. In a large API with dozens of nested paths, placing all endpoint logic in a single controller class creates a monolith that becomes increasingly difficult to understand, test, and modify. A method handling `POST /sensors/{id}/readings` has no business sitting alongside a method handling `GET /rooms/{id}`, they address entirely different concerns. The locator pattern enforces the Single Responsibility Principle: `SensorResource` is responsible only for sensor level operations, and `SensorReadingResource` is responsible only for reading-level operations within the context of a specific sensor.

This separation also makes unit testing significantly easier. Each class can be instantiated and tested independently with mock data, rather than requiring the entire resource hierarchy to be set up together. It also enables team scalability in real world development, where different developers can work on separate resource classes simultaneously without merge conflicts. Compared to a single massive controller class where every nested path is defined as a method, the locator pattern produces a codebase that scales gracefully with the complexity of the domain model and clearly mirrors the hierarchical relationships of the physical system it represents.

---
### Part 5.1 - Why 422 is More Semantically Accurate Than 404

The distinction between HTTP 404 and HTTP 422 is a matter of semantic precision. Both indicate that the request cannot be fulfilled, but they communicate fundamentally different diagnoses with different implications for the client developer.

HTTP 404 Not Found means the URL itself does not correspond to any resource on the server. It is the correct response when a client requests `GET /api/v1/rooms/NONEXISTENT`, the path does not resolve to anything. Using 404 for a failed POST to `/api/v1/sensors` would be actively misleading, because the URL `/api/v1/sensors` is perfectly valid and resolves correctly. The server accepted and parsed the request without issue.

HTTP 422 Unprocessable Entity means the server successfully received the request, correctly identified the content type, and fully parsed the JSON body, but cannot act on the semantic instructions within it because they violate a business rule or reference integrity constraint. When a client POSTs a sensor with `"roomId": "FAKE-999"` and that room does not exist, the problem is not the URL, it is the broken reference inside an otherwise well formed payload. 422 communicates this distinction precisely: the request was understood, but the data it contains is semantically invalid.

This precision is critically important for API usability. A client receiving 404 might conclude that the `/sensors` endpoint does not exist and report a routing bug. A client receiving 422 immediately understands that the payload content is the problem and knows to verify that the referenced `roomId` exists before retrying. Accurate status codes reduce debugging time, improve developer experience and are a clear marker of a professionally designed API.

---

### Part 5.2 - Cybersecurity Risks of Exposing Stack Traces

Exposing raw Java stack traces in API responses is classified as an **information disclosure vulnerability** under the OWASP Top 10 security risks. It provides an attacker with a structured map of the application's internal architecture at no cost and with no authorisation required.

**Library versions and framework details:** A stack trace referencing `org.glassfish.jersey.server.ServerRuntime` immediately reveals that the server runs Jersey JAX-RS. The attacker cross-references specific class paths against the CVE (Common Vulnerabilities and Exposures) database to identify known, published exploits targeting that exact version. If the server runs an unpatched version with a known deserialisation vulnerability, the stack trace has provided the precise attack vector.

**Internal file paths and directory structure:** Stack frames include absolute file paths, revealing the server's directory layout. This assists in directory traversal attacks and helps an attacker confirm successful path injection attempts.

**Business logic and class hierarchy:** Method names in stack frames such as `DataStore.getSensor()` or `SensorReadingResource.addReading()` expose the internal design of the application, its data model, service layer structure, and data flow. This enables an attacker to craft targeted inputs designed to exploit specific code paths.

**Exact error locations:** Line numbers in stack frames pinpoint precisely where in the source code an error occurred, dramatically accelerating reverse engineering efforts.

The `GlobalExceptionMapper` in this implementation intercepts all `Throwable` instances, logs full technical details server-side for system administrators only, and returns a generic non-technical 500 message to the client, implementing the principle of least information disclosure and eliminating this entire class of vulnerability.

---

### Part 5.5 - JAX-RS Filters vs Manual Logging

A cross cutting concern is a behaviour that must apply uniformly across the entire application regardless of which specific business function is executing. Logging every HTTP request and response is the canonical example. It must happen for every endpoint equally with no exceptions.

Manually inserting `Logger.info()` calls inside every resource method violates the DRY principle (Don't Repeat Yourself) and creates a fragile, inconsistent implementation. If a new endpoint is added and the developer forgets to add logging, that endpoint becomes invisible in the audit trail. If the log format needs to change, every method across every resource class must be individually updated, which is error-prone and time-consuming.

A JAX-RS filter registered with `@Provider` is applied automatically by the framework to every single request and response without any modification to the resource classes. `ContainerRequestFilter.filter()` executes before the request reaches any resource method, and `ContainerResponseFilter.filter()` executes after every response, including responses generated by exception mappers. This means logging is complete and guaranteed: even requests that result in a 404 or 500 are logged, whereas manual in-method logging would miss those cases entirely since the method body may never execute. This approach keeps resource classes focused exclusively on business logic, produces a consistent and maintainable logging implementation, and follows the Separation of Concerns principle that underpins professional API architecture.
