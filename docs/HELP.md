# Job Posting Service - Help Guide

## Quick Start

### Prerequisites

- Java 21 or higher
- Maven 3.9+
- PostgreSQL 15+
- Docker (optional, for containerized deployment)

### Running Locally

1. **Clone the repository:**
   ```bash
   git clone https://github.com/PROD-B2B-GGJ-Platform/job-posting-service.git
   cd job-posting-service
   ```

2. **Configure database:**
   ```bash
   # Create database
   createdb gograbjob_jobs
   
   # Set environment variables
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=gograbjob_jobs
   export DB_USERNAME=postgres
   export DB_PASSWORD=password
   ```

3. **Run the service:**
   ```bash
   mvn spring-boot:run
   ```

4. **Access Swagger UI:**
   ```
   http://localhost:8091/swagger-ui.html
   ```

---

## API Usage

### Create a Job

```bash
curl -X POST http://localhost:8091/api/v1/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "title": "Senior Software Engineer",
    "description": "We are looking for an experienced engineer...",
    "department": "Engineering",
    "location": "New York, NY",
    "employmentType": "FULL_TIME",
    "salaryMin": 120000,
    "salaryMax": 180000
  }'
```

### Get Job by ID

```bash
curl http://localhost:8091/api/v1/jobs/{id} \
  -H "Authorization: Bearer <token>"
```

### List All Jobs

```bash
curl "http://localhost:8091/api/v1/jobs?page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

### Search Jobs

```bash
curl -X POST http://localhost:8091/api/v1/jobs/search \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "keyword": "engineer",
    "status": ["PUBLISHED"],
    "location": "New York"
  }'
```

### Publish a Job

```bash
curl -X PUT http://localhost:8091/api/v1/jobs/{id}/publish \
  -H "Authorization: Bearer <token>" \
  -d '{
    "expiresAt": "2025-03-11T00:00:00Z"
  }'
```

---

## Troubleshooting

### Common Issues

#### 1. Service won't start

**Symptoms:** Application fails to start with database connection error

**Solution:**
```bash
# Check PostgreSQL is running
pg_isready -h localhost -p 5432

# Verify connection details
psql -h localhost -U postgres -d gograbjob_jobs -c "SELECT 1"
```

#### 2. 401 Unauthorized errors

**Symptoms:** All API calls return 401

**Solution:**
- Ensure Keycloak is running
- Verify token is valid and not expired
- Check KEYCLOAK_URL environment variable

```bash
# Test token
curl -X POST http://keycloak:8088/realms/gograbjob/protocol/openid-connect/token \
  -d "grant_type=password" \
  -d "client_id=job-posting-service" \
  -d "username=admin" \
  -d "password=admin"
```

#### 3. Job not appearing in search

**Symptoms:** Created job not found in search results

**Solution:**
- Verify job status is PUBLISHED
- Check organization_id matches
- Wait for index to update (up to 1 minute)

#### 4. Custom fields not saved

**Symptoms:** Custom fields return null after save

**Solution:**
- Ensure customFields is valid JSON
- Check for special characters that need escaping
- Verify JSONB column exists in database

---

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_PORT` | 8091 | Service port |
| `DB_HOST` | localhost | Database host |
| `DB_PORT` | 5432 | Database port |
| `DB_NAME` | gograbjob_jobs | Database name |
| `DB_USERNAME` | postgres | Database user |
| `DB_PASSWORD` | - | Database password |
| `KEYCLOAK_URL` | - | Keycloak base URL |
| `KEYCLOAK_REALM` | gograbjob | Keycloak realm |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka servers |

### Application Properties

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false

  flyway:
    enabled: true
    baseline-on-migrate: true
```

---

## Health Checks

### Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Overall health status |
| `/actuator/health/liveness` | Kubernetes liveness probe |
| `/actuator/health/readiness` | Kubernetes readiness probe |

### Example Response

```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP"
    }
  }
}
```

---

## Logging

### Log Levels

| Logger | Level | Description |
|--------|-------|-------------|
| ROOT | INFO | Default level |
| com.platform.talent | DEBUG | Service logs |
| org.hibernate | WARN | JPA/Hibernate |
| org.springframework | INFO | Spring Framework |

### Changing Log Level at Runtime

```bash
curl -X POST http://localhost:8091/actuator/loggers/com.platform.talent \
  -H "Content-Type: application/json" \
  -d '{"configuredLevel": "DEBUG"}'
```

---

## Frequently Asked Questions

### Q: How do I add custom fields to a job?

Custom fields are stored as JSONB. Include them in the `customFields` property:

```json
{
  "title": "Engineer",
  "customFields": {
    "skills": ["Java", "Python"],
    "remote": true,
    "clearanceLevel": "Secret"
  }
}
```

### Q: What happens when a job expires?

Jobs are automatically marked as EXPIRED by a scheduled task that runs every hour. Expired jobs:
- Are removed from public listings
- Remain searchable in admin portal
- Can be manually re-opened

### Q: Can I bulk create jobs?

Currently, bulk creation is not supported via API. For bulk operations:
1. Use the CSV import feature in the Recruiter Portal
2. Or call the create API in a loop with rate limiting

### Q: How long are deleted jobs retained?

Deleted jobs are soft-deleted and retained for 90 days before permanent deletion. During this period, they can be restored by an admin.

---

## Support

### Getting Help

- **Documentation:** Check the `/docs` folder for detailed specifications
- **Swagger UI:** http://localhost:8091/swagger-ui.html
- **GitHub Issues:** Report bugs at https://github.com/PROD-B2B-GGJ-Platform/job-posting-service/issues

### Contact

- **Team:** Talent & Recruitment Team
- **Email:** talent-team@company.com
- **Slack:** #talent-recruitment-support

