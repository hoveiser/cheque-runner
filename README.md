# Cheque Runner Service

This project is a centralized Spring Boot service designed to manage the full lifecycle of Sayad (Iranian national system) cheques, including issuance, presentation, bouncing, and associated account balance management. The API is secured using JWT with an initial Basic Authentication handshake.

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

## 📚 API Endpoints and Examples