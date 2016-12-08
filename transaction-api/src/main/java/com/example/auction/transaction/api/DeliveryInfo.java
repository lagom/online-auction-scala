package com.example.auction.transaction.api;

import com.example.auction.item.api.DeliveryOption;

public final class DeliveryInfo {

    private final String addressLine1;
    private final String addressLine2;
    private final String city;
    private final String state;
    private final int postalCode;
    private final String country;
    private final DeliveryOption selectedDeliveryOption;

    public DeliveryInfo(String addressLine1, String addressLine2, String city, String state, int postalCode, String country, DeliveryOption selectedDeliveryOption) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.selectedDeliveryOption = selectedDeliveryOption;
    }
}
