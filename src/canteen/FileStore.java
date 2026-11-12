package canteen;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

/** File handling: saves and loads orders as CSV, with try-with-resources. */
public class FileStore {
    private final Path path;

    public FileStore(String fileName) { this.path = Paths.get(fileName); }

    public void save(List<Order> orders) {
        try (PrintWriter out = new PrintWriter(new FileWriter(path.toFile()))) {
            for (Order o : orders) out.println(o.toCsv());
        } catch (IOException e) {
            System.err.println("Could not save orders: " + e.getMessage());
        }
    }

    public List<Order> load(Map<Integer, FoodItem> menuById) {
        List<Order> result = new ArrayList<>();
        if (!Files.exists(path)) return result;
        try (BufferedReader in = new BufferedReader(new FileReader(path.toFile()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.isBlank()) continue;
                try {
                    String[] p = line.split(",", 5);
                    Map<FoodItem, Integer> items = new LinkedHashMap<>();
                    for (String pair : p[4].split(";")) {
                        String[] kv = pair.split(":");
                        FoodItem f = menuById.get(Integer.parseInt(kv[0]));
                        if (f != null) items.put(f, Integer.parseInt(kv[1]));
                    }
                    result.add(new Order(Integer.parseInt(p[0]), new Student(p[1]), items,
                            OrderStatus.valueOf(p[2]), LocalDateTime.parse(p[3])));
                } catch (RuntimeException bad) {
                    System.err.println("Skipping corrupt line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read orders: " + e.getMessage());
        }
        return result;
    }
}
