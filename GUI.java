import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

//  ARIA CRM
//
//  GUI Engineer : Yu Wu (yw4529)

//  1. CUSTOM EXCEPTIONS  

class DuplicateEmailException extends Exception {
    public DuplicateEmailException(String message) { super(message); }
}

class CustomerNotFoundException extends Exception {
    public CustomerNotFoundException(String message) { super(message); }
}

class InvalidInputException extends Exception {
    public InvalidInputException(String message) { super(message); }
}

//  2. CUSTOMER MODEL

class Customer {
    private int        id;
    private String     name;
    private String     phone;
    private String     email;
    private String     address;
    private String     notes;
    private LocalDate  dateAdded;

    public Customer(int id, String name, String phone,
                    String email, String address, String notes) {
        this.id        = id;
        this.name      = name;
        this.phone     = phone;
        this.email     = email;
        this.address   = address;
        this.notes     = notes;
        this.dateAdded = LocalDate.now();
    }

    public int       getId()        { return id; }
    public String    getName()      { return name; }
    public String    getPhone()     { return phone; }
    public String    getEmail()     { return email; }
    public String    getAddress()   { return address; }
    public String    getNotes()     { return notes; }
    public LocalDate getDateAdded() { return dateAdded; }

    public void setName(String v)    { this.name    = v; }
    public void setPhone(String v)   { this.phone   = v; }
    public void setEmail(String v)   { this.email   = v; }
    public void setAddress(String v) { this.address = v; }
    public void setNotes(String v)   { this.notes   = v; }
}

//  3. CUSTOMER REPOSITORY

class CustomerRepository {

    // ArrayList-based in-memory store — no database required
    private final List<Customer> customers = new ArrayList<>();
    private int nextId = 1;

    /** Add a new customer. Throws if email already exists. */
    public Customer add(String name, String phone, String email,
                        String address, String notes)
            throws DuplicateEmailException, InvalidInputException {

        // Input validation
        if (name == null || name.isBlank())
            throw new InvalidInputException("Name cannot be empty.");
        if (email == null || email.isBlank())
            throw new InvalidInputException("Email cannot be empty.");
        if (!email.contains("@"))
            throw new InvalidInputException("Email format is invalid: " + email);

        // Duplicate email check
        boolean taken = customers.stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(email));
        if (taken)
            throw new DuplicateEmailException("Email already exists: " + email);

        Customer c = new Customer(nextId++, name, phone, email, address, notes);
        customers.add(c);
        return c;
    }

    /** Update an existing customer by ID. */
    public void update(int id, String name, String phone, String email,
                       String address, String notes)
            throws CustomerNotFoundException, DuplicateEmailException,
                   InvalidInputException {

        Customer target = customers.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "No customer found with ID: " + id));

        if (name == null || name.isBlank())
            throw new InvalidInputException("Name cannot be empty.");
        if (email == null || email.isBlank())
            throw new InvalidInputException("Email cannot be empty.");
        if (!email.contains("@"))
            throw new InvalidInputException("Email format is invalid: " + email);

        boolean taken = customers.stream()
                .anyMatch(c -> c.getId() != id
                        && c.getEmail().equalsIgnoreCase(email));
        if (taken)
            throw new DuplicateEmailException(
                    "Email already used by another customer: " + email);

        target.setName(name);
        target.setPhone(phone);
        target.setEmail(email);
        target.setAddress(address);
        target.setNotes(notes);
    }

    /** Delete a customer by ID. */
    public void delete(int id) throws CustomerNotFoundException {
        Customer target = customers.stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "No customer found with ID: " + id));
        customers.remove(target);
    }

    /** Search by name, email, or phone using Stream.filter() */
    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAll();
        String kw = keyword.toLowerCase();
        return customers.stream()
                .filter(c -> c.getName().toLowerCase().contains(kw)
                        || c.getEmail().toLowerCase().contains(kw)
                        || c.getPhone().contains(kw))
                .collect(Collectors.toList());
    }

    public List<Customer> getAll() { return new ArrayList<>(customers); }

    /** Pre-load sample records so the table is never empty on launch */
    public void loadSampleData() {
        try {
            add("Alice Johnson", "212-555-0101", "alice@example.com",
                    "10 Main St, NY",   "VIP client");
            add("Bob Martinez",  "646-555-0202", "bob@example.com",
                    "25 Oak Ave, NY",   "Referred by Alice");
            add("Carol Lee",     "917-555-0303", "carol@example.com",
                    "5 Park Blvd, NY",  "Annual contract");
            add("David Kim",     "718-555-0404", "david@example.com",
                    "88 River Rd, NJ",  "Pending follow-up");
            add("Eva Chen",      "212-555-0505", "eva@example.com",
                    "200 5th Ave, NY",  "New lead Q1 2026");
        } catch (DuplicateEmailException | InvalidInputException e) {
            System.err.println("Sample data error: " + e.getMessage());
        }
    }
}

