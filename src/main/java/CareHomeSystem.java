import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * The main system managing staff, beds, residents, prescriptions, and logs.
 * It enforces authorization and roster checks for various operations.
 */
public class CareHomeSystem {
    private final Map<String, Staff> staff = new HashMap<>();
    private final Map<String, Bed> beds = new HashMap<>();
    private final Map<String, Resident> residents = new HashMap<>();
    private final Map<String, Prescription> prescriptions = new HashMap<>();
    private final List<MedicationLog> medLogs = new ArrayList<>();
    private final Logger logger = new Logger();

    // CONFIG
    private static final int WEEKLY_HOUR_LIMIT = 40;
    private static final String[] DEFAULT_STAFF_IDS = {"MGR01", "D01", "N01"};

    // ---- CONSTRUCTORS ----
    public CareHomeSystem() {
        this(true); // default = load demo data
    }

    public CareHomeSystem(boolean loadDemoData) {
        DatabaseManager.initialize();
        
        // 1) Initialise beds
        initializeBedsFixed4();

        // 2) Optionally seed demo data
        if (loadDemoData) {
            seedDemoData();
        }
        else{
            loadFromDatabase();
        }
    }

    private void initializeBedsFixed4() {
        for (int w = 1; w <= 2; w++) {          // Ward 1..2
            for (int r = 1; r <= 6; r++) {      // Room 1..6
                for (int b = 1; b <= 4; b++) {  // 4 beds/room → 24 per ward → 48 total
                    String id = "W" + w + "R" + r + "B" + b;
                    beds.put(id, new Bed(id));
                }
            }
        }
        System.out.println("Beds initialised: " + beds.size());
    }

    private void seedDemoData() {
        // Manager (bypass auth/roster), plus a doctor and a nurse
        Staff mgr = new Doctor("MGR01", "Manager", "admin");
        Staff doc = new Doctor("D01", "Dr Smith", "pw");
        Staff nur = new Nurse("N01", "Nurse Lee", "pw");
        addStaff(mgr);
        addStaff(doc);
        addStaff(nur);

        // Give doctor & nurse an active shift covering 'now'
        LocalDateTime now = LocalDateTime.now();
        try {
            addShift("D01", new Shift(now.minusHours(2), now.plusHours(2)));
            addShift("N01", new Shift(now.minusHours(2), now.plusHours(2)));
        } catch (ShiftException ignore) { /* never happens for empty schedules */ }

        // Two residents; pre-assign one to a bed so "Move" works immediately
        addResident(new Resident("R01", "Alice", Resident.Gender.F));
        addResident(new Resident("R02", "Bob", Resident.Gender.M));
        Bed b = beds.get("W1R1B1");
        if (b != null && b.isVacant()) b.assign(residents.get("R01"));

        System.out.println("Demo data seeded: staff=" + staff.keySet()
                + ", residents=" + residents.keySet()
                + ", R01 in W1R1B1");
    }

    // ---- STAFF ----
    public void addStaff(Staff s){
        staff.put(s.getId(), s);
        logger.log(s.getId(), "ADD_STAFF", s.toString());
    }

    public void modifyStaffPassword(String staffId, String newPassword){
        Staff s = mustGetStaff(staffId);
        s.setPassword(newPassword);
        logger.log(staffId, "MODIFY_STAFF_PASSWORD", "updated password");
    }

    public void addShift(String staffId, Shift shift) throws ShiftException {
        Staff s = mustGetStaff(staffId);
        s.addShift(shift, WEEKLY_HOUR_LIMIT);
        logger.log(staffId, "ADD_SHIFT", shift.toString());
    }

    // ---- RESIDENTS ----
    public void addResident(Resident r) {
        residents.put(r.getId(), r);
    }

    public List<Resident> getAllResidents() {
        return new ArrayList<>(residents.values());
    }

    public Resident getResident(String id) { return residents.get(id); }
    public int getResidentCount() { return residents.size(); }

    // ---- BEDS ----
    public void addBed(Bed b){ beds.put(b.getId(), b); }

