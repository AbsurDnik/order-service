package org.megasoft.processor

import io.micrometer.core.instrument.MeterRegistry
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import org.megasoft.entity.OrderStatus
import org.megasoft.repository.InventoryRepository
import org.megasoft.repository.OrderRepository
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.random.Random

@ApplicationScoped
class OrderProcessor {

    @Inject
    lateinit var orderRepository: OrderRepository

    @Inject
    lateinit var inventoryRepository: InventoryRepository

    @Inject
    lateinit var meterRegistry: MeterRegistry

    private val logger = Logger.getLogger(OrderProcessor::class.java)

    @Incoming("orders-in")
    @Transactional
    fun processOrder(orderIdStr: String) {
        val orderId = orderIdStr.toLongOrNull()
        if (orderId == null) {
            logger.error("Invalid order ID received: $orderIdStr")
            return
        }

        logger.info("Processing order: $orderId")

        val order = orderRepository.findById(orderId)
        if (order == null) {
            logger.error("Order not found: $orderId")
            return
        }

        try {
            // Update status to PROCESSING
            order.status = OrderStatus.PROCESSING
            orderRepository.persist(order)

            // Business logic: Validate inventory
            var inventoryValid = true
            for (item in order.items) {
                val inventory = inventoryRepository.findByProductCode(item.productCode)
                if (inventory == null || inventory.availableQuantity < item.quantity) {
                    logger.warn("Insufficient inventory for product: ${item.productCode}")
                    inventoryValid = false
                    break
                }
            }

            if (!inventoryValid) {
                order.status = OrderStatus.FAILED
                orderRepository.persist(order)
                logger.warn("Order $orderId failed due to inventory issues")
                meterRegistry.counter("orders.failed").increment()
                return
            }

            // Business logic: Apply discount (10% for orders over 100)
            if (order.totalAmount > BigDecimal(100)) {
                order.discount = order.totalAmount.multiply(BigDecimal("0.10"))
                logger.info("Applied 10% discount to order $orderId: ${order.discount}")
            }

            // Business logic: Validate total amount
            val calculatedTotal = order.items.sumOf { it.price.multiply(BigDecimal(it.quantity)) }
            if (calculatedTotal != order.totalAmount) {
                logger.warn("Total amount mismatch for order $orderId")
            }

            // Mark as processed
            order.status = OrderStatus.PROCESSED
            order.processedAt = LocalDateTime.now()
            orderRepository.persist(order)

            logger.info("Order $orderId processed successfully")
            meterRegistry.counter("orders.processed").increment()

        } catch (e: Exception) {
            logger.error("Error processing order $orderId", e)
            order.status = OrderStatus.FAILED
            orderRepository.persist(order)
            meterRegistry.counter("orders.failed").increment()
        }
    }
}
