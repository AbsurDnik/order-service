package org.megasoft.repository

import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository
import jakarta.enterprise.context.ApplicationScoped
import org.megasoft.entity.Order
import org.megasoft.entity.OrderStatus

@ApplicationScoped
class OrderRepository : PanacheRepository<Order> {

}
