/**
 * Represents a resident (patient) in the care home.
 * A Resident can be assigned to a Bed and may have prescriptions attached.
 */

import java.util.Objects;

public class Resident {
    public enum Gender { M , F }

    private final String id;
    private String fullName;
    private Gender gender;

    /** Create a new Resident with an ID and name. */
    public Resident(String id, String fullName, Gender gender){
        this.id = Objects.requireNonNull(id);
        this.fullName = Objects.requireNonNull(fullName);
        this.gender = Objects.requireNonNull(gender);
    }

    /** Backward-compat (if you created residents earlier without gender): default to M. */
    public Resident(String id, String fullName){
        this(id, fullName, Gender.M);
    }

    public String getId(){ return id; }
    public String getFullName(){ return fullName; }
    public void setFullName(String name){ this.fullName = Objects.requireNonNull(name); }
    public Gender getGender(){ return gender; }
    public void setGender(Gender g){ this.gender = Objects.requireNonNull(g); }

    @Override public String toString(){
        return "Resident{id="+id+", name="+fullName+", gender="+gender+"}";
    }
}
