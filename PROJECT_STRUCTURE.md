# Project Structure

```
Backend Los/
├── pom.xml                           # Maven project configuration
├── README.md                         # Main documentation
├── SAMPLE_CONFIGS.md                 # Sample configurations and API examples
├── PROJECT_STRUCTURE.md              # This file
├── .gitignore                        # Git ignore rules
│
└── src/
    ├── main/
    │   ├── java/com/los/
    │   │   ├── LosConfigServiceApplication.java    # Main application class
    │   │   │
    │   │   ├── config/                             # Configuration classes
    │   │   │   ├── entity/                         # Configuration entities
    │   │   │   │   ├── ScreenConfig.java
    │   │   │   │   ├── ValidationConfig.java
    │   │   │   │   ├── FieldMappingConfig.java
    │   │   │   │   ├── FlowConfig.java
    │   │   │   │   └── FlowSnapshot.java
    │   │   │   │
    │   │   │   ├── converter/
    │   │   │   │   └── JsonConverter.java          # JPA JSON converter
    │   │   │   │
    │   │   │   └── OpenApiConfig.java              # Swagger configuration
    │   │   │
    │   │   ├── controller/                         # REST Controllers
    │   │   │   ├── RuntimeController.java          # Runtime API
    │   │   │   ├── ScreenConfigController.java
    │   │   │   ├── ValidationConfigController.java
    │   │   │   └── FlowConfigController.java
    │   │   │
    │   │   ├── domain/                             # Domain entities
    │   │   │   ├── LoanApplication.java
    │   │   │   ├── Applicant.java
    │   │   │   ├── Business.java
    │   │   │   ├── Partner.java
    │   │   │   ├── Product.java
    │   │   │   └── VerificationRecord.java
    │   │   │
    │   │   ├── dto/                                # Data Transfer Objects
    │   │   │   ├── runtime/
    │   │   │   │   ├── NextScreenRequest.java
    │   │   │   │   ├── NextScreenResponse.java
    │   │   │   │   ├── ValidationErrorResponse.java
    │   │   │   │   └── ErrorResponse.java
    │   │   │   │
    │   │   │   └── config/
    │   │   │       ├── ScreenConfigDto.java
    │   │   │       ├── ValidationConfigDto.java
    │   │   │       ├── FieldMappingConfigDto.java
    │   │   │       └── FlowConfigDto.java
    │   │   │
    │   │   ├── exception/                          # Exception handling
    │   │   │   ├── ValidationException.java
    │   │   │   ├── ConfigNotFoundException.java
    │   │   │   └── GlobalExceptionHandler.java
    │   │   │
    │   │   ├── filter/                             # Servlet filters
    │   │   │   └── CorrelationIdFilter.java
    │   │   │
    │   │   ├── flow/                               # Flow engine
    │   │   │   └── FlowEngine.java
    │   │   │
    │   │   ├── mapping/                            # Field mapping
    │   │   │   ├── FieldMappingEngine.java
    │   │   │   ├── FieldTransformer.java
    │   │   │   │
    │   │   │   └── transformers/
    │   │   │       ├── FullNameTransformer.java
    │   │   │       └── UpperCaseTransformer.java
    │   │   │
    │   │   ├── repository/                         # Data repositories
    │   │   │   ├── LoanApplicationRepository.java
    │   │   │   ├── ApplicantRepository.java
    │   │   │   ├── BusinessRepository.java
    │   │   │   ├── PartnerRepository.java
    │   │   │   ├── ProductRepository.java
    │   │   │   ├── VerificationRecordRepository.java
    │   │   │   ├── ScreenConfigRepository.java
    │   │   │   ├── ValidationConfigRepository.java
    │   │   │   ├── FieldMappingConfigRepository.java
    │   │   │   ├── FlowConfigRepository.java
    │   │   │   └── FlowSnapshotRepository.java
    │   │   │
    │   │   ├── security/                           # Security configuration
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   ├── JwtTokenProvider.java
    │   │   │   └── SecurityConfig.java
    │   │   │
    │   │   ├── service/                            # Business services
    │   │   │   ├── RuntimeOrchestrationService.java
    │   │   │   ├── ConfigResolutionService.java
    │   │   │   └── ScreenConfigService.java
    │   │   │
    │   │   ├── util/                               # Utilities
    │   │   │   └── CorrelationIdHolder.java
    │   │   │
    │   │   └── validation/                         # Validation engine
    │   │       ├── ValidationEngine.java
    │   │       ├── ValidationRule.java
    │   │       ├── ValidationResult.java
    │   │       ├── VerificationService.java
    │   │       ├── VerificationResult.java
    │   │       │
    │   │       └── rules/
    │   │           ├── RequiredValidationRule.java
    │   │           ├── MinMaxValidationRule.java
    │   │           ├── RegexValidationRule.java
    │   │           └── MultiSelectValidationRule.java
    │   │
    │   └── resources/
    │       ├── application.yml                     # Application configuration
    │       │
    │       └── db/migration/                       # Flyway migrations
    │           ├── V1__create_master_data_tables.sql
    │           ├── V2__create_domain_tables.sql
    │           ├── V3__create_config_tables.sql
    │           └── V4__seed_master_data.sql
    │
    └── test/
        └── java/com/los/
            └── (test files - to be added)
```

