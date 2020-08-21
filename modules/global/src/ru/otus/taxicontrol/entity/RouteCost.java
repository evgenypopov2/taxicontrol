package ru.otus.taxicontrol.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "TAXICONTROL_ROUTE_COST")
@Entity(name = "taxicontrol_RouteCost")
public class RouteCost extends StandardEntity {
    private static final long serialVersionUID = 9152167295091284910L;

    @Column(name = "TAXI_TYPE")
    private String taxiType;

    @Column(name = "COST_PER_ONE")
    private Integer costPerOne;

    public Integer getCostPerOne() {
        return costPerOne;
    }

    public void setCostPerOne(Integer costPerOne) {
        this.costPerOne = costPerOne;
    }

    public TaxiType getTaxiType() {
        return taxiType == null ? null : TaxiType.fromId(taxiType);
    }

    public void setTaxiType(TaxiType taxiType) {
        this.taxiType = taxiType == null ? null : taxiType.getId();
    }
}