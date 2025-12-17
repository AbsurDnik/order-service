package org.megasoft.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import org.megasoft.entity.Inventory

@ApplicationScoped
class InventoryRepository : PanacheRepository<Inventory> {

    fun findByProductCode(productCode: String): Inventory? {
        return find("productCode", productCode).firstResult()
    }
}