    public void assignResidentToVacantBed(String staffId, String residentId, String bedId)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "ASSIGN_RESIDENT");
        requireRostered(staffId, LocalDateTime.now());
        Bed bed = mustGetBed(bedId);
        Resident r = mustGetResident(residentId);
        if(!bed.isVacant()) throw new IllegalStateException("Bed not vacant");
        bed.assign(r);
        logger.log(staffId, "ASSIGN_RESIDENT", "resident=" + residentId + " -> bed=" + bedId);
    }

    public void moveResident(String staffId, String fromBedId, String toBedId)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "MOVE_RESIDENT");
        requireRostered(staffId, LocalDateTime.now());
        Bed from = mustGetBed(fromBedId);
        Bed to = mustGetBed(toBedId);
        if(from.isVacant()) throw new IllegalStateException("From bed is vacant");
        if(!to.isVacant()) throw new IllegalStateException("To bed is occupied");
        Resident r = from.getResident();
        from.vacant();
        to.assign(r);
        logger.log(staffId, "MOVE_RESIDENT",
                "resident="+r.getId()+" "+fromBedId+" -> "+toBedId);
    }

    public Resident checkResidentInBed(String staffId, String bedId)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "CHECK_BED");
        requireRostered(staffId, LocalDateTime.now());
        Bed b = mustGetBed(bedId);
        logger.log(staffId, "CHECK_BED", b.toString());
        return b.getResident();
    }

    // ---- PRESCRIPTIONS ----
    public Prescription createPrescription(String staffId, String residentId, List<Prescription.Line> lines)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "CREATE_PRESCRIPTION");
        requireRostered(staffId, LocalDateTime.now());
        Staff author = mustGetStaff(staffId);
        if(!(author instanceof Doctor)){
            throw new AuthorizationException("Only doctors can create prescriptions");
        }
        Resident r = mustGetResident(residentId);
        Prescription p = new Prescription("RX-"+(prescriptions.size()+1), r.getId(), author.getId());
        for(Prescription.Line l: lines) p.addLine(l);
        prescriptions.put(p.getId(), p);
        logger.log(staffId, "CREATE_PRESCRIPTION", p.toString());
        return p;
    }

    public void updatePrescription(String staffId, String rxId, List<Prescription.Line> newLines)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "UPDATE_PRESCRIPTION");
        requireRostered(staffId, LocalDateTime.now());
        Prescription p = mustGetPrescription(rxId);
        // for simplicity just append new lines (keeps history)
        for(Prescription.Line l: newLines){
            p.addLine(l);
        }
        logger.log(staffId, "UPDATE_PRESCRIPTION", p.toString());
    }

    public void administer(String staffId, String residentId, String medicine, double dose, String unit)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "ADMINISTER");
        requireRostered(staffId, LocalDateTime.now());
        Resident r = mustGetResident(residentId);
        MedicationLog log = new MedicationLog(r.getId(), staffId, medicine, dose, unit, LocalDateTime.now());
        medLogs.add(log);
        logger.log(staffId, "ADMINISTER", log.toString());
    }

    /** Save residents, beds, prescriptions and medication logs to a simple text file. */
    public void saveToFile(String path) {
    try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(Path.of(path)))) {
        // ------------------------
        // RESIDENTS
        // ------------------------
        out.println("[RESIDENTS]");
        for (Resident r : residents.values()) {
            out.println(r.getId()+","+escape(r.getFullName())+","+r.getGender());
        }

        // ------------------------
        // BEDS
        // ------------------------
        out.println("[BEDS]");
        for (Bed b : beds.values()) {
            String rid = b.getResident()==null ? "" : b.getResident().getId();
            out.println(b.getId()+","+rid);
        }

        // ------------------------
        // PRESCRIPTIONS
        // ------------------------
        out.println("[PRESCRIPTIONS]");
        for (Prescription p : prescriptions.values()) {
            for (Prescription.Line l : p.getLines()) {
                out.println(p.getId()+","+p.getResidentId()+","+p.getDoctorId()+","
                        +escape(l.medicine)+","+l.dose+","+l.unit+","+escape(l.schedule));
            }
        }

        // ------------------------
        // MEDLOGS
        // ------------------------
        out.println("[MEDLOGS]");
        for (MedicationLog m : medLogs) {
            out.println(m.residentId+","+m.staffId+","+escape(m.medicine)+","
                    +m.dose+","+m.unit+","+m.time);
        }

        // ------------------------
        // STAFF
        // ------------------------
        out.println("[STAFF]");
        for (Staff s : staff.values()) {
            String role = s.getClass().getSimpleName(); // Nurse / Doctor / Manager
            out.println(s.getId() + "," + escape(s.getFullName()) + "," + role);
        }

        // ------------------------
        // ROSTERS
        // ------------------------
        out.println("[ROSTERS]");
        for (Staff s : staff.values()) {
            // Depending on your implementation, this could be s.getShifts() or s.getRosters()
            for (Shift shift : s.getShifts()) {
                out.println(s.getId() + "," + shift.getStart() + "," + shift.getEnd());
            }
        }

    } catch (IOException e) {
        System.err.println("Failed saving data: " + e.getMessage());
    }
}


    public void loadFromFile(String path) {
        Path p = Path.of(path);
        if (!Files.exists(p)) return;

        try {
            List<String> lines = Files.readAllLines(p);
            String section = "";
            Map<String, String> bedAssign = new HashMap<>();

            for (String line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("[")) {
                    section = line.trim();
                    continue;
                }

                switch (section) {
                    // ------------------------
                    // RESIDENTS SECTION
                    // ------------------------
                    case "[RESIDENTS]": {
                        String[] parts = splitCsv(line, 3);
                        Resident.Gender g = Resident.Gender.valueOf(parts[2].trim());
                        residents.put(parts[0].trim(),
                            new Resident(parts[0].trim(), unescape(parts[1].trim()), g));
                        break;
                    }

                    // ------------------------
                    // BEDS SECTION
                    // ------------------------
                    case "[BEDS]": {
                        String[] parts = splitCsv(line, 2);
                        beds.putIfAbsent(parts[0].trim(), new Bed(parts[0].trim()));
                        if (parts[1] != null && !parts[1].isEmpty()) {
                            bedAssign.put(parts[0].trim(), parts[1].trim());
                        }
                        break;
                    }

                    // ------------------------
                    // STAFF SECTION
                    // ------------------------
                    case "[STAFF]": {
                        String[] parts = splitCsv(line, 3);
                        String role = parts[2].trim();
                        String id = parts[0].trim();
                        String name = unescape(parts[1].trim());
                        String defaultPassword = "1234"; // default for testing

                        if (role.equalsIgnoreCase("Nurse")) {
                            staff.put(id, new Nurse(id, name, defaultPassword));
                        } else if (role.equalsIgnoreCase("Doctor")) {
                            staff.put(id, new Doctor(id, name, defaultPassword));
                        } else if (role.equalsIgnoreCase("Manager")) {
                            staff.put(id, new Manager(id, name, defaultPassword));
                        }
                        break;
                    }

                    // ------------------------
                    // ROSTERS SECTION
                    // ------------------------
                    case "[ROSTERS]": {
                        String[] parts = splitCsv(line, 3);
                        Staff s = staff.get(parts[0].trim());
                        if (s != null) {
                            LocalDateTime start = LocalDateTime.parse(parts[1].trim());
                            LocalDateTime end = LocalDateTime.parse(parts[2].trim());
                            try {
                                // Use Shift so GUI can detect roster correctly
                                s.addShift(new Shift(start, end), 40);
                            } catch (Exception ex) {
                                System.err.println("Failed to add shift for " + s.getId() + ": " + ex.getMessage());
                            }
                        }
                        break;
                    }

                    // ------------------------
                    // Ignore other sections
                    // ------------------------
                    default:
                        break;
                }
            }

            // ------------------------
            // Re-link bed assignments
            // ------------------------
            for (Map.Entry<String, String> e : bedAssign.entrySet()) {
                Resident r = residents.get(e.getValue());
                if (r != null && beds.containsKey(e.getKey())) {
                    beds.get(e.getKey()).assign(r);
                }
            }

            // ------------------------
            // FALLBACK ROSTER FOR TESTING
            // ------------------------
            try {
                LocalDateTime now = LocalDateTime.now();

                // Ensure each nurse has at least one shift for demo purposes
                for (Staff s : staff.values()) {
                    if (s instanceof Nurse && s.getShifts().isEmpty()) {
                        s.addShift(new Shift(now.minusHours(2), now.plusHours(2)), 40);
                        System.out.println("[INFO] Auto-added fallback shift for nurse " + s.getId());
                    }
                }

                // Ensure doctor and manager also have coverage
                for (Staff s : staff.values()) {
                    if ((s instanceof Doctor || s instanceof Manager) && s.getShifts().isEmpty()) {
                        s.addShift(new Shift(now.minusHours(4), now.plusHours(4)), 40);
                        System.out.println("[INFO] Auto-added fallback shift for " + s.getId());
                    }
                }

            } catch (Exception ex) {
                System.err.println("Failed adding fallback shift: " + ex.getMessage());
            }

        } catch (Exception e) {
            System.err.println("Failed loading data: " + e.getMessage());
            e.printStackTrace();
        }
    }

        /* --- helpers --- */
        private static String escape(String s){ return s.replace("\\","\\\\").replace(",","\\,"); }
        private static String unescape(String s){
            StringBuilder sb = new StringBuilder(); boolean esc = false;
            for(char c: s.toCharArray()){
                if (esc) { sb.append(c); esc=false; }
                else if (c=='\\') esc=true; else sb.append(c);
            }
            return sb.toString();
        }
        private static String[] splitCsv(String line, int expected){
            List<String> parts = new ArrayList<>(); StringBuilder cur = new StringBuilder(); boolean esc=false;
            for(char c: line.toCharArray()){
                if (esc) { cur.append(c); esc=false; }
                else if (c=='\\') esc=true;
                else if (c==',') { parts.add(cur.toString()); cur.setLength(0); }
                else cur.append(c);
            }
            parts.add(cur.toString());
            while (parts.size()<expected) parts.add("");
            return parts.toArray(new String[0]);
        }

    // ---- LOOKUPS / GUARDS ----
    private Staff mustGetStaff(String id){
        Staff s = staff.get(id);
        if(s==null) throw new IllegalArgumentException("No such staff: "+id);
        return s;
    }
    private Bed mustGetBed(String id){
        Bed b = beds.get(id);
        if(b==null) throw new IllegalArgumentException("No such bed: "+id);
        return b;
    }
    private Resident mustGetResident(String id){
        Resident r = residents.get(id);
        if(r==null) throw new IllegalArgumentException("No such resident: "+id);
        return r;
    }
    private Prescription mustGetPrescription(String id){
        Prescription p = prescriptions.get(id);
        if(p==null) throw new IllegalArgumentException("No such prescription: "+id);
        return p;
    }

    private void requireAuthorized(String staffId, String action) throws AuthorizationException {
        if (staffId.startsWith("MGR")) return; // manager bypass
        Staff s = mustGetStaff(staffId);
        if (!s.can(action)) {
            throw new AuthorizationException("Staff " + staffId + " not authorized for " + action);
        }
    }

    private void requireRostered(String staffId, LocalDateTime at) throws NotRosteredException {
        if (staffId.startsWith("MGR")) return; // manager bypass
        Staff s = mustGetStaff(staffId);
        if (!s.isOnDuty(at)) {
            throw new NotRosteredException("Staff " + staffId + " not rostered at " + at);
        }
    }

    public void saveToDatabase() {
        try (Connection conn = DatabaseManager.connect()) {

            // 1) Clear existing data (so we don’t duplicate)
            try (Statement clear = conn.createStatement()) {
                clear.executeUpdate("DELETE FROM staff");
                clear.executeUpdate("DELETE FROM residents");
                clear.executeUpdate("DELETE FROM beds");
                clear.executeUpdate("DELETE FROM prescriptions");
                clear.executeUpdate("DELETE FROM logs");
            }

            // 2) Save Staff
            String staffSql = "INSERT INTO staff (id, name, role) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(staffSql)) {
                for (Staff s : staff.values()) {
                    ps.setString(1, s.getId());
                    ps.setString(2, s.getFullName());
                    ps.setString(3, s.getClass().getSimpleName());
                    ps.executeUpdate();
                }
            }

            // 3) Save Residents
            String residentSql = "INSERT INTO residents (id, name, gender) VALUES (?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(residentSql)) {
                for (Resident r : residents.values()) {
                    ps.setString(1, r.getId());
                    ps.setString(2, r.getFullName());
                    ps.setString(3, r.getGender().toString());
                    ps.executeUpdate();
                }
            }

            // 4) Save Beds
            String bedSql = "INSERT INTO beds (id, resident_id) VALUES (?,?)";
            try (PreparedStatement ps = conn.prepareStatement(bedSql)) {
                for (Bed b : beds.values()) {
                    ps.setString(1, b.getId());
                    ps.setString(2, (b.getResident() != null) ? b.getResident().getId() : null);
                    ps.executeUpdate();
                }
            }

            // 5) Save Prescriptions (optional if you have details)
            String presSql = "INSERT INTO prescriptions (id, resident_id, doctor_id, details) VALUES (?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(presSql)) {
                for (Prescription p : prescriptions.values()) {
                    ps.setString(1, p.getId());
                    ps.setString(2, p.getResidentId());
                    ps.setString(3, p.getDoctorId());
                    ps.setString(4, p.toLineString()); // assuming you have this
                    ps.executeUpdate();
                }
            }

            // 6) Save Logs
            String logSql = "INSERT INTO logs (entry) VALUES (?)";
            try (PreparedStatement ps = conn.prepareStatement(logSql)) {
                for (String log : logger.getEntries()) {
                    ps.setString(1, log);
                    ps.executeUpdate();
                }
            }

            System.out.println("Data successfully saved to SQLite.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void loadFromDatabase() {
        try (Connection conn = DatabaseManager.connect()) {

            // Clear current in-memory collections
            staff.clear();
            residents.clear();
            beds.clear();
            prescriptions.clear();
            medLogs.clear();
            logger.clear();

            // 1) Load Staff
            try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM staff")) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    String role = rs.getString("role");
                    Staff s;
                    if ("Doctor".equals(role)) s = new Doctor(id, name, "");
                    else if ("Nurse".equals(role)) s = new Nurse(id, name, "");
                    if ("Doctor".equals(role)) {
                        s = new Doctor(id, name, "");
                    } else if ("Nurse".equals(role)) {
                        s = new Nurse(id, name, "");
                    } else {
                        // fallback if unknown
                        s = new Nurse(id, name, "");
                    }
                    addStaff(s);
                }
            }

            // 2) Load Residents
            try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM residents")) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String name = rs.getString("name");
                    Resident.Gender gender = Resident.Gender.valueOf(rs.getString("gender"));
                    addResident(new Resident(id, name, gender));
                }
            }

            // 3) Load Beds
            try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM beds")) {
                while (rs.next()) {
                    String id = rs.getString("id");
                    String residentId = rs.getString("resident_id");
                    Bed bed = new Bed(id);
                    if (residentId != null && residents.containsKey(residentId)) {
                        bed.assign(residents.get(residentId));
                    }
                    addBed(bed);
                }
            }

            // 4) Load Logs
            try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT * FROM logs")) {
                while (rs.next()) {
                    String entry = rs.getString("entry");
                    logger.addEntry(entry);
                }
            }

            System.out.println("Data successfully loaded from SQLite.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    // ---- READONLY VIEWS ----
    public List<String> getLogs(){ return logger.getEntries(); }
    public Map<String, Bed> getBeds(){ return Collections.unmodifiableMap(beds); }
    public Map<String, Staff> getStaff(){ return Collections.unmodifiableMap(staff); }
    public Map<String, Resident> getResidents(){ return Collections.unmodifiableMap(residents); }
    public List<MedicationLog> getMedicationLogs(){ return Collections.unmodifiableList(medLogs); }
    public Map<String, Prescription> getPrescriptions(){ return Collections.unmodifiableMap(prescriptions); }
}
