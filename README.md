# Cheque Runner Service

This project is a centralized Spring Boot service designed to manage the full lifecycle of Sayad (Iranian national
system) cheques, including issuance, presentation, bouncing, and associated account balance management. The API is
secured using JWT with an initial Basic Authentication handshake.

## 🚀 Technologies Used

* **Language:** Java 21 (or later)
* **Framework:** Spring Boot 3.2.x
* **Database:** H2 Database (for development/testing) / PostgreSQL (for production)
* **Persistence:** Spring Data JPA / Hibernate
* **API Documentation:** SpringDoc OpenAPI (Swagger UI)
* **Security:** Spring Security (Basic Auth / JWT)
* **Validation:** Jakarta Validation (Hibernate Validator)
* **Dependency Management:** Maven

## 🛠️ Prerequisites

The following software must be installed to run the project:

* JDK 21
* Maven 3.6+

## ⚙️ Setup and Execution (Build & Run)

### 1. Clone the Repository

```bash
git clone [https://github.com/hoveiser/cheque-runner]
cd cheque-runner
```

### 2. Build Project

Use the Maven command to compile and create the final executable JAR file:

```bash
mvn clean install
```

### 3. Run the Application

Execute the application directly from the JAR file located in the ``target`` folder:

```bash
java -jar target/cheque-runner-0.0.1-SNAPSHOT.jar
```

Or you can use ``maven`` command:

```bash
mvn spring-boot:run
```

* **Default Port**: 8544 (Configured in your application)

## 🔑 Authentication (Securing the API)

```bash
# Note: This snippet uses the 'base64' command for encoding credentials.
# Username: teller1
# Password: teller
# Assuming user want to present some cheque: /api/auth/token
curl -X POST "http://localhost:8544/api/cheques/1/present" \
     -H "Content-Type: application/json" \
     -H "Authorization: Basic $(echo -n teller1:teller | base64)" \
     -d ''
```
you should give this response like this(on happy scenario) :
````
{"message":"cheque paid.","statusCode":"OK","reason":"Sufficient Funds"}
````

## 📚 API Endpoints and Examples

**1. Issue Cheque (``POST /api/cheques``)**

Creates and registers a new cheque. Requires sufficient balance in the drawer account.

**Sample**
````bash
curl -X POST "http://localhost:8544/api/cheques" \
     -H "Content-Type: application/json" \
     -H "Authorization: Basic $(echo -n teller1:teller | base64)" \
     -d '{
           "drawerId": 1, 
           "number": "YT-2025-001", 
           "amount": 1500.00,
         }'
````

**2. Present Cheque (POST /api/cheques/present/{number})**

**Sample**
````bash
curl -X POST "http://localhost:8544/api/cheques/1/present" \
     -H "Content-Type: application/json" \
     -H "Authorization: Basic $(echo -n teller1:teller | base64)" \
     -d ''
````

**📖 API Documentation (Swagger UI)**

* **Swagger UI (Graphical Interface)** : ``http://localhost:8544/swagger-ui.html``
* **API Docs (Raw JSON)** : ``http://localhost:8544/v3/api-docs``