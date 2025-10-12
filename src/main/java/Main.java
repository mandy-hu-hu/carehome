import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

/**
 * A simple demo application to interact with the CareHomeSystem.
 * It seeds some initial data and provides a console menu for operations.
 */

public class Main {
    private static final Scanner sc = new Scanner(System.in);

    public static void main(String[] args) throws Exception {
        CareHomeSystem sys = new CareHomeSystem();

        // ---- Seed demo data ----
        Doctor d1 = new Doctor("D01", "Dr. Smith", "pass");
        Nurse n1 = new Nurse("N01", "Nurse Lee", "pass");
        sys.addStaff(d1);
        sys.addStaff(n1);

        LocalDateTime now = LocalDateTime.now();
        sys.addShift("D01", new Shift(now.minusHours(1), now.plusHours(3)));
        sys.addShift("N01", new Shift(now.minusHours(1), now.plusHours(3)));

        sys.addBed(new Bed("B1"));
        sys.addBed(new Bed("B2"));
        sys.addResident(new Resident("R01", "Alice"));
        sys.addResident(new Resident("R02", "Bob"));

        // ---- Demo loop ----
        while (true) {
            System.out.println("\n=== CareHome Demo Menu ===");
            System.out.println("Available Staff: D01=Dr. Smith, N01=Nurse Lee");
            System.out.println("Available Beds:  B1, B2");
            System.out.println("Available Residents: R01=Alice, R02=Bob");
            System.out.println("-----------------------------------");
            System.out.println("1) Assign resident to vacant bed");
            System.out.println("2) Move resident to another bed (nurse)");
            System.out.println("3) Check resident in bed");
            System.out.println("4) Create prescription (doctor)");
            System.out.println("5) Administer medicine");
            System.out.println("6) Show logs");
            System.out.println("7) Exit");
            System.out.print("Choose: ");
            int choice = Integer.parseInt(sc.nextLine().trim());

            try {
                switch (choice) {
                    case 1 -> {
                        System.out.print("Use Nurse ID (N01): ");
                        String sid = sc.nextLine().trim();
                        System.out.print("Resident ID (R01 or R02): ");
                        String rid = sc.nextLine().trim();
                        System.out.print("Bed ID (B1 or B2): ");
                        String bid = sc.nextLine().trim();
                        sys.assignResidentToVacantBed(sid, rid, bid);
                        System.out.println("Resident assigned.");
                    }
                    case 2 -> {
                        System.out.print("Use Nurse ID (N01): ");
                        String sid = sc.nextLine().trim();
                        System.out.print("From Bed (B1 or B2): ");
                        String fb = sc.nextLine().trim();
                        System.out.print("To Bed (B1 or B2): ");
                        String tb = sc.nextLine().trim();
                        sys.moveResident(sid, fb, tb);
                        System.out.println("Resident moved.");
                    }
                    case 3 -> {
                        System.out.print("Staff ID (D01 or N01): ");
                        String sid = sc.nextLine().trim();
                        System.out.print("Bed ID (B1 or B2): ");
                        String bid = sc.nextLine().trim();
                        Resident r = sys.checkResidentInBed(sid, bid);
                        System.out.println("Resident in bed: " + (r == null ? "<vacant>" : r));
                    }
                    case 4 -> {
                        System.out.print("Doctor ID (D01): ");
                        String sid = sc.nextLine().trim();
                        System.out.print("Resident ID (R01 or R02): ");
                        String rid = sc.nextLine().trim();
                        System.out.print("Medicine: "); String med = sc.nextLine();
                        System.out.print("Dose (number): "); double dose = Double.parseDouble(sc.nextLine());
                        System.out.print("Unit (mg/ml): "); String unit = sc.nextLine();
                        System.out.print("Times (csv HH:MM): "); String sched = sc.nextLine();
                        Prescription p = sys.createPrescription(sid, rid,
                            List.of(new Prescription.Line(med, dose, unit, sched)));
                        System.out.println("Prescription created: " + p);
                    }
                    case 5 -> {
                        System.out.print("Staff ID (D01 or N01): ");
                        String sid = sc.nextLine().trim();
                        System.out.print("Resident ID (R01 or R02): ");
                        String rid = sc.nextLine().trim();
                        System.out.print("Medicine: "); String med = sc.nextLine();
                        System.out.print("Dose: "); double dose = Double.parseDouble(sc.nextLine());
                        System.out.print("Unit: "); String unit = sc.nextLine();
                        sys.administer(sid, rid, med, dose, unit);
                        System.out.println("Medicine administered.");
                    }
                    case 6 -> {
                        System.out.println("--- Logs ---");
                        sys.getLogs().forEach(System.out::println);
                    }
                    case 7 -> {
                        System.out.println("Bye");
                        return;
                    }
                    default -> System.out.println("Invalid option");
                }
            } catch (Exception ex) {
                System.out.println("Error: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            }
        }
    }
}
