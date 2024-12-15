package com.example.MealMate.model;

public class Item {
    String id;
    private String name, description, price, imageUrl, timestamp, location;
    private boolean isPurchased, isTagged;
    private double latTag, lonTag;

    public Item() {
    }

    public Item(String name, String description, String price, String imageUrl, boolean isPurchased, String timeStamp, String location) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isPurchased = isPurchased;
        this.timestamp = timeStamp;
        this.location = location;
    }

    public Item(String name, String description, String price, String imageUrl, boolean isPurchased, boolean isTagged,
                double latTag, double lonTag, String timeStamp, String location) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.isPurchased = isPurchased;
        this.isTagged = isTagged;
        this.latTag = latTag;
        this.lonTag = lonTag;
        this.timestamp = timeStamp;
        this.location = location;
    }

    public Item(String itemId, String itemName, String itemDescription, String price, String imageUrl, String timestamp, String location) {

    }

    public Item(String itemName, String itemDescription, double price, String toString, String timestamp, String location) {
    }

    public Item(String itemId, String imageUrl) {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setLocation(String location){
        this.location = location;
    }

    public String getLocation(String location){
        return location;
    }

    public String getTimeStamp() {
        return timestamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timestamp = timeStamp;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getPrice() {
        return price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public boolean isPurchased() {
        return isPurchased;
    }

    public void setPurchased(boolean purchased) {
        isPurchased = purchased;
    }

    public boolean isTagged() {
        return isTagged;
    }

    public void setTagged(boolean tagged) {
        isTagged = tagged;
    }

    public double getLatTag() {
        return latTag;
    }

    public void setLatTag(double latTag) {
        this.latTag = latTag;
    }

    public double getLonTag() {
        return lonTag;
    }

    public void setLonTag(double lonTag) {
        this.lonTag = lonTag;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(String price) {
        this.price = price;
    }
}