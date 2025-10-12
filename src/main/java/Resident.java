/**
 * Represents a resident (patient) in the care home.
 * A Resident can be assigned to a Bed and may have prescriptions attached.
 */

import java.util.Objects;

public class Resident {
    private final String id;
    private String fullName;

    /**
    * Create a new Resident with an ID and name.
    */
    public Resident(String id, String fullName){
        this.id = Objects.requireNonNull(id);
        this.fullName = Objects.requireNonNull(fullName);
    }

    /** @return the resident's unique ID */
    public String getId(){ return id; }

    /** @return the resident's full name */
    public String getFullName(){ return fullName; }
    /** Update the resident's full name */
    public void setFullName(String name){ this.fullName = Objects.requireNonNull(name); }

    @Override public String toString(){ return "Resident{id="+id+", name="+fullName+"}"; }
}
