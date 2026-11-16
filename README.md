# Campus Canteen Pre-Order & Queue Management
Course: 25CSE0202 Programming in Java | Swing GUI + CSV file storage

## How to run (needs a JDK, Java 17+)
    mkdir out
    javac -d out src/canteen/*.java
    java -cp out canteen.CanteenApp      # GUI
    java -cp out canteen.DemoRunner      # console demo of the core logic

## Features (from the project brief)
| Requirement                     | Where                                              |
|---------------------------------|----------------------------------------------------|
| Browse daily menus              | Student tab, menu table (CanteenService.menu)      |
| Place and cancel orders         | placeOrder(), cancelOrder()                        |
| Generate pickup tokens          | Order.getToken() -> T101, T102...                  |
| Estimate queue position         | queuePosition(), estimatedWaitMinutes()            |
| Manage kitchen preparation      | Kitchen tab, advance() with OrderStatus.next()     |
| Daily sales analytics           | Analytics tab, 
|   - extras                      | Avg order value, items sold, best seller           |

dailySalesReport() using Streams    |

## Class design (OOP)
- Person (abstract) -> Student : inheritance, abstraction, polymorphism (getRole)
- FoodItem : encapsulation (private final fields, getters), equals/hashCode
- Order : encapsulation, owns a Map<FoodItem,Integer>, toCsv()
- OrderStatus (enum) : PLACED -> PREPARING -> READY -> COLLECTED / CANCELLED
- InvalidOrderException : custom checked exception
- FileStore : file handling (BufferedReader/PrintWriter, try-with-resources)
- CanteenService : business logic, synchronized methods (thread safety)
- CanteenApp : Swing GUI;  DemoRunner : console demo

## Course topics covered
Collections (ArrayList, LinkedHashMap, TreeMap), Generics, Exceptions (custom + try-with-resources),
File handling (CSV read/write), Lambdas and Streams (filter, map, takeWhile, groupBy-style merge, sorted),
Synchronization (synchronized service methods), Inheritance/Abstraction/Enums.

## Live demo script (about 3 minutes)
1. Start app. Student tab: pick Masala Dosa, qty 1, Add. Pick Cold Coffee, qty 2, Add.
2. Enter name "Jasmine", Place Order. Popup shows token, queue position, wait time.
3. Place a second order as "Aarav". Type Jasmine's token, Check status: position 1; Aarav is 2.
4. Cancel Aarav's order (token + Cancel order). Status shows CANCELLED.
5. Kitchen tab: select Jasmine's order, click "Move to next status" until READY, then COLLECTED.
6. Back on Student tab, check the token: READY says collect at counter.
7. Analytics tab: orders, cancellations, revenue, items sold.
8. Close app, reopen: orders are still there (orders.csv). That shows persistence.
9. Error cases: empty cart, blank name, cancelling an order that is already PREPARING.

## Code walkthrough order (for the viva)
1. FoodItem, OrderStatus: simple building blocks. Mention why enum.next() beats if/else chains.
2. Person -> Student: inheritance and abstraction.
3. Order: items map, token from id, getTotal()/getPrepMinutes() with streams, toCsv().
4. CanteenService.placeOrder: validation, throws InvalidOrderException, saves to file.
5. queuePosition(): stream of PLACED/PREPARING orders with takeWhile(o != target). Wait time = sum of prep times ahead + own.
6. advance(): status moves one step via OrderStatus.next().
7. dailySalesReport(): filter by date, exclude cancelled, aggregate with merge, sort descending.
8. FileStore: CSV format id,name,status,time,itemId:qty;... and how corrupt lines are skipped.
9. CanteenApp: three tabs, buttons call service methods, GUI holds no business logic.

## Likely viva questions
- Why abstract class Person instead of an interface? Shared state (name) plus a required method.
- Why checked exception? The caller (GUI) must handle bad input and show a message.
- Why synchronized? Several threads (EDT, future auto-refresh) could modify the orders list.
- How would you move to a database? Replace FileStore with a JDBC-based class using PreparedStatement; the service stays unchanged.
- Limitation: wait time is an estimate (sum of prep times, one kitchen worker assumed).
