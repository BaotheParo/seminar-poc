# Audio Feature Testing & Troubleshooting Guide


This document outlines the steps to verify the Location-Based Audio feature and documents common issues encountered during development.


## 1. Prerequisites
- **Java 17+**
- **Docker** (for PostgreSQL + PostGIS)
- **Maven** (via `mvnw`)


## 2. Setup Database
Ensure you have a PostgreSQL container with PostGIS enabled.
```sh
# Helper command to start a fresh DB container if needed
docker run --name street-voice-db-new -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=password -e POSTGRES_DB=street_voice_db -p 5432:5432 -d postgis/postgis
```


## 3. Testing Flow


### Step 1: Start the Application
Recommended to run via JAR to avoid Maven wrapper environment issues:
```sh
# Build
./mvnw.cmd clean package -DskipTests


# Run
java -jar target/street-voice-backend-0.0.1-SNAPSHOT.jar
```


### Step 2: Seed Mock Data
Since the database might be empty, create a dummy food stall.
**Request:** `POST http://localhost:8080/api/v1/stalls`
```json
{
  "name": "Test Stall",
  "description": "Delicious food",
  "latitude": 10.76,
  "longitude": 106.70,
  "imageUrl": "test.jpg"
}
```


### Step 3: Test Audio Generation (Sync API)
Call the sync endpoint. The backend should generate an audio file if it doesn't exist.
**Request:** `GET http://localhost:8080/api/v1/stalls/sync?lat=10.76&lng=106.70&radius=5000`


**Expected Response:**
```json
[
  {
    "id": 1,
    "name": "Test Stall",
    ...
    "audioUrl": "/audio/120110422_vi.mp3"  <-- Check this
  }
]
```


**Verify File:**
Check the project folder: `uploads/audio/`. You should see a `.mp3` file corresponding to the hash in `audioUrl`.


---


## 4. Troubleshooting & Known Issues


During development, the following errors were encountered and fixed. If you see them, apply the corresponding fix.


### Issue 1: `application.yaml` Indentation Error
**Symptom:** Application fails to connect to DB, properties not loaded.
**Cause:** `datasource` and `jpa` were incorrectly nested under `spring.application`.
**Fix:** Ensure `datasource` and `jpa` are direct children of `spring` (same level as `application`).
```yaml
spring:
  application:
    name: ...
  datasource:  # Correct indentation
    url: ...
```


### Issue 2: `PostgisPG95Dialect` Deprecation
**Symptom:** `java.lang.ClassNotFoundException: org.hibernate.spatial.dialect.postgis.PostgisPG95Dialect`
**Cause:** Explicitly defining a specific dialect version can cause conflicts with newer Hibernate versions (Hibernate 6+).
**Fix:** Remove the `database-platform` line in `application.yaml`. Let Hibernate auto-detect the dialect.
```yaml
jpa:
  # database-platform: ... (REMOVE THIS)
  hibernate:
    ddl-auto: update
```


### Issue 3: Missing `@Builder` on DTO
**Symptom:** Compilation error: `symbol: method builder() location: class FoodStallResponse`.
**Cause:** The service layer used `.builder()` but the DTO class was missing `@Builder`.
**Fix:** Add `@Builder` annotation to `FoodStallResponse.java`.
```java
@Data
@Builder // Add this
@AllArgsConstructor
public class FoodStallResponse { ... }
```


### Issue 4: `FoodStallRepository` Query Mismatch
**Symptom:** Error executing `findStallsWithinRadius`.
**Cause:** The SQL query used generic parameters that didn't match the method signature or PostGIS types.
**Fix:** Use explicit casting and parameter binding.
```java
// BEFORE (Error)
WHERE ST_DWithin(f.location, :userLocation, :radius)


// AFTER (Fixed)
WHERE ST_DWithin(f.location::geography, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radiusInMeters)
```