package com.osler.analysers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private Spinner countrySpinner;
    private TextView countryInfoText;
    private TextView countryInfoData;
    private ProgressBar progressBar;
    private Button fullReportButton;

    private List<String> countryList;
    private Map<String, CountryData> countryDataMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        countrySpinner = findViewById(R.id.country_spinner);
        countryInfoText = findViewById(R.id.country_info_text);
        countryInfoData = findViewById(R.id.country_info_data);
        progressBar = findViewById(R.id.progress_bar);
        fullReportButton = findViewById(R.id.full_report_button);

        // Load data from CSV and update UI
        try {
            readDataFromCSV();
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading data from CSV", Toast.LENGTH_SHORT).show();
        }

        // Set listener for Spinner selection
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCountry = countryList.get(position);
                updateCountryInfo(selectedCountry);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // Set up "Full Report" button click listener
        fullReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateManagementReport();
            }
        });
    }

    private void updateCountryInfo(String selectedCountry) {
        CountryData countryData = countryDataMap.get(selectedCountry);
        if (countryData != null) {
            int tested = countryData.getTested();
            int positive = countryData.getPositive();
            int percentagePositive = (tested == 0) ? 0 : (positive * 100) / tested;

            String countryInfo = getString(R.string.country_info_data,
                    tested, percentagePositive, positive, tested);
            countryInfoText.setText(getString(R.string.country_info, selectedCountry));
            countryInfoData.setText(countryInfo);
            progressBar.setProgress(percentagePositive);
        }
    }


    private void updateUI() {
        // Calculate the percentage of positive cases
        List<CountryData> countryDataList = new ArrayList<>(countryDataMap.values());
        int totalPositive = 0;
        int totalTested = 0;

        for (CountryData countryData : countryDataList) {
            totalPositive += countryData.getPositive();
            totalTested += countryData.getTested();
        }

        int percentagePositive = (totalTested == 0) ? 0 : (totalPositive * 100) / totalTested;

        // Update the SeekBar
        progressBar.setProgress(percentagePositive);
    }

    private void generateManagementReport() {
        List<CountryData> countryDataList = new ArrayList<>(countryDataMap.values());

        // Sort the country data based on the percentage of positive tests in descending order
        Collections.sort(countryDataList, new Comparator<CountryData>() {
            @Override
            public int compare(CountryData c1, CountryData c2) {
                double percentage1 = (c1.getTested() == 0) ? 0 : ((double) c1.getPositive() / c1.getTested()) * 100;
                double percentage2 = (c2.getTested() == 0) ? 0 : ((double) c2.getPositive() / c2.getTested()) * 100;
                return Double.compare(percentage2, percentage1);
            }
        });

        // Generate the management report
        StringBuilder managementReport = new StringBuilder();
        for (CountryData countryData : countryDataList) {
            double percentagePositive = (countryData.getTested() == 0) ? 0 : ((double) countryData.getPositive() / countryData.getTested()) * 100;
            String line = String.format("%s, Tested = %d, Positive = %.2f%% (%d/%d)\n",
                    countryData.getCountry(), countryData.getTested(), percentagePositive, countryData.getPositive(), countryData.getTested());
            managementReport.append(line);
        }

        // Write the report to mgmt_report.txt (you may need to handle file I/O here)

        // Display the management report in a Toast (optional)
        Toast.makeText(this, managementReport.toString(), Toast.LENGTH_LONG).show();
    }

    private void readDataFromCSV() {
        InputStream inputStream = getResources().openRawResource(R.raw.data);
        Scanner scanner = new Scanner(inputStream);
        countryList = new ArrayList<>();
        countryDataMap = new HashMap<>();

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            String[] nextLine = line.split(",");

            if (nextLine.length >= 6) {
                String country = nextLine[0].trim();
                try {
                    int tested = Integer.parseInt(nextLine[2].trim());
                    int positive = Integer.parseInt(nextLine[3].trim());

                    CountryData countryData = countryDataMap.get(country);
                    if (countryData == null) {
                        countryData = new CountryData();
                        countryData.setTested(tested);
                        countryData.setPositive(positive);
                        countryData.setCountry(country); // Set the country name using the setCountry method
                        countryDataMap.put(country, countryData);
                        countryList.add(country);
                    } else {
                        countryData.setTested(countryData.getTested() + tested);
                        countryData.setPositive(countryData.getPositive() + positive);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error parsing data from CSV", Toast.LENGTH_SHORT).show();
                    Log.e("CSV_DEBUG", "Error parsing line: " + line);
                }
            }
        }
        scanner.close();

        // Populate the spinner with country names
        populateSpinner();
    }

    private void populateSpinner() {
        // Add "Select" as the first item in the countryList
        countryList.add(0, "Select");

        // Create the custom ArrayAdapter
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                R.layout.custom_spinner_item, countryList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (position >= 0 && position < 4) {
                    // Show the first 4 items with a custom background color
                    view.setBackgroundColor(getResources().getColor(R.color.spinner_dropdown_highlight));
                } else {
                    // For items beyond the first 4, set the default background color
                    view.setBackgroundColor(getResources().getColor(android.R.color.white));
                }
                return view;
            }
        };

        adapter.setDropDownViewResource(R.layout.custom_spinner_item);
        countrySpinner.setAdapter(adapter);

        // Set a default selection to "Select"
        countrySpinner.setSelection(0);

        // Add a listener to the spinner to handle item selection
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Check if the user selected a valid country (not "Select")
                if (position > 0) {
                    String selectedCountry = countryList.get(position);
                    updateCountryInfo(selectedCountry);
                } else {
                    // Clear the country info and progress bar if "Select" is chosen
                    countryInfoText.setText("");
                    countryInfoData.setText("");
                    progressBar.setProgress(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });

        // Set the Spinner dropdown height to show only 4 items at a time
        int dropdownHeightInPixels = getResources().getDimensionPixelSize(R.dimen.spinner_dropdown_item_height);
        countrySpinner.setDropDownVerticalOffset(dropdownHeightInPixels);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            int itemCount = 4; // Change this value to set the number of visible items
            countrySpinner.setSelection(Math.min(itemCount, countryList.size()) - 1);
        }
        return super.onTouchEvent(event);
    }
}