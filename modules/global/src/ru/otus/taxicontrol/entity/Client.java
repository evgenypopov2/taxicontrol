package ru.otus.taxicontrol.entity;

import com.haulmont.chile.core.annotations.MetaProperty;
import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import org.locationtech.jts.geom.Point;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Table(name = "TAXICONTROL_CLIENT")
@Entity(name = "taxicontrol_Client")
@NamePattern("%s|name")
public class Client extends StandardEntity {
    private static final long serialVersionUID = 5108072193529917732L;

    @Column(name = "NAME")
    private String name;

    @Column(name = "PHONE")
    private String phone;

    @MetaProperty(datatype = "GeoPoint")
    @Column(name = "START_POINT")
    private Point startPoint;

    @MetaProperty(datatype = "GeoPoint")
    @Column(name = "END_POINT")
    private Point endPoint;

    public Point getEndPoint() {
        return endPoint;
    }

    public void setEndPoint(Point endPoint) {
        this.endPoint = endPoint;
    }

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}