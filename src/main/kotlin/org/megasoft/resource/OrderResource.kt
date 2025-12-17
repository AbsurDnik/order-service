package org.megasoft.resource

import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.jboss.logging.Logger
import org.megasoft.dto.OrderRequest
import org.megasoft.dto.OrderResponse
import org.megasoft.service.OrderService

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class OrderResource {

    @Inject
    lateinit var orderService: OrderService

    private val logger = Logger.getLogger(OrderResource::class.java)

    @POST
    fun submitOrder(request: OrderRequest): Response {
        return try {
            logger.info("Received order request for customer: ${request.customerId}")
            val response = orderService.createOrder(request)
            Response.status(Response.Status.ACCEPTED).entity(response).build()
        } catch (e: Exception) {
            logger.error("Error processing order", e)
            Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(mapOf("error" to "Failed to process order: ${e.message}"))
                .build()
        }
    }

    @GET
    @Path("/{orderId}")
    fun getOrder(@PathParam("orderId") orderId: Long): Response {
        val order = orderService.getOrder(orderId)
        return if (order != null) {
            Response.ok(order).build()
        } else {
            Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Order not found"))
                .build()
        }
    }

    @GET
    fun getAllOrders(): Response {
        val orders = orderService.getAllOrders()
        return Response.ok(orders).build()
    }
}