## Key Components

### 1. Domain Layer (`domain/`)
Core business entities representing the domain model:
- **LoanApplication** - Main application entity
- **Applicant** - Applicant information
- **Business** - Business details
- **Partner** & **Product** - Master data
- **VerificationRecord** - Verification tracking

### 2. Configuration Layer (`config/entity/`)
Configuration entities that drive the system:
- **ScreenConfig** - UI screen definitions
- **ValidationConfig** - Business validation rules
- **FieldMappingConfig** - UI to domain mappings
- **FlowConfig** - Navigation flow definitions
- **FlowSnapshot** - Immutable config snapshots

### 3. Repository Layer (`repository/`)
Spring Data JPA repositories with custom queries for:
- Scope resolution (branch → partner → product)
- Configuration lookup
- Domain entity persistence

### 4. Service Layer (`service/`)
Business logic orchestration:
- **RuntimeOrchestrationService** - Main runtime coordination
- **ConfigResolutionService** - Scope-based config resolution
- **ScreenConfigService** - Config management

### 5. Validation Layer (`validation/`)
Extensible validation framework:
- **ValidationEngine** - Orchestrates validation rules
- **ValidationRule** - Interface for validation rules
- **VerificationService** - OTP/API verification

### 6. Mapping Layer (`mapping/`)
Field transformation and persistence:
- **FieldMappingEngine** - Applies mappings
- **FieldTransformer** - Transformer interface
- **Transformers** - Built-in transformers

### 7. Flow Layer (`flow/`)
Flow navigation and decision logic:
- **FlowEngine** - Flow orchestration
- Snapshot management
- Conditional navigation

### 8. Controller Layer (`controller/`)
REST API endpoints:
- **RuntimeController** - Runtime orchestration API
- **Config Controllers** - CRUD operations

### 9. Security Layer (`security/`)
JWT-based authentication:
- **JwtAuthenticationFilter** - Token validation
- **JwtTokenProvider** - Token parsing
- **SecurityConfig** - Security configuration

### 10. Exception Handling (`exception/`)
Centralized error handling:
- **GlobalExceptionHandler** - Exception interceptor
- Custom exceptions
- Structured error responses

## Database Schema Layers

### Master Data
- Partners (SAMASTA, SONATA)
- Products (ENTREPRENEURIAL, JLG)

### Domain Data
- Loan applications
- Applicants
- Businesses
- Verification records

### Configuration Data
- Screen configs
- Validation configs
- Field mapping configs
- Flow configs
- Flow snapshots

## Design Patterns Used

### 1. Strategy Pattern
- Validation rules (`ValidationRule` interface)
- Field transformers (`FieldTransformer` interface)

### 2. Repository Pattern
- Spring Data JPA repositories
- Abstraction over data access

### 3. Service Layer Pattern
- Business logic separation
- Transaction management

### 4. Filter Chain Pattern
- JWT authentication filter
- Correlation ID filter

### 5. Builder Pattern
- Entity creation (via Lombok `@Builder`)
- Response objects

### 6. Singleton Pattern
- Spring beans (controllers, services)

## Key Features by Layer

### Configuration Management
- ✅ CRUD operations
- ✅ Versioning
- ✅ Cloning
- ✅ Scope resolution (branch → partner → product)
- ✅ Status management (DRAFT, ACTIVE, INACTIVE)

### Runtime Orchestration
- ✅ Validation execution
- ✅ Field mapping
- ✅ Flow navigation
- ✅ Snapshot management
- ✅ Transactional consistency

### Security
- ✅ JWT authentication
- ✅ Role-based authorization
- ✅ Method-level security
- ✅ CORS support

### Observability
- ✅ Correlation ID tracking
- ✅ Structured logging
- ✅ Exception handling
- ✅ API documentation (Swagger)

## Extension Points

### Adding New Validation Rules
Create class in `validation/rules/` implementing `ValidationRule`

### Adding New Transformers
Create class in `mapping/transformers/` implementing `FieldTransformer`

### Adding New Endpoints
Create controller in `controller/` with proper security annotations

### Adding Database Changes
Create Flyway migration in `db/migration/`

## Build & Run

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run

# Test
mvn test
```

## Access Points

- **Application**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **H2 Console**: http://localhost:8080/h2-console
- **API Docs**: http://localhost:8080/api-docs

