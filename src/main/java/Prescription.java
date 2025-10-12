
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a medical prescription for a resident.
 * A Prescription is created by a Doctor and contains multiple medication lines.
 */

public class Prescription {
    public static class Line {
        public final String medicine;
        public final double dose;
        public final String unit;
        public final String schedule; // e.g., "08:00, 14:00, 20:00"
        public Line(String medicine, double dose, String unit, String schedule){
            this.medicine = medicine;
            this.dose = dose;
            this.unit = unit;
            this.schedule = schedule;
        }
        @Override public String toString(){
            return medicine+" "+dose+unit+" @ "+schedule;
        }
    }

    private final String id;
    private final String residentId;
    private final String doctorId;
    private final LocalDateTime createdAt;
    private final List<Line> lines = new ArrayList<>();

    public Prescription(String id, String residentId, String doctorId){
        this.id = Objects.requireNonNull(id);
        this.residentId = Objects.requireNonNull(residentId);
        this.doctorId = Objects.requireNonNull(doctorId);
        this.createdAt = LocalDateTime.now();
    }

    public String getId(){ return id; }
    public String getResidentId(){ return residentId; }
    public String getDoctorId(){ return doctorId; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
    public List<Line> getLines(){ return Collections.unmodifiableList(lines); }
    public void addLine(Line l){ lines.add(l); }

    @Override public String toString(){
        return "Prescription{id="+id+", resident="+residentId+", doctor="+doctorId+", lines="+lines+"}";
    }
}
