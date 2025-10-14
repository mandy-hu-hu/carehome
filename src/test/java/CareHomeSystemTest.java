import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CareHomeSystemTest {

    CareHomeSystem sys;
    Doctor doc;
    Nurse nurse;

    @BeforeEach
    void setup() throws Exception {
        sys = new CareHomeSystem();
        doc = new Doctor("D01", "Dr. Test", "pw");
        nurse = new Nurse("N01", "Nurse Test", "pw");
        sys.addStaff(doc);
        sys.addStaff(nurse);
        sys.addBed(new Bed("B1"));
        sys.addBed(new Bed("B2"));
        sys.addResident(new Resident("R1", "Alice", Resident.Gender.F));

        LocalDateTime now = LocalDateTime.now();
        sys.addShift("D01", new Shift(now.minusHours(2), now.plusHours(2)));
        sys.addShift("N01", new Shift(now.minusHours(2), now.plusHours(2)));
    }

    @Test
    void testAddResident() {
        Resident r = new Resident("R2", "Bob", Resident.Gender.M);
        sys.addResident(r);
        assertEquals(2, sys.getResidentCount());
    }

    @Test
    void testAssignResidentToBed() throws Exception {
        sys.assignResidentToVacantBed("D01", "R1", "B1");
        Bed b = sys.getBeds().get("B1");
        assertEquals("R1", b.getResident().getId());
    }

    @Test
    void testDoctorCreatesPrescription() throws Exception {
        List<Prescription.Line> lines = List.of(
            new Prescription.Line("Paracetamol", 500, "mg", "08:00,20:00")
        );
        Prescription p = sys.createPrescription("D01", "R1", lines);
        assertNotNull(p);
        assertEquals("R1", p.getResidentId());
    }

    @Test
    void testNurseCannotCreatePrescription() {
        List<Prescription.Line> lines = List.of(
            new Prescription.Line("Paracetamol", 500, "mg", "08:00,20:00")
        );
        assertThrows(AuthorizationException.class,
            () -> sys.createPrescription("N01", "R1", lines));
    }

    @Test
    void testNurseNotRostered() {
        Nurse n2 = new Nurse("N02", "Nurse Off", "pw");
        sys.addStaff(n2);
        sys.addBed(new Bed("B3"));
        sys.addResident(new Resident("R2", "Bob", Resident.Gender.M));
        assertThrows(NotRosteredException.class,
            () -> sys.assignResidentToVacantBed("N02", "R2", "B3"));
    }

    @Test
    void testWeeklyHourLimitExceeded() throws Exception {
        Nurse n3 = new Nurse("N03", "Nurse Cap", "pw");
        sys.addStaff(n3);

        LocalDateTime monday = LocalDateTime.now().with(java.time.DayOfWeek.MONDAY).withHour(9);
        // Add 5 full-day shifts (8h)
        for (int i = 0; i < 5; i++) {
            sys.addShift("N03", new Shift(monday.plusDays(i), monday.plusDays(i).plusHours(8)));
        }

        // Exceed limit (40h)
        Shift extra = new Shift(monday.plusDays(5), monday.plusDays(5).plusHours(4));
        assertThrows(ShiftException.class, () -> sys.addShift("N03", extra));
    }

    @Test
    void testLoggerRecordsAction() throws Exception {
        sys.assignResidentToVacantBed("D01", "R1", "B1");
        List<String> logs = sys.getLogs();
        boolean found = logs.stream().anyMatch(l -> l.contains("ASSIGN_RESIDENT"));
        assertTrue(found);
    }
}
