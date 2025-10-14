import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final int WEEKLY_HOUR_LIMIT = 40;

    // ---- CONSTRUCTOR ----
    public CareHomeSystem() {
        // Initialize 48 beds across 2 wards × 6 rooms × variable beds per room
        for (int w = 1; w <= 2; w++) {              // W1, W2
            for (int r = 1; r <= 6; r++) {          // 6 rooms per ward
                int bedsInRoom;
                if (r == 1) bedsInRoom = 1;
                else if (r == 2) bedsInRoom = 2;
                else bedsInRoom = 4;                // rooms 3–6 each have 4 beds

                for (int b = 1; b <= bedsInRoom; b++) {
                    String id = "W" + w + "R" + r + "B" + b;  // e.g., W1R3B2
                    beds.put(id, new Bed(id));
                }
            }
        }
        System.out.println("Initialized " + beds.size() + " beds in CareHomeSystem");
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

    /** Add a new resident */
    public void addResident(Resident r) {
        residents.put(r.getId(), r);
    }

    /** Get all residents as a list */
    public List<Resident> getAllResidents() {
        return new ArrayList<>(residents.values());
    }

    /** Optional helper — find by ID */
    public Resident getResident(String id) {
        return residents.get(id);
    }

    /** Optional helper — total count */
    public int getResidentCount() {
        return residents.size();
    }

    public void replaceShift(String staffId, Shift oldShift, Shift newShift) throws ShiftException {
        Staff s = mustGetStaff(staffId);
        s.replaceShift(oldShift, newShift, WEEKLY_HOUR_LIMIT);
        logger.log(staffId, "REPLACE_SHIFT", oldShift+" -> "+newShift);
    }

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
        logger.log(staffId, "MOVE_RESIDENT", "resident="+r.getId()+" "+fromBedId+" -> "+toBedId);
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
        p.getLines(); // read check
        // Replace by creating a new list internally (simple approach)
        // For demo, we recreate a new Prescription object would lose timestamps; better: reflectively update lines
        // Here, we simulate by appending a "revision" line
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
            out.println("[RESIDENTS]");
            for (Resident r : residents.values()) {
                out.println(r.getId()+","+escape(r.getFullName())+","+r.getGender());
            }

            out.println("[BEDS]");
            for (Bed b : beds.values()) {
                String rid = b.getResident()==null ? "" : b.getResident().getId();
                out.println(b.getId()+","+rid);
            }

            out.println("[PRESCRIPTIONS]");
            for (Prescription p : prescriptions.values()) {
                for (Prescription.Line l : p.getLines()) {
                    out.println(p.getId()+","+p.getResidentId()+","+p.getDoctorId()+","
                            +escape(l.medicine)+","+l.dose+","+l.unit+","+escape(l.schedule));
                }
            }

            out.println("[MEDLOGS]");
            for (MedicationLog m : medLogs) {
                out.println(m.residentId+","+m.staffId+","+escape(m.medicine)+","
                        +m.dose+","+m.unit+","+m.time);
            }
        } catch (IOException e) {
            System.err.println("Failed saving data: "+e.getMessage());
        }
    }

    /** Basic restore for residents and bed assignments (simple/demo). */
    public void loadFromFile(String path) {
        Path p = Path.of(path);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p);
            String section = "";
            Map<String,String> bedAssign = new HashMap<>();
            for (String line : lines) {
                if (line.isBlank()) continue;
                if (line.startsWith("[")) { section = line; continue; }
                switch (section) {
                    case "[RESIDENTS]": {
                        String[] parts = splitCsv(line, 3);
                        Resident.Gender g = Resident.Gender.valueOf(parts[2]);
                        residents.put(parts[0], new Resident(parts[0], unescape(parts[1]), g));
                        break;
                    }
                    case "[BEDS]": {
                        String[] parts = splitCsv(line, 2);
                        beds.putIfAbsent(parts[0], new Bed(parts[0]));
                        if (parts[1] != null && !parts[1].isEmpty()) bedAssign.put(parts[0], parts[1]);
                        break;
                    }
                    default: /* ignore other sections in this simple loader for now */ break;
                }
            }
            // re-link bed assignments
            for (Map.Entry<String,String> e : bedAssign.entrySet()) {
                Resident r = residents.get(e.getValue());
                if (r != null) beds.get(e.getKey()).assign(r);
            }
        } catch (Exception e) {
            System.err.println("Failed loading data: "+e.getMessage());
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


    // ---- HELPERS ----
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

    public List<String> getLogs(){ return logger.getEntries(); }
    public Map<String, Bed> getBeds(){ return Collections.unmodifiableMap(beds); }
    public Map<String, Staff> getStaff(){ return Collections.unmodifiableMap(staff); }
    public Map<String, Resident> getResidents(){ return Collections.unmodifiableMap(residents); }
    public List<MedicationLog> getMedicationLogs(){ return Collections.unmodifiableList(medLogs); }
}
