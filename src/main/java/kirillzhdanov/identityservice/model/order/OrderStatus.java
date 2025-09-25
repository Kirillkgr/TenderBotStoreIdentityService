package kirillzhdanov.identityservice.model.order;

public enum OrderStatus {
    QUEUED,            // в очереди
    PREPARING,         // готовится
    READY_FOR_PICKUP,  // готов к выдаче
    OUT_FOR_DELIVERY,  // передан в доставку/в пути
    DELIVERED,         // доставлен
    COMPLETED,         // завершён
    CANCELED           // отменён
}
