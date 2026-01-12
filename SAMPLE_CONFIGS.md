# Sample Configurations

This file contains sample JSON configurations for testing the LOS Config Service.

## 1. Sample Screen Configuration

```json
{
  "screenId": "personal-info",
  "productCode": "ENTREPRENEURIAL",
  "partnerCode": "SAMASTA",
  "status": "ACTIVE",
  "uiConfig": {
    "title": "Personal Information",
    "description": "Please provide your personal details",
    "fields": [
      {
        "id": "firstName",
        "label": "First Name",
        "type": "TEXT",
        "placeholder": "Enter first name",
        "required": true
      },
      {
        "id": "middleName",
        "label": "Middle Name",
        "type": "TEXT",
        "placeholder": "Enter middle name",
        "required": false
      },
      {
        "id": "lastName",
        "label": "Last Name",
        "type": "TEXT",
        "placeholder": "Enter last name",
        "required": true
      },
      {
        "id": "mobile",
        "label": "Mobile Number",
        "type": "TEXT",
        "placeholder": "Enter 10-digit mobile number",
        "required": true,
        "inputType": "tel"
      },
      {
        "id": "email",
        "label": "Email Address",
        "type": "TEXT",
        "placeholder": "Enter email",
        "required": false,
        "inputType": "email"
      },
      {
        "id": "dob",
        "label": "Date of Birth",
        "type": "DATE",
        "required": true
      },
      {
        "id": "gender",
        "label": "Gender",
        "type": "DROPDOWN",
        "required": true,
        "options": [
          {"value": "MALE", "label": "Male"},
          {"value": "FEMALE", "label": "Female"},
          {"value": "OTHER", "label": "Other"}
        ]
      }
    ],
    "actions": [
      {
        "id": "next",
        "label": "Next",
        "type": "PRIMARY"
      }
    ]
  },
  "createdBy": "admin"
}
```

## 2. Sample Validation Configuration

```json
{
  "screenId": "personal-info",
  "productCode": "ENTREPRENEURIAL",
  "partnerCode": "SAMASTA",
  "status": "ACTIVE",
  "validationRules": {
    "fields": {
      "firstName": {
        "required": true,
        "dataType": "STRING",
        "minLength": 2,
        "maxLength": 50,
        "pattern": "^[a-zA-Z\\s]+$",
        "patternMessage": "Name should contain only letters"
      },
      "lastName": {
        "required": true,
        "dataType": "STRING",
        "minLength": 2,
        "maxLength": 50,
        "pattern": "^[a-zA-Z\\s]+$",
        "patternMessage": "Name should contain only letters"
      },
      "mobile": {
        "required": true,
        "dataType": "STRING",
        "pattern": "^[6-9][0-9]{9}$",
        "patternMessage": "Invalid mobile number. Must be 10 digits starting with 6-9",
        "requiresVerification": {
          "type": "OTP",
          "maxAttempts": 3,
          "timeout": 300
        }
      },
      "email": {
        "required": false,
        "dataType": "STRING",
        "pattern": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
        "patternMessage": "Invalid email format"
      },
      "dob": {
        "required": true,
        "dataType": "DATE"
      },
      "gender": {
        "required": true,
        "dataType": "STRING"
      }
    }
  },
  "createdBy": "admin"
}
```

## 3. Sample Field Mapping Configuration

```json
{
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
        "sourceFields": ["middleName"],
        "target": {
          "entity": "Applicant",
          "fields": ["middleName"]
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
      },
      {
        "mappingType": "ONE_TO_ONE",
        "sourceFields": ["email"],
        "target": {
          "entity": "Applicant",
          "fields": ["email"]
        },
        "dataType": "STRING"
      },
      {
        "mappingType": "ONE_TO_ONE",
        "sourceFields": ["dob"],
        "target": {
          "entity": "Applicant",
          "fields": ["dob"]
        },
        "dataType": "DATE"
      },
      {
        "mappingType": "ONE_TO_ONE",
        "sourceFields": ["gender"],
        "target": {
          "entity": "Applicant",
          "fields": ["gender"]
        },
        "dataType": "STRING"
      }
    ]
  },
  "createdBy": "admin"
}
```

## 4. Sample Flow Configuration

```json
{
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
        "next": {
          "conditions": [
            {
              "field": "businessType",
              "operator": "equals",
              "value": "RETAIL",
              "screen": "retail-details"
            },
            {
              "field": "businessType",
              "operator": "equals",
              "value": "SERVICE",
              "screen": "service-details"
            }
          ],
          "default": "loan-details"
        }
      },
      "retail-details": {
        "name": "Retail Business Details",
        "next": "loan-details"
      },
      "service-details": {
        "name": "Service Business Details",
        "next": "loan-details"
      },
      "loan-details": {
        "name": "Loan Details",
        "next": "review"
      },
      "review": {
        "name": "Review and Submit",
        "next": null
      }
    }
  },
  "createdBy": "admin"
}
```

## 5. Sample Runtime API Request

### First Screen (No Application ID)

```bash
curl -X POST http://localhost:8080/api/v1/runtime/next-screen \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "currentScreenId": "personal-info",
    "formData": {
      "firstName": "John",
      "middleName": "Michael",
      "lastName": "Doe",
      "mobile": "9876543210",
      "email": "john.doe@example.com",
      "dob": "1990-01-15",
      "gender": "MALE"
    },
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA"
  }'
```

### Subsequent Screen (With Application ID)

```bash
curl -X POST http://localhost:8080/api/v1/runtime/next-screen \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{
    "applicationId": 1,
    "currentScreenId": "business-info",
    "formData": {
      "businessName": "ABC Traders",
      "businessType": "RETAIL",
      "businessAddress": "123 Main Street",
      "businessVintageMonths": 24,
      "annualTurnover": 500000
    },
    "productCode": "ENTREPRENEURIAL",
    "partnerCode": "SAMASTA"
  }'
```

## 6. Sample JWT Token (for testing)

For MVP testing without auth-service, you can generate a JWT token using:

**Claims:**
```json
{
  "sub": "admin@example.com",
  "roles": ["ADMIN", "CONFIG_EDITOR"],
  "exp": 1735689600
}
```

**Secret:** Use the same secret configured in `application.yml`

**Tools:**
- https://jwt.io
- https://token.dev

## 7. Testing Sequence

### Step 1: Start the application
```bash
mvn spring-boot:run
```

### Step 2: Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### Step 3: Create configurations
1. Create Flow Config (use sample #4)
2. Create Screen Config (use sample #1)
3. Create Validation Config (use sample #2)
4. Create Field Mapping Config (use sample #3)

### Step 4: Test Runtime API
1. Submit first screen with sample request #5
2. Note the `applicationId` in response
3. Submit next screen with sample request #6

### Step 5: Verify in H2 Console
```
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:losdb
Username: sa
Password: (empty)
```

Check tables:
- `loan_applications`
- `applicants`
- `businesses`
- `flow_snapshots`

## 8. Expected Response Examples

### Success Response (200)
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

### Validation Error Response (422)
```json
{
  "errors": [
    {
      "fieldId": "mobile",
      "code": "INVALID_FORMAT",
      "message": "Invalid mobile number. Must be 10 digits starting with 6-9"
    },
    {
      "fieldId": "firstName",
      "code": "REQUIRED",
      "message": "This field is required"
    }
  ]
}
```

### System Error Response (500)
```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000"
}
```

