
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Represents a work shift for staff members.
 * A Shift has a start and end time, and provides utility methods.
 */

public class Shift {
    private final LocalDateTime start;
    private final LocalDateTime end;

    public Shift(LocalDateTime start, LocalDateTime end){
        if(end.isBefore(start) || end.equals(start)){
            throw new IllegalArgumentException("Shift end must be after start");
        }
        this.start = start.truncatedTo(ChronoUnit.MINUTES);
        this.end = end.truncatedTo(ChronoUnit.MINUTES);
    }

    public LocalDateTime getStart(){ return start; }
    public LocalDateTime getEnd(){ return end; }

    public boolean contains(LocalDateTime at){
        return ( !at.isBefore(start) && at.isBefore(end) );
    }

    public boolean overlaps(Shift other){
        return this.start.isBefore(other.end) && other.start.isBefore(this.end);
    }

    public long getDurationHours(){
        return Duration.between(start, end).toHours();
    }

    public long overlappingHours(LocalDateTime a, LocalDateTime b){
        LocalDateTime maxStart = start.isAfter(a) ? start : a;
        LocalDateTime minEnd = end.isBefore(b) ? end : b;
        if(minEnd.isAfter(maxStart)){
            return Duration.between(maxStart, minEnd).toHours();
        }
        return 0;
    }

    @Override public String toString(){
        return "Shift["+start+" to "+end+"]";
    }

    @Override public boolean equals(Object o){
        if(!(o instanceof Shift)) return false;
        Shift s=(Shift)o;
        return start.equals(s.start) && end.equals(s.end);
    }
    @Override public int hashCode(){ return start.hashCode()*31 + end.hashCode(); }
}