//  4. MAIN APPLICATION 

public class GUI extends Application {

    // ── Repository ────────────────────────────────────────────
    private final CustomerRepository repo = new CustomerRepository();

    // ── ObservableList bound to TableView (auto-refreshes UI) ─
    private final ObservableList<Customer> tableData =
            FXCollections.observableArrayList();

    // ── Form input fields ─────────────────────────────────────
    private final TextField tfName    = new TextField();
    private final TextField tfPhone   = new TextField();
    private final TextField tfEmail   = new TextField();
    private final TextField tfAddress = new TextField();
    private final TextArea  taNotes   = new TextArea();
    private final TextField tfSearch  = new TextField();

    // ── TableView ─────────────────────────────────────────────
    private final TableView<Customer> table = new TableView<>();

    // ── Status bar ────────────────────────────────────────────
    private final Label lblStatus = new Label("Ready.");

    // ── Tracks edit mode (null = add mode) ───────────────────
    private Customer editingCustomer = null;

    // ── Buttons ───────────────────────────────────────────────
    private final Button btnSave   = new Button("Add Customer");
    private final Button btnDelete = new Button("Delete");
    private final Button btnClear  = new Button("Clear");

    // ──────────────────────────────────────────────────────────
    @Override
    public void start(Stage primaryStage) {
        repo.loadSampleData();

        // BorderPane 
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #F1F5F9;");
        root.setTop(buildHeader());
        root.setLeft(buildForm());
        root.setCenter(buildTablePanel());
        root.setBottom(buildStatusBar());
        BorderPane.setMargin(root.getLeft(),   new Insets(10, 0,  10, 10));
        BorderPane.setMargin(root.getCenter(), new Insets(10, 10, 10, 10));

        refreshTable(repo.getAll());

        Scene scene = new Scene(root, 1000, 660);

        primaryStage.setTitle("ARIA CRM — Customer Records");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(860);
        primaryStage.setMinHeight(560);
        primaryStage.show();
    }

    //  LAYOUT

    /** NORTH — header bar */
    private HBox buildHeader() {
        Text title = new Text("ARIA CRM");
        title.setFont(Font.font("Calibri", FontWeight.BOLD, 26));
        title.setFill(Color.WHITE);

        Text sub = new Text("Customer Records Manager");
        sub.setFont(Font.font("Calibri", 14));
        sub.setFill(Color.web("#CBD5E1"));

        VBox box = new VBox(2, title, sub);
        box.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(box);
        header.setPadding(new Insets(16, 24, 16, 24));
        header.setStyle("-fx-background-color: #1E3A5F;");
        return header;
    }

