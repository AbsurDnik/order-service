package org.megasoft.dto

import java.math.BigDecimal

data class OrderRequest(
    val customerId: String,
    val items: List<OrderItemRequest>,
    val totalAmount: BigDecimal
)

data class OrderItemRequest(
    val productCode: String,
    val quantity: Int,
    val price: BigDecimal
)
