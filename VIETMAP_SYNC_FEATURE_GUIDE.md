# VietMap Sync Feature documentation

This document provides a guide on how to setup, run, test, and troubleshoot the **VietMap Data Sync Module**.

## 1. Feature Overview
The Sync Module allows administrators to fetch restaurant data from **VietMap API v3** and save it to the local database.
- **Search**: Scans for locations based on keyword and coordinates.
- **Duplicate Check**: Skips stores that already exist in the DB (by name).
- **Detail Fallback**: If `lat`/`lng` is null in search results, it automatically calls the *Place Detail API* to fetch precise coordinates.
- **Security**: API Keys are managed via `.env` file (not committed to git).

## 2. Setup & Installation

*Note: The application automatically loads this file at startup.*

### B. Database
The project uses PostgreSQL + PostGIS.
**Important**: Due to port conflicts on 5432, we configured the database to run on **port 5434**.
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5434/street_voice_db
```
Ensure your Docker container is running:
```sh
docker start street-voice-db-new
```

## 3. Running the Application

Recommended method (more stable than `mvn spring-boot:run`):
1.  **Build the JAR**:
    ```sh
    ./mvnw.cmd clean package -DskipTests
    ```
2.  **Run the JAR**:
    ```sh
    java -jar target/street-voice-backend-0.0.1-SNAPSHOT.jar
    ```


## 4. API Usage & Testing

### A. Admin Sync API
*   **Method**: `POST`
*   **URL**: `/api/v1/admin/sync-vietmap`
*   **Body** (JSON):
    ```json
    {
        "lat": 10.7607739,
        "lng": 106.7006542,
        "keyword": "ốc"
    }
    ```
*   **Response**: Returns the count of newly saved items.

### B. Postman Collection
A Postman collection is available for easy import:
*   File: `vietmap_sync_postman_collection.json`
*   **Usage**: Open Postman -> Import -> Select this file -> Run requests.

## 5. Geofence Tuning (New Feature)

We have added the ability to fine-tune the geofence (Anchor Point & Radius) for each store.

### A. Admin Tuning API
Allows Admin to shift the "Anchor Point" (to the street side) and adjust the radius without changing the store's metadata.
*   **Method**: `PATCH`
*   **URL**: `/api/v1/admin/stores/{id}/geofence`
*   **Body** (JSON):
    ```json
    {
        "latitude": 10.762900,
        "longitude": 106.700300,
        "triggerRadius": 10.0
    }
    ```
*   **Note**: Fields are optional. You can update only `triggerRadius` if desired.

### B. Mobile Sync API Update
The `GET /api/v1/stalls/sync` endpoint now returns:
*   `latitude` / `longitude`: Extracted directly from the updated PostGIS geometry.
*   `triggerRadius`: The specific radius for that store (default 8.0m).

## 6. Troubleshooting Log (Recent Issues)

Reference this if you face similar problems during development.

### Issue 1: Database Port Conflict (Bind for 0.0.0.0:5432 failed)
**Symptom:** Docker container fails to start; Application fails to connect.
**Cause:** Port 5432 was occupied by another process/container.
**Fix:** Changed host port mapping to **5434** in Docker and updated `url` in `application.yaml`.

### Issue 2: Hibernate Syntax Error (`:` near `::geography`)
**Symptom:** API fails with `PSQLException: ERROR: syntax error at or near ":"`.
**Cause:** Hibernate JPQL parser confuses the PostgreSQL cast operator `::` with a named parameter `:param`.
**Fix:** Replaced shorthand `::geography` with standard SQL `CAST(... AS geography)`.
*   **File**: `FoodStallRepository.java`

### Issue 3: Missing `RestClient` Bean (`NoSuchBeanDefinitionException`)
**Symptom:** Application fails to start, saying `RestClient.Builder` required a bean.
**Cause:** `pom.xml` had `spring-boot-starter-webmvc` (invalid) instead of `spring-boot-starter-web` and was using Spring Boot `4.x`.
**Fix:**
1.  Changed artifact to `spring-boot-starter-web`.
2.  Downgraded Spring Boot to **3.2.2** (stable).

### Issue 4: Controller Path Conflict (404/500 on PATCH)
**Symptom:** Calling `PATCH /api/v1/admin/stores/...` returned 404 or 500.
**Cause:** `FoodStallController` had `@RequestMapping("/api/v1/stalls")`, so the method inside it was actually mapped to `/api/v1/stalls/admin/stores/...`.
**Fix:** Moved the `updateGeofence` method to `AdminSyncController` which is mapped to `/api/v1/admin`.
