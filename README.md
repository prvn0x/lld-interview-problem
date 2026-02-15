# Low-Level Design Interview Problems

A comprehensive collection of LLD/Machine Coding interview problems commonly asked in product-based companies. Each problem includes complete working code with in-memory data storage (no database required).

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![LLD](https://img.shields.io/badge/LLD-System_Design-blue?style=for-the-badge)

## 📋 Table of Contents

- [About](#about)
- [Problems List](#problems-list)
- [Project Structure](#project-structure)
- [How to Run](#how-to-run)
- [What to Expect in Interviews](#what-to-expect-in-interviews)
- [Contributing](#contributing)

## 🎯 About

This repository contains **working implementations** of popular LLD interview problems. Each problem includes:

✅ **Complete class design** with proper OOP principles
✅ **Working code** that you can run immediately
✅ **In-memory data storage** (HashMap, ArrayList - no DB needed)
✅ **Dummy data examples** to demonstrate functionality
✅ **Design patterns** applied where appropriate
✅ **Documentation** explaining the approach

## 📝 Problems List

> **Note:** Problems will be added progressively

### Booking & Reservation Systems
- [ ] Parking Lot
- [ ] Movie Ticket Booking (BookMyShow)
- [ ] Hotel Booking System
- [ ] Flight Booking System

### Social & Communication
- [ ] Tinder (Dating App)
- [ ] Facebook/Social Network
- [ ] Instagram
- [ ] Twitter
- [ ] WhatsApp/Chat Application

### E-Commerce & Payments
- [ ] Amazon/E-Commerce Platform
- [ ] Splitwise (Expense Sharing)
- [ ] Payment Gateway
- [ ] Wallet System

### Transportation & Delivery
- [ ] Uber/Ola (Ride Sharing)
- [ ] Food Delivery (Swiggy/Zomato)
- [ ] Logistics System

### Gaming & Entertainment
- [ ] Chess Game
- [ ] Snake & Ladder
- [ ] Tic Tac Toe
- [ ] Card Game (Poker/Blackjack)

### Productivity & Collaboration
- [ ] Google Calendar
- [ ] Task Management System (Jira)
- [ ] Meeting Scheduler
- [ ] File Storage System (Google Drive)

### Others
- [ ] ATM System
- [ ] Library Management System
- [ ] Elevator System
- [ ] Vending Machine
- [ ] Car Rental System

## 🗂️ Project Structure

```
lld-interview-problems/
├── src/
│   └── com/
│       └── lld/
│           ├── parkinglot/
│           │   ├── models/           # Vehicle, ParkingSpot, Ticket, etc.
│           │   ├── services/         # ParkingLotService, PaymentService
│           │   ├── enums/            # VehicleType, SpotType, Status
│           │   ├── strategies/       # Pricing, Spot allocation strategies
│           │   └── Main.java         # Demo with dummy data
│           ├── tinder/
│           │   ├── models/
│           │   ├── services/
│           │   └── Main.java
│           └── <other-problems>/
├── docs/
│   ├── parkinglot/
│   │   ├── README.md              # Problem statement & approach
│   │   ├── class-diagram.png      # UML diagrams
│   │   └── requirements.md        # Functional & non-functional requirements
│   └── <other-problems>/
├── .gitignore
└── README.md
```

## 🚀 How to Run

### Method 1: IntelliJ IDEA (Recommended - One Click!)

1. **Open the project** in IntelliJ IDEA
2. **Navigate to any problem:**
   ```
   src → com → lld → <problem> → Main.java
   ```
3. **Right-click on `Main.java`** → Select **"Run 'Main.main()'"**
4. **See output** in the console!

**Example:**
```
src/com/lld/parkinglot/Main.java
→ Right-click → Run ✅
```

### Method 2: Command Line

#### Compile:
```bash
javac -cp src src/com/lld/<problem>/**/*.java
```

#### Run:
```bash
java -cp src com.lld.<problem>.Main
```

#### Example (Parking Lot):
```bash
javac -cp src src/com/lld/parkinglot/**/*.java
java -cp src com.lld.parkinglot.Main
```

## 💡 What to Expect in Interviews

### Interview Format

**1. Problem Discussion (10-15 min)**
- Clarify requirements (functional & non-functional)
- Ask questions about scale, features, constraints
- Discuss high-level components

**2. Class Design (15-20 min)**
- Design classes, interfaces, enums
- Define relationships (inheritance, composition)
- Apply SOLID principles
- Identify design patterns

**3. Coding (30-40 min)**
- Implement key classes and methods
- Write working code (not pseudocode!)
- Use dummy data to demonstrate
- Handle edge cases

**4. Testing & Discussion (5-10 min)**
- Walk through your code
- Explain design decisions
- Discuss trade-offs and improvements

### Key Points for Interviews

✅ **DO:**
- Clarify requirements before coding
- Think out loud - explain your thought process
- Use proper OOP principles (encapsulation, inheritance, polymorphism)
- Apply design patterns where appropriate
- Write clean, readable code
- Use meaningful variable names
- Handle edge cases
- Test with examples

❌ **DON'T:**
- Jump straight to coding without clarifying
- Hardcode values everywhere
- Create God classes (too many responsibilities)
- Ignore SOLID principles
- Write overly complex solutions
- Forget to handle null/edge cases

### Data Storage Approach

In interviews, **always use in-memory data structures**:
- `HashMap<String, Object>` for key-value storage
- `ArrayList<Object>` for lists
- `HashSet<Object>` for unique collections
- `PriorityQueue<Object>` for priority-based operations
- `ConcurrentHashMap` if discussing thread-safety

**Example:**
```java
// Good for interviews ✅
private Map<String, User> users = new HashMap<>();
private List<Ride> activeRides = new ArrayList<>();

// NOT needed in interviews ❌
// Database connections, SQL queries, ORMs, etc.
```

## 🤝 Contributing

Want to add more problems or improve existing solutions? Contributions are welcome!

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/NewProblem`)
3. Follow the existing package structure
4. Add documentation in the `docs/` folder
5. Commit your changes (`git commit -m 'Add NewProblem'`)
6. Push to the branch (`git push origin feature/NewProblem`)
7. Open a Pull Request

## 📚 Learning Resources

- [Design Patterns](https://refactoring.guru/design-patterns)
- [SOLID Principles](https://www.digitalocean.com/community/conceptual_articles/s-o-l-i-d-the-first-five-principles-of-object-oriented-design)
- [System Design Primer](https://github.com/donnemartin/system-design-primer)
- [LLD Interview Guide](https://workat.tech/machine-coding/article/how-to-practice-for-machine-coding-kp0oj3sw2jca)

## 👨‍💻 Author

Praveen Singh

---

**Note:** This repository is created for educational purposes to help developers prepare for LLD/Machine Coding interviews.

## ⭐ Star this repo if it helps you!
