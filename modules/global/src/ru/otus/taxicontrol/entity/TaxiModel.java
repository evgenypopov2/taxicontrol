package ru.otus.taxicontrol.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import ru.otus.common.model.TaxiType;

import javax.persistence.*;

@Table(name = "TAXICONTROL_AUTO_MODEL")
@Entity(name = "taxicontrol_AutoModel")
@NamePattern("%s|name")
public class TaxiModel extends StandardEntity {
    private static final long serialVersionUID = 8586457859102629069L;

    @Column(name = "NAME")
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VENDOR_ID")
    private TaxiVendor vendor;

    @Column(name = "TYPE_")
    private String type;

    public TaxiType getType() {
        return type == null ? null : TaxiType.fromId(type);
    }

    public void setType(TaxiType type) {
        this.type = type == null ? null : type.getId();
    }

    public TaxiVendor getVendor() {
        return vendor;
    }

    public void setVendor(TaxiVendor vendor) {
        this.vendor = vendor;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}