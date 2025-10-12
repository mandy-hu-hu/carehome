# CareHome Demo

Java 17 + Maven project implementing required functionalities with a simple text menu and JUnit 5 tests.

## Build & Run (with Maven)
- Use 'mvn package' to build

Then use the menu to demo operations.
## Run Menu Demo

```bash
cd src/main/java
javac Main.java
java Main
```

## Run Tests

```bash
mvn test
```

## Key Features

- Assign resident to vacant bed
- Add/modify staff and password
- Allocate/replace shifts with overlap and weekly-hour-limit checks (40h default)
- Authorization & roster checks (roles: Doctor, Nurse)
- Create/update prescriptions (doctors only)
- Move resident between beds (nurses authorized)
- Administer medication (log resident, staff, medicine, dose, time)
- All actions logged with timestamp and staff id
- Exceptions: AuthorizationException, NotRosteredException, ShiftException

