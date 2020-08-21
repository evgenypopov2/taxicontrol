package ru.otus.taxicontrol.web.screens;

import com.haulmont.addon.maps.web.gui.components.GeoMap;
import com.haulmont.addon.maps.web.gui.components.layer.TileLayer;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.util.Objects;

@UiController("taxicontrol_MainMap")
@UiDescriptor("main-map.xml")
public class MainMap extends Screen {

    @Inject
    private GeoMap map;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        map.setCenter(37.616543D, 55.751451D);

        map.setZoomLevel(13);

        TileLayer tileLayer = new TileLayer(Objects.requireNonNull(map.getId()), "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png");
        tileLayer.setAttributionString("&copy; <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors");
        map.addLayer(tileLayer);
    }
}