package ru.otus.taxicontrol.web.screens;

import com.haulmont.addon.maps.gis.utils.GeometryUtils;
import com.haulmont.addon.maps.web.gui.components.CanvasLayer;
import com.haulmont.addon.maps.web.gui.components.GeoMap;
import com.haulmont.addon.maps.web.gui.components.layer.TileLayer;
import com.haulmont.addon.maps.web.gui.components.layer.style.FontPointIcon;
import com.haulmont.addon.maps.web.gui.components.layer.style.PointStyle;
import com.haulmont.addon.maps.web.gui.components.layer.style.PolylineStyle;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.Timer;
import com.haulmont.cuba.gui.executors.BackgroundTask;
import com.haulmont.cuba.gui.executors.BackgroundTaskHandler;
import com.haulmont.cuba.gui.executors.BackgroundWorker;
import com.haulmont.cuba.gui.executors.TaskLifeCycle;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.LineString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import ru.otus.common.dto.*;
import ru.otus.common.model.TaxiColor;
import ru.otus.common.model.TaxiStatus;
import ru.otus.common.model.TaxiType;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ru.otus.common.model.GeoConstants.COORDS_SHIFT_1KM_X;
import static ru.otus.common.model.GeoConstants.COORDS_SHIFT_1KM_Y;

@UiController("taxicontrol_MainMap")
@UiDescriptor("main-map.xml")
public class MainMap extends Screen {

    private static final Logger log = LoggerFactory.getLogger(MainMap.class);
    private static final String SERVER_PORT = "localhost:8084";
    private static final String SERVER_REST_URL = "http://" + SERVER_PORT;

    private static final String CLIENT_WEBSOCKET_URL = "ws://" + SERVER_PORT + "/client-websocket";
    private static final String CLIENT_REGISTER_URL = SERVER_REST_URL + "/client/register";
    private static final String CLIENT_LOGIN_URL = SERVER_REST_URL + "/client/auth";
    private static final String CLIENT_GET_ROUTE_URL = SERVER_REST_URL + "/client/order-request";
    private static final String CLIENT_MAKE_ORDER_URL = SERVER_REST_URL + "/client/order-order";
    private static final String CLIENT_ORDER_CANCEL_URL = SERVER_REST_URL + "/client/order-cancel";

    private static final String TAXI_WEBSOCKET_URL = "ws://" + SERVER_PORT + "/taxi-websocket";
    private static final String TAXI_GET_CAR_INFO_URL = SERVER_REST_URL + "/taxi/car-info";
    private static final String TAXI_REGISTER_URL = SERVER_REST_URL + "/taxi/register";
    private static final String TAXI_LOGIN_URL = SERVER_REST_URL + "/taxi/auth";
    private static final String TAXI_LOCATION_URL = SERVER_REST_URL + "/taxi/location";
    private static final String TAXI_TAKE_ORDER_URL = SERVER_REST_URL + "/taxi/take-order";
    private static final String TAXI_STATUS_URL = SERVER_REST_URL + "/taxi/status";

    private AuthResponseDTO clientAuth = null;
    private AuthResponseDTO taxiAuth = null;

    private OrderRequestResponseDTO clientOrderRequest = null;
    private OrderOrderResponseDTO clientOrderOrder = null;
    private OrderOrderResponseDTO clientReceivedOrderOrder = null;
    private TaxiLocationDTO clientReceivedTaxiLocation = null;

    private CommonStompSessionHandler clientStompSessionHandler;
    private TaxiStompSessionHandler taxiStompSessionHandler;

    private final BlockingQueue<Object> messageForTaxiQueue = new ArrayBlockingQueue<>(10);
    private ClientOrderForTaxiDTO orderForTaxi = null;

    private CanvasLayer.Point clientPoint;
    private CanvasLayer.Point clientTaxiPoint;
    private CanvasLayer.Point clientDestinationPoint = null;
    private CanvasLayer.Polyline clientRoute = null;

    private CanvasLayer.Point taxiPoint;
    private CanvasLayer.Point taxiStartPoint;
    private CanvasLayer.Point taxiEndPoint;
    private CanvasLayer.Polyline taxiRoute = null;

    private PointStyle clientPointStyle;
    private PointStyle taxiPointStyle;

    private CarInfoDTO carInfoDTO = null;
    private BackgroundTaskHandler<Void> taxiMoveTaskHandler;

