import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.shape.Rectangle;
import java.util.*;
import java.time.LocalDateTime;

public class CareHomeApp extends Application {
    private final CareHomeSystem system = new CareHomeSystem();
    private static final String DATA_FILE = "carehome-data.txt";

    private String currentStaffId = null;
    private boolean isManager = false;
    private Label activeUserLabel; // shows who is logged in

    @Override
    public void start(Stage stage) {
        // Restore previous data if any
        system.loadFromFile(DATA_FILE);

        Label title = new Label("🏥 Care Home Management System");
        title.setFont(Font.font(18));

        activeUserLabel = new Label("Not logged in");

        // Buttons
        Button btnLoginStaff   = new Button("Login as Staff");
        Button btnLoginManager = new Button("Login as Manager");
        Button addResident     = new Button("Add Resident");
        Button viewResidents   = new Button("View Residents");
        Button assignResident  = new Button("Assign Resident to Bed");
        Button manageStaff     = new Button("Manage Staff");
        Button moveResident    = new Button("Move Resident (Nurse)");
        Button createRx        = new Button("Create Prescription (Doctor)");
        Button updateRx        = new Button("Update Prescription (Doctor)");
        Button administer      = new Button("Administer Medicine (Nurse)");
        Button checkBed        = new Button("Check Bed Details");
        Button bedOverview     = new Button("Bed Overview (M=Blue / F=Red)");
        Button exit            = new Button("Exit");

        // Actions
        btnLoginStaff.setOnAction(e   -> loginAsStaff());
        btnLoginManager.setOnAction(e -> loginAsManager());
        addResident.setOnAction(e     -> showAddResidentWindow());
        viewResidents.setOnAction(e   -> showResidentList());
        assignResident.setOnAction(e  -> showAssignResidentWindow());
        manageStaff.setOnAction(e     -> showManageStaffWindow());
        moveResident.setOnAction(e    -> showMoveResidentWindow());
        createRx.setOnAction(e        -> showCreatePrescriptionWindow());
        updateRx.setOnAction(e        -> showUpdatePrescriptionWindow());
        administer.setOnAction(e      -> showAdministerWindow());
        checkBed.setOnAction(e        -> showCheckBedWindow());
        bedOverview.setOnAction(e     -> showBedOverviewWindow());
        exit.setOnAction(e -> {
            system.saveToFile(DATA_FILE);
            stage.close();
        });

        // Root (make sure we declare it before use)
        VBox root = new VBox(
                15, title, activeUserLabel,
                btnLoginStaff, btnLoginManager,
                addResident, viewResidents, assignResident,
                manageStaff, moveResident, createRx,
                updateRx, administer, checkBed, bedOverview, exit
        );
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20));

        stage.setScene(new Scene(root, 520, 640));
        stage.setTitle("CareHome GUI");
        stage.show();
    }

    @Override
    public void stop() {
        system.saveToFile(DATA_FILE);
    }

    // ========================== LOGIN =================================

    private void loginAsManager() {
        TextInputDialog dialog = new TextInputDialog("MGR01");
        dialog.setTitle("Manager Login");
        dialog.setHeaderText("Enter Manager ID to log in");
        dialog.setContentText("Manager ID:");

        dialog.showAndWait().ifPresent(id -> {
            this.currentStaffId = id.trim();
            this.isManager = true;
            showInfo("Login Successful", "Manager logged in as: " + currentStaffId);
            updateActiveUserLabel();
        });
    }

    private void loginAsStaff() {
        Dialog<Map<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Staff Login");
        dialog.setHeaderText("Enter Staff Credentials");

        Label idLabel  = new Label("Staff ID:");
        Label pwdLabel = new Label("Password:");
        TextField idField   = new TextField();
        PasswordField pwdField = new PasswordField();

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(idLabel, 0, 0);  grid.add(idField, 1, 0);
        grid.add(pwdLabel, 0, 1); grid.add(pwdField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        ButtonType loginButton = new ButtonType("Login", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(loginButton, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == loginButton) {
                Map<String, String> map = new HashMap<>();
                map.put("id", idField.getText());
                map.put("pwd", pwdField.getText());
                return map;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(res -> {
            String id  = res.get("id").trim();
            String pwd = res.get("pwd").trim();
            Staff s = system.getStaff().get(id);
            if (s != null && s.checkPassword(pwd)) {
                this.currentStaffId = id;
                this.isManager = false;
                showInfo("Login Successful", "Welcome, " + s.getFullName());
                updateActiveUserLabel();
            } else {
                showError("Login Failed", "Invalid staff ID or password.");
            }
        });
    }

    // ========================== WINDOWS ================================

    private void showAddResidentWindow() {
        Stage w = new Stage();
        w.setTitle("Add New Resident");

        TextField idField   = new TextField();   idField.setPromptText("Enter Resident ID");
        TextField nameField = new TextField();   nameField.setPromptText("Enter Full Name");
        ComboBox<Resident.Gender> genderBox = new ComboBox<>();
        genderBox.getItems().addAll(Resident.Gender.M, Resident.Gender.F);
        genderBox.getSelectionModel().select(Resident.Gender.M);
        Button save = new Button("Save");

        save.setOnAction(e -> {
            String id   = idField.getText().trim();
            String name = nameField.getText().trim();
            Resident.Gender g = genderBox.getValue();
            if (id.isEmpty() || name.isEmpty()) {
                alert(Alert.AlertType.WARNING, "Please fill in all fields.");
                return;
            }
            system.addResident(new Resident(id, name, g));
            alert(Alert.AlertType.INFORMATION, "Resident added successfully!");
            w.close();
        });

        VBox layout = new VBox(10,
                new Label("Resident ID:"), idField,
                new Label("Full Name:"), nameField,
                new Label("Gender:"), genderBox,
                save
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 320, 260));
        w.show();
    }

    private void showResidentList() {
        Stage w = new Stage();
        w.setTitle("All Residents");
        ListView<String> list = new ListView<>();
        for (Resident r : system.getAllResidents()) list.getItems().add(r.toString());
        VBox layout = new VBox(10, new Label("Resident List:"), list);
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 360, 320));
        w.show();
    }

    private void showAssignResidentWindow() {
        Stage w = new Stage();
        w.setTitle("Assign Resident to Vacant Bed");

        TextField staffId    = new TextField(); staffId.setPromptText("Manager ID");
        TextField residentId = new TextField(); residentId.setPromptText("Resident ID");
        TextField bedId      = new TextField(); bedId.setPromptText("Vacant Bed ID");
        Button assign = new Button("Assign");

        assign.setOnAction(e -> {
            try {
                system.assignResidentToVacantBed(
                        staffId.getText().trim(),
                        residentId.getText().trim(),
                        bedId.getText().trim()
                );
                alert(Alert.AlertType.INFORMATION, "Resident assigned successfully!");
                w.close();
            } catch (Exception ex) {
                alert(Alert.AlertType.ERROR, ex.getMessage());
            }
        });

        VBox layout = new VBox(10,
                new Label("Manager ID:"), staffId,
                new Label("Resident ID:"), residentId,
                new Label("Vacant Bed ID:"), bedId,
                assign
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 360, 260));
        w.show();
    }

    private void showManageStaffWindow() {
        Stage w = new Stage();
        w.setTitle("Manage Staff");

        TextField id = new TextField();        id.setPromptText("Staff ID");
        TextField name = new TextField();      name.setPromptText("Full Name");
        PasswordField password = new PasswordField(); password.setPromptText("Password");
        ComboBox<String> roleBox = new ComboBox<>();
        roleBox.getItems().addAll("Doctor", "Nurse");
        roleBox.getSelectionModel().select(0);

        TextField shiftStart = new TextField(); shiftStart.setPromptText("Shift start (e.g. 2025-10-13T09:00)");
        TextField shiftEnd   = new TextField(); shiftEnd.setPromptText("Shift end   (e.g. 2025-10-13T17:00)");
        Button addStaffBtn = new Button("Add Staff");
        Button modifyPassBtn = new Button("Modify Password");
        Button addShiftBtn = new Button("Add Shift");

        addStaffBtn.setOnAction(e -> {
            try {
                Staff s = roleBox.getValue().equals("Doctor")
                        ? new Doctor(id.getText().trim(), name.getText().trim(), password.getText())
                        : new Nurse(id.getText().trim(), name.getText().trim(), password.getText());
                system.addStaff(s);
                alert(Alert.AlertType.INFORMATION, "Staff added successfully!");
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        modifyPassBtn.setOnAction(e -> {
            try {
                system.modifyStaffPassword(id.getText().trim(), password.getText());
                alert(Alert.AlertType.INFORMATION, "Password updated!");
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        addShiftBtn.setOnAction(e -> {
            try {
                Shift sh = new Shift(
                        LocalDateTime.parse(shiftStart.getText().trim()),
                        LocalDateTime.parse(shiftEnd.getText().trim())
                );
                system.addShift(id.getText().trim(), sh);
                alert(Alert.AlertType.INFORMATION, "Shift added!");
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        VBox layout = new VBox(10,
                new Label("Staff ID:"), id,
                new Label("Full Name:"), name,
                new Label("Password:"), password,
                new Label("Role:"), roleBox,
                addStaffBtn, modifyPassBtn,
                new Separator(),
                new Label("Add Shift:"), shiftStart, shiftEnd, addShiftBtn
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 420, 420));
        w.show();
    }

    private void showMoveResidentWindow() {
        Stage w = new Stage();
        w.setTitle("Move Resident Between Beds");

        TextField staffId = new TextField(); staffId.setPromptText("Nurse ID");
        TextField fromBed = new TextField(); fromBed.setPromptText("From Bed ID");
        TextField toBed   = new TextField(); toBed.setPromptText("To Bed ID");
        Button move = new Button("Move");

        move.setOnAction(e -> {
            try {
                system.moveResident(staffId.getText().trim(), fromBed.getText().trim(), toBed.getText().trim());
                alert(Alert.AlertType.INFORMATION, "Resident moved!");
                w.close();
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        VBox layout = new VBox(10,
                new Label("Nurse ID:"), staffId,
                new Label("From Bed:"), fromBed,
                new Label("To Bed:"), toBed,
                move
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 360, 250));
        w.show();
    }

    private void showCreatePrescriptionWindow() {
        Stage w = new Stage();
        w.setTitle("Create Prescription");

        TextField doctorId = new TextField(); doctorId.setPromptText("Doctor ID");
        TextField residentId = new TextField(); residentId.setPromptText("Resident ID");

        TextArea lines = new TextArea();
        lines.setPromptText("One line per medicine:  medicine,dose,unit,schedule\nExample: Paracetamol,500,mg,08:00/20:00");

        Button create = new Button("Create");

        create.setOnAction(e -> {
            try {
                List<Prescription.Line> parsed = parseLines(lines.getText());
                system.createPrescription(doctorId.getText().trim(), residentId.getText().trim(), parsed);
                alert(Alert.AlertType.INFORMATION, "Prescription created!");
                w.close();
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        VBox layout = new VBox(10,
                new Label("Doctor ID:"), doctorId,
                new Label("Resident ID:"), residentId,
                new Label("Medicines:"), lines,
                create
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 480, 360));
        w.show();
    }

    private void showUpdatePrescriptionWindow() {
        Stage w = new Stage();
        w.setTitle("Update Prescription");

        TextField doctorId = new TextField(); doctorId.setPromptText("Doctor ID");
        TextField rxId     = new TextField(); rxId.setPromptText("Prescription ID");

        TextArea lines = new TextArea();
        lines.setPromptText("One line per medicine:  medicine,dose,unit,schedule");

        Button update = new Button("Append lines");

        update.setOnAction(e -> {
            try {
                List<Prescription.Line> parsed = parseLines(lines.getText());
                system.updatePrescription(doctorId.getText().trim(), rxId.getText().trim(), parsed);
                alert(Alert.AlertType.INFORMATION, "Prescription updated!");
                w.close();
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        VBox layout = new VBox(10,
                new Label("Doctor ID:"), doctorId,
                new Label("Prescription ID:"), rxId,
                new Label("New lines:"), lines,
                update
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 480, 340));
        w.show();
    }

    private void showAdministerWindow() {
        Stage w = new Stage();
        w.setTitle("Administer Medicine");

        TextField nurseId    = new TextField(); nurseId.setPromptText("Nurse ID");
        TextField residentId = new TextField(); residentId.setPromptText("Resident ID");
        TextField med  = new TextField(); med.setPromptText("Medicine");
        TextField dose = new TextField(); dose.setPromptText("Dose (number)");
        TextField unit = new TextField(); unit.setPromptText("Unit (mg, ml, etc.)");
        Button record = new Button("Record administration");

        record.setOnAction(e -> {
            try {
                system.administer(
                        nurseId.getText().trim(),
                        residentId.getText().trim(),
                        med.getText().trim(),
                        Double.parseDouble(dose.getText().trim()),
                        unit.getText().trim()
                );
                alert(Alert.AlertType.INFORMATION, "Administration recorded!");
                w.close();
            } catch (Exception ex) { alert(Alert.AlertType.ERROR, ex.getMessage()); }
        });

        VBox layout = new VBox(10,
                new Label("Nurse ID:"), nurseId,
                new Label("Resident ID:"), residentId,
                new Label("Medicine:"), med,
                new Label("Dose:"), dose,
                new Label("Unit:"), unit,
                record
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 420, 360));
        w.show();
    }

    private void showCheckBedWindow() {
        Stage w = new Stage();
        w.setTitle("Check Bed Details");

        TextField staffId = new TextField(); staffId.setPromptText("Medical Staff ID");
        TextField bedId   = new TextField(); bedId.setPromptText("Bed ID");
        Button check = new Button("Check");
        Label result = new Label("");

        check.setOnAction(e -> {
            try {
                Resident r = system.checkResidentInBed(staffId.getText().trim(), bedId.getText().trim());
                if (r == null) {
                    result.setText("Bed is vacant");
                    result.setTextFill(Color.BLACK);
                } else {
                    result.setText("Resident: " + r.getFullName() + " (" + r.getGender() + ")");
                    result.setTextFill(r.getGender() == Resident.Gender.M ? Color.BLUE : Color.RED);
                }
            } catch (Exception ex) {
                result.setText("Error: " + ex.getMessage());
                result.setTextFill(Color.BLACK);
            }
        });

        VBox layout = new VBox(10,
                new Label("Staff ID:"), staffId,
                new Label("Bed ID:"), bedId,
                check, result
        );
        layout.setPadding(new Insets(16));
        w.setScene(new Scene(layout, 420, 240));
        w.show();
    }

    private void showBedOverviewWindow() {
        Stage w = new Stage();
        w.setTitle("Ward Overview (Blue=M, Red=F, White=Vacant)");

        Map<String, Bed> allBeds = system.getBeds();
        if (allBeds.isEmpty()) {
            alert(Alert.AlertType.WARNING, "No beds found in the system!");
            return;
        }

        HBox wardsBox = new HBox(60);
        wardsBox.setPadding(new Insets(20));
        wardsBox.setAlignment(Pos.CENTER);

        // Group beds by ward and room
        Map<String, List<Bed>> ward1 = new LinkedHashMap<>();
        Map<String, List<Bed>> ward2 = new LinkedHashMap<>();

        for (Bed bed : allBeds.values()) {
            String id = bed.getId(); // e.g. W1R2B3
            String wardKey = id.substring(0, 2); // W1 or W2
            String roomKey = id.substring(0, 4); // W1R1 ... W2R3
            if (wardKey.equals("W1"))
                ward1.computeIfAbsent(roomKey, k -> new ArrayList<>()).add(bed);
            else
                ward2.computeIfAbsent(roomKey, k -> new ArrayList<>()).add(bed);
        }

        // Build UI for both wards
        wardsBox.getChildren().addAll(
                createWardView("Ward 1", ward1),
                createWardView("Ward 2", ward2)
        );

        ScrollPane scroll = new ScrollPane(wardsBox);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        w.setScene(new Scene(scroll, 1000, 650));
        w.show();
    }

    private VBox createWardView(String wardName, Map<String, List<Bed>> wardRooms) {
        VBox wardBox = new VBox(15);
        wardBox.setAlignment(Pos.TOP_CENTER);
        wardBox.setStyle("-fx-border-color: gray; -fx-border-width: 2; -fx-padding: 10;");

        Label wardLabel = new Label(wardName);
        wardLabel.setFont(Font.font("Arial", 20));

        GridPane roomGrid = new GridPane();
        roomGrid.setHgap(20);
        roomGrid.setVgap(20);

        int row = 0, col = 0;
        for (List<Bed> roomBeds : wardRooms.values()) {
            GridPane roomBox = new GridPane();
            roomBox.setHgap(6);
            roomBox.setVgap(6);
            roomBox.setStyle("-fx-border-color: #000000; -fx-padding: 8;");

            int rr = 0, cc = 0;
            for (Bed bed : roomBeds) {
                Rectangle rect = new Rectangle(30, 30);
                rect.setStroke(Color.BLACK);

                if (bed.isVacant()) rect.setFill(Color.WHITE);
                else if (bed.getResident().getGender() == Resident.Gender.M)
                    rect.setFill(Color.DODGERBLUE);
                else rect.setFill(Color.RED);

                Tooltip tip = new Tooltip(bed.isVacant()
                        ? bed.getId() + " (Vacant)"
                        : bed.getId() + " → " + bed.getResident().getFullName()
                        + " (" + bed.getResident().getGender() + ")");
                Tooltip.install(rect, tip);

                roomBox.add(rect, cc++, rr);
                if (cc >= 2) { cc = 0; rr++; } // arrange up to 2 columns
            }

            roomGrid.add(roomBox, col++, row);
            if (col == 2) { col = 0; row++; }
        }

        wardBox.getChildren().addAll(wardLabel, roomGrid);
        return wardBox;
    }

    // ========================== HELPERS ================================

    private static List<Prescription.Line> parseLines(String text){
        List<Prescription.Line> list = new ArrayList<>();
        if (text == null || text.isBlank()) return list;
        String[] rows = text.split("\\r?\\n");
        for (String r : rows) {
            if (r.isBlank()) continue;
            String[] p = r.split("\\s*,\\s*");
            if (p.length < 4)
                throw new IllegalArgumentException(
                        "Line must be: medicine,dose,unit,schedule -> " + r);
            list.add(new Prescription.Line(p[0],
                    Double.parseDouble(p[1]), p[2], p[3]));
        }
        return list;
    }

    private void updateActiveUserLabel() {
        if (activeUserLabel == null) return;
        if (currentStaffId == null) {
            activeUserLabel.setText("Not logged in");
        } else if (isManager) {
            activeUserLabel.setText("Logged in as Manager (" + currentStaffId + ")");
        } else {
            activeUserLabel.setText("Logged in as Staff: " + currentStaffId);
        }
    }

    private static void alert(Alert.AlertType t, String msg){
        new Alert(t, msg).showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
