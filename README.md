# Mastercard Dispute Management System

## Overview
This is a Spring Boot application designed to manage payment disputes, primarily by integrating with Mastercard's Mastercom API. It provides a local system for creating and managing disputes, while leveraging Mastercard's services for the actual dispute resolution process in their sandbox environment.

## Features
-   **Local Dispute Management:** CRUD operations for dispute entities within a PostgreSQL database.
-   **Mastercard API Integration:** Connects to Mastercard's Sandbox APIs using OAuth 1.0a for secure communication.
-   **Mastercom Health Check:** Includes an endpoint to verify connectivity with the Mastercard Mastercom API.
-   **Configurable Credentials:** Externalized Mastercard API credentials for easy management.

## Technologies Used
*   **Backend:** Spring Boot (v4.0.3)
*   **Language:** Java 21
*   **Build Tool:** Apache Maven
*   **Database:** PostgreSQL
*   **ORM:** Spring Data JPA / Hibernate
*   **HTTP Client:** OkHttp 3
*   **Authentication:** Mastercard OAuth 1.0a Signer Library
*   **Serialization:** Gson
*   **Utility:** Lombok

## Setup Instructions

### 0. Clone the Repository
If you haven't already, clone this repository to your local machine:
```bash
git clone <repository-url>
cd mastercard-dispute-management
```
Make sure you're in the `mastercard-dispute-management` directory for all subsequent steps.

### Prerequisites
Before you begin, ensure you have the following installed:
*   **Java Development Kit (JDK) 21** or higher.
*   **Apache Maven** (compatible with Java 21).
*   **PostgreSQL 18.3** database running locally (or accessible).
*   A **Mastercard Developer Portal** account with a project created for Mastercom API.
*   **OAuth 1.0a Signing Keys** (a `.p12` file), Consumer Key, and Keystore Password obtained from your Mastercard Developer Portal project.

### 1. Prerequisites Verification
Before proceeding, verify that all prerequisites are installed:
```bash
# Verify Java 21
java -version

# Verify Maven
mvn -version

# Verify PostgreSQL is running (Windows)
where psql
```

### 2. Database Setup
1.  **Start PostgreSQL Database:**
    - **Windows (using PowerShell):**
      Locate your PostgreSQL installation's `pg_ctl.exe` (usually in `bin` directory) and your data directory. Then, execute a command similar to this:
      ```powershell
      Start-Process -FilePath "<PATH_TO_PG_CTL_EXE>" -ArgumentList "-D", "`"<PATH_TO_POSTGRESQL_DATA_DIR>`"", "start" -NoNewWindow
      ```
      *Example: If `pg_ctl.exe` is at `C:\Program Files\PostgreSQL\18\bin\pg_ctl.exe` and data is at `C:\Program Files\PostgreSQL\18\data`, replace the placeholders accordingly.*
      
      Alternative (using Services, if PostgreSQL is installed as a service):
      ```powershell
      Start-Service -Name postgresql-x64-18
      ```
    
    - **Linux:**
      ```bash
      sudo systemctl start postgresql
      ```
    
    - **macOS:**
      ```bash
      brew services start postgresql
      ```
      Or if you installed via PostgreSQL.app, click the Start button in the application.

2.  **Verify PostgreSQL is running:**
    ```bash
    psql --version
    ```

3.  Create a database named `dispute_db`. Open a terminal and connect to PostgreSQL:
    ```bash
    psql -U postgres
    ```
    Then execute:
    ```sql
    CREATE DATABASE dispute_db;
    ```
    Exit with `\q`

4.  Update the database connection details in `src/main/resources/application.properties` if they differ from the default:
    ```properties
    spring.datasource.url=jdbc:postgresql://localhost:5432/dispute_db
    spring.datasource.username=postgres
    spring.datasource.password=password
    ```
    *(Note: The `spring.jpa.hibernate.ddl-auto=update` setting will automatically create the `dispute` table on application startup.)*

### 3. Mastercard API Credentials
1.  Place your downloaded `.p12` signing key file (e.g., `Opus Dispute Management System-sandbox-signing.p12`) into the `src/main/resources` directory of this project.
2.  Update the `application.properties` file with your Mastercard API details:
    ```properties
    # Mastercard API Configuration
    mastercard.base-url=https://sandbox.api.mastercard.com/mastercom # Or adjust if using a different base for specific APIs
    mastercard.consumer-key=YOUR_CONSUMER_KEY_HERE
    mastercard.keystore-password=YOUR_KEYSTORE_PASSWORD_HERE
    mastercard.keystore-path=src/main/resources/Opus Dispute Management System-sandbox-signing.p12
    ```
    *Replace `YOUR_CONSUMER_KEY_HERE` and `YOUR_KEYSTORE_PASSWORD_HERE` with your actual credentials.*

### 4. Build the Project
Navigate to the `mastercard-dispute-management` directory in your terminal and build the project using Maven:
```bash
mvn clean install -DskipTests -U
```
*(The `-DskipTests` flag skips running tests, and `-U` forces a check for updated releases.)*

### 5. Run the Application
After a successful build, you can run the Spring Boot application from the `mastercard-dispute-management` directory:
```bash
java -jar target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar
```
The application should start on `http://localhost:8080`. You should see console output indicating that the Spring Boot application has started successfully.

### 6. Verify Mastercard API Connectivity
Now that the application is running, verify that it can successfully communicate with the Mastercard Mastercom API:

**Open a new PowerShell terminal and execute:**
```powershell
Invoke-WebRequest -Uri http://localhost:8080/api/mastercard/test -Method GET -UseBasicParsing
```

**Expected Response (Success):**
```
StatusCode        : 200
StatusDescription : OK
Content           : {"status":true}
RawContent        : HTTP/1.1 200 OK
                    ...
                    {"status":true}
```

**What This Test Does:**
- The `/api/mastercard/test` endpoint makes an authenticated request to Mastercard's Mastercom API (`/v6/healthcheck`)
- It verifies that your OAuth 1.0a credentials are correctly configured (Consumer Key, Keystore Password, and .p12 file)
- A successful response (`"status":true`) means your application can securely communicate with Mastercard's sandbox environment

**If You Receive Errors:**
- **StatusCode 401 (Unauthorized):** Your Consumer Key, Keystore Password, or .p12 file path is incorrect. Double-check `application.properties`.
- **StatusCode 500 (Internal Server Error):** Check the application logs for detailed error messages. Common issues include missing .p12 file or incorrect keystore password.
- **Connection Refused:** Ensure the Spring Boot application is still running on `http://localhost:8080`.

### 7. Verify on Mastercard Developer Portal
To confirm that your API requests are reaching the Mastercard sandbox and being processed:

1. Log in to your **Mastercard Developer Portal** account at `https://developer.mastercard.com`
2. Navigate to your project dashboard
3. Select the **Analytics** or **API Usage** section
4. Look for recent API calls to `GET /v6/healthcheck`
5. You should see successful responses (HTTP 200) from your test in the previous step

This confirms that:
- Your OAuth 1.0a signature is valid
- Your Consumer Key is properly registered
- Requests are being authenticated and processed by Mastercard

## API Endpoints

Once the application is running, you can interact with it via the following endpoints:

### Local Dispute Management
*   **GET /api/disputes**: Retrieve all disputes.
*   **POST /api/disputes**: Create a new dispute.
    *   **Example Body (JSON):**
        ```json
        {
            "claimId": "123e4567-e89b-12d3-a456-426614174000",
            "transactionId": "TXN123",
            "reasonCode": "FRAUD",
            "status": "NEW"
        }
        ```
*   **GET /api/disputes/{id}**: Retrieve a specific dispute by its internal ID.