    @Inject
    protected BackgroundWorker backgroundWorker;
    @Inject
    private Dialogs dialogs;
    @Inject
    private Notifications notifications;
    @Inject
    private GeoMap clientMap;
    @Inject
    private VBoxLayout clientLoginPanel;
    @Inject
    private VBoxLayout clientRegisterPanel;
    @Inject
    private TextField<String> clientLoginField;
    @Inject
    private PasswordField clientPasswordField;
    @Inject
    private GeoMap taxiMap;
    @Inject
    private VBoxLayout taxiRegisterPanel;
    @Inject
    private VBoxLayout taxiLoginPanel;
    @Inject
    private HBoxLayout clientConnectRegisterPanel;
    @Inject
    private HBoxLayout taxiConnectRegisterPanel;
    @Inject
    private TextField<String> taxiLoginField;
    @Inject
    private PasswordField taxiPasswordField;
    @Inject
    private VBoxLayout clientOrderPanel;
    @Inject
    private VBoxLayout clientSelectDestinationPointPanel;
    @Inject
    private Label<String> clientOrderRequestDescription;
    @Inject
    private LookupField<TaxiType> clientSelectTaxiTypeField;
    @Inject
    private HBoxLayout taxiWaitingForClientPanel;
    @Inject
    private Timer taxiTimer;
    @Inject
    private VBoxLayout clientWaitForTaxiPanel;
    @Inject
    private Timer taxiWaitForMessageTimer;
    @Inject
    private VBoxLayout taxiClientOrderPanel;
    @Inject
    private Label<String> taxiClientOrderDesc;
    @Inject
    private Label<String> taxiClientOrderPrice;
    @Inject
    private HBoxLayout taxiClientOrderButtonsPanel;
    @Inject
    private Timer clientWaitForTaxiTimer;
    @Inject
    private VBoxLayout clientTaxiInfoPanel;
    @Inject
    private Label<String> carBrandLabel;
    @Inject
    private Label<String> carModelLabel;
    @Inject
    private Label<String> carColorLabel;
    @Inject
    private Label<String> carNumberLabel;
    @Inject
    private Label<String> driverNameLabel;
    @Inject
    private Label<String> driverPhoneLabel;
    @Inject
    private LookupField<String> taxiVendorLookupField;
    @Inject
    private LookupField<String> taxiModelLookupField;
    @Inject
    private LookupField<TaxiColor> taxiColorLookupField;
    @Inject
    private TextField<String> taxiRegLoginField;
    @Inject
    private PasswordField taxiRegPassword1Field;
    @Inject
    private PasswordField taxiRegPassword2Field;
    @Inject
    private TextField<String> taxiRegFirstNameField;
    @Inject
    private TextField<String> taxiRegLastNameField;
    @Inject
    private TextField<String> taxiRegPhoneField;
    @Inject
    private TextField<String> taxiRegEmailField;
    @Inject
    private TextField<String> taxiNumberField;
    @Inject
    private PasswordField clientRegPassword1Field;
    @Inject
    private PasswordField clientRegPassword2Field;
    @Inject
    private TextField<String> clientRegLoginField;
    @Inject
    private TextField<String> clientRegFirstNameField;
    @Inject
    private TextField<String> clientRegLastNameField;
    @Inject
    private TextField<String> clientRegPhoneField;
    @Inject
    private TextField<String> clientRegEmailField;
    @Inject
    private Timer clientGetTaxiLocationTimer;
    @Inject
    private HBoxLayout taxiTripStartCancelPanel;
    @Inject
    private Button taxiStartTripButton;
    @Inject
    private Label<String> taxiWaitForTripEnd;
    @Inject
    private Button clientCancelOrderButton;
    @Inject
    private HBoxLayout clientFinishCancelPanel;
    @Inject
    private HBoxLayout clientWaitForTripEnd;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        //55.910419, 37.346812 - top left
        //55.571049, 37.873610 bottom right

        final TileLayer tileLayer = new TileLayer(Objects.requireNonNull(clientMap.getId()), "https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png");
        tileLayer.setAttributionString("&copy; <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors");

        clientPointStyle = new PointStyle();
        clientPointStyle.setIcon(new FontPointIcon(CubaIcon.USER));
        clientMap.addLayer(tileLayer);
        clientPoint = addPoint(clientMap, randomDouble(37.346812, 37.873610), randomDouble(55.571049, 55.910419), clientPointStyle);
        clientMap.setCenter(clientPoint.getGeometry().getX(), clientPoint.getGeometry().getY());
        clientMap.setZoomLevel(13);

