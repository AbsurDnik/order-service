package org.megasoft.service

import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.jboss.logging.Logger
import org.megasoft.dto.OrderRequest
import org.megasoft.dto.OrderResponse
import org.megasoft.entity.Order
import org.megasoft.entity.OrderItem
import org.megasoft.entity.OrderStatus
import org.megasoft.repository.OrderRepository
import java.time.LocalDateTime

@ApplicationScoped
class OrderService {

    @Inject
    private lateinit var orderRepository: OrderRepository

    @Inject
    private lateinit var meterRegistry: MeterRegistry

    @Inject
    @Channel("orders-out")
    private lateinit var orderEmitter: Emitter<Long>

    private val logger = Logger.getLogger(OrderService::class.java)

    @Transactional
    fun createOrder(request: OrderRequest): OrderResponse {
        logger.info("Creating order for customer: ${request.customerId}")

        val order = Order().apply {
            customerId = request.customerId
            items = request.items.map { OrderItem(it.productCode, it.quantity, it.price) }
            totalAmount = request.totalAmount
            status = OrderStatus.PENDING
            createdAt = LocalDateTime.now()
        }

        orderRepository.persist(order)
        logger.info("Order created with ID: ${order.id}")

        // Send order ID to message queue for async processing
        try {
            orderEmitter.send(order.id!!)
            logger.info("Order ID ${order.id} sent to queue for processing")
        } catch (e: Exception) {
            logger.error("Failed to send order to queue", e)
            throw e
        }

        // Increment counter for orders received
        meterRegistry.counter("orders.received").increment()

        return OrderResponse(
            orderId = order.id!!,
            customerId = order.customerId,
            totalAmount = order.totalAmount,
            status = order.status,
            createdAt = order.createdAt
        )
    }

    fun getOrder(orderId: Long): Order? {
        return orderRepository.findById(orderId)
    }

    fun getAllOrders(): List<Order> {
        return orderRepository.listAll()
    }
}
