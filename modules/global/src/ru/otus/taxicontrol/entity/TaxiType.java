package ru.otus.taxicontrol.entity;

import com.haulmont.chile.core.datatypes.impl.EnumClass;

import javax.annotation.Nullable;


public enum TaxiType implements EnumClass<String> {

    STANDARD("Standard"),
    COMFORT("Comfort"),
    BUSINESS("Business"),
    PREMIUM("Premium");

    private String id;

    TaxiType(String value) {
        this.id = value;
    }

    public String getId() {
        return id;
    }

    @Nullable
    public static TaxiType fromId(String id) {
        for (TaxiType at : TaxiType.values()) {
            if (at.getId().equals(id)) {
                return at;
            }
        }
        return null;
    }
}