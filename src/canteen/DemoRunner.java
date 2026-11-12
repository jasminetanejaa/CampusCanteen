package canteen;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** Console demo of the core logic (no GUI). Handy for testing and for the viva. */
public class DemoRunner {
    public static void main(String[] args) throws Exception {
        CanteenService s = new CanteenService(new FileStore("demo-orders.csv"));

        Map<FoodItem, Integer> c1 = new LinkedHashMap<>();
        c1.put(s.findItem(2), 1); c1.put(s.findItem(5), 2);          // dosa + 2 coffee
        Map<FoodItem, Integer> c2 = new LinkedHashMap<>();
        c2.put(s.findItem(3), 1);                                     // chole bhature
        Map<FoodItem, Integer> c3 = new LinkedHashMap<>();
        c3.put(s.findItem(6), 3);                                     // 3 samosa

        Order a = s.placeOrder("Aarav", c1);
        Order b = s.placeOrder("Jasmine", c2);
        Order c = s.placeOrder("Riya", c3);
        for (Order o : new Order[]{a, b, c})
            System.out.printf("%s %-8s pos=%d wait=%d min total=Rs %.0f%n", o.getToken(),
                    o.getStudent().getName(), s.queuePosition(o), s.estimatedWaitMinutes(o), o.getTotal());

        System.out.println("\n-- Riya cancels --");
        s.cancelOrder(c.getToken());
        System.out.println(c.getToken() + " -> " + c.getStatus());

        System.out.println("\n-- Kitchen advances Aarav's order twice --");
        s.advance(a.getToken()); s.advance(a.getToken());
        System.out.println(a.getToken() + " -> " + a.getStatus());
        System.out.println(b.getToken() + " position now " + s.queuePosition(b)
                + ", wait " + s.estimatedWaitMinutes(b) + " min");

        System.out.println("\n-- Error handling --");
        try { s.placeOrder("Test", new LinkedHashMap<>()); }
        catch (InvalidOrderException e) { System.out.println("Caught: " + e.getMessage()); }
        try { s.cancelOrder(a.getToken()); }
        catch (InvalidOrderException e) { System.out.println("Caught: " + e.getMessage()); }

        System.out.println("\n" + s.dailySalesReport(LocalDate.now()));
    }
}
