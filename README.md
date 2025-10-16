# CareHome Management System (GUI)

## 1. Overview
The **CareHome Management System** is a JavaFX-based desktop application designed to manage staff, residents, beds, and prescriptions in a care home environment.  
It provides both **Manager** and **Staff** interfaces, offering functionality for adding and viewing residents, assigning beds, recording medications, and maintaining logs.  
Data is persisted locally to ensure continuity between sessions.

---

## 2. How to Run the Program

### Prerequisites
- **Java 17** or higher  
- **Maven 3.8+**  
- **JavaFX SDK** (automatically managed via Maven dependencies)  
- Works on macOS, Windows, or Linux  

### Build the Project
Open a terminal in the project directory and run:
```bash
mvn clean compile
```
This command compiles the code, runs JUnit tests, and packages the application into a `.jar` file under the `target/` folder.

### Run the GUI
You can launch the JavaFX GUI in theways:

#### **Run via Maven**
```bash
mvn javafx:run
```

```
Upon startup, the application automatically loads existing data from:
```
carehome-data.txt

## 3. GUI Operation Guide

### **Menu Buttons & Functionality**

| Button | Role | Description |
|--------|------|-------------|
| **Login as Staff** | Staff | Grants access to nurse and doctor features (prescriptions, medications). |
| **Login as Manager** | Manager | Allows access to administrative actions such as adding residents and managing staff. |
| **Add Resident** | Manager | Opens a form to input new resident details (name, age, gender, etc.). |
| **View Residents** | All | Displays the list of all residents currently in the system. |
| **Assign Resident to Bed** | Manager | Assigns an unassigned resident to an available bed. |
| **Manage Staff** | Manager | Opens management panel for adding/removing staff. |
| **Move Resident (Nurse)** | Nurse | Transfers a resident to another bed. |
| **Create Prescription (Doctor)** | Doctor | Creates a new prescription record for a resident. |
| **Update Prescription (Doctor)** | Doctor | Edits existing prescription information. |
| **Administer Medicine (Nurse)** | Nurse | Logs medicine administration for a resident. |
| **Check Bed Details** | All | Displays the current occupancy and bed information. |
| **Bed Overview (M=Blue / F=Red)** | All | Provides a visual map of beds with gender-coded coloring. |
| **Exit** | All | Saves all data to `carehome-data.txt` and closes the application. |

---

### **Interface Logic**
- The **title label** displays the current login status (`Not logged in`, `Staff: ID`, or `Manager: ID`).  
- Buttons dynamically enable or disable based on user role to prevent unauthorized actions.  
- All actions are handled through the backend `CareHomeSystem` instance (`system`), ensuring consistent data synchronization.  
- The GUI maintains simple vertical alignment using `VBox` with spacing and padding for clarity.

---

### **User Flow Summary**
1. Launch the app → `CareHome Management System` main screen opens.  
2. Choose **Login as Staff** or **Login as Manager**.  
3. Perform permitted actions according to role.  
4. Exit safely — system auto-saves all records.

## 4. Design Outline

### **Architecture**
The application follows a **modular MVC-inspired structure** separating GUI, logic, and data:
```
CareHomeApp.java         → GUI (JavaFX front-end)
CareHomeSystem.java      → Core system logic (backend manager)
Staff.java / Resident.java / Bed.java / Prescription.java / MedicationLog.java
                         → Domain model classes
Logger.java              → Records system actions
```

### **Design Highlights**
1. **Encapsulation of Logic:**  
   - The `CareHomeSystem` class encapsulates all management operations (add, assign, log, etc.).
   - The GUI (`CareHomeApp`) interacts with it through public methods only, preserving data integrity.

2. **JavaFX GUI Abstraction:**  
   - All UI controls (buttons, labels, and panes) are built dynamically.
   - Scene transitions reflect login status (manager vs. staff) to avoid multiple stages.

3. **Persistence Layer:**  
   - Data is serialized in a human-readable text format (`carehome-data.txt`).
   - Auto-loading on startup ensures smooth continuity without requiring a database.

4. **Authorization and Roles:**  
   - Distinct actions for **Manager** and **Staff** ensure clear role-based access control.

---

## 5. Design Decisions

| Decision | Rationale |
|-----------|------------|
| **JavaFX** for GUI | Provides a clean and interactive interface suitable for desktop apps |
| **Maven** for build | Simplifies dependency management and ensures cross-platform builds |
| **Text file persistence** | Lightweight, easy for testing and submission (no DB needed) |
| **Map-based data structures** | Enables fast lookup for staff, beds, and residents by ID |
| **Role-based logic** | Keeps the system secure and avoids logic duplication |
| **Logger component** | Allows traceability of staff actions and medication logs |

---

## 6. Refactoring Report

| Refactoring Area | Before | After | Benefit |
|------------------|--------|-------|----------|
| **Data Initialization** | Hardcoded in multiple places | Centralized in `CareHomeSystem` constructor | Easier to extend and debug |
| **Login Handling** | GUI directly modified system fields | Introduced `currentStaffId` and `isManager` with clear UI update method | Improved separation of concerns |
| **File Loading/Saving** | Spread across multiple methods | Consolidated into `loadFromFile()` and `saveToFile()` | More reliable and maintainable |
| **UI Components** | Repeated code for buttons and labels | Refactored into reusable JavaFX components with spacing/alignment | Cleaner layout and reduced duplication |
| **Validation** | Minimal checking | Added simple validation for IDs, roles, and null entries | Prevents runtime crashes and bad data |

---

## 7. Testing
The system includes JUnit 5 tests for backend logic (in `CareHomeSystemTest.java`):
```bash
mvn test
```
Tests cover:
- Resident creation and assignment  
- Bed vacancy tracking  
- Data persistence validation  
- Logging and staff operations  

All tests pass successfully after build (`BUILD SUCCESS`).

---

## 8. Future Improvements
- Replace file storage with an embedded database (e.g., SQLite)  
- Add GUI table views for real-time resident and bed monitoring  
- Integrate notifications for medication reminders  
- Add user authentication and role-based login credentials  

---

## 9. Author
Developed by **Mengti Hu**  
RMIT University — COSC1295 Advanced Programming Assignment 2 Project (2025)