    /** LEFT — input form using GridPane */
    private VBox buildForm() {
        Label formTitle = new Label("Customer Details");
        formTitle.setFont(Font.font("Calibri", FontWeight.BOLD, 15));
        formTitle.setStyle("-fx-text-fill: #1E3A5F;");

        tfName.setPromptText("Full name *");
        tfPhone.setPromptText("e.g. 212-555-0100");
        tfEmail.setPromptText("email@example.com *");
        tfAddress.setPromptText("Street, City, State");
        taNotes.setPromptText("Any notes...");
        taNotes.setPrefRowCount(3);
        taNotes.setWrapText(true);

        // GridPane 
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(10);
        grid.setPadding(new Insets(10, 0, 10, 0));

        addRow(grid, 0, "Name *",   tfName);
        addRow(grid, 1, "Phone",    tfPhone);
        addRow(grid, 2, "Email *",  tfEmail);
        addRow(grid, 3, "Address",  tfAddress);
        grid.add(fieldLabel("Notes"), 0, 4);
        grid.add(taNotes,             0, 5, 2, 1);
        GridPane.setHgrow(taNotes, Priority.ALWAYS);

        // Style buttons
        style(btnSave,   "#1E3A5F", "white");
        style(btnDelete, "#C0392B", "white");
        style(btnClear,  "#6B7280", "white");
        btnSave.setMaxWidth(Double.MAX_VALUE);
        btnDelete.setMaxWidth(Double.MAX_VALUE);
        btnClear.setMaxWidth(Double.MAX_VALUE);

        // Lambda event handlers 
        btnSave.setOnAction(e   -> handleSave());
        btnDelete.setOnAction(e -> handleDelete());
        btnClear.setOnAction(e  -> clearForm());

        VBox form = new VBox(12, formTitle, grid,
                new VBox(8, btnSave, btnDelete, btnClear));
        form.setPadding(new Insets(16));
        form.setPrefWidth(260);
        form.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: #CBD5E1;" +
            "-fx-border-radius: 6;" +
            "-fx-background-radius: 6;");
        return form;
    }

    /** CENTER — search bar + TableView */
    private VBox buildTablePanel() {
        tfSearch.setPromptText("Search by name, email or phone...");
        tfSearch.setPrefWidth(320);

        // ChangeListener — live search
        tfSearch.textProperty().addListener(
                (obs, oldVal, newVal) -> handleSearch(newVal));

        Label lblCount = new Label();
        lblCount.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 12px;");
        tableData.addListener(
                (javafx.collections.ListChangeListener<Customer>) c ->
                        lblCount.setText(tableData.size() + " records"));

        HBox searchBar = new HBox(12, tfSearch, lblCount);
        searchBar.setAlignment(Pos.CENTER_LEFT);

        // TableView
        table.setItems(tableData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.getColumns().addAll(
                col("ID",         "id",        60),
                col("Name",       "name",      155),
                col("Phone",      "phone",     115),
                col("Email",      "email",     175),
                col("Address",    "address",   155),
                col("Date Added", "dateAdded", 105)
        );

        // Selecting a row populates the form (edit mode)
        table.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> {
                    if (newSel != null) populateForm(newSel);
                });

        table.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #CBD5E1;" +
            "-fx-border-radius: 6;");

        VBox panel = new VBox(12, searchBar, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return panel;
    }

    /** BOTTOM — status bar */
    private HBox buildStatusBar() {
        lblStatus.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;");
        HBox bar = new HBox(lblStatus);
        bar.setPadding(new Insets(6, 16, 6, 16));
        bar.setStyle("-fx-background-color: #E5E7EB;");
        return bar;
    }

    //  EVENT HANDLERS  

    /** Handles both ADD and EDIT via try-catch */
    private void handleSave() {
        String name    = tfName.getText().trim();
        String phone   = tfPhone.getText().trim();
        String email   = tfEmail.getText().trim();
        String address = tfAddress.getText().trim();
        String notes   = taNotes.getText().trim();

        if (editingCustomer == null) {
            // ── ADD mode ──────────────────────────────────────
            try {
                Customer added = repo.add(name, phone, email, address, notes);
                refreshTable(repo.getAll());
                setStatus("Added: " + added.getName()
                          + "  (ID " + added.getId() + ")");
                clearForm();
            } catch (InvalidInputException ex) {
                showError("Validation Error", ex.getMessage());
            } catch (DuplicateEmailException ex) {
                showError("Duplicate Email", ex.getMessage());
            }
        } else {
            // ── EDIT mode ─────────────────────────────────────
            try {
                repo.update(editingCustomer.getId(),
                            name, phone, email, address, notes);
                refreshTable(repo.getAll());
                setStatus("Updated: " + name
                          + "  (ID " + editingCustomer.getId() + ")");
                clearForm();
            } catch (InvalidInputException ex) {
                showError("Validation Error", ex.getMessage());
            } catch (CustomerNotFoundException ex) {
                showError("Not Found", ex.getMessage());
            } catch (DuplicateEmailException ex) {
                showError("Duplicate Email", ex.getMessage());
            }
        }
    }

    private void handleDelete() {
        Customer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("No Selection",
                      "Please select a customer from the table to delete.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + selected.getName() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        try {
            repo.delete(selected.getId());
            refreshTable(repo.getAll());
            setStatus("Deleted: " + selected.getName());
            clearForm();
        } catch (CustomerNotFoundException ex) {
            showError("Delete Error", ex.getMessage());
        }
    }

    private void handleSearch(String keyword) {
        List<Customer> results = repo.search(keyword);
        refreshTable(results);
        setStatus(results.size() + " result(s) for \"" + keyword + "\"");
    }

    //  HELPERS

    private void populateForm(Customer c) {
        editingCustomer = c;
        tfName.setText(c.getName());
        tfPhone.setText(c.getPhone());
        tfEmail.setText(c.getEmail());
        tfAddress.setText(c.getAddress());
        taNotes.setText(c.getNotes());
        btnSave.setText("Save Changes");
        style(btnSave, "#1D6A3A", "white");
        setStatus("Editing: " + c.getName() + "  (ID " + c.getId() + ")");
    }

    private void clearForm() {
        editingCustomer = null;
        tfName.clear(); tfPhone.clear(); tfEmail.clear();
        tfAddress.clear(); taNotes.clear();
        table.getSelectionModel().clearSelection();
        btnSave.setText("Add Customer");
        style(btnSave, "#1E3A5F", "white");
        setStatus("Ready.");
    }

    private void refreshTable(List<Customer> list) { tableData.setAll(list); }
    private void setStatus(String msg)             { lblStatus.setText(msg); }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void addRow(GridPane g, int row, String label, Control field) {
        g.add(fieldLabel(label), 0, row);
        g.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: #374151; -fx-font-size: 12px;" +
                   "-fx-font-weight: bold;");
        return l;
    }

    private void style(Button btn, String bg, String fg) {
        btn.setStyle(
            "-fx-background-color: " + bg + ";" +
            "-fx-text-fill: " + fg + ";" +
            "-fx-background-radius: 4;" +
            "-fx-font-size: 13px;" +
            "-fx-cursor: hand;");
    }

    @SuppressWarnings("unchecked")
    private TableColumn<Customer, ?> col(String title,
                                          String property,
                                          int    minWidth) {
        TableColumn<Customer, Object> c = new TableColumn<>(title);
        c.setCellValueFactory(new PropertyValueFactory<>(property));
        c.setMinWidth(minWidth);
        return c;
    }

    public static void main(String[] args) { launch(args); }
}
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
public class StudentCRM extends JFrame {
    public static void main(String[] args) {
        StudentCRM studentCRM = new StudentCRM();
        JFrame frame = new JFrame("Student Records");
        JPanel panel = new JPanel();
        JTextField name = new JTextField();
        JTextField ID = new JTextField();
        JPasswordField pass = new JPasswordField();
        JTextField major = new JTextField();
        JTextField GPA = new JTextField();

        frame.add(panel);
        frame.add(name);
        frame.add(ID);
        frame.add(pass);
        frame.add(major);
        frame.add(GPA);
        frame.add(panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 400);

    }

}
