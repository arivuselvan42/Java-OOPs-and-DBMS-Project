package com.findit.model;

public class Item {
    private int itemId;
    private String itemName;
    private int categoryId;
    private String categoryName;
    private String description;
    private String imagePath;

    public Item() {
    }

    public Item(int itemId, String itemName,
                int categoryId, String description) {
        this(itemId, itemName, categoryId, null, description, null);
    }

    public Item(int itemId, String itemName,
                int categoryId, String categoryName, String description) {
        this(itemId, itemName, categoryId, categoryName, description, null);
    }

    public Item(int itemId, String itemName,
                int categoryId, String categoryName, String description, String imagePath) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.description = description;
        this.imagePath = imagePath;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
