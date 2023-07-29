package com.osler.analysers;

public class CountryData {
    private String country;
    private int tested;
    private int positive;

    // Constructor
    public CountryData() {
        //
    }

    // Getters and setters for country, tested, and positive fields

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getTested() {
        return tested;
    }

    public void setTested(int tested) {
        this.tested = tested;
    }

    public int getPositive() {
        return positive;
    }

    public void setPositive(int positive) {
        this.positive = positive;
    }
}