### Mastercard API Integration (Mastercom Sandbox)
*   **GET /api/mastercard/test**: Tests connectivity to the Mastercard Mastercom `/v6/healthcheck` endpoint. *(Use this to verify that the Mastercard API is being hit as described in Section 6 above.)*
    *   **Example Usage (PowerShell):**
        ```powershell
        Invoke-WebRequest -Uri http://localhost:8080/api/mastercard/test -Method GET -UseBasicParsing
        ```
    *   **Expected Response (Success):**
        ```json
        {"status":true}
        ```

*   **GET /api/mastercard/fraud-risk/{accountId}**: Retrieves a fraud risk assessment for a given account ID.
*   **POST /api/mastercard/transactions/search**: Searches for transactions using the Mastercom API.
    *   *(Requires a JSON request body for search criteria)*
*   **POST /api/mastercard/disputes/file**: Files a dispute claim with Mastercom.
    *   *(Requires a JSON request body for dispute details)*
*   **GET /api/mastercard/disputes/{claimId}**: Retrieves details for a specific Mastercom claim.

## Troubleshooting

### General Issues
*   **Port 8080 in Use:** If the application fails to start due to "Port 8080 was already in use," identify and terminate the process using that port:
    - **Windows:** `netstat -ano | findstr :8080` then `taskkill /PID <PID> /F`
    - **Linux/macOS:** `lsof -i :8080` then `kill <PID>`

### Database Connection Issues
*   **"Database does not exist" or "Role does not exist":** 
    1. Verify PostgreSQL is running
    2. Ensure the `dispute_db` database is created (run `CREATE DATABASE dispute_db;` in PostgreSQL)
    3. Check that the username/password in `application.properties` matches your PostgreSQL configuration
    4. Test connectivity: `psql -h localhost -U postgres -d dispute_db`

### Mastercard API Credential Issues
*   **`.p12` File Not Found / Access Denied:**
    - Ensure the `.p12` file is placed in `src/main/resources/` directory
    - Verify the `mastercard.keystore-path` in `application.properties` has the correct filename
    - Windows example: `mastercard.keystore-path=src/main/resources/Opus Dispute Management System-sandbox-signing.p12`

*   **StatusCode 401 (Unauthorized) when testing API:**
    - Your OAuth 1.0a credentials are incorrect
    - **Check the following in `application.properties`:**
      - `mastercard.consumer-key` - Should be the 97-character key from your Mastercard Developer Portal
      - `mastercard.keystore-password` - Should match the password you used when downloading the .p12 file
      - `mastercard.keystore-path` - Should point to the correct .p12 file location
    - **Regenerate credentials if needed:**
      1. Log into Mastercard Developer Portal
      2. Go to your project → API Keys
      3. Download a fresh set of OAuth keys
      4. Update `application.properties` with the new Consumer Key and Keystore Password

*   **StatusCode 500 (Internal Server Error) when testing API:**
    - Check the application console for detailed error messages
    - Common causes:
      - Incorrect keystore password for the .p12 file
      - Corrupted .p12 file (re-download from Mastercard Developer Portal)
      - Missing or incorrect key alias in the .p12 file
    - **Solution:** Delete the application logs, restart the application, run the test again, and check the console output

*   **"Connection refused" when testing API:**
    - The Spring Boot application is not running
    - Ensure you executed `java -jar target/mastercard-dispute-management-0.0.1-SNAPSHOT.jar` and wait for "Started" message
    - Verify the application is listening on port 8080: `netstat -ano | findstr :8080` (Windows)

### Build Issues
*   **Maven Build Fails:**
    - Clear the Maven cache: `mvn clean`
    - Force update dependencies: `mvn install -U`
    - Ensure you're using JDK 21: `java -version` should show "21.x.x"

*   **Dependency Download Failures:**
    - Verify your internet connection
    - Try updating Maven: `mvn -version` should show a recent version
    - Clear local Maven repository if corruption is suspected: Delete `C:\Users\<YourUsername>\.m2\repository` and rebuild

---
