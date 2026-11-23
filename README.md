# ⚡ Intelligent Daily Schedule Optimizer

A modern web-based daily task management application that helps users organize their daily activities according to deadlines, priorities, and estimated completion times.

## 🛠️ Technology Stack
- **Frontend:** HTML5, CSS3, Modern Vanilla JavaScript (Fetch API)
- **Backend:** Core Java (JDK 17 / 21 / 27), OOP Concepts, Built-in Java HTTP Server
- **Database Connectivity:** JDBC (Java Database Connectivity)
- **Database:** MySQL 8.x
- **Development Tool:** VS Code / IntelliJ IDEA / Eclipse

---

## 📁 Project Structure
```
ScheduleOptimizer/
├── run.bat                 <-- 1-Click Runner for Windows Command Prompt
├── run.ps1                 <-- 1-Click Runner for VS Code PowerShell Terminal
├── build.ps1               <-- Compiles all Java files to bin/
├── db.properties           <-- MySQL configuration (username, password, port)
├── database.sql            <-- Database schema & sample seed data
├── lib/
│   └── mysql-connector-j.jar <-- MySQL JDBC Driver
├── src/
│   └── com/scheduler/
│       ├── Main.java              <-- Server bootstrap & route binding
│       ├── config/
│       │   └── DBConnection.java  <-- JDBC connection & auto-table creation
│       ├── model/
│       │   ├── User.java          <-- User entity (id, username, email, etc.)
│       │   ├── Task.java          <-- Task entity (priority, deadline, est time, slots)
│       │   └── ScheduleSummary.java <-- Daily metrics summary model
│       ├── dao/
│       │   ├── UserDao.java       <-- User registration & login with SHA-256
│       │   └── TaskDao.java       <-- Task CRUD, status toggling, summary query
│       ├── service/
│       │   └── ScheduleOptimizer.java <-- Hybrid EDF + Priority scheduling algorithm
│       ├── controller/
│       │   ├── AuthHandler.java   <-- /api/auth/register, /api/auth/login
│       │   ├── TaskHandler.java   <-- /api/tasks (GET, POST, PUT, DELETE, toggle)
│       │   ├── ScheduleHandler.java <-- /api/schedule/optimize, /api/schedule/summary
│       │   └── StaticFileHandler.java <-- Serves HTML/CSS/JS frontend files
│       └── util/
│           └── JsonUtil.java      <-- Lightweight JSON serializer/parser
└── web/
    ├── index.html          <-- Dashboard with stats, timeline, and task table
    ├── login.html          <-- Auth page with sign-in and registration tabs
    ├── css/
    │   └── style.css       <-- Modern responsive SaaS UI design
    └── js/
        ├── auth.js         <-- Client-side session and auth logic
        └── app.js          <-- Task CRUD, filters, timeline & optimizer trigger
```

---

## 🚀 Quick Start (1 Command Run)

### Step 1: Open MySQL & Import Database
Open **MySQL Workbench** or command line and run `database.sql`:
```sql
SOURCE database.sql;
```
*(Or simply make sure MySQL Server is running on port 3306! The application can auto-create the database and tables automatically on first startup).*

### Step 2: Configure Password in `db.properties`
Open `db.properties` and verify your MySQL password:
```properties
db.url=jdbc:mysql://localhost:3306/schedule_optimizer_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=root      <-- Change this to your MySQL root password (e.g. root, admin, or empty)
```

### Step 3: Run the Application in VS Code
Open the `ScheduleOptimizer` folder in **VS Code**, open the integrated terminal (`Ctrl + ~`), and run:

```powershell
.\run.ps1
```
*(or double click `run.bat` in Windows Explorer)*

The application will start at **http://localhost:8080/login.html** and open automatically in your browser!

### Default Demo Account:
- **Username:** `demo`
- **Password:** `admin123`

---

## ✨ Features Implemented
1. **User Registration & Login:** Secure authentication with SHA-256 password hashing.
2. **Add, Update & Delete Tasks:** Full task management with title, description, priority, deadline, and estimated duration.
3. **Set Task Priorities & Deadlines:** High (Red), Medium (Amber), Low (Green) priority levels with deadline tracking.
4. **Intelligent Schedule Optimizer:** Calculates optimal sequence using Earliest Deadline First (EDF) + Priority Weighted Scoring (PWS) with 10-minute rest buffers and deadline risk indicators.
5. **Mark Tasks as Completed:** Instant 1-click status toggle with visual line-through and progress bar update.
6. **Daily Task Summary:** Real-time metrics tracking total tasks, completed percentage, pending focus hours, and priority breakdown.
