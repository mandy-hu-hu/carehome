import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a staff member in the care home (Doctor, Nurse, Manager).
 * Staff have shifts and can perform actions based on their role.
 */
public abstract class Staff {
    private final String id;
    private String fullName;
    private String passwordHash;
    private final List<Shift> shifts = new ArrayList<>();

    public Staff(String id, String fullName, String password) {
        this.id = Objects.requireNonNull(id);
        this.fullName = Objects.requireNonNull(fullName);
        setPassword(password);
    }

    public String getId() { return id; }

    public String getFullName() { return fullName; }    // ✅ aligned with CareHomeApp expectations
    public void setFullName(String name){ this.fullName = Objects.requireNonNull(name); }

    // ✅ returns hashed password (simple, for demo)
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password){
        // trivial hash for demo; DO NOT use in production
        this.passwordHash = Integer.toHexString(Objects.requireNonNull(password).hashCode());
    }

    public boolean checkPassword(String password){
        return passwordHash.equals(Integer.toHexString(Objects.requireNonNull(password).hashCode()));
    }

    public List<Shift> getShifts(){ return Collections.unmodifiableList(shifts); }

    public void addShift(Shift shift, int weeklyHourLimit) throws ShiftException {
        // Validate overlap
        for(Shift s: shifts){
            if(s.overlaps(shift)){
                throw new ShiftException("Shift overlaps with existing shift: " + s);
            }
        }
        // Validate weekly hour cap
        long weekHours = getWeekHours(shift.getStart());
        long total = weekHours + shift.getDurationHours();
        if(total > weeklyHourLimit){
            throw new ShiftException("Weekly hour limit exceeded: " + total + " > " + weeklyHourLimit);
        }
        shifts.add(shift);
    }

    public void replaceShift(Shift oldShift, Shift newShift, int weeklyHourLimit) throws ShiftException {
        if(!shifts.remove(oldShift)){
            throw new ShiftException("Old shift not found for staff " + id);
        }
        try{
            addShift(newShift, weeklyHourLimit);
        } catch(ShiftException ex){
            // rollback
            shifts.add(oldShift);
            throw ex;
        }
    }

    public boolean isOnDuty(LocalDateTime at){
        for(Shift s: shifts){
            if(s.contains(at)) return true;
        }
        return false;
    }

    private long getWeekHours(LocalDateTime ref){
        LocalDate weekStart = ref.toLocalDate().with(java.time.DayOfWeek.MONDAY);
        LocalDate weekEnd = weekStart.plusDays(7);
        long hours = 0;
        for(Shift s: shifts){
            hours += s.overlappingHours(weekStart.atStartOfDay(), weekEnd.atStartOfDay());
        }
        return hours;
    }

    public abstract boolean can(String action);

    @Override public String toString(){
        return getClass().getSimpleName()+"{id="+id+", name="+fullName+"}";
    }
}

// ------------------------------
// Subclasses for roles
// ------------------------------

class Doctor extends Staff {
    public Doctor(String id, String name, String password){ super(id,name,password); }
    @Override public boolean can(String action){
        return true; // doctors can do all actions
    }
}

class Nurse extends Staff {
    public Nurse(String id, String name, String password){ super(id,name,password); }
    @Override public boolean can(String action){
        return !action.equals("CREATE_PRESCRIPTION");
    }
}

class Manager extends Staff {   // ✅ Added manager for login system
    public Manager(String id, String name, String password){ super(id,name,password); }
    @Override public boolean can(String action){
        return true; // manager can perform all actions
    }
}
