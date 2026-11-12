package canteen;

/** Lifecycle of an order. Each state knows what comes next. */
public enum OrderStatus {
    PLACED, PREPARING, READY, COLLECTED, CANCELLED;

    public OrderStatus next() {
        return switch (this) {
            case PLACED -> PREPARING;
            case PREPARING -> READY;
            case READY -> COLLECTED;
            default -> this; // COLLECTED / CANCELLED are final
        };
    }

    /** Orders that still need kitchen attention or pickup. */
    public boolean isActive() {
        return this == PLACED || this == PREPARING || this == READY;
    }
}
