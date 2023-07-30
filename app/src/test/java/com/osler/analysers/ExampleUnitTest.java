package com.osler.analysers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.*;

import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {Build.VERSION_CODES.O_MR1}) // Set the appropriate Android SDK version
public class ExampleUnitTest {

    private MainActivity mainActivity;

    @Before
    public void setUp() {
        mainActivity = new MainActivity();
    }

    @Test
    public void testCalculatePercentagePositive() throws Exception {
        // Arrange
        int tested = 100;
        int positive = 25;
        double expectedPercentage = 25.0;
        MainActivity mainActivity = new MainActivity();

        // Act
        Method calculatePercentagePositive = MainActivity.class.getDeclaredMethod("calculatePercentagePositive", int.class, int.class);
        calculatePercentagePositive.setAccessible(true);
        double actualPercentage = (double) calculatePercentagePositive.invoke(mainActivity, tested, positive);

        // Assert
        assertEquals(expectedPercentage, actualPercentage, 0.001); // Specify a delta for double comparison
    }

    @Test
    public void testUpdateCountryInfo() throws NoSuchFieldException, IllegalAccessException {
        // Arrange
        String selectedCountry = "Sample Country";
        CountryData countryData = new CountryData();
        countryData.setCountry(selectedCountry);
        countryData.setTested(100);
        countryData.setPositive(25);

        // Use reflection to access and set the private field countryDataMap in MainActivity
        Field field = MainActivity.class.getDeclaredField("countryDataMap");
        field.setAccessible(true);
        HashMap<String, CountryData> countryDataMap = new HashMap<>();
        countryDataMap.put(selectedCountry, countryData);
        field.set(mainActivity, countryDataMap);

        // Act
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        // Assert
        assertEquals("Country: " + selectedCountry, mainActivity.mTested.getText());
        assertEquals("Tested = 100, Positive = 25.00% (25/100)", mainActivity.mPositive.getText());
    }
}