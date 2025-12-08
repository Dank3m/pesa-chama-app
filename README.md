# Table Banking Loan Management System

A scalable loan management system designed for table banking groups (Chama), built with Spring Boot, PostgreSQL, Redis, and Kafka.

## Features

### Core Functionality
- **Member Management**: Register and manage group members with full profile support
- **Contribution Management**: Monthly contribution tracking with automatic defaulter handling
- **Loan Management**: Loan applications, approvals, disbursements, and repayments
- **Interest Calculation**: Daily compound interest that accrues to 10% monthly
- **Financial Year Management**: December-November financial year cycle
- **Automatic Default Conversion**: Unpaid contributions automatically converted to loans

### Technical Features
- **Event-Driven Architecture**: Kafka for async event processing
- **Caching**: Redis for improved performance
- **Security**: JWT-based authentication with role-based access control
- **API Documentation**: OpenAPI/Swagger UI
- **Database Migrations**: Flyway for schema management
- **Containerization**: Docker and Docker Compose support

## System Requirements

- Java 21+
- Maven 3.8+
- PostgreSQL 16+
- Redis 7+
- Apache Kafka 3.5+
- Docker & Docker Compose (for containerized deployment)

## Quick Start

### Using Docker Compose (Recommended)

```bash
# Clone the repository
cd table-banking-app

# Start all services
docker-compose up -d

# Application will be available at http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Kafka UI: http://localhost:8090
```

### Manual Setup

1. **Start PostgreSQL**
```bash
docker run -d --name postgres \
  -e POSTGRES_DB=tablebanking \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 \
  postgres:16-alpine
```

2. **Start Redis**
```bash
docker run -d --name redis -p 6379:6379 redis:7-alpine
```

3. **Start Kafka** (with Zookeeper)
```bash
docker run -d --name zookeeper \
  -e ZOOKEEPER_CLIENT_PORT=2181 \
  -p 2181:2181 \
  confluentinc/cp-zookeeper:7.5.0

docker run -d --name kafka \
  -e KAFKA_BROKER_ID=1 \
  -e KAFKA_ZOOKEEPER_CONNECT=host.docker.internal:2181 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -p 9092:9092 \
  confluentinc/cp-kafka:7.5.0
```

4. **Run the Application**
```bash
mvn spring-boot:run
```

## Configuration

### Default Settings

| Setting | Default Value | Description |
|---------|---------------|-------------|
| Monthly Contribution | KES 3,500 | Default monthly contribution amount |
| Interest Rate | 10% | Monthly interest rate |
| Financial Year Start | December | Start month of financial year |
| Max Loan Duration | 12 months | Maximum loan repayment period |
| Max Members per Group | 50 | Maximum group capacity |

### Environment Variables

```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=tablebanking
DB_USERNAME=postgres
DB_PASSWORD=postgres

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# JWT
JWT_SECRET=your-256-bit-secret-key

# Server
SERVER_PORT=8080
```

## API Endpoints

### Authentication
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/refresh` - Refresh access token

### Groups
- `POST /api/v1/groups` - Create banking group
- `GET /api/v1/groups` - List all groups
- `GET /api/v1/groups/{id}` - Get group details
- `PATCH /api/v1/groups/{id}/contribution-amount` - Update contribution amount
- `PATCH /api/v1/groups/{id}/interest-rate` - Update interest rate

### Members
- `POST /api/v1/members` - Register new member
- `GET /api/v1/members/{id}` - Get member details
- `GET /api/v1/members/{id}/details` - Get member with balance
- `GET /api/v1/members/group/{groupId}` - List group members
- `PATCH /api/v1/members/{id}/status` - Change member status

### Contributions
- `POST /api/v1/contributions/record` - Record contribution payment
- `GET /api/v1/contributions/cycle/{cycleId}` - Get cycle contributions
- `GET /api/v1/contributions/cycles/current` - Get current cycle
- `POST /api/v1/contributions/cycles/{id}/process-defaults` - Process defaults

### Loans
- `POST /api/v1/loans/apply` - Apply for loan
- `POST /api/v1/loans/{id}/approve` - Approve loan
- `POST /api/v1/loans/{id}/disburse` - Disburse loan
- `POST /api/v1/loans/repay` - Make repayment
- `GET /api/v1/loans/{id}/details` - Get loan details
- `GET /api/v1/loans/overdue` - List overdue loans

### Financial Years
- `POST /api/v1/financial-years` - Create financial year
- `GET /api/v1/financial-years/current` - Get current year
- `GET /api/v1/financial-years/{id}/summary` - Get year summary
- `POST /api/v1/financial-years/{id}/close` - Close financial year

## Interest Calculation

The system uses **daily compound interest** that accumulates to exactly 10% per month.

### Formula
```
Daily Rate = (1 + Monthly Rate)^(1/days_in_month) - 1

For 10% monthly:
- 30-day month: Daily rate ≈ 0.318%
- 31-day month: Daily rate ≈ 0.308%
```

### Example
```
Principal: KES 10,000
Monthly Rate: 10%
Duration: 1 month (30 days)

Day 1: 10,000 × 0.00318 = 31.80 → Balance: 10,031.80
Day 2: 10,031.80 × 0.00318 = 31.90 → Balance: 10,063.70
...
Day 30: Balance ≈ 11,000 (10% increase)
```

## Scheduled Jobs

| Job | Schedule | Description |
|-----|----------|-------------|
| Interest Accrual | Daily 1 AM | Accrue daily interest on active loans |
| Overdue Check | Daily 2 AM | Identify and flag overdue loans |
| Default Processing | 1st of month | Convert unpaid contributions to loans |
| Contribution Reminders | 25th at 9 AM | Send reminders for pending contributions |

## Roles and Permissions

| Role | Permissions |
|------|-------------|
| ADMIN | Full access - manage groups, members, approve loans |
| TREASURER | Record contributions, approve/disburse loans |
| MEMBER | View own data, apply for loans, view balances |

## Database Schema

The system uses the following core tables:
- `banking_groups` - Group configuration
- `members` - Member profiles
- `users` - Authentication credentials
- `financial_years` - Financial year periods
- `contribution_cycles` - Monthly contribution periods
- `contributions` - Individual contribution records
- `loans` - Loan records
- `loan_interest_accruals` - Daily interest tracking
- `loan_repayments` - Payment records
- `transactions` - Financial transaction ledger
- `member_balances` - Cached member balances

## Scaling Considerations

The system is designed for scalability:

1. **Database**: PostgreSQL with proper indexing for high-volume queries
2. **Caching**: Redis caching for frequently accessed data
3. **Event Processing**: Kafka for async processing and event sourcing
4. **Stateless**: Application is stateless for horizontal scaling
5. **Connection Pooling**: HikariCP for efficient database connections

## Development

### Running Tests
```bash
mvn test
```

### Building
```bash
mvn clean package
```

### Code Style
The project uses standard Java conventions with Lombok for reducing boilerplate.

## License

MIT License

## Support

For issues and feature requests, please create an issue in the repository.
