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

    public void replaceShift(String staffId, Shift oldShift, Shift newShift) throws ShiftException {
        Staff s = mustGetStaff(staffId);
        s.replaceShift(oldShift, newShift, WEEKLY_HOUR_LIMIT);
        logger.log(staffId, "REPLACE_SHIFT", oldShift+" -> "+newShift);
    }

    // ---- BEDS & RESIDENTS ----
    public void addBed(Bed b){ beds.put(b.getId(), b); }
    public void addResident(Resident r){ residents.put(r.getId(), r); }

    public void assignResidentToVacantBed(String staffId, String residentId, String bedId)
            throws AuthorizationException, NotRosteredException {
        requireAuthorized(staffId, "ASSIGN_RESIDENT");
        requireRostered(staffId, LocalDateTime.now());
        Bed bed = mustGetBed(bedId);
        Resident r = mustGetResident(residentId);
        if(!bed.isVacant()) throw new IllegalStateException("Bed not vacant");
        bed.assign(r);
        logger.log(staffId, "ASSIGN_RESIDENT", "resident="+residentId+" -> bed="+bedId);
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
        from.vacate();
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
        Staff s = mustGetStaff(staffId);
        if(!s.can(action)){
            throw new AuthorizationException("Staff "+staffId+" not authorized for "+action);
        }
    }
    private void requireRostered(String staffId, LocalDateTime at) throws NotRosteredException {
        Staff s = mustGetStaff(staffId);
        if(!s.isOnDuty(at)){
            throw new NotRosteredException("Staff "+staffId+" not rostered at "+at);
        }
    }

    public List<String> getLogs(){ return logger.getEntries(); }
    public Map<String, Bed> getBeds(){ return Collections.unmodifiableMap(beds); }
    public Map<String, Staff> getStaff(){ return Collections.unmodifiableMap(staff); }
    public Map<String, Resident> getResidents(){ return Collections.unmodifiableMap(residents); }
    public List<MedicationLog> getMedicationLogs(){ return Collections.unmodifiableList(medLogs); }
}
