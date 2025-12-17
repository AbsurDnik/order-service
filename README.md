# Order Service - Asynchronous Microservice

A Kotlin-based microservice built with Quarkus that processes orders asynchronously using RabbitMQ and stores them in PostgreSQL.

## Architecture Overview

The system consists of three main components:
- **HTTP API**: RESTful endpoint for submitting orders
- **Message Queue**: RabbitMQ for asynchronous order processing
- **Database**: PostgreSQL for persistent storage

### Core Entities

1. **Order**: Represents customer orders with items, total amount, and processing status
2. **Inventory**: Tracks product availability and pricing

### Order Processing Flow

1. Client submits order via POST `/api/orders`
2. Order is saved to database with `PENDING` status
3. Order ID is immediately sent to RabbitMQ queue
4. HTTP response returns immediately (202 Accepted)
5. Background worker consumes message from queue
6. Worker processes order (validates inventory, calculates discounts, enriches data)
7. Order status updated to `PROCESSED` or `FAILED`

## Design Decisions

### Why RabbitMQ?

**Chosen**: RabbitMQ over Redis for message queueing

**Justification**:
- **Reliability**: RabbitMQ provides message persistence and acknowledgments, ensuring no orders are lost
- **Message Ordering**: FIFO guarantees for order processing
- **Dead Letter Queues**: Built-in support for failed message handling
- **Management UI**: Easy monitoring at http://localhost:15672

**Trade-offs**:
- More resource-intensive than Redis
- Additional complexity compared to Redis pub/sub
- For this minimal example, Redis would also work, but RabbitMQ is better suited for production order processing

### Technology Stack

- **Quarkus**: Fast startup, low memory footprint, native Kubernetes integration
- **Kotlin**: Concise syntax, null-safety, excellent Java interoperability
- **Hibernate with Panache**: Simplified repository pattern
- **PostgreSQL**: ACID compliance for financial transactions
- **Micrometer + Prometheus**: Industry-standard observability

## Prerequisites

- Docker & Docker Compose
- Java 21 (if running locally without Docker)
- Maven 3.9+ (if building locally)

## How to Run

### Option 1: Using Docker Compose (Recommended)

```bash
# Start all services (PostgreSQL, RabbitMQ, Application)
docker-compose up --build

# The application will be available at:
# - API: http://localhost:8080
# - RabbitMQ Management: http://localhost:15672 (guest/guest)
# - Metrics: http://localhost:8080/q/metrics
```

### Option 2: Local Development

```bash
# Start dependencies only
docker-compose up postgres rabbitmq -d

# Run the application
./mvnw quarkus:dev
```

## API Usage

### Submit an Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "customer-123",
    "items": [
      {
        "productCode": "PROD-001",
        "quantity": 2,
        "price": 50.00
      },
      {
        "productCode": "PROD-002",
        "quantity": 1,
        "price": 30.00
      }
    ],
    "totalAmount": 130.00
  }'
```

**Response** (202 Accepted):
```json
{
  "orderId": 1,
  "customerId": "customer-123",
  "totalAmount": 130.00,
  "status": "PENDING",
  "createdAt": "2025-12-16T10:30:00",
  "message": "Order received and queued for processing"
}
```

### Get Order Status

```bash
curl http://localhost:8080/api/orders/1
```

### Get All Orders

```bash
curl http://localhost:8080/api/orders
```

### View Metrics

```bash
curl http://localhost:8080/q/metrics
```

**Key Metrics**:
- `orders_received_total`: Total orders received via API
- `orders_processed_total`: Successfully processed orders
- `orders_failed_total`: Failed order processing attempts

## Observability

### Logs

The application uses structured logging with different levels:

```bash
# View application logs
docker-compose logs -f order-service

