package org.megasoft.dto

import org.megasoft.entity.OrderStatus
import java.math.BigDecimal
import java.time.LocalDateTime

data class OrderResponse(
    val orderId: Long,
    val customerId: String,
    val totalAmount: BigDecimal,
    val status: OrderStatus,
    val createdAt: LocalDateTime,
    val message: String = "Order received and queued for processing"
)
