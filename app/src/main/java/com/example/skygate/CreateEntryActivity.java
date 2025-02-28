package com.example.skygate;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreateEntryActivity extends AppCompatActivity {
    private static final String TAG = "CreateEntryActivity";

    // Firebase instance
    private FirebaseFirestore db;

    // UI Elements
    private TableLayout productsTable;
    private TextView controlNumberView;
    private EditText dateEdit;
    private Spinner driverSpinner;
    private Spinner entryTypeSpinner;
    private Button addRowButton;
    private Button saveButton;
    private TextView rateHeader;
    private TextView valueHeader;
    private TextView totalAmountView;
    private EditText cashEditText;
    private TextView expectedTransferView;

    // Tracking variables
    private int rowCount = 0;
    private int currentControlNumber = 1;
    private Calendar selectedDate;
    private boolean isLoading = false;
    private boolean hasInitializedInventory = false; // Flag to track inventory initialization

    // Constants
    private static final String FIELD_CONTROL_NUMBER = "controlNumber"; // Match exact field name in Firestore
    private static final String COLLECTION_CONTROL_NUMBERS = "control_numbers";
    private static final String TAG_CONTROL_NUMBER = "ControlNumberManager";
    private static final String COLLECTION_ENTRIES = "entries";
    private static final String COLLECTION_INVENTORY = "inventory";
    private static final String SUPPLY_DRIVER_ID = "supply_001";
    private static final Map<String, String> STAFF_IDS = new HashMap<String, String>() {{
        put("usman", "Usman");
        put("isaac", "Isaac");
        put("timothy", "Timothy");
        put("edward", "Edward");
        put("enoch", "Enoch");
        put("shop_sales", "Shop Sales");
        put("director", "Director");
        put("oke", "Oke");
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_entry);

        Log.d(TAG, "Activity onCreate started");

        try {
            initializeFirebase();
            initializeViews();
            setupEntryTypeListener();
            setupEntryTypeSpinner();
            setupDriverSpinner();
            setupDatePicker();
            setupButtons();
            getLatestControlNumber();
            addNewRow();

            // Check and initialize inventory documents (one-time process)
            if (!hasInitializedInventory) {
                initializeInventory();
                hasInitializedInventory = true;
            }

            Log.d(TAG, "Activity initialization completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during activity initialization", e);
            showErrorDialog("Initialization Error", "Failed to initialize the application. Please try again.");
        }
    }

    private void initializeFirebase() {
        Log.d(TAG, "Initializing Firebase");
        db = FirebaseFirestore.getInstance();
    }

    private boolean initializeViews() {
        Log.d(TAG, "Initializing views");

        try {
            productsTable = findViewById(R.id.productsTable);
            controlNumberView = findViewById(R.id.controlNumber);
            dateEdit = findViewById(R.id.date);
            driverSpinner = findViewById(R.id.driverSpinner);
            entryTypeSpinner = findViewById(R.id.entryTypeSpinner);
            rateHeader = findViewById(R.id.rateHeader);
            valueHeader = findViewById(R.id.valueHeader);
            addRowButton = findViewById(R.id.addRowButton);
            saveButton = findViewById(R.id.saveButton);
            totalAmountView = findViewById(R.id.totalAmountView);
            cashEditText = findViewById(R.id.cashEditText);
            expectedTransferView = findViewById(R.id.expectedTransferView);

            selectedDate = Calendar.getInstance();
            updateDateDisplay();

            // Add listener for cash input
            cashEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    updateExpectedTransfer();
                }
            });

            Log.d(TAG, "Views initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing views", e);
            throw e;
        }

        InputValidator validator = new InputValidator(this);
        boolean isValid = true;

        if (!validator.validateDate(dateEdit)) {
            isValid = false;
        }

        for (int i = 1; i < productsTable.getChildCount(); i++) {
            TableRow row = (TableRow) productsTable.getChildAt(i);
            EditText outEdit = (EditText) row.getChildAt(2);
            EditText inEdit = (EditText) row.getChildAt(3);

            if (!validator.validateQuantity(outEdit, "OUT quantity") ||
                    !validator.validateQuantity(inEdit, "IN quantity")) {
                isValid = false;
            }

            // Add quantity watchers to new rows
            outEdit.addTextChangedListener(new InputFormatWatcher(outEdit, validator, "quantity"));
            inEdit.addTextChangedListener(new InputFormatWatcher(inEdit, validator, "quantity"));
        }

        return isValid;
    }

    private void setupDriverSpinner() {
        Log.d(TAG, "Setting up Driver spinner");

        try {
            // The spinner will show display names but we'll store IDs
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            driverSpinner.setAdapter(adapter);

            // Update spinner based on entry type
            updateDriverSpinnerContent();

            Log.d(TAG, "Driver spinner setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up driver spinner", e);
            showErrorDialog("Setup Error", "Failed to setup drivers. Please restart the application.");
        }
    }

    // Add this new method to update spinner content
    private void updateDriverSpinnerContent() {
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) driverSpinner.getAdapter();
        adapter.clear();

        String selectedEntryType = entryTypeSpinner.getSelectedItem().toString();

        if ("Stock Received".equals(selectedEntryType)) {
            adapter.add("Supply");
        } else {
            // Add all staff names for sales entries
            for (String displayName : STAFF_IDS.values()) {
                adapter.add(displayName);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void setupEntryTypeListener() {
        entryTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedType = parent.getItemAtPosition(position).toString();
                boolean isSales = "Sales".equals(selectedType);

                // Update driver spinner content
                updateDriverSpinnerContent();

                // Show/hide rate and value columns
                rateHeader.setVisibility(isSales ? View.VISIBLE : View.GONE);
                valueHeader.setVisibility(isSales ? View.VISIBLE : View.GONE);

                // Update existing rows
                for (int i = 1; i < productsTable.getChildCount(); i++) {
                    TableRow row = (TableRow) productsTable.getChildAt(i);
                    if (row.getChildCount() > 5) {  // If rate and value views exist
                        row.getChildAt(5).setVisibility(isSales ? View.VISIBLE : View.GONE);
                        row.getChildAt(6).setVisibility(isSales ? View.VISIBLE : View.GONE);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupEntryTypeSpinner() {
        Log.d(TAG, "Setting up entry type spinner");

        try {
            List<String> entryTypes = Arrays.asList("Sales", "Stock Received");
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, entryTypes);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            entryTypeSpinner.setAdapter(adapter);

            Log.d(TAG, "Entry type spinner setup completed");
        } catch (Exception e) {
            Log.e(TAG, "Error setting up entry type spinner", e);
            showErrorDialog("Setup Error", "Failed to setup entry types. Please restart the application.");
        }
    }

    private void setupDatePicker() {
        Log.d(TAG, "Setting up date picker");

        dateEdit.setOnClickListener(v -> {
            try {
                showDatePickerDialog();
            } catch (Exception e) {
                Log.e(TAG, "Error showing date picker", e);
                showErrorDialog("Date Selection Error", "Failed to show date picker. Please try again.");
            }
        });
    }

    private void showDatePickerDialog() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);

                    // Check if selected date is in the future
                    if (selected.after(Calendar.getInstance())) {
                        showError("Cannot select future dates");
                        return;
                    }

                    Log.d(TAG, String.format("Date selected: %d/%d/%d", dayOfMonth, month + 1, year));

                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                    updateDateDisplay();
                    checkAndUpdateControlNumber(year, month);
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set the maximum date to today
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

        datePickerDialog.show();
    }

    private void setupButtons() {
        Log.d(TAG, "Setting up buttons");

        addRowButton.setOnClickListener(v -> {
            if (!isLoading) {
                addNewRow();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (!isLoading) {
                validateAndShowPreview();
            }
        });
    }

    private void updateDateDisplay() {
        String date = String.format(Locale.US, "%02d/%02d/%d",
                selectedDate.get(Calendar.DAY_OF_MONTH),
                selectedDate.get(Calendar.MONTH) + 1,
                selectedDate.get(Calendar.YEAR));
        dateEdit.setText(date);

        Log.d(TAG, "Date display updated: " + date);
    }

    private void showError(String message) {
        Log.w(TAG, "Showing error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void getLatestControlNumber() {
        Log.d(TAG_CONTROL_NUMBER, "Fetching latest control number");
        setLoading(true);

        Timestamp currentTimestamp = Timestamp.now();
        Date currentDate = currentTimestamp.toDate();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        int currentYear = calendar.get(Calendar.YEAR);
        int currentMonth = calendar.get(Calendar.MONTH);

        db.collection(COLLECTION_ENTRIES)
                .whereEqualTo("year", currentYear)
                .whereEqualTo("month", currentMonth)
                .orderBy(FIELD_CONTROL_NUMBER, Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                // Get the current control number and increment by 1
                                currentControlNumber = document.getLong(FIELD_CONTROL_NUMBER).intValue() + 1;
                            } else {
                                // If no documents exist, start from 1
                                currentControlNumber = 1;
                            }
                            controlNumberView.setText(String.valueOf(currentControlNumber));
                            Log.d(TAG_CONTROL_NUMBER, "New control number set: " + currentControlNumber);
                        } else {
                            Log.e(TAG_CONTROL_NUMBER, "Error fetching control number", task.getException());
                            showErrorDialog("Database Error",
                                    "Failed to get control number. Please try again.");
                        }
                        setLoading(false);
                    }
                });
    }

    private void checkAndUpdateControlNumber(int year, int month) {
        Log.d(TAG_CONTROL_NUMBER, String.format("Checking control number for year: %d, month: %d",
                year, month));

        Calendar currentCal = Calendar.getInstance();
        if (year != currentCal.get(Calendar.YEAR) ||
                month != currentCal.get(Calendar.MONTH)) {

            setLoading(true);

            db.collection(COLLECTION_ENTRIES)
                    .whereEqualTo("year", year)
                    .whereEqualTo("month", month)
                    .orderBy(FIELD_CONTROL_NUMBER, Query.Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (task.isSuccessful()) {
                                if (!task.getResult().isEmpty()) {
                                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                                    // Get the current control number and increment by 1
                                    currentControlNumber = document.getLong(FIELD_CONTROL_NUMBER).intValue() + 1;
                                } else {
                                    // If no documents exist, start from 1
                                    currentControlNumber = 1;
                                }
                                controlNumberView.setText(String.valueOf(currentControlNumber));
                                Log.d(TAG_CONTROL_NUMBER, "Control number updated for new month: " +
                                        currentControlNumber);
                            } else {
                                Log.e(TAG_CONTROL_NUMBER, "Error updating control number for new month",
                                        task.getException());
                                showErrorDialog("Database Error",
                                        "Failed to update control number. Please try again.");
                            }
                            setLoading(false);
                        }
                    });
        }
    }

    private void addNewRow() {
        Log.d(TAG, "Adding new table row");

        try {
            TableRow row = new TableRow(this);
            rowCount++;

            // Serial Number
            TextView serialNo = new TextView(this);
            serialNo.setText(String.valueOf(rowCount));
            serialNo.setPadding(3, 3, 3, 3);

            // Product Spinner
            Spinner productSpinner = new Spinner(this);
            ArrayAdapter<String> productAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item,
                    getProductsList());
            productAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            productSpinner.setAdapter(productAdapter);

            // Out EditText
            EditText outEdit = new EditText(this);
            outEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

            // In EditText
            EditText inEdit = new EditText(this);
            inEdit.setInputType(InputType.TYPE_CLASS_NUMBER);

            // Sold TextView (calculated)
            TextView soldText = new TextView(this);
            soldText.setPadding(3, 3, 3, 3);

            // Rate EditText
            EditText rateEdit = new EditText(this);
            rateEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            rateEdit.setPadding(3, 3, 3, 3);
            rateEdit.setVisibility(entryTypeSpinner.getSelectedItem().toString().equals("Sales") ?
                    View.VISIBLE : View.GONE);

            // Value TextView
            TextView valueText = new TextView(this);
            valueText.setPadding(3, 3, 3, 3);
            valueText.setVisibility(entryTypeSpinner.getSelectedItem().toString().equals("Sales") ?
                    View.VISIBLE : View.GONE);

            setupRowCalculations(outEdit, inEdit, soldText, rateEdit, valueText);

            // Add views to row
            row.addView(serialNo);
            row.addView(productSpinner);
            row.addView(outEdit);
            row.addView(inEdit);
            row.addView(soldText);
            row.addView(rateEdit);
            row.addView(valueText);

            productsTable.addView(row);

            Log.d(TAG, "New row added successfully: " + rowCount);
        } catch (Exception e) {
            Log.e(TAG, "Error adding new row", e);
            showErrorDialog("Error", "Failed to add new row. Please try again.");
        }
    }

    private void showErrorDialog(String title, String message) {
        Log.e(TAG, "Showing error dialog: " + title + " - " + message);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void setupRowCalculations(EditText outEdit, EditText inEdit, TextView soldText, EditText rateEdit, TextView valueText) {
        TextWatcher calculateSold = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    int out = outEdit.getText().toString().isEmpty() ? 0 :
                            Integer.parseInt(outEdit.getText().toString());
                    int in = inEdit.getText().toString().isEmpty() ? 0 :
                            Integer.parseInt(inEdit.getText().toString());
                    float rate = rateEdit.getText().toString().isEmpty() ? 0 :
                            Float.parseFloat(rateEdit.getText().toString());

                    String entryType = entryTypeSpinner.getSelectedItem().toString();
                    int soldValue;

                    if (entryType.equals("Sales")) {
                        soldValue = out - in;
                    } else {
                        soldValue = Math.abs(out - in);
                    }

                    soldText.setText(String.valueOf(soldValue));

                    // Calculate and display value
                    int totalValue = (int) (soldValue * rate);
                    valueText.setText(String.format(Locale.US, "%d", totalValue));

                    // Update total amount
                    updateTotalAmount();

                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error calculating values", e);
                    soldText.setText("0");
                    valueText.setText("0");
                }
            }
        };

        outEdit.addTextChangedListener(calculateSold);
        inEdit.addTextChangedListener(calculateSold);
        rateEdit.addTextChangedListener(calculateSold);
    }

    private void updateTotalAmount() {
        int totalAmount = 0;
        for (int i = 1; i < productsTable.getChildCount(); i++) {
            TableRow row = (TableRow) productsTable.getChildAt(i);
            TextView valueText = (TextView) row.getChildAt(6);
            try {
                if (!TextUtils.isEmpty(valueText.getText())) {
                    totalAmount += Integer.parseInt(String.format("%.0f", Float.parseFloat(valueText.getText().toString())));
                }
            } catch (NumberFormatException e) {
                Log.e(TAG, "Error parsing value", e);
            }
        }
        totalAmountView.setText(String.valueOf(totalAmount));
        updateExpectedTransfer();
    }

    private void updateExpectedTransfer() {
        int totalAmount = Integer.parseInt(totalAmountView.getText().toString());
        int cash = TextUtils.isEmpty(cashEditText.getText()) ? 0 : Integer.parseInt(cashEditText.getText().toString());
        int expectedTransfer = totalAmount - cash;
        expectedTransferView.setText(String.valueOf(expectedTransfer));
    }

    private List<String> getProductsList() {
        return Arrays.asList(
                "30CL", "35CL 7UP", "35CL M.D", "50CL",
                "PEPSI", "TBL", "G.APPLE", "PINEAPPLE", "75CL AQUAFINA",
                "RED APPLE", "7UP", "ORANGE", "SODA",
                "S.ORANGE", "S.7UP", "SK 50CL", "SK 30CL"
        );
    }

    private void validateAndShowPreview() {
        Log.d(TAG, "Validating inputs before showing preview");

        if (validateInputs()) {
            showPreviewDialog();
        }
    }

    private boolean validateInputs() {
        if (driverSpinner.getSelectedItem() == null) {
            showError("Please select a driver");
            return false;
        }

        if (TextUtils.isEmpty(dateEdit.getText())) {
            showError("Please select a date");
            return false;
        }

        for (int i = 1; i < productsTable.getChildCount(); i++) {
            TableRow row = (TableRow) productsTable.getChildAt(i);
            EditText outEdit = (EditText) row.getChildAt(2);
            EditText inEdit = (EditText) row.getChildAt(3);

            if (TextUtils.isEmpty(outEdit.getText()) && TextUtils.isEmpty(inEdit.getText())) {
                showError("Please fill in OUT or IN values for row " + i);
                return false;
            }
        }

        Log.d(TAG, "Input validation passed");
        return true;
    }

    private void showPreviewDialog() {
        Log.d(TAG, "Showing preview dialog");

        try {
            StringBuilder preview = new StringBuilder();

            // Header information
            preview.append("Control No: ").append(currentControlNumber).append("\n");
            preview.append("Driver: ").append(driverSpinner.getSelectedItem().toString()).append("\n");
            preview.append("Entry Type: ").append(entryTypeSpinner.getSelectedItem().toString()).append("\n");
            preview.append("Date: ").append(dateEdit.getText()).append("\n\n");

            // Products section
            preview.append("Products:\n");
            for (int i = 1; i < productsTable.getChildCount(); i++) {
                TableRow row = (TableRow) productsTable.getChildAt(i);
                String sn = ((TextView) row.getChildAt(0)).getText().toString();
                String product = ((Spinner) row.getChildAt(1)).getSelectedItem().toString();
                String sold = ((TextView) row.getChildAt(4)).getText().toString();

                preview.append(sn).append(". ")
                        .append(product).append(" - ")
                        .append(sold).append("\n");
            }

            // Financial summary section
            preview.append("\nFinancial Summary:\n");
            preview.append("Total Amount: ₦").append(totalAmountView.getText()).append("\n");
            preview.append("Cash Received: ₦").append(cashEditText.getText()).append("\n");
            preview.append("Expected Transfer: ₦").append(expectedTransferView.getText()).append("\n");

            new AlertDialog.Builder(this)
                    .setTitle("Preview")
                    .setMessage(preview.toString())
                    .setPositiveButton("Confirm", (dialog, which) -> {
                        saveToDatabase();
                        dialog.dismiss();
                    })
                    .setNegativeButton("Edit", (dialog, which) -> dialog.dismiss())
                    .show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing preview dialog", e);
            showErrorDialog("Preview Error", "Failed to show preview. Please try again.");
        }
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        runOnUiThread(() -> {
            addRowButton.setEnabled(!loading);
            saveButton.setEnabled(!loading);

            // Disable all input fields while loading
            dateEdit.setEnabled(!loading);
            driverSpinner.setEnabled(!loading);
            entryTypeSpinner.setEnabled(!loading);

            for (int i = 1; i < productsTable.getChildCount(); i++) {
                TableRow row = (TableRow) productsTable.getChildAt(i);
                row.setEnabled(!loading);
            }

            if (loading) {
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content),
                        "Processing...", Snackbar.LENGTH_INDEFINITE);
                snackbar.show();

                View snackbarView = snackbar.getView();
                snackbarView.setClickable(false);
            } else {
                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "", Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        });
    }

    private void saveToDatabase() {
        Log.d(TAG, "Starting database save operation");
        setLoading(true);

        try {
            WriteBatch batch = db.batch();
            String entryType = entryTypeSpinner.getSelectedItem().toString();
            String selectedDriverName = driverSpinner.getSelectedItem().toString();
            String driverId;

            // Create main entry document
            Map<String, Object> entryData = new HashMap<>();
            entryData.put("controlNumber", currentControlNumber);
            if ("Supply".equals(entryType)) {
                driverId = "supply";
            } else {
                // Find the ID for the selected driver name
                driverId = STAFF_IDS.entrySet().stream()
                        .filter(entry -> entry.getValue().equals(selectedDriverName))
                        .map(Map.Entry::getKey)
                        .findFirst()
                        .orElse("");
            }
            entryData.put("driverId", driverId);
            entryData.put("entryType", entryType);
            entryData.put("date", selectedDate.getTimeInMillis());
            entryData.put("year", selectedDate.get(Calendar.YEAR));
            entryData.put("month", selectedDate.get(Calendar.MONTH));
            entryData.put("createdAt", Calendar.getInstance().getTimeInMillis());

            // Create products list
            List<Map<String, Object>> products = new ArrayList<>();
            double totalSaleAmount = 0;

            for (int i = 1; i < productsTable.getChildCount(); i++) {
                TableRow row = (TableRow) productsTable.getChildAt(i);
                String product = ((Spinner) row.getChildAt(1)).getSelectedItem().toString();
                String outStr = ((EditText) row.getChildAt(2)).getText().toString();
                String inStr = ((EditText) row.getChildAt(3)).getText().toString();
                String soldStr = ((TextView) row.getChildAt(4)).getText().toString();

                int out = TextUtils.isEmpty(outStr) ? 0 : Integer.parseInt(outStr);
                int in = TextUtils.isEmpty(inStr) ? 0 : Integer.parseInt(inStr);
                int sold = Integer.parseInt(soldStr);

                Map<String, Object> productData = new HashMap<>();
                productData.put("product", product);
                productData.put("out", out);
                productData.put("in", in);
                productData.put("sold", sold);
                if (entryType.equals("Sales")) {
                    String rateStr = ((EditText) row.getChildAt(5)).getText().toString();
                    int rate = TextUtils.isEmpty(rateStr) ? 0 : (int) Float.parseFloat(rateStr);
                    String valueStr = ((TextView) row.getChildAt(6)).getText().toString();
                    int value = TextUtils.isEmpty(valueStr) ? 0 : (int) Float.parseFloat(valueStr);
                    productData.put("rate", rate);
                    productData.put("value", value);
                    totalSaleAmount += value;

                    // Create sales document
                    Map<String, Object> saleData = new HashMap<>();
                    saleData.put("amount", value);
                    saleData.put("date", new Date(selectedDate.getTimeInMillis()));
                    saleData.put("product", product);
                    saleData.put("quantity", sold);
                    saleData.put("driverId", driverId);
                    DocumentReference salesRef = db.collection("sales").document();
                    batch.set(salesRef, saleData);
                }
                products.add(productData);

                // Update inventory
                updateInventory(batch, product, out, in, entryType);
            }

            entryData.put("products", products);

            // Add entry document to batch
            DocumentReference entryRef = db.collection(COLLECTION_ENTRIES).document();
            batch.set(entryRef, entryData);

            // Create order document if it's a sales entry
            if (entryType.equals("Sales")) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("date", new Date(selectedDate.getTimeInMillis()));
                orderData.put("amount", totalSaleAmount);
                orderData.put("status", "completed");
                orderData.put("driverId", driverId);  // Store ID instead of name
                DocumentReference orderRef = db.collection("orders").document();
                batch.set(orderRef, orderData);
            }

            // Commit the batch
            batch.commit()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Database save successful");
                        setLoading(false);
                        showSuccessAndFinish();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Database save failed", e);
                        setLoading(false);
                        showErrorDialog("Save Error", "Failed to save entry. Please try again.");
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error preparing database save", e);
            setLoading(false);
            showErrorDialog("Save Error", "Failed to prepare entry data. Please try again.");
        }
    }

    private void showSuccessAndFinish() {
        new AlertDialog.Builder(this)
                .setTitle("Success")
                .setMessage("Entry saved successfully!")
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void updateInventory(WriteBatch batch, String product, int out, int in, String entryType) {
        DocumentReference inventoryRef = db.collection(COLLECTION_INVENTORY).document(product);

        // Calculate quantity change based on entry type
        long quantityChange;
        if (entryType.equals("Sales")) {
            quantityChange = -(out - in); // Decrease for sales
        } else {
            quantityChange = in - out; // Increase for stock received
        }

        batch.update(inventoryRef, "quantity", FieldValue.increment(quantityChange));
    }

    private void initializeInventory() {
        Log.d(TAG, "Initializing inventory");

        List<String> products = Arrays.asList(
                "30CL", "35CL 7UP", "35CL M.D", "50CL",
                "PEPSI", "TBL", "G.APPLE", "PINEAPPLE", "75CL AQUAFINA",
                "RED APPLE", "7UP", "ORANGE", "SODA",
                "S.ORANGE", "S.7UP", "SK 50CL", "SK 30CL"
        );

        for (String product : products) {
            DocumentReference docRef = db.collection(COLLECTION_INVENTORY).document(product);
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (!document.exists()) {
                            Map<String, Object> data = new HashMap<>();
                            data.put("quantity", 0); // Initialize quantity to 0
                            docRef.set(data)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Inventory document created for: " + product);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error creating inventory document for: " + product, e);
                                    });
                        }
                    } else {
                        Log.e(TAG, "Error getting document:", task.getException());
                    }
                }
            });
        }
    }

}