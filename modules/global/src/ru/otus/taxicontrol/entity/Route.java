package ru.otus.taxicontrol.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import org.locationtech.jts.geom.LineString;

import javax.persistence.*;
import java.util.Date;

@Table(name = "TAXICONTROL_ROUTE")
@Entity(name = "taxicontrol_Route")
public class Route extends StandardEntity {
    private static final long serialVersionUID = -7666445145975238723L;

    @Lookup(type = LookupType.SCREEN, actions = {"lookup", "open"})
    @MetaProperty
    @Transient
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAXI_ID")
    @Lookup(type = LookupType.SCREEN, actions = {"lookup", "open"})
    private Taxi taxi;

    @MetaProperty(datatype = "GeoPolyline")
    @Column(name = "PATH")
    private LineString path;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ORDER_TIME")
    private Date orderTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "START_TIME")
    private Date startTime;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "END_TIME")
    private Date endTime;

    @Column(name = "COST")
    private Integer cost;

    public Integer getCost() {
        return cost;
    }

    public void setCost(Integer cost) {
        this.cost = cost;
    }

    public Date getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(Date orderTime) {
        this.orderTime = orderTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public LineString getPath() {
        return path;
    }

    public void setPath(LineString path) {
        this.path = path;
    }

    public Taxi getTaxi() {
        return taxi;
    }

    public void setTaxi(Taxi taxi) {
        this.taxi = taxi;
    }

    public Client getClient() {
        return client;
    }

    public void setClient(Client client) {
        this.client = client;
    }
}