package org.megasoft.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "orders")
class Order : PanacheEntity() {
    lateinit var customerId: String

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = [JoinColumn(name = "order_id")])
    lateinit var items: List<OrderItem>

    lateinit var totalAmount: BigDecimal

    @Enumerated(EnumType.STRING)
    var status: OrderStatus = OrderStatus.PENDING

    var createdAt: LocalDateTime = LocalDateTime.now()
    var processedAt: LocalDateTime? = null
    var discount: BigDecimal = BigDecimal.ZERO
}

@Embeddable
data class OrderItem(
    var productCode: String = "",
    var quantity: Int = 0,
    var price: BigDecimal = BigDecimal.ZERO
)

enum class OrderStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
