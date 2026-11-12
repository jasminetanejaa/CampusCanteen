package canteen;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** One student's order: items + quantities, a pickup token and a status. */
public class Order {
    private static int counter = 100;

    private final int id;
    private final Student student;
    private final Map<FoodItem, Integer> items;
    private final LocalDateTime placedAt;
    private OrderStatus status;

    public Order(Student student, Map<FoodItem, Integer> items) {
        this(++counter, student, items, OrderStatus.PLACED, LocalDateTime.now());
    }

    // Used when loading from file
    Order(int id, Student student, Map<FoodItem, Integer> items, OrderStatus status, LocalDateTime placedAt) {
        this.id = id;
        this.student = student;
        this.items = new LinkedHashMap<>(items);
        this.status = status;
        this.placedAt = placedAt;
        if (id > counter) counter = id;
    }

    public int getId() { return id; }
    public String getToken() { return "T" + id; }
    public Student getStudent() { return student; }
    public Map<FoodItem, Integer> getItems() { return items; }
    public OrderStatus getStatus() { return status; }
    public LocalDateTime getPlacedAt() { return placedAt; }
    void setStatus(OrderStatus s) { this.status = s; }

    public double getTotal() {
        return items.entrySet().stream()
                .mapToDouble(e -> e.getKey().getPrice() * e.getValue()).sum();
    }

    /** Rough kitchen time for this order. */
    public int getPrepMinutes() {
        return items.entrySet().stream()
                .mapToInt(e -> e.getKey().getPrepMinutes() * e.getValue()).sum();
    }

    public String itemsSummary() {
        return items.entrySet().stream()
                .map(e -> e.getValue() + "x " + e.getKey().getName())
                .collect(Collectors.joining(", "));
    }

    /** CSV line: id,student,status,placedAt,itemId:qty;itemId:qty */
    public String toCsv() {
        String itemPart = items.entrySet().stream()
                .map(e -> e.getKey().getId() + ":" + e.getValue())
                .collect(Collectors.joining(";"));
        return id + "," + student.getName() + "," + status + "," + placedAt + "," + itemPart;
    }
}
