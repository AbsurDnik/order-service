# Order Service - Asynchronous Microservice

## Order Processing Flow

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
- **Management UI**: Easy monitoring

**Trade-offs**:
- More resource-intensive than Redis
- Additional complexity compared to Redis pub/sub
- For this minimal example, Redis would also work, but RabbitMQ is better suited for production order processing

### Assumptions

1. **No Authentication**: Simplified for demo; production would need OAuth2/JWT
2. **No Inventory Deduction**: Validation only; actual inventory decrement not implemented
3. **Single Instance**: No distributed transaction handling or idempotency keys
4. **No Idempotency Key on Order Creation**: Assumes order creation is idempotent
5. **API and Worker in the Same Application**: In production, the API and background workers should be separated to allow independent scaling, better fault isolation, and clearer responsibility boundaries
6. **Basic Error Handling**: Production would need retry policies, circuit breakers
7. **Simplified Discount Logic**: Hardcoded 10% rule for demonstration
8. **No Order Cancellation**: Orders can only be created and processed
9. **Message Delivery**: Assumes RabbitMQ acknowledgment is enough (no DLQ configured)

### Future Enhancements

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
- etc...

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

## Observability

### Logs

The application uses structured logging with different levels:

Key log indicators:
- `Creating order for customer: X` - Order received
- `Order ID X sent to queue` - Order queued successfully
- `Processing order: X` - Background worker started
- `Order X processed successfully` - Processing completed

### Metrics

Access Prometheus metrics at: http://localhost:8080/q/metrics

**Key Metrics**:
- `orders_received_total`: Total orders received via API
- `orders_processed_total`: Successfully processed orders
- `orders_failed_total`: Failed order processing attempts

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
