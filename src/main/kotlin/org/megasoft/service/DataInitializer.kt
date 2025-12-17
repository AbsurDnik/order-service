package org.megasoft.service

import io.quarkus.runtime.StartupEvent
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import org.megasoft.entity.Inventory
import org.megasoft.repository.InventoryRepository
import java.math.BigDecimal

@ApplicationScoped
class DataInitializer {

    @Inject
    private lateinit var inventoryRepository: InventoryRepository

    private val logger = Logger.getLogger(DataInitializer::class.java)

    @Transactional
    fun onStart(@Observes ev: StartupEvent) {
        logger.info("Initializing inventory data...")

        // Check if inventory already exists
        val existingCount = inventoryRepository.count()
        if (existingCount > 0) {
            logger.info("Inventory already contains $existingCount items, skipping initialization")
            return
        }

        // Create sample inventory items
        val inventory1 = Inventory().apply {
            productCode = "SKU-001"
            productName = "Premium Wireless Headphones"
            availableQuantity = 100
            price = BigDecimal("49.99")
        }

        val inventory2 = Inventory().apply {
            productCode = "SKU-002"
            productName = "USB-C Charging Cable"
            availableQuantity = 250
            price = BigDecimal("19.50")
        }

        val inventory3 = Inventory().apply {
            productCode = "PROD-001"
            productName = "Laptop Stand"
            availableQuantity = 50
            price = BigDecimal("50.00")
        }

        val inventory4 = Inventory().apply {
            productCode = "PROD-002"
            productName = "Keyboard"
            availableQuantity = 75
            price = BigDecimal("30.00")
        }

        inventoryRepository.persist(inventory1)
        inventoryRepository.persist(inventory2)
        inventoryRepository.persist(inventory3)
        inventoryRepository.persist(inventory4)

        logger.info("Inventory initialized with 4 products")
        logger.info("Available products: SKU-001, SKU-002, PROD-001, PROD-002")
    }
}
