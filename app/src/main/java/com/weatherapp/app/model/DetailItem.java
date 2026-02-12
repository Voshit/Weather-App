package com.weatherapp.app.model;

public class DetailItem {
    private String title;
    private String value;
    private String subtitle;
    private int iconResId;
    private int color;

    public DetailItem(String title, String value, String subtitle, int iconResId, int color) {
        this.title = title;
        this.value = value;
        this.subtitle = subtitle;
        this.iconResId = iconResId;
        this.color = color;
    }

    public DetailItem(String title, String value, String subtitle, int iconResId) {
        this(title, value, subtitle, iconResId, 0xFFFFFFFF); // Default white
    }

    public String getTitle() { return title; }
    public String getValue() { return value; }
    public String getSubtitle() { return subtitle; }
    public int getIconResId() { return iconResId; }
    public int getColor() { return color; }
}
