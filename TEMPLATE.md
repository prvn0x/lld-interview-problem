# LLD Problem Template

Use this template when adding a new LLD problem to the repository.

## Folder Structure for Each Problem

```
src/com/lld/<problem-name>/
├── models/              # Data models (User, Booking, Payment, etc.)
├── services/            # Business logic services
├── enums/               # Enums (Status, Type, etc.)
├── strategies/          # Strategy pattern implementations (optional)
├── exceptions/          # Custom exceptions (optional)
└── Main.java           # Demo with dummy data
```

## Documentation Structure

```
docs/<problem-name>/
├── README.md           # Complete problem documentation
├── requirements.md     # Functional & non-functional requirements
└── diagrams/          # UML class diagrams (optional)
```

## Checklist for Each Problem

- [ ] Clear problem statement
- [ ] Requirements (functional & non-functional)
- [ ] Class diagram (can be text-based)
- [ ] All model classes implemented
- [ ] Service classes with business logic
- [ ] Enums for constants
- [ ] Main.java with working demo
- [ ] Uses in-memory data structures (HashMap, ArrayList, etc.)
- [ ] No database/external dependencies
- [ ] Code is runnable
- [ ] Comments explaining key logic
- [ ] Design patterns applied where appropriate

## Code Guidelines

1. **Use proper OOP principles**
   - Encapsulation (private fields, public getters/setters)
   - Inheritance where applicable
   - Polymorphism and interfaces

2. **Follow SOLID principles**
   - Single Responsibility
   - Open/Closed
   - Liskov Substitution
   - Interface Segregation
   - Dependency Inversion

3. **Naming Conventions**
   - Classes: PascalCase (User, ParkingLot)
   - Methods: camelCase (bookTicket, findAvailableSpot)
   - Constants: UPPER_SNAKE_CASE (MAX_CAPACITY)

4. **In-Memory Storage Examples**
   ```java
   // Store users
   private Map<String, User> users = new HashMap<>();

   // Store bookings
   private List<Booking> bookings = new ArrayList<>();

   // Store by ID
   private Map<Long, Ticket> ticketsById = new HashMap<>();

   // Priority queue for ordering
   private PriorityQueue<Request> pendingRequests = new PriorityQueue<>();
   ```

5. **Main.java Structure**
   ```java
   public class Main {
       public static void main(String[] args) {
           // 1. Initialize system
           // 2. Create dummy data
           // 3. Demonstrate key functionalities
           // 4. Print outputs
       }
   }
   ```

## Example Main.java Template

```java
package com.lld.problemname;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Problem Name System Demo ===\n");

        // Initialize service
        ProblemService service = new ProblemService();

        // Create dummy data
        User user1 = new User("U1", "John Doe", "john@example.com");
        User user2 = new User("U2", "Jane Smith", "jane@example.com");

        // Register users
        service.registerUser(user1);
        service.registerUser(user2);

        // Demo Scenario 1
        System.out.println("--- Scenario 1 ---");
        // ... demo code ...

        // Demo Scenario 2
        System.out.println("\n--- Scenario 2 ---");
        // ... demo code ...

        System.out.println("\n=== Demo Complete ===");
    }
}
```
