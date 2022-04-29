package com.journi.challenge.models;

/**
 * Response for Product
 * added support for currencyCode
 */
public class ProductResponse {
    private final String id;
    private final String description;
    private final Double price;
    private final String currencyCode;

    public ProductResponse(String id, String description, Double price, String currencyCode) {
        this.id = id;
        this.description = description;
        this.price = price;
        this.currencyCode = currencyCode;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public Double getPrice() {
        return price;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

}