# View RabbitMQ logs
docker-compose logs -f rabbitmq
```

Key log indicators:
- `Creating order for customer: X` - Order received
- `Order ID X sent to queue` - Order queued successfully
- `Processing order: X` - Background worker started
- `Order X processed successfully` - Processing completed

### Metrics

Access Prometheus metrics at: http://localhost:8080/q/metrics

Filter for custom metrics:
```bash
curl http://localhost:8080/q/metrics | grep orders_
```

### RabbitMQ Management

Monitor queue depth and message rates:
- URL: http://localhost:15672
- Username: `guest`
- Password: `guest`

## Testing the Async Flow

1. Submit multiple orders rapidly:

```bash
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/orders \
    -H "Content-Type: application/json" \
    -d "{
      \"customerId\": \"customer-$i\",
      \"items\": [{\"productCode\": \"PROD-001\", \"quantity\": 1, \"price\": 50.0}],
      \"totalAmount\": 50.0
    }"
  echo ""
done
```

2. Observe:
   - All requests return immediately (202 Accepted)
   - Check logs to see orders being processed asynchronously
   - Query orders to see status changes from `PENDING` → `PROCESSING` → `PROCESSED`
   - View metrics to see counters incrementing

## Business Logic Simulation

The background worker (`OrderProcessor`) simulates the following:

1. **Processing Time**: Random delay (1-3 seconds) per order
2. **Inventory Validation**: Checks if products exist and have sufficient quantity
3. **Discount Calculation**: Applies 10% discount for orders over $100
4. **Amount Validation**: Verifies total matches sum of line items
5. **Status Tracking**: Updates order status throughout processing

## Database Schema

Tables created automatically by Hibernate:

- `orders`: Main order information
- `order_items`: Line items (embedded collection)
- `inventory`: Product catalog and availability

## Project Structure

```
src/main/kotlin/org/megasoft/
├── entity/
│   ├── Order.kt              # Order entity with items and status
│   └── Inventory.kt          # Product inventory entity
├── repository/
│   ├── OrderRepository.kt    # Order data access
│   └── InventoryRepository.kt
├── service/
│   └── OrderService.kt       # Business logic & message emission
├── processor/
│   └── OrderProcessor.kt     # Async message consumer
├── resource/
│   └── OrderResource.kt      # REST API endpoints
└── dto/
    ├── OrderRequest.kt       # API request models
    └── OrderResponse.kt      # API response models
```

## Assumptions

1. **No Authentication**: Simplified for demo; production would need OAuth2/JWT
2. **No Inventory Deduction**: Validation only; actual inventory decrement not implemented
3. **Single Instance**: No distributed transaction handling or idempotency keys
4. **Basic Error Handling**: Production would need retry policies, circuit breakers
5. **Simplified Discount Logic**: Hardcoded 10% rule for demonstration
6. **No Order Cancellation**: Orders can only be created and processed
7. **Message Delivery**: Assumes RabbitMQ acknowledgment is sufficient (no DLQ configured)

## Stopping the Application

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (reset database)
docker-compose down -v
```

## Troubleshooting

### Connection Issues

If the application can't connect to PostgreSQL or RabbitMQ:

```bash
# Check service health
docker-compose ps

# Restart specific service
docker-compose restart postgres
docker-compose restart rabbitmq
```

### Port Conflicts

If ports 5432, 5672, or 8080 are in use:

1. Edit `docker-compose.yml` to change port mappings
2. Update `application.properties` if running locally

### Build Failures

```bash
# Clean build
./mvnw clean package

# Update dependencies
./mvnw dependency:purge-local-repository
```

## Future Enhancements

For production readiness, consider:
- [ ] Add integration tests with Testcontainers
- [ ] Implement API versioning
- [ ] Add request validation with Hibernate Validator
- [ ] Configure dead letter queue for failed messages
- [ ] Implement idempotency with unique order keys
- [ ] Add distributed tracing (OpenTelemetry)
- [ ] Set up health checks and readiness probes
- [ ] Configure database migrations (Flyway/Liquibase)
- [ ] Add API documentation (OpenAPI/Swagger)
- [ ] Implement rate limiting and backpressure

## License

This is a demonstration project created as a technical task.
