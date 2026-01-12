# Quick Start Guide

Get the LOS Config Service up and running in 5 minutes!

## Prerequisites

- **JDK 21** installed
- **Maven 3.8+** installed
- Port **8080** available

## Step 1: Build the Project

```bash
cd "/Users/yeswanthchowdary/Desktop/yesh/Backend Los"
mvn clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
```

## Step 2: Run the Application

```bash
mvn spring-boot:run
```

Wait for:
```
Started LosConfigServiceApplication in X.XXX seconds
```

## Step 3: Verify Installation

### Access Swagger UI
Open browser: http://localhost:8080/swagger-ui.html

You should see the API documentation interface.

### Access H2 Console
Open browser: http://localhost:8080/h2-console

**Connection Settings:**
- JDBC URL: `jdbc:h2:mem:losdb`
- Username: `sa`
- Password: (leave empty)

Click "Connect"

### Verify Master Data
In H2 console, run:
```sql
SELECT * FROM partners;
SELECT * FROM products;
```

You should see:
- Partners: SAMASTA, SONATA
- Products: ENTREPRENEURIAL, JLG

## Step 4: Generate Test JWT Token

For testing without auth-service, generate a JWT token at https://jwt.io

**Header:**
```json
{
  "alg": "HS256",
  "typ": "JWT"
}
```

**Payload:**
```json
{
  "sub": "test@example.com",
  "roles": ["ADMIN"],
  "exp": 9999999999
}
```

**Secret:** (from application.yml)
```
your-256-bit-secret-key-here-change-in-production-minimum-32-characters-required
```

Copy the generated token.

## Step 5: Test the API

### 5.1 Create Flow Configuration

```bash
curl -X POST http://localhost:8080/api/v1/configs/flows \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "flowId": "default",
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA",
    "status": "ACTIVE",
    "flowDefinition": {
      "startScreen": "personal-info",
      "screens": {
        "personal-info": {
          "name": "Personal Information",
          "next": "business-info"
        },
        "business-info": {
          "name": "Business Information",
          "next": null
        }
      }
    },
    "createdBy": "admin"
  }'
```

### 5.2 Create Screen Configuration

```bash
curl -X POST http://localhost:8080/api/v1/configs/screens \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "screenId": "personal-info",
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA",
    "status": "ACTIVE",
    "uiConfig": {
      "title": "Personal Information",
      "fields": [
        {
          "id": "firstName",
          "label": "First Name",
          "type": "TEXT",
          "required": true
        },
        {
          "id": "lastName",
          "label": "Last Name",
          "type": "TEXT",
          "required": true
        },
        {
          "id": "mobile",
          "label": "Mobile Number",
          "type": "TEXT",
          "required": true
        }
      ]
    },
    "createdBy": "admin"
  }'
```

### 5.3 Create Validation Configuration

```bash
curl -X POST http://localhost:8080/api/v1/configs/validations \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "screenId": "personal-info",
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA",
    "status": "ACTIVE",
    "validationRules": {
      "fields": {
        "firstName": {
          "required": true,
          "minLength": 2,
          "maxLength": 50
        },
        "lastName": {
          "required": true,
          "minLength": 2,
          "maxLength": 50
        },
        "mobile": {
          "required": true,
          "pattern": "^[6-9][0-9]{9}$",
          "patternMessage": "Invalid mobile number"
        }
      }
    },
    "createdBy": "admin"
  }'
```

### 5.4 Create Field Mapping Configuration

```bash
curl -X POST http://localhost:8080/api/v1/configs/field-mappings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "screenId": "personal-info",
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA",
    "status": "ACTIVE",
    "mappings": {
      "mappings": [
        {
          "mappingType": "ONE_TO_ONE",
          "sourceFields": ["firstName"],
          "target": {
            "entity": "Applicant",
            "fields": ["firstName"]
          },
          "dataType": "STRING"
        },
        {
          "mappingType": "ONE_TO_ONE",
          "sourceFields": ["lastName"],
          "target": {
            "entity": "Applicant",
            "fields": ["lastName"]
          },
          "dataType": "STRING"
        },
        {
          "mappingType": "ONE_TO_ONE",
          "sourceFields": ["mobile"],
          "target": {
            "entity": "Applicant",
            "fields": ["mobile"]
          },
          "dataType": "STRING"
        }
      ]
    },
    "createdBy": "admin"
  }'
```

**Note:** You may need to create a controller for field mappings. For now, you can skip this step if the endpoint is not available.

### 5.5 Test Runtime API

```bash
curl -X POST http://localhost:8080/api/v1/runtime/next-screen \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "currentScreenId": "personal-info",
    "formData": {
      "firstName": "John",
      "lastName": "Doe",
      "mobile": "9876543210"
    },
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA"
  }'
```

**Expected Response:**
```json
{
  "applicationId": 1,
  "nextScreenId": "business-info",
  "screenConfig": {
    "title": "Business Information",
    "fields": [...]
  },
  "status": "IN_PROGRESS"
}
```

## Step 6: Verify Data Persistence

Go to H2 Console and run:

```sql
-- Check loan application
SELECT * FROM loan_applications;

-- Check applicant data
SELECT * FROM applicants;

-- Check flow snapshot
SELECT * FROM flow_snapshots;
```

You should see:
- 1 loan application record
- 1 applicant record
- 1 flow snapshot record

## Common Issues & Solutions

### Issue 1: Port 8080 already in use
**Solution:** Kill the process or change port in `application.yml`
```bash
# Find process
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Issue 2: JWT Token validation fails
**Solution:** Ensure:
- Token is in `Authorization: Bearer <token>` format
- Secret matches the one in `application.yml`
- Token is not expired

### Issue 3: H2 Console connection fails
**Solution:** Check JDBC URL is exactly: `jdbc:h2:mem:losdb`

### Issue 4: Flyway migration fails
**Solution:** Drop and recreate the database
```bash
# Stop application
# Delete H2 database files (if any)
# Restart application
```

## Next Steps

1. ✅ **Explore Swagger UI** - Test all endpoints interactively
2. ✅ **Review Logs** - Check correlation IDs in console output
3. ✅ **Read SAMPLE_CONFIGS.md** - More detailed examples
4. ✅ **Read PROJECT_STRUCTURE.md** - Understand architecture
5. ✅ **Read README.md** - Complete documentation

## Testing Checklist

- [ ] Application starts successfully
- [ ] Swagger UI accessible
- [ ] H2 Console accessible
- [ ] Master data seeded (partners, products)
- [ ] JWT token generated
- [ ] Flow config created
- [ ] Screen config created
- [ ] Validation config created
- [ ] Runtime API works
- [ ] Data persisted in database
- [ ] Correlation ID in response headers

## Useful Commands

```bash
# Build without tests
mvn clean install -DskipTests

# Run with debug
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Package as JAR
mvn clean package

# Run packaged JAR
java -jar target/los-config-service-1.0.0-SNAPSHOT.jar

# Check Java version
java -version

# Check Maven version
mvn -version
```

## Support

If you encounter issues:
1. Check logs in console
2. Verify correlation ID
3. Check H2 database state
4. Review error messages
5. Consult README.md

Happy coding! 🚀

