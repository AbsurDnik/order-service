package org.megasoft.entity

import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "inventory")
class Inventory : PanacheEntity() {
    lateinit var productCode: String
    lateinit var productName: String
    var availableQuantity: Int = 0
    lateinit var price: BigDecimal
}
