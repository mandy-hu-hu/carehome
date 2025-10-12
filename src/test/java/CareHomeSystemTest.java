
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.time.*;
import java.util.*;

public class CareHomeSystemTest {

    CareHomeSystem sys;
    Doctor doc;
    Nurse nurse;

    @BeforeEach
    void setup() throws Exception {
        sys = new CareHomeSystem();
        doc = new Doctor("D01","Dr. Test","pw");
        nurse = new Nurse("N01","Nurse Test","pw");
        sys.addStaff(doc);
        sys.addStaff(nurse);
        sys.addBed(new Bed("B1"));
        sys.addBed(new Bed("B2"));
        sys.addResident(new Resident("R1","Alice"));

        LocalDateTime now = LocalDateTime.now();
        sys.addShift("D01", new Shift(now.minusHours(2), now.plusHours(2)));
        sys.addShift("N01", new Shift(now.minusHours(2), now.plusHours(2)));
    }

    @Test
    void assignResidentToVacantBed() throws Exception {
        sys.assignResidentToVacantBed("N01","R1","B1");
        assertNotNull(sys.getBeds().get("B1").getResident());
    }

    @Test
    void movingResidentRequiresNurseAuthButNurseIsAuthorized() throws Exception {
        sys.assignResidentToVacantBed("N01","R1","B1");
        sys.moveResident("N01","B1","B2");
        assertNull(sys.getBeds().get("B1").getResident());
        assertEquals("R1", sys.getBeds().get("B2").getResident().getId());
    }

    @Test
    void doctorCreatesPrescription() throws Exception {
        sys.assignResidentToVacantBed("N01","R1","B1");
        List<Prescription.Line> lines = List.of(new Prescription.Line("Paracetamol",500,"mg","08:00,20:00"));
        Prescription p = sys.createPrescription("D01","R1",lines);
        assertEquals("R1", p.getResidentId());
        assertEquals("D01", p.getDoctorId());
        assertEquals(1, p.getLines().size());
    }

    @Test
    void nurseCannotCreatePrescription() throws Exception {
        sys.assignResidentToVacantBed("N01","R1","B1");
        List<Prescription.Line> lines = List.of(new Prescription.Line("Paracetamol",500,"mg","08:00,20:00"));
        assertThrows(AuthorizationException.class, () -> sys.createPrescription("N01","R1",lines));
    }

    @Test
    void mustBeRostered() throws Exception {
        // remove roster by replacing shift in the past
        // (simpler: create a fresh nurse with no shifts)
        Nurse n2 = new Nurse("N02","Nurse Off","pw");
        sys.addStaff(n2);
        assertThrows(NotRosteredException.class, () -> sys.assignResidentToVacantBed("N02","R1","B1"));
    }

    @Test
    void addShiftOverWeeklyLimitThrows() throws Exception {
        Nurse n = nurse;
        // Add enough hours to reach 40, then adding more should fail
        LocalDateTime monday = LocalDate.now().with(java.time.DayOfWeek.MONDAY).atTime(8,0);
        // clear existing by creating a new nurse for deterministic
        Nurse n3 = new Nurse("N03","Nurse Cap","pw");
        sys.addStaff(n3);
        // Schedule 5x8h = 40h
        for(int i=0;i<5;i++){
            sys.addShift("N03", new Shift(monday.plusDays(i), monday.plusDays(i).plusHours(8)));
        }
        Shift extra = new Shift(monday.plusDays(5), monday.plusDays(5).plusHours(4));
        assertThrows(ShiftException.class, () -> sys.addShift("N03", extra));
    }

    @Test
    void administerLogsEntry() throws Exception {
        sys.assignResidentToVacantBed("N01","R1","B1");
        sys.administer("N01","R1","Ibuprofen",200,"mg");
        boolean found = sys.getLogs().stream().anyMatch(s -> s.contains("ADMINISTER") && s.contains("Ibuprofen"));
        assertTrue(found);
    }
}
