package canteen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Swing GUI with three tabs: Student, Kitchen, Analytics. */
public class CanteenApp extends JFrame {
    private final CanteenService service = new CanteenService(new FileStore("orders.csv"));
    private final Map<FoodItem, Integer> cart = new LinkedHashMap<>();

    private DefaultTableModel menuModel, kitchenModel;
    private JTable menuTable, kitchenTable;
    private JTextArea cartArea, reportArea;
    private JTextField nameField, tokenField;
    private JLabel trackLabel;

    public CanteenApp() {
        super("Campus Canteen - Pre-Order & Queue Management");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(820, 560);
        setLocationRelativeTo(null);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Student", buildStudentTab());
        tabs.addTab("Kitchen", buildKitchenTab());
        tabs.addTab("Analytics", buildAnalyticsTab());
        tabs.addChangeListener(e -> { refreshKitchen(); refreshReport(); });
        add(tabs);
        refreshKitchen();
        refreshReport();
    }

    // ---------------- Student tab ----------------
    private JPanel buildStudentTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));

        menuModel = new DefaultTableModel(new String[]{"ID", "Item", "Price (Rs)", "Prep (min)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (FoodItem f : service.getMenu())
            menuModel.addRow(new Object[]{f.getId(), f.getName(), f.getPrice(), f.getPrepMinutes()});
        menuTable = new JTable(menuModel);
        JScrollPane menuScroll = new JScrollPane(menuTable);
        menuScroll.setBorder(BorderFactory.createTitledBorder("Today's Menu"));

        JSpinner qty = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        JButton addBtn = new JButton("Add to cart");
        addBtn.addActionListener(e -> {
            int row = menuTable.getSelectedRow();
            if (row < 0) { msg("Select a menu item first."); return; }
            FoodItem f = service.findItem((int) menuModel.getValueAt(row, 0));
            cart.merge(f, (int) qty.getValue(), Integer::sum);
            refreshCart();
        });
        JButton clearBtn = new JButton("Clear cart");
        clearBtn.addActionListener(e -> { cart.clear(); refreshCart(); });
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addRow.add(new JLabel("Qty:")); addRow.add(qty); addRow.add(addBtn); addRow.add(clearBtn);

        cartArea = new JTextArea(6, 24);
        cartArea.setEditable(false);
        JScrollPane cartScroll = new JScrollPane(cartArea);
        cartScroll.setBorder(BorderFactory.createTitledBorder("Your Cart"));

        nameField = new JTextField(12);
        JButton placeBtn = new JButton("Place Order");
        placeBtn.addActionListener(e -> placeOrder());
        JPanel placeRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        placeRow.add(new JLabel("Your name:")); placeRow.add(nameField); placeRow.add(placeBtn);

        JPanel right = new JPanel(new BorderLayout(5, 5));
        right.add(cartScroll, BorderLayout.CENTER);
        right.add(placeRow, BorderLayout.SOUTH);

        JPanel top = new JPanel(new BorderLayout(10, 10));
        JPanel left = new JPanel(new BorderLayout());
        left.add(menuScroll, BorderLayout.CENTER);
        left.add(addRow, BorderLayout.SOUTH);
        top.add(left, BorderLayout.CENTER);
        top.add(right, BorderLayout.EAST);

        // Track / cancel
        tokenField = new JTextField(8);
        JButton checkBtn = new JButton("Check status");
        checkBtn.addActionListener(e -> trackOrder());
        JButton cancelBtn = new JButton("Cancel order");
        cancelBtn.addActionListener(e -> cancelOrder());
        trackLabel = new JLabel("Enter your pickup token (e.g. T101).");
        JPanel track = new JPanel(new FlowLayout(FlowLayout.LEFT));
        track.setBorder(BorderFactory.createTitledBorder("Track / Cancel"));
        track.add(new JLabel("Token:")); track.add(tokenField);
        track.add(checkBtn); track.add(cancelBtn); track.add(trackLabel);

        p.add(top, BorderLayout.CENTER);
        p.add(track, BorderLayout.SOUTH);
        return p;
    }

    private void refreshCart() {
        StringBuilder sb = new StringBuilder();
        double total = 0;
        for (Map.Entry<FoodItem, Integer> e : cart.entrySet()) {
            double line = e.getKey().getPrice() * e.getValue();
            total += line;
            sb.append(String.format("%dx %-16s Rs %.0f%n", e.getValue(), e.getKey().getName(), line));
        }
        if (!cart.isEmpty()) sb.append(String.format("%nTotal: Rs %.0f", total));
        cartArea.setText(sb.toString());
    }

    private void placeOrder() {
        try {
            Order o = service.placeOrder(nameField.getText(), new LinkedHashMap<>(cart));
            cart.clear();
            refreshCart();
            tokenField.setText(o.getToken());
            msg("Order placed!\n\nPickup token: " + o.getToken()
                    + "\nQueue position: " + service.queuePosition(o)
                    + "\nEstimated wait: " + service.estimatedWaitMinutes(o) + " min"
                    + "\nTotal: Rs " + (int) o.getTotal());
            trackOrder();
        } catch (InvalidOrderException ex) {
            msg(ex.getMessage());
        }
    }

    private void trackOrder() {
        Order o = service.findByToken(tokenField.getText());
        if (o == null) { trackLabel.setText("No order found."); return; }
        String extra = "";
        if (o.getStatus() == OrderStatus.PLACED || o.getStatus() == OrderStatus.PREPARING)
            extra = " | position " + service.queuePosition(o) + " | ~" + service.estimatedWaitMinutes(o) + " min";
        else if (o.getStatus() == OrderStatus.READY) extra = " | collect at the counter!";
        trackLabel.setText(o.getToken() + ": " + o.getStatus() + extra);
    }

    private void cancelOrder() {
        try {
            service.cancelOrder(tokenField.getText());
            trackOrder();
        } catch (InvalidOrderException ex) {
            msg(ex.getMessage());
        }
    }

    // ---------------- Kitchen tab ----------------
    private JPanel buildKitchenTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        kitchenModel = new DefaultTableModel(
                new String[]{"Token", "Student", "Items", "Status", "Queue pos", "Wait (min)"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        kitchenTable = new JTable(kitchenModel);
        JButton advance = new JButton("Move to next status");
        advance.addActionListener(e -> {
            int row = kitchenTable.getSelectedRow();
            if (row < 0) { msg("Select an order first."); return; }
            try {
                service.advance((String) kitchenModel.getValueAt(row, 0));
                refreshKitchen();
                refreshReport();
            } catch (InvalidOrderException ex) { msg(ex.getMessage()); }
        });
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> refreshKitchen());
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.add(advance); bar.add(refresh);
        bar.add(new JLabel("  PLACED -> PREPARING -> READY -> COLLECTED"));
        p.add(new JScrollPane(kitchenTable), BorderLayout.CENTER);
        p.add(bar, BorderLayout.SOUTH);
        return p;
    }

    private void refreshKitchen() {
        kitchenModel.setRowCount(0);
        List<Order> active = service.activeOrders();
        for (Order o : active)
            kitchenModel.addRow(new Object[]{o.getToken(), o.getStudent().getName(), o.itemsSummary(),
                    o.getStatus(), service.queuePosition(o) == 0 ? "-" : service.queuePosition(o),
                    service.estimatedWaitMinutes(o)});
    }

    // ---------------- Analytics tab ----------------
    private JPanel buildAnalyticsTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBorder(new EmptyBorder(12, 12, 12, 12));
        reportArea = new JTextArea();
        reportArea.setEditable(false);
        reportArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JButton refresh = new JButton("Refresh report");
        refresh.addActionListener(e -> refreshReport());
        p.add(new JScrollPane(reportArea), BorderLayout.CENTER);
        p.add(refresh, BorderLayout.SOUTH);
        return p;
    }

    private void refreshReport() { reportArea.setText(service.dailySalesReport(LocalDate.now())); }

    private void msg(String s) { JOptionPane.showMessageDialog(this, s); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CanteenApp().setVisible(true));
    }
}
