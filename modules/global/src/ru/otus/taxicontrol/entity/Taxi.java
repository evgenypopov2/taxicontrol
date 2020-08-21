package ru.otus.taxicontrol.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;

@Table(name = "TAXICONTROL_TAXI")
@Entity(name = "taxicontrol_Taxi")
public class Taxi extends StandardEntity {
    private static final long serialVersionUID = -1688609614149972366L;

    private @MetaProperty
    @Transient
    TaxiModel model;

    @Column(name = "DRIVER_NAME")
    private String driverName;

    @NotNull
    @Column(name = "DRIVER_PHONE", nullable = false, unique = true)
    private String driverPhone;

    @Column(name = "NUMBER_")
    private String number;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public void setDriverPhone(String driverPhone) {
        this.driverPhone = driverPhone;
    }

    public TaxiModel getModel() {
        return model;
    }

    public void setModel(TaxiModel model) {
        this.model = model;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }
}