package com.osler.analysers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity {

    private Spinner countrySpinner;
    private TextView mTested, mPositive;
    private ProgressBar progressBar;

    private List<String> countryList;
    private Map<String, CountryData> countryDataMap;
    private Map<String, Long> siteCalibrationExpiryMap;

    // Declare a boolean variable to keep track of selection status
    private boolean isItemSelected = false;
    private int selectedPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        countrySpinner = findViewById(R.id.country_spinner);
        mTested = findViewById(R.id.tested);
        mPositive = findViewById(R.id.positive);
        progressBar = findViewById(R.id.progress_bar);

        // Set the initial progress of the SeekBar to zero
        progressBar.setProgress(0);

        // Find the Full Report button and set its click listener
        Button fullReportButton = findViewById(R.id.full_report_button);
        fullReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                generateManagementReport();
            }
        });

        // Load data from CSV and update UI
        try {
            readDataFromCSV();
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading data from CSV", Toast.LENGTH_SHORT).show();
        }

        // Disable SeekBar touch interactions
        progressBar.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return true; // Prevent touch events from being propagated
            }
        });

        // Set listener for Spinner selection
        countrySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Check if the user selected a valid country (not "Select")
                if (position > 0) {
                    String selectedCountry = countryList.get(position);
                    updateCountryInfo(selectedCountry);

                    // Update SeekBar progress and color based on the selected country's positive percentage
                    CountryData countryData = countryDataMap.get(selectedCountry);
                    if (countryData != null) {
                        int positivePercentage = (countryData.getTested() == 0) ? 0 : (int) (((double) countryData.getPositive() / countryData.getTested()) * 100);
                        progressBar.setProgress(positivePercentage);

                        // Change the color of the SeekBar progress dynamically
                        if (positivePercentage > 50) {
                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        } else {
                            progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                        }
                    }
                } else {
                    // Clear the country info and reset SeekBar progress and color if "Select" is chosen
                    String defaultTestedInfo = getString(R.string.tested);
                    String defaultPositiveInfo = getString(R.string.positive);
                    mTested.setText(defaultTestedInfo);
                    mPositive.setText(defaultPositiveInfo);
                    progressBar.setProgress(0);
                    progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.default_color))); // Set default color to your defined default_color
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing when nothing is selected
            }
        });

        // Set the layout's touch listener to detect touch events outside the cardView
        findViewById(R.id.main_layout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Check if an item was previously selected before clearing the text and progress bar
                if (isItemSelected) {
                    // Clear the country info and progress bar
                    mTested.setText("");
                    mPositive.setText("");
                    progressBar.setProgress(0);

                    // Deselect the spinner item by setting the selection to -1
                    countrySpinner.setSelection(-1);

                    // Set the boolean variable to false as "Select" is chosen
                    isItemSelected = false;

                    // Consume the touch event to prevent it from propagating to other views
                    return true;
                }
                // Return false to allow the touch event to propagate to other views
                return false;
            }
        });
    }

    // New method to update the UI for the selected country
    private void updateUIForSelectedCountry(String selectedCountry) {
        CountryData countryData = countryDataMap.get(selectedCountry);
        if (countryData != null) {
            String testedInfo = getString(R.string.country_tested_info, countryData.getTested());
            String positiveInfo = getString(R.string.country_info_data, (int) (((double) countryData.getPositive() / countryData.getTested()) * 100),
                    countryData.getPositive(), countryData.getTested());
            mTested.setText(testedInfo);
            mPositive.setText(positiveInfo);

            // Set the SeekBar progress and color based on positive percentage
            int positivePercentage = (int) ((countryData.getPositive() * 100.0) / countryData.getTested());
            progressBar.setProgress(positivePercentage);
            if (positivePercentage < 50) {
                progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
            } else {
                progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorRed)));
            }
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
                        countryData.setCountry(country);
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
                    mTested.setText("");
                    mPositive.setText("");
                    progressBar.setProgress(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (isItemSelected) {
                    String selectedCountry = countryList.get(selectedPosition);
                    updateCountryInfo(selectedCountry);
                }
            }
        });
    }

    private void updateCountryInfo(String selectedCountry) {
        CountryData countryData = countryDataMap.get(selectedCountry);
        if (countryData != null) {
            String testedInfo = getString(R.string.country_tested_info, countryData.getTested());
            String positiveInfo = getString(R.string.country_info_data, (int) (((double) countryData.getPositive() / countryData.getTested()) * 100),
                    countryData.getPositive(), countryData.getTested());
            mTested.setText(testedInfo);
            mPositive.setText(positiveInfo);

            // Set the SeekBar progress and color based on positive percentage
            int positivePercentage = (int) ((countryData.getPositive() * 100.0) / countryData.getTested());
            progressBar.setProgress(positivePercentage);
            if (positivePercentage < 50) {
                progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
            } else {
                progressBar.setProgressTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorRed)));
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            // Get the x and y coordinates of the touch event
            float x = event.getX();
            float y = event.getY();

            // Get the position of the cardView relative to the screen
            int[] location = new int[2];
            View cardView = findViewById(R.id.country_info_layout);
            cardView.getLocationOnScreen(location);
            int cardViewX = location[0];
            int cardViewY = location[1];

            // Get the width and height of the cardView
            int cardViewWidth = cardView.getWidth();
            int cardViewHeight = cardView.getHeight();

            // Check if the touch event is outside the bounds of the cardView
            if (x < cardViewX || x > cardViewX + cardViewWidth || y < cardViewY || y > cardViewY + cardViewHeight) {
                // Check if an item was previously selected before clearing the text and progress bar
                if (isItemSelected) {
                    // Clear the country info and progress bar
                    mTested.setText("");
                    mPositive.setText("");
                    progressBar.setProgress(0);

                    // Deselect the spinner item by setting the selection to -1
                    countrySpinner.setSelection(-1);

                    // Set the boolean variable to false as "Select" is chosen
                    isItemSelected = false;
                }

                // Return true to consume the touch event and prevent it from propagating to other views
                return true;
            }
        }
        // Return false to allow the touch event to propagate to other views
        return super.onTouchEvent(event);
    }
}