# LOS Config Service

**Loan Origination System Configuration Service** - The single source of truth for screen, validation, flow, and field mapping configurations.

## Overview

This backend service provides:
- **Screen Configuration** - UI structure and actions
- **Validation Configuration** - Business correctness rules
- **Flow Configuration** - Navigation and decisioning
- **Field Mapping Configuration** - UI → Domain → Database mapping
- **Runtime Orchestration** - Coordinated execution for Android & Web clients

## Tech Stack

- **Java 21** (LTS)
- **Spring Boot 3.2.1**
- **Spring Web** - REST APIs
- **Spring Data JPA** - Database access
- **Spring Security** - JWT authentication
- **Spring Validation** - Request validation
- **H2 Database** - In-memory database (MVP only)
- **Flyway** - Database migrations
- **Lombok** - Code generation
- **Jackson** - JSON processing
- **OpenAPI/Swagger** - API documentation
- **Maven** - Build tool

## Architecture

### Domain Model

#### Core Entities
- `LoanApplication` - Main application entity
- `Applicant` - Applicant personal information
- `Business` - Business information

#### Master Data
- `Partner` - Partners (SAMASTA, SONATA)
- `Product` - Products (ENTREPRENEURIAL, JLG)

#### Configuration Entities
- `ScreenConfig` - UI screen definitions
- `ValidationConfig` - Validation rules
- `FlowConfig` - Flow definitions
- `FieldMappingConfig` - Field mappings
- `FlowSnapshot` - Immutable config snapshots

### Key Features

#### 1. Validation Engine
Supports:
- **REQUIRED** - Mandatory fields
- **MIN/MAX** - Numeric and length constraints
- **REGEX** - Pattern matching
- **MULTI_SELECT** - Multi-select count validation
- **REQUIRES_VERIFICATION** - OTP/API verification

#### 2. Field Mapping Engine
- **ONE_TO_ONE** - Single field mapping
- **ONE_TO_MANY** - Split into multiple fields
- **MANY_TO_ONE** - Combine multiple fields
- **Transformers** - Custom transformation logic

Built-in transformers:
- `fullNameTransformer` - Concatenate names
- `upperCaseTransformer` - Convert to uppercase

#### 3. Flow Engine
- **Conditional Navigation** - Dynamic screen flow
- **Flow Snapshots** - Immutable config per application
- **Scope Resolution** - Branch → Partner → Product

#### 4. Configuration Management
- **CRUD Operations** - Create, Read, Update, Delete
- **Versioning** - Auto-increment version numbers
- **Cloning** - Duplicate configs
- **Status Management** - DRAFT, ACTIVE, INACTIVE

## Database Schema

### Master Data Tables
- `partners` - Partner master data
- `products` - Product master data

### Domain Tables
- `loan_applications` - Loan applications
- `applicants` - Applicant information
- `businesses` - Business information
- `verification_records` - Verification history

### Configuration Tables
- `screen_configs` - Screen configurations
- `validation_configs` - Validation configurations
- `field_mapping_configs` - Field mapping configurations
- `flow_configs` - Flow configurations
- `flow_snapshots` - Immutable config snapshots

## API Endpoints

### Runtime API

#### POST /api/v1/runtime/next-screen
Process screen submission and get next screen.

**Request:**
```json
{
  "applicationId": 123,
  "currentScreenId": "personal-info",
  "formData": {
    "firstName": "John",
    "lastName": "Doe",
    "mobile": "9876543210"
  },
  "productCode": "ENTREPRENEURIAL",
  "partnerCode": "SAMASTA",
  "branchCode": "BRANCH001"
}
```

**Response (Success - 200):**
```json
{
  "applicationId": 123,
  "nextScreenId": "business-info",
  "screenConfig": {
    "title": "Business Information",
    "fields": [...]
  },
  "status": "IN_PROGRESS"
}
```

**Response (Validation Error - 422):**
```json
{
  "errors": [
    {
      "fieldId": "mobile",
      "code": "INVALID_FORMAT",
      "message": "Invalid mobile number"
    }
  ]
}
```

### Configuration APIs

#### Screen Configuration
- `GET /api/v1/configs/screens` - List all screen configs
- `GET /api/v1/configs/screens/{id}` - Get specific config
- `POST /api/v1/configs/screens` - Create new config
- `PUT /api/v1/configs/screens/{id}` - Update config
- `POST /api/v1/configs/screens/{id}/clone` - Clone config
- `DELETE /api/v1/configs/screens/{id}` - Delete config

#### Validation Configuration
- `GET /api/v1/configs/validations` - List all validation configs
- `GET /api/v1/configs/validations/{id}` - Get specific config
- `POST /api/v1/configs/validations` - Create new config
- `PUT /api/v1/configs/validations/{id}` - Update config
- `POST /api/v1/configs/validations/{id}/clone` - Clone config
- `DELETE /api/v1/configs/validations/{id}` - Delete config

#### Flow Configuration
- `GET /api/v1/configs/flows` - List all flow configs
- `GET /api/v1/configs/flows/{id}` - Get specific config
- `POST /api/v1/configs/flows` - Create new config
- `PUT /api/v1/configs/flows/{id}` - Update config
- `POST /api/v1/configs/flows/{id}/clone` - Clone config
- `DELETE /api/v1/configs/flows/{id}` - Delete config

## Security

### Authentication
JWT-based authentication with roles:
- **ADMIN** - Full access
- **CONFIG_EDITOR** - Create/update configs
- **VIEWER** - Read-only access

### Authorization
Method-level security using `@PreAuthorize`:
- Runtime API - Requires authentication
- Config read - ADMIN, CONFIG_EDITOR, VIEWER
- Config write - ADMIN, CONFIG_EDITOR
- Config delete - ADMIN only

### JWT Token
Include in `Authorization` header:
```
Authorization: Bearer <jwt-token>
```

## Observability

### Correlation ID
- Auto-generated for each request
- Propagated in response headers: `X-Correlation-Id`
- Included in all logs

### Logging
- Correlation ID in all log messages
- Sensitive fields masked (OTP, PAN, Aadhaar)
- Structured logging format

## Getting Started

### Prerequisites
- JDK 21
- Maven 3.8+

### Build
```bash
mvn clean install
```

### Run
```bash
mvn spring-boot:run
```

### Access
- Application: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:losdb`
  - Username: `sa`
  - Password: (empty)

## Configuration

### Application Properties
Key configurations in `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:losdb;MODE=MySQL
  
security:
  jwt:
    secret: your-secret-key
    expiration: 86400000

rate-limit:
  runtime:
    requests-per-minute: 30
  config:
    requests-per-minute: 60
```

## Development

### Adding New Validation Rules
1. Create class implementing `ValidationRule`
2. Add `@Component` annotation
3. Implement `isApplicable()` and `validate()` methods

Example:
```java
@Component
public class EmailValidationRule implements ValidationRule {
    @Override
    public boolean isApplicable(Map<String, Object> fieldRules) {
        return "EMAIL".equals(fieldRules.get("type"));
    }
    
    @Override
    public ValidationResult validate(String fieldId, Object fieldValue, 
                                     Map<String, Object> fieldRules, 
                                     Map<String, Object> allFormData) {
        // Validation logic
        return ValidationResult.success();
    }
}
```

### Adding New Field Transformers
1. Create class implementing `FieldTransformer`
2. Add `@Component("transformerName")` annotation
3. Implement `transform()` method

Example:
```java
@Component("emailNormalizerTransformer")
public class EmailNormalizerTransformer implements FieldTransformer {
    @Override
    public Object transform(Map<String, Object> formData, List<String> sourceFields) {
        String email = formData.get(sourceFields.get(0)).toString();
        return email.toLowerCase().trim();
    }
}
```

## Database Migrations

All schema and data changes via Flyway migrations:
- Location: `src/main/resources/db/migration`
- Naming: `V{version}__{description}.sql`
- Examples:
  - `V1__create_master_data_tables.sql`
  - `V2__create_domain_tables.sql`
  - `V3__create_config_tables.sql`
  - `V4__seed_master_data.sql`

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

## Production Considerations

### Database
- Replace H2 with MySQL/PostgreSQL
- Update Flyway compatibility mode
- Configure connection pooling

### Security
- Use public key from auth-service for JWT validation
- Enable HTTPS
- Configure CORS appropriately
- Implement rate limiting

### Monitoring
- Add APM integration (New Relic, Datadog)
- Configure alerts
- Enable metrics endpoint

### High Availability
- Deploy multiple instances
- Configure load balancer
- Enable database replication

## API Documentation

Full API documentation available at:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/api-docs

## Support

For issues and questions, contact the LOS team.

## License

Proprietary - All rights reserved.

