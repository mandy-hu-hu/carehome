/**
  * A Bed in the care home, which can be assigned to a Resident.
*/

public class Bed {
    private final String id;
    private Resident resident; // null when vacant

    public Bed(String id){
        this.id = id;
    }

    public String getId(){ return id; }
    public Resident getResident(){ return resident; }
    public boolean isVacant(){ return resident == null; }

    public void assign(Resident r){
        if(!isVacant()) throw new IllegalStateException("Bed "+id+" is occupied");
        this.resident = r;
    }

    public void vacate(){
        this.resident = null;
    }

    @Override public String toString(){
        return "Bed{id="+id+", resident="+(resident==null?"<vacant>":resident.getId())+"}";
    }
}