        taxiPointStyle = new PointStyle();
        taxiPointStyle.setIcon(new FontPointIcon(CubaIcon.TAXI));
        taxiPoint = addPoint(taxiMap,
                randomDouble(clientPoint.getGeometry().getX() - COORDS_SHIFT_1KM_X, clientPoint.getGeometry().getX() + COORDS_SHIFT_1KM_X),
                randomDouble(clientPoint.getGeometry().getY() - COORDS_SHIFT_1KM_Y, clientPoint.getGeometry().getY() + COORDS_SHIFT_1KM_Y),
                taxiPointStyle);
        taxiMap.setCenter(taxiPoint.getGeometry().getX(), taxiPoint.getGeometry().getY());
        taxiMap.setZoomLevel(13);
        taxiMap.addLayer(tileLayer);
    }

    public synchronized Object getMessageForTaxi() throws InterruptedException {
        return messageForTaxiQueue.size() > 0 ? messageForTaxiQueue.take() : null;
    }

    public synchronized void addMessageForTaxi(Object messageForTaxi) {
        this.messageForTaxiQueue.offer(messageForTaxi);
    }

    public synchronized OrderOrderResponseDTO getClientOrderOrder() {
        return clientReceivedOrderOrder;
    }

    public synchronized void setClientOrderOrder(OrderOrderResponseDTO clientOrderOrder) {
        this.clientReceivedOrderOrder = clientOrderOrder;
    }

    public synchronized TaxiLocationDTO getTaxiLocation() {
        return clientReceivedTaxiLocation;
    }

    public synchronized void setTaxiLocation(TaxiLocationDTO taxiLocationDTO) {
        this.clientReceivedTaxiLocation = taxiLocationDTO;
    }

    public void onClientConnectClick() {
        clientConnectRegisterPanel.setVisible(false);
        clientLoginPanel.setVisible(true);
    }

    public void onClientLoginButtonClick() {
        clientStompSessionHandler = new ClientStompSessionHandler(this::clientMessageHandler);
        clientAuth = loginAndGetToken(clientLoginField.getValue(), clientPasswordField.getValue(),
                CLIENT_LOGIN_URL, CLIENT_WEBSOCKET_URL, clientStompSessionHandler);
        if (clientAuth != null) {
            clientStompSessionHandler.setSubscribePoints("/client." + clientAuth.getPhone());
            clientLoginPanel.setVisible(false);
            clientSelectDestinationPointPanel.setVisible(true);
        }
    }

    public void onClientCancelLoginButtonClick() {
        clientLoginPanel.setVisible(false);
        clientConnectRegisterPanel.setVisible(true);
    }

    public void onClientRegisterButtonClick() {
        clientConnectRegisterPanel.setVisible(false);
        clientRegisterPanel.setVisible(true);
    }

    public void onClientRegCancelButtonClick() {
        clientRegisterPanel.setVisible(false);
        clientConnectRegisterPanel.setVisible(true);
    }

    public void onClientRegOkButtonClick() {
        if (!Objects.equals(clientRegPassword1Field.getValue(), clientRegPassword2Field.getValue())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Passwords are not equal")
                    .withPosition(Notifications.Position.BOTTOM_CENTER)
                    .show();
            return;
        }
        ClientRegistrationDTO clientRegistrationDTO = new ClientRegistrationDTO();
        clientRegistrationDTO.setLogin(clientRegLoginField.getValue());
        clientRegistrationDTO.setPassword(clientRegPassword1Field.getValue());
        clientRegistrationDTO.setFirstName(clientRegFirstNameField.getValue());
        clientRegistrationDTO.setLastName(clientRegLastNameField.getValue());
        clientRegistrationDTO.setPhone(clientRegPhoneField.getValue());
        clientRegistrationDTO.setEmail(clientRegEmailField.getValue());
        ClientDTO clientDTO = new RestTemplate().postForObject(CLIENT_REGISTER_URL, clientRegistrationDTO, ClientDTO.class);

        if (clientDTO != null) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Successfully registered")
                    .withPosition(Notifications.Position.BOTTOM_CENTER)
                    .show();
            clientRegisterPanel.setVisible(false);
            clientLoginPanel.setVisible(true);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Registration error")
                    .withPosition(Notifications.Position.BOTTOM_CENTER)
                    .show();
        }
    }

    public void onTaxiConnectClick() {
        taxiConnectRegisterPanel.setVisible(false);
        taxiLoginPanel.setVisible(true);
    }

    public void onTaxiLoginButtonClick() {
        taxiStompSessionHandler = new TaxiStompSessionHandler(this::taxiMessageHandler);
        taxiAuth = loginAndGetToken(taxiLoginField.getValue(), taxiPasswordField.getValue(),
                TAXI_LOGIN_URL, TAXI_WEBSOCKET_URL, taxiStompSessionHandler);

        if (taxiAuth != null) {
            taxiStompSessionHandler.setSubscribePoints("/taxi." + taxiAuth.getPhone());
            taxiLoginPanel.setVisible(false);
            taxiWaitingForClientPanel.setVisible(true);
            taxiTimer.start();
            taxiWaitForMessageTimer.start();
            sendTaxiLocation();
        }
    }

    @Subscribe("taxiTimer")
    private void onTaxiTimerEvent(Timer.TimerActionEvent event) {
        log.info("Taxi timer send location event: {}", sendTaxiLocation());
    }

    private String sendTaxiLocation() {
        final TaxiLocationDTO taxiLocationDTO = new TaxiLocationDTO();
        taxiLocationDTO.setLocationLat(taxiPoint.getGeometry().getY());
        taxiLocationDTO.setLocationLon(taxiPoint.getGeometry().getX());

        if (orderForTaxi != null) {
            taxiLocationDTO.setPhone(orderForTaxi.getClientPhone());
        }

        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(taxiAuth.getToken());
        final HttpEntity<TaxiLocationDTO> request = new HttpEntity<>(taxiLocationDTO, headers);
        return restTemplate.postForObject(TAXI_LOCATION_URL, request, String.class);
    }

    public void onTaxiCancelLoginButtonClick() {
        taxiLoginPanel.setVisible(false);
        taxiConnectRegisterPanel.setVisible(true);
    }

    public void onTaxiRegisterButtonClick() {
        taxiConnectRegisterPanel.setVisible(false);
        if (carInfoDTO == null) {
            carInfoDTO = new RestTemplate().getForObject(TAXI_GET_CAR_INFO_URL, CarInfoDTO.class);
            if (carInfoDTO != null) {
                taxiVendorLookupField.setOptionsList(
                        carInfoDTO.getCarVendorList().stream().map(CarVendorDTO::getName).collect(Collectors.toList()));
                taxiVendorLookupField.addValueChangeListener(taxiVendorChangeValueEvent -> {
                    taxiModelLookupField.setOptionsList(carInfoDTO.getCarVendorList().stream()
                            .filter(taxiVendor -> Objects.equals(taxiVendorChangeValueEvent.getValue(), taxiVendor.getName()))
                            .flatMap(taxiVendor -> taxiVendor.getCarModelList().stream().map(CarModelDTO::getName))
                            .collect(Collectors.toList()));
                    taxiModelLookupField.setValue(null);
                });
                taxiColorLookupField.setOptionsMap(
                        Stream.of(TaxiColor.values())
                                .collect(Collectors.toMap(TaxiColor::getId, taxiColor -> taxiColor))
                );
            }
        }
        taxiRegisterPanel.setVisible(true);
    }

    public void onTaxiRegCancelButtonClick() {
        taxiRegisterPanel.setVisible(false);
        taxiConnectRegisterPanel.setVisible(true);
    }

    public void onTaxiRegOkButtonClick() {
        if (!Objects.equals(taxiRegPassword1Field.getValue(), taxiRegPassword2Field.getValue())) {
            notifications.create(Notifications.NotificationType.WARNING)
                    .withCaption("Passwords are not equal")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
            return;
        }
        TaxiRegistrationDTO taxiRegistrationDTO = new TaxiRegistrationDTO();
        taxiRegistrationDTO.setLogin(taxiRegLoginField.getValue());
        taxiRegistrationDTO.setPassword(taxiRegPassword1Field.getValue());
        taxiRegistrationDTO.setFirstName(taxiRegFirstNameField.getValue());
        taxiRegistrationDTO.setLastName(taxiRegLastNameField.getValue());
        taxiRegistrationDTO.setPhone(taxiRegPhoneField.getValue());
        taxiRegistrationDTO.setEmail(taxiRegEmailField.getValue());
        taxiRegistrationDTO.setCarColor(
                taxiColorLookupField.getValue() != null
                ? taxiColorLookupField.getValue().getId()
                : null);
        taxiRegistrationDTO.setCarVendor(taxiVendorLookupField.getValue());
        taxiRegistrationDTO.setCarModel(taxiModelLookupField.getValue());
        taxiRegistrationDTO.setCarNumber(taxiNumberField.getValue());
        TaxiDTO taxiDTO = new RestTemplate().postForObject(TAXI_REGISTER_URL, taxiRegistrationDTO, TaxiDTO.class);

        if (taxiDTO != null) {
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Successfully registered")
                    .withPosition(Notifications.Position.BOTTOM_CENTER)
                    .show();
            taxiRegisterPanel.setVisible(false);
            taxiLoginPanel.setVisible(true);
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Registration error")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
        }
    }

    private AuthResponseDTO loginAndGetToken(String login, String password, String authUrl, String wsUrl,
                                    StompSessionHandlerAdapter stompSessionHandler) {
        final RestTemplate restTemplate = new RestTemplate();
        final AuthRequestDTO authRequestDTO = new AuthRequestDTO(login, password);
        AuthResponseDTO authResponseDTO = null;

        try {
            authResponseDTO = restTemplate.postForObject(authUrl, authRequestDTO, AuthResponseDTO.class);
        } catch (Exception e) {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Login error")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
        }

        if (authResponseDTO != null) {
            final WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
            stompClient.setMessageConverter(new MappingJackson2MessageConverter());
            stompClient.connect(wsUrl, stompSessionHandler);

            notifications.create()
                    .withCaption("Login successful")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
        }
        return authResponseDTO;
    }

    private double randomDouble(double min, double max) {
        return Math.random() * (max - min) + min;
    }

    private synchronized CanvasLayer.Point addPoint(GeoMap map, double longitude, double latitude, PointStyle style) {
        CanvasLayer canvas = map.getCanvas();
        CanvasLayer.Point point = canvas.addPoint(GeometryUtils.createPoint(longitude, latitude))
                .setEditable(true);
        if (style != null) {
            point.setStyle(style);
        }
        return point;
    }

    @Subscribe("clientMap")
    public void onMapClick(GeoMap.ClickEvent event) {
        if (clientAuth != null && clientDestinationPoint == null) {
            clientDestinationPoint = addPoint(clientMap, event.getPoint().getX(), event.getPoint().getY(), null);
            sendOrderRequest(clientPoint.getGeometry().getY(), clientPoint.getGeometry().getX(),
                    clientDestinationPoint.getGeometry().getY(), clientDestinationPoint.getGeometry().getX());
        }
    }

    private void sendOrderRequest(double startLat, double startLon, double endLat, double endLon) {
        final RestTemplate restTemplate = new RestTemplate();
        final OrderRequestDTO orderRequest = new OrderRequestDTO(clientAuth.getPhone(), startLat, startLon, endLat, endLon);
        try {
            final HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientAuth.getToken());
            final HttpEntity<OrderRequestDTO> request = new HttpEntity<>(orderRequest, headers);
            clientOrderRequest = restTemplate.postForObject(CLIENT_GET_ROUTE_URL, request, OrderRequestResponseDTO.class);
            assert clientOrderRequest != null;
            clientRoute = drawRoute(clientMap, clientOrderRequest.getRoute());

            clientSelectDestinationPointPanel.setVisible(false);
            clientOrderPanel.setVisible(true);
            clientOrderRequestDescription.setValue(clientOrderDescription(
                    clientOrderRequest.getStartLat()
                    ,clientOrderRequest.getStartLon()
                    ,clientOrderRequest.getEndLat()
                    ,clientOrderRequest.getEndLon()
                    ,clientOrderRequest.getRoute().getLength()
            ));

            final Map<String, TaxiType> options = new LinkedHashMap<>();
            clientOrderRequest.getRoute().getPrices()
                    .forEach(p -> options.put(p.getTaxiType().getId() + " - " + p.getPrice() + " RUR", p.getTaxiType()));
            clientSelectTaxiTypeField.setOptionsMap(options);
            clientSelectTaxiTypeField.setNullOptionVisible(false);
            clientSelectTaxiTypeField.setValue(TaxiType.STANDARD);

        } catch (Exception e) {
            removeDestinationPointAndRoute();
            log.error("Error", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Error getting route")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
        }
    }

    private String clientOrderDescription(double startLat, double startLon, double endLat, double endLon, double length) {
        return "From ("
                + String.format("%.3f", startLat) + ":"
                + String.format("%.3f", startLon)
                + ") to ("
                + String.format("%.3f", endLat) + ":"
                + String.format("%.3f", endLon)
                + "), length: "
                + String.format("%.2f", length) + " km";
    }

    private CanvasLayer.Polyline drawRoute(GeoMap map, RouteDTO route) {
        final Coordinate[] coordinates = new Coordinate[route.getRouteParts().size()];
        int i = 0;
        for (RoutePartDTO routePartDTO: route.getRouteParts()) {
            coordinates[i++] = new CoordinateXY(routePartDTO.getLon(), routePartDTO.getLat());
        }
        final LineString polylineString = GeometryUtils.getGeometryFactory().createLineString(coordinates);
        final CanvasLayer canvas = map.getCanvas();
        CanvasLayer.Polyline polyline = canvas.addPolyline(polylineString)
                .setStyle(new PolylineStyle()
                        .setStrokeColor("#0068A3")
                        .setStrokeWeight(3)
                        .setStrokeOpacity(0.5));
        map.zoomToGeometry(polyline.getGeometry());
        return polyline;
    }

    public void onClientOrderTaxiButtonClick() {
        clientOrderPanel.setVisible(false);
        final OrderOrderDTO orderOrderDTO = new OrderOrderDTO();
        orderOrderDTO.setOrderId(clientOrderRequest.getId());
        orderOrderDTO.setTaxiType(clientSelectTaxiTypeField.getValue());

        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(clientAuth.getToken());
        final HttpEntity<OrderOrderDTO> request = new HttpEntity<>(orderOrderDTO, headers);
        try {
            String resp = restTemplate.postForObject(CLIENT_MAKE_ORDER_URL, request, String.class);
            log.info(resp);
            if ("No taxi found around".equals(resp)) {
                clientOrderPanel.setVisible(true);
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption(resp)
                        .withPosition(Notifications.Position.BOTTOM_CENTER)
                        .show();
            } else {
                clientWaitForTaxiPanel.setVisible(true);
                clientWaitForTaxiTimer.start();
            }
        } catch (Exception e) {
            log.error("Error", e);
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Order error")
                    .withPosition(Notifications.Position.BOTTOM_CENTER)
                    .show();
        }
    }

    public void onClientCancelOrderTaxiButtonClick() {
        removeDestinationPointAndRoute();
        clientOrderPanel.setVisible(false);
        clientSelectDestinationPointPanel.setVisible(true);
    }

    private void removeDestinationPointAndRoute() {
        if (clientDestinationPoint != null) {
            clientMap.getCanvas().removePoint(clientDestinationPoint);
            clientDestinationPoint = null;
        }
        if (clientRoute != null) {
            clientMap.getCanvas().removePolyline(clientRoute);
            clientRoute = null;
        }
    }

    @Subscribe("taxiWaitForMessageTimer")
    public void onTaxiWaitForMessageTimerAction(Timer.TimerActionEvent event) throws InterruptedException {
        Object messageForTaxi = getMessageForTaxi();

        if (messageForTaxi instanceof ClientOrderForTaxiDTO) {
            orderForTaxi = (ClientOrderForTaxiDTO) messageForTaxi;
            //taxiWaitForOrderTimer.stop();
            RouteDTO route = orderForTaxi.getRoute();
            double startLat = route.getRouteParts().get(0).getLat();
            double startLon = route.getRouteParts().get(0).getLon();
            double endLat = route.getRouteParts().get(route.getRouteParts().size() - 1).getLat();
            double endLon = route.getRouteParts().get(route.getRouteParts().size() - 1).getLon();
            taxiStartPoint = addPoint(taxiMap, startLon, startLat, clientPointStyle);
            taxiEndPoint = addPoint(taxiMap, endLon, endLat, null);
            taxiRoute = drawRoute(taxiMap, route);
            taxiMap.zoomToGeometry(taxiRoute.getGeometry());
            taxiClientOrderDesc.setValue(clientOrderDescription(startLat, startLon, endLat, endLon, route.getLength()));
            taxiClientOrderPrice.setValue("Price: " + orderForTaxi.getRoute().getPrices().get(0).getPrice());
            taxiWaitingForClientPanel.setVisible(false);
            taxiClientOrderPanel.setVisible(true);
            taxiClientOrderButtonsPanel.setVisible(true);
            taxiTripStartCancelPanel.setVisible(false);
        } else if (messageForTaxi instanceof OrderCancelDTO) {
            taxiReturnToWait();
            notifications.create(Notifications.NotificationType.TRAY)
                    .withCaption("Order cancelled by client")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
        }
    }

    public void onTaxiIgnoreOrderClick() {
        taxiReturnToWait();
    }

    public void onTaxiTakeOrderClick() {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(taxiAuth.getToken());
        final TaxiTakeOrderDTO taxiTakeOrderDTO = new TaxiTakeOrderDTO(orderForTaxi.getOrderId());
        final HttpEntity<TaxiTakeOrderDTO> request = new HttpEntity<>(taxiTakeOrderDTO, headers);
        TaxiTakeOrderDTO response = restTemplate.postForObject(TAXI_TAKE_ORDER_URL, request, TaxiTakeOrderDTO.class);
        if (response != null) {
            if (response.getOrderId() != null) {
                notifications.create(Notifications.NotificationType.TRAY)
                        .withCaption("Order taken OK")
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .show();
                orderForTaxi.setClientPhone(response.getPhone());
                taxiClientOrderButtonsPanel.setVisible(false);
                taxiWaitForTripEnd.setVisible(false);
                taxiTripStartCancelPanel.setVisible(true);
                taxiStartTripButton.setEnabled(false);
                executeTaxiMoving();
            } else {
                notifications.create(Notifications.NotificationType.WARNING)
                        .withCaption("It's too late")
                        .withPosition(Notifications.Position.BOTTOM_RIGHT)
                        .show();
                taxiReturnToWait();
            }
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption("Error taking order")
                    .withPosition(Notifications.Position.BOTTOM_RIGHT)
                    .show();
            taxiReturnToWait();
        }
    }

    private void executeTaxiMoving() {
        double speed = 16.67; // m/sec = 60 km/hour
        double startX = taxiPoint.getGeometry().getX();
        double startY = taxiPoint.getGeometry().getY();
        double endX = taxiStartPoint.getGeometry().getX();
        double endY = taxiStartPoint.getGeometry().getY();
        double distance = calcDistance(startX, startY, endX, endY);
        long steps = Math.round(distance/speed);
        double stepX = (endX-startX)/steps;
        double stepY = (endY-startY)/steps;

        BackgroundTask<CoordinateXY, Void> task = new BackgroundTask<CoordinateXY, Void>(1000, this) {
            @Override
            public Void run(TaskLifeCycle<CoordinateXY> taskLifeCycle) throws Exception {
                double x = startX;
                double y = startY;
                for (long i = 0; i < steps; i++) {
                    if (taskLifeCycle.isCancelled()) {
                        return null;
                    }
                    TimeUnit.SECONDS.sleep(1); // time consuming computations
                    taskLifeCycle.publish(new CoordinateXY(x += stepX, y += stepY));
                }
                TimeUnit.SECONDS.sleep(1); // time consuming computations
                return null;
            }
            @Override
            public void canceled() {
                taxiMoveTaskHandler = null;
            }
            @Override
            public void done(Void result) {
                taxiStartTripButton.setEnabled(true);
                taxiPoint = movePoint(taxiMap,
                        new CoordinateXY(taxiStartPoint.getGeometry().getCoordinate().getX(),
                                taxiStartPoint.getGeometry().getCoordinate().getY()),
                        taxiPoint, taxiPointStyle);
                sendTaxiLocation();
                taxiMoveTaskHandler = null;
            }
            @Override
            public void progress(List<CoordinateXY> changes) {
                taxiPoint = movePoint(taxiMap, changes.get(changes.size() - 1), taxiPoint, taxiPointStyle);
                sendTaxiLocation();
            }
        };
        // Get task handler object and run the task
        taxiMoveTaskHandler = backgroundWorker.handle(task);
        taxiMoveTaskHandler.execute();
    }

    private double calcDistance(double x1, double y1, double x2, double y2) {
        return Math.sqrt(square(1000*(x1-x2)/COORDS_SHIFT_1KM_X) + square(1000*(y1-y2)/COORDS_SHIFT_1KM_Y));
    }

    private double square(double num) {
        return num*num;
    }

    private synchronized CanvasLayer.Point movePoint(GeoMap map, double lon, double lat, CanvasLayer.Point point, PointStyle style) {
        return movePoint(map, new CoordinateXY(lon, lat), point, style);
    }

    private synchronized CanvasLayer.Point movePoint(GeoMap map, CoordinateXY coordinateXY,
                                                     CanvasLayer.Point point, PointStyle style) {
        map.getCanvas().removePoint(point);
        return addPoint(map, coordinateXY.getX(), coordinateXY.getY(), style);
    }

    public void onTaxiStartTripButtonClick() {

        taxiTripStartCancelPanel.setVisible(false);
        taxiWaitForTripEnd.setVisible(true);

        BackgroundTask<CoordinateXY, Void> task = new BackgroundTask<CoordinateXY, Void>(1000, this) {
            @Override
            public Void run(TaskLifeCycle<CoordinateXY> taskLifeCycle) throws Exception {
                for (int i = 0; i < orderForTaxi.getRoute().getRouteParts().size(); i++) {
                    if (taskLifeCycle.isCancelled()) {
                        return null;
                    }
                    TimeUnit.SECONDS.sleep(1); // time consuming computations
                    RoutePartDTO routePartDTO = orderForTaxi.getRoute().getRouteParts().get(i);
                    taskLifeCycle.publish(new CoordinateXY(routePartDTO.getLon(), routePartDTO.getLat()));
                }
                TimeUnit.SECONDS.sleep(1); // time consuming computations
                return null;
            }
            @Override
            public void canceled() {
                taxiMoveTaskHandler = null;
            }
            @Override
            public void done(Void result) {
                RoutePartDTO lastRoutePartDTO = orderForTaxi.getRoute().getRouteParts().get(orderForTaxi.getRoute().getRouteParts().size() - 1);
                CoordinateXY coordinateXY = new CoordinateXY(lastRoutePartDTO.getLon(), lastRoutePartDTO.getLat());

                taxiPoint = movePoint(taxiMap, coordinateXY, taxiPoint, taxiPointStyle);
                taxiReturnToWait();

                clientPoint = movePoint(clientMap, coordinateXY, clientPoint, clientPointStyle);
                clientReturnToOrder();

                taxiMoveTaskHandler = null;
            }
            @Override
            public void progress(List<CoordinateXY> changes) {
                CoordinateXY coordinateXY = changes.get(changes.size() - 1);

                taxiStartPoint = movePoint(taxiMap, coordinateXY, taxiStartPoint, clientPointStyle);
                taxiPoint = movePoint(taxiMap, coordinateXY, taxiPoint, taxiPointStyle);

                clientTaxiPoint = movePoint(clientMap, coordinateXY, clientTaxiPoint, taxiPointStyle);
                clientPoint = movePoint(clientMap, coordinateXY, clientPoint, clientPointStyle);
            }
        };
        // Get task handler object and run the task
        taxiMoveTaskHandler = backgroundWorker.handle(task);
        taxiMoveTaskHandler.execute();
    }

    private void taxiReturnToWait() {
        orderForTaxi = null;
        taxiMap.getCanvas().removePoint(taxiStartPoint);
        taxiMap.getCanvas().removePoint(taxiEndPoint);
        taxiMap.getCanvas().removePolyline(taxiRoute);
        taxiClientOrderPanel.setVisible(false);
        taxiWaitingForClientPanel.setVisible(true);
        sendTaxiStatus(TaxiStatus.FREE);
    }

    private void sendTaxiStatus(TaxiStatus status) {
        final RestTemplate restTemplate = new RestTemplate();
        final HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(taxiAuth.getToken());
        final TaxiStatusDTO taxiStatusDTO = new TaxiStatusDTO(taxiAuth.getPhone(), status);
        final HttpEntity<TaxiStatusDTO> request = new HttpEntity<>(taxiStatusDTO, headers);
        restTemplate.postForObject(TAXI_STATUS_URL, request, String.class);
    }

    private void clientReturnToOrder() {
        clientMap.getCanvas().removePoint(clientTaxiPoint);
        clientMap.getCanvas().removePoint(clientDestinationPoint);
        clientMap.getCanvas().removePolyline(clientRoute);
        clientOrderRequest = null;
        clientOrderOrder = null;
        clientReceivedOrderOrder = null;
        clientDestinationPoint = null;
        clientTaxiInfoPanel.setVisible(false);
        clientSelectDestinationPointPanel.setVisible(true);
    }

    public void onClientCancelOrderClick() {
        dialogs.createOptionDialog()
                .withCaption("Confirm please")
                .withMessage("Are you sure to cancel order?")
                .withActions(
                        new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY).withHandler(e -> {
                            sendCancelOrder();
                            clientWaitForTaxiTimer.stop();
                            clientGetTaxiLocationTimer.stop();
                            if (clientDestinationPoint != null) {
                                clientMap.getCanvas().removePoint(clientDestinationPoint);
                                clientDestinationPoint = null;
                            }
                            if (clientRoute != null) {
                                clientMap.getCanvas().removePolyline(clientRoute);
                                clientRoute = null;
                            }
                            if (clientTaxiPoint != null) {
                                clientMap.getCanvas().removePoint(clientTaxiPoint);
                                clientTaxiPoint = null;
                            }
                            clientWaitForTaxiPanel.setVisible(false);
                            clientTaxiInfoPanel.setVisible(false);
                            clientSelectDestinationPointPanel.setVisible(true);

                            notifications.create(Notifications.NotificationType.TRAY)
                                    .withCaption("Order cancelled")
                                    .withPosition(Notifications.Position.BOTTOM_CENTER)
                                    .show();
                        }),
                        new DialogAction(DialogAction.Type.NO)
                )
                .show();
    }

    private void sendCancelOrder() {
        if (clientOrderOrder != null) {
            final OrderCancelDTO orderCancelDTO = new OrderCancelDTO(clientOrderOrder.getOrderId(), clientAuth.getPhone());
            clientOrderOrder = null;
            final RestTemplate restTemplate = new RestTemplate();
            final HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(clientAuth.getToken());
            final HttpEntity<OrderCancelDTO> request = new HttpEntity<>(orderCancelDTO, headers);
            restTemplate.postForObject(CLIENT_ORDER_CANCEL_URL, request, String.class);
        }
    }

    @Subscribe("clientWaitForTaxiTimer")
    public void onClientWaitForTaxiTimerAction(Timer.TimerActionEvent event) {
        OrderOrderResponseDTO orderResponseDTO = getClientOrderOrder();
        if (orderResponseDTO != null) {
            setClientOrderOrder(null);
            clientWaitForTaxiTimer.stop();
            clientOrderOrder = orderResponseDTO;
            clientWaitForTaxiPanel.setVisible(false);
            carBrandLabel.setValue(clientOrderOrder.getCarVendor());
            carModelLabel.setValue(clientOrderOrder.getCarModel());
            carColorLabel.setValue(clientOrderOrder.getCarColor());
            carNumberLabel.setValue(clientOrderOrder.getCarNumber());
            driverNameLabel.setValue(clientOrderOrder.getDriverName());
            driverPhoneLabel.setValue(clientOrderOrder.getDriverPhone());
            clientTaxiPoint = addPoint(clientMap, clientOrderOrder.getLocationLon(), clientOrderOrder.getLocationLat(),
                    taxiPointStyle);
            clientMap.zoomToBounds(clientPoint.getGeometry(), clientTaxiPoint.getGeometry());
            clientTaxiInfoPanel.setVisible(true);
            clientFinishCancelPanel.setVisible(true);
            clientWaitForTripEnd.setVisible(false);
            clientGetTaxiLocationTimer.start();
        }
    }

    @Subscribe("clientGetTaxiLocationTimer")
    public void onClientGetTaxiLocationTimerTimerAction(Timer.TimerActionEvent event) {
        TaxiLocationDTO taxiLocationDTO = getTaxiLocation();
        if (taxiLocationDTO != null) {
            setTaxiLocation(null);
            if (clientTaxiPoint != null) {
                double lat = taxiLocationDTO.getLocationLat();
                double lon = taxiLocationDTO.getLocationLon();
                clientTaxiPoint = movePoint(clientMap, lon, lat, clientTaxiPoint, taxiPointStyle);
                if (calcDistance(clientPoint.getGeometry().getX(), clientPoint.getGeometry().getY(), lon, lat) < 20) {
                    clientFinishCancelPanel.setVisible(false);
                    clientWaitForTripEnd.setVisible(true);
                }
            }
        }
    }

    // methods for parsing of incoming websocket messages

    @SuppressWarnings("unchecked")
    private String clientMessageHandler(Object message) {
        Map<String, Object> messageMap = (Map<String, Object>) message;
        String messageClass = (String) messageMap.get("messageClass");

        if (OrderOrderResponseDTO.class.getSimpleName().equals(messageClass)) {    // it's order message
            OrderOrderResponseDTO orderResponseDTO = new OrderOrderResponseDTO();
            orderResponseDTO.setOrderId(Long.valueOf((Integer) messageMap.get("orderId")));
            orderResponseDTO.setLocationLat((Double) messageMap.get("locationLat"));
            orderResponseDTO.setLocationLon((Double) messageMap.get("locationLon"));
            orderResponseDTO.setCarType((String) messageMap.get("carType"));
            orderResponseDTO.setCarModel((String) messageMap.get("carModel"));
            orderResponseDTO.setCarVendor((String) messageMap.get("carVendor"));
            orderResponseDTO.setCarColor((String) messageMap.get("carColor"));
            orderResponseDTO.setCarNumber((String) messageMap.get("carNumber"));
            orderResponseDTO.setDriverName((String) messageMap.get("driverName"));
            orderResponseDTO.setDriverPhone((String) messageMap.get("driverPhone"));
            setClientOrderOrder(orderResponseDTO);
        } else if (TaxiLocationDTO.class.getSimpleName().equals(messageClass)) {
            TaxiLocationDTO taxiLocationDTO = new TaxiLocationDTO(
                    (Double) messageMap.get("locationLat"),
                    (Double) messageMap.get("locationLon")
            );
            setTaxiLocation(taxiLocationDTO);
        }
        return "Ok";
    }

    @SuppressWarnings("unchecked")
    private String taxiMessageHandler(Object message) {
        Map<String, Object> messageMap = (Map<String, Object>) message;
        String messageClass = (String) messageMap.get("messageClass");
        Long orderId = Long.valueOf((Integer) messageMap.get("orderId"));

        if (ClientOrderForTaxiDTO.class.getSimpleName().equals(messageClass)) {    // order message
            ClientOrderForTaxiDTO clientOrderForTaxi = new ClientOrderForTaxiDTO();
            clientOrderForTaxi.setOrderId(orderId);
            clientOrderForTaxi.setClientPhone((String) messageMap.get("clientPhone"));
            clientOrderForTaxi.setRoute(makeRoute((Map<String, Object>) messageMap.get("route")));
            addMessageForTaxi(clientOrderForTaxi);
        } else if (OrderCancelDTO.class.getSimpleName().equals(messageClass)) {    // cancel order message
            String phone = (String) messageMap.get("phone");
            if (orderForTaxi != null && orderForTaxi.getOrderId().equals(orderId)
                    && orderForTaxi.getClientPhone().equals(phone)) {
                OrderCancelDTO orderCancelDTO = new OrderCancelDTO(orderId, phone);
                addMessageForTaxi(orderCancelDTO);
                if (taxiMoveTaskHandler != null) {
                    taxiMoveTaskHandler.cancel();
                }
            }
        }
        return "Ok";
    }

    @SuppressWarnings("unchecked")
    private RouteDTO makeRoute(Map<String, Object> routeMap) {
        RouteDTO route = new RouteDTO();
        route.setLength((Double) routeMap.get("length"));
        route.setPrices(makePrices((List<Object>) routeMap.get("prices")));
        route.setRouteParts(makeRouteParts((List<Object>) routeMap.get("routeParts")));
        return route;
    }

    @SuppressWarnings("unchecked")
    private List<RoutePartDTO> makeRouteParts(List<Object> routePartList) {
        List<RoutePartDTO> routeParts = new ArrayList<>();
        routePartList.forEach(v -> {
            Map<String, Object> routePartMap = (Map<String, Object>) v;
            routeParts.add(new RoutePartDTO(
                    (Double) routePartMap.get("lat"),
                    (Double) routePartMap.get("lon"),
                    (Double) routePartMap.get("speed")
            ));
        });
        return routeParts;
    }

    @SuppressWarnings("unchecked")
    private List<RoutePriceDTO> makePrices(List<Object> pricesList) {
        List<RoutePriceDTO> routePrices = new ArrayList<>();
        pricesList.forEach(v -> {
            Map<String, Object> priceMap = (Map<String, Object>) v;
            routePrices.add(
                new RoutePriceDTO(str2TaxiType((String) priceMap.get("taxiType")), Long.valueOf((Integer) priceMap.get("price")))
            );
        });
        return routePrices;
    }

    private TaxiType str2TaxiType(String taxiTypeStr) {
        switch (taxiTypeStr) {
            case "STANDARD": return TaxiType.STANDARD;
            case "COMFORT": return TaxiType.COMFORT;
            case "BUSINESS": return TaxiType.BUSINESS;
            case "PREMIUM": return TaxiType.PREMIUM;
        }
        return null;
    }

    public void onTaxiCancelOrderButtonClick() {
    }
}
