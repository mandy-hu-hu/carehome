
import java.time.*;

public class MedicationLog {
    public final String residentId;
    public final String staffId;
    public final String medicine;
    public final double dose;
    public final String unit;
    public final LocalDateTime time;

    public MedicationLog(String residentId, String staffId, String medicine, double dose, String unit, LocalDateTime time){
        this.residentId = residentId;
        this.staffId = staffId;
        this.medicine = medicine;
        this.dose = dose;
        this.unit = unit;
        this.time = time;
    }

    @Override public String toString(){
        return "MedicationLog{resident="+residentId+", staff="+staffId+", "+medicine+" "+dose+unit+" @ "+time+"}";
    }
}
