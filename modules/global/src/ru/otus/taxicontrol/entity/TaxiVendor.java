package ru.otus.taxicontrol.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "TAXICONTROL_AUTO_VENDOR")
@Entity(name = "taxicontrol_AutoVendor")
@NamePattern("%s|name")
public class TaxiVendor extends StandardEntity {
    private static final long serialVersionUID = 2136170417095562938L;

    @Column(name = "NAME")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}