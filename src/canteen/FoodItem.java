package canteen;

/** A dish on the canteen menu. Immutable, so it is safe to share everywhere. */
public class FoodItem {
    private final int id;
    private final String name;
    private final double price;
    private final int prepMinutes;

    public FoodItem(int id, String name, double price, int prepMinutes) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.prepMinutes = prepMinutes;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getPrice() { return price; }
    public int getPrepMinutes() { return prepMinutes; }

    @Override public boolean equals(Object o) {
        return o instanceof FoodItem f && f.id == id;
    }
    @Override public int hashCode() { return Integer.hashCode(id); }
    @Override public String toString() { return name; }
}
