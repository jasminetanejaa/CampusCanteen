package canteen;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/** Core logic: menu, orders, queue estimation, kitchen workflow, analytics. */
public class CanteenService {
    private final List<FoodItem> menu = new ArrayList<>();
    private final List<Order> orders = new ArrayList<>();   // insertion order = queue order
    private final FileStore store;

    public CanteenService(FileStore store) {
        this.store = store;
        menu.add(new FoodItem(1, "Veg Sandwich", 40, 5));
        menu.add(new FoodItem(2, "Masala Dosa", 60, 8));
        menu.add(new FoodItem(3, "Chole Bhature", 70, 10));
        menu.add(new FoodItem(4, "Paneer Roll", 65, 7));
        menu.add(new FoodItem(5, "Cold Coffee", 50, 3));
        menu.add(new FoodItem(6, "Samosa (2 pcs)", 25, 2));
        orders.addAll(store.load(menu.stream().collect(Collectors.toMap(FoodItem::getId, f -> f))));
    }

    public List<FoodItem> getMenu() { return Collections.unmodifiableList(menu); }

    public FoodItem findItem(int id) {
        return menu.stream().filter(f -> f.getId() == id).findFirst().orElse(null);
    }

    // ---- Student side ----

    public synchronized Order placeOrder(String studentName, Map<FoodItem, Integer> cart)
            throws InvalidOrderException {
        if (studentName == null || studentName.isBlank())
            throw new InvalidOrderException("Please enter your name.");
        if (cart == null || cart.isEmpty())
            throw new InvalidOrderException("Your cart is empty.");
        Order o = new Order(new Student(studentName.trim().replace(",", " ")), cart);
        orders.add(o);
        store.save(orders);
        return o;
    }

    public synchronized void cancelOrder(String token) throws InvalidOrderException {
        Order o = findByToken(token);
        if (o == null) throw new InvalidOrderException("No order found for token " + token);
        if (o.getStatus() != OrderStatus.PLACED)
            throw new InvalidOrderException("Order is already " + o.getStatus() + " and cannot be cancelled.");
        o.setStatus(OrderStatus.CANCELLED);
        store.save(orders);
    }

    public synchronized Order findByToken(String token) {
        if (token == null) return null;
        return orders.stream().filter(o -> o.getToken().equalsIgnoreCase(token.trim()))
                .findFirst().orElse(null);
    }

    /** Orders still in the kitchen (PLACED or PREPARING) that were placed before this one. */
    private List<Order> kitchenQueueAhead(Order target) {
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.PREPARING)
                .takeWhile(o -> o != target)
                .collect(Collectors.toList());
    }

    /** 1 = next in line. 0 means the order is not waiting in the kitchen queue. */
    public synchronized int queuePosition(Order o) {
        if (o.getStatus() != OrderStatus.PLACED && o.getStatus() != OrderStatus.PREPARING) return 0;
        return kitchenQueueAhead(o).size() + 1;
    }

    public synchronized int estimatedWaitMinutes(Order o) {
        if (o.getStatus() != OrderStatus.PLACED && o.getStatus() != OrderStatus.PREPARING) return 0;
        return kitchenQueueAhead(o).stream().mapToInt(Order::getPrepMinutes).sum() + o.getPrepMinutes();
    }

    // ---- Kitchen side ----

    public synchronized List<Order> activeOrders() {
        return orders.stream().filter(o -> o.getStatus().isActive()).collect(Collectors.toList());
    }

    public synchronized void advance(String token) throws InvalidOrderException {
        Order o = findByToken(token);
        if (o == null) throw new InvalidOrderException("No order found for token " + token);
        if (!o.getStatus().isActive()) throw new InvalidOrderException("Order is already " + o.getStatus());
        o.setStatus(o.getStatus().next());
        store.save(orders);
    }

    // ---- Analytics (Streams) ----

    public synchronized String dailySalesReport(LocalDate day) {
        List<Order> today = orders.stream()
                .filter(o -> o.getPlacedAt().toLocalDate().equals(day))
                .collect(Collectors.toList());
        List<Order> valid = today.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED).collect(Collectors.toList());

        double revenue = valid.stream().mapToDouble(Order::getTotal).sum();
        long cancelled = today.size() - valid.size();

        Map<String, Integer> qtyByItem = new TreeMap<>();
        valid.forEach(o -> o.getItems().forEach((f, q) -> qtyByItem.merge(f.getName(), q, Integer::sum)));

        StringBuilder sb = new StringBuilder();
        sb.append("DAILY SALES REPORT - ").append(day).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("Orders placed : %d%n", today.size()));
        sb.append(String.format("Cancelled     : %d%n", cancelled));
        sb.append(String.format("Revenue       : Rs %.2f%n", revenue));
        sb.append(String.format("Avg order     : Rs %.2f%n", valid.isEmpty() ? 0.0 : revenue / valid.size()));
        sb.append(String.format("Items sold    : %d%n", qtyByItem.values().stream().mapToInt(Integer::intValue).sum()));
        sb.append("Best seller   : ").append(qtyByItem.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(e -> e.getKey() + " (" + e.getValue() + ")")
                .orElse("-")).append("\n");
        sb.append("\nItems sold (high to low):\n");
        if (qtyByItem.isEmpty()) sb.append("  (no sales yet)\n");
        qtyByItem.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("  %-18s x %d%n", e.getKey(), e.getValue())));
        return sb.toString();
    }
}
