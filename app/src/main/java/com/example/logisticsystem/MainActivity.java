package com.example.logisticsystem;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yandex.mapkit.GeoObjectCollection;
import com.yandex.mapkit.MapKit;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.RequestPoint;
import com.yandex.mapkit.RequestPointType;
import com.yandex.mapkit.directions.DirectionsFactory;
import com.yandex.mapkit.directions.driving.DrivingOptions;
import com.yandex.mapkit.directions.driving.DrivingRoute;
import com.yandex.mapkit.directions.driving.DrivingRouter;
import com.yandex.mapkit.directions.driving.DrivingSession;
import com.yandex.mapkit.directions.driving.VehicleOptions;
import com.yandex.mapkit.geometry.Geo;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.layers.ObjectEvent;
import com.yandex.mapkit.logo.Alignment;
import com.yandex.mapkit.logo.HorizontalAlignment;
import com.yandex.mapkit.logo.VerticalAlignment;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.MapObjectCollection;
import com.yandex.mapkit.map.VisibleRegionUtils;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.mapkit.search.Response;
import com.yandex.mapkit.search.SearchFactory;
import com.yandex.mapkit.search.SearchManager;
import com.yandex.mapkit.search.SearchManagerType;
import com.yandex.mapkit.search.SearchOptions;
import com.yandex.mapkit.search.Session;
import com.yandex.mapkit.user_location.UserLocationLayer;
import com.yandex.mapkit.user_location.UserLocationObjectListener;
import com.yandex.mapkit.user_location.UserLocationView;
import com.yandex.runtime.Error;
import com.yandex.runtime.image.ImageProvider;
import com.yandex.runtime.network.NetworkError;
import com.yandex.runtime.network.RemoteError;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/**
 * Класс используется как основная форма приложения, предназначен для работы с картой и маршрутами.
 * @see android.app.Activity
 * @see com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
 * @see com.yandex.mapkit.search.Session.SearchListener
 * @see com.yandex.mapkit.user_location.UserLocationObjectListener
 */
public class MainActivity extends Activity implements UserLocationObjectListener, Session.SearchListener, DrivingSession.DrivingRouteListener {
    private static final int PERMISSIONS_REQUEST_FINE_LOCATION = 1;
    private MapView mapView;
    private UserLocationLayer userLocationLayer;
    private SearchManager searchManager;
    private DrivingRouter drivingRouter;
    private final ArrayList<OrderItem> items = new ArrayList<>();
    private OrderItem currentItem;
    private final Point currentLocation = new Point(45.067392, 38.977882);

    /**
     * Расчет расстояния по формуле Хаверсина
     * @param from Первая точка
     * @param to Вторая точка
     * @return Расстояние между точками в километроах
     */
    public double distance(Point from, Point to){
        final double R = 6372.8;
        double dLat = Math.toRadians(to.getLatitude() - from.getLatitude());
        double dLon = Math.toRadians(to.getLongitude() - from.getLongitude());
        double lat1 = Math.toRadians(from.getLatitude());
        double lat2 = Math.toRadians(to.getLatitude());
        double a = Math.pow(Math.sin(dLat/2),2) + Math.pow(Math.cos(lat1)*Math.cos(lat2)*Math.sin(dLon/2),2);
        double c = 2*Math.asin(Math.sqrt(a));
        return R*c;
    }

    /**
     * Начинает пострение маршрута из нынешнего расположения, включает поиск координаты расположения по адресу
     * @param position Адрес точки назначения
     */
    public void routingStart(String position) {
        searchManager.submit(
                position, VisibleRegionUtils.toPolygon(mapView.getMap().getVisibleRegion()),
                new SearchOptions(), this);
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Orders");
        reference.child(Long.toString(currentItem.Id)).child("Status").setValue((long)1);
        reference.child(Long.toString(currentItem.Id))
                .child("TakeDate").setValue(Calendar.getInstance().getTime().toString());
        findViewById(R.id.btnAuth).setEnabled(false);
        findViewById(R.id.data_items).setVisibility(View.GONE);
        findViewById(R.id.confirmMenu).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.txtMainDir)).setText(String
                .format("%s, %s", currentItem.CurrentOrder.Address, currentItem.CurrentOrder.City));
        ((TextView) findViewById(R.id.txtMainDistance)).setText("5.4 км.");
    }

    /**
     * Инициализирует класс заказа и добавляет заказ в список
     * @param data Запрос с таблицей заказов
     * @param id Код добавляемого заказа
     */
    public void addItem(DataSnapshot data, int id){
        Order order = data.getValue(Order.class);
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (order == null){
            return;
        }
        assert user != null;
        if (Objects.equals(order.UserId, user.getUid()) && order.Status == 0){
            OrderItem item = new OrderItem(this, order, id);
            LinearLayout dataItems = findViewById(R.id.data_items);
            dataItems.addView(item);
            items.add(item);
        }
    }

    /**
     * Обновляет список заказов
     */
    public void updateItemsList(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Orders");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                findViewById(R.id.warnContainer).setVisibility(View.GONE);
                ((LinearLayout)findViewById(R.id.data_items)).removeAllViews();
                items.clear();
                for (int i = 0; i <= (int)dataSnapshot.getChildrenCount(); i++){
                    addItem(dataSnapshot.child(Long.toString(i)), i);
                }
                if (items.size() == 0){
                    findViewById(R.id.warnContainer).setVisibility(View.VISIBLE);
                    ((TextView)findViewById(R.id.txtWarn)).setText("Заказы не найдены");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getBaseContext(),"Failed to read value." + error.toException(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Выбирает указанный заказ
     * @param item Заказ, который будет выбран
     */
    public void selectItem(OrderItem item){
        for (OrderItem i : items) {
            i.Disactivate();
        }
        item.Activate();
        currentItem = item;
    }

    /**
     * Предназначен для получения разрешения на получение данных о геопозиции с устройства
     */
    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                "android.permission.ACCESS_FINE_LOCATION")
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{"android.permission.ACCESS_FINE_LOCATION"},
                    PERMISSIONS_REQUEST_FINE_LOCATION);
        }
    }

    /**
     * Вызывается при создании activity в первый раз.
     * @param savedInstanceState Если действие повторно инициализируется после предыдущего закрытия,
     *                           тогда этот пакет содержит данные, которые он предоставил последним.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SearchFactory.initialize(this);
        DirectionsFactory.initialize(this);

        setContentView(R.layout.activity_main);
        super.onCreate(savedInstanceState);

        searchManager = SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED);
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter();

        MapKitFactory.initialize(this);
        mapView = findViewById(R.id.mapview);
        mapView.getMap().getLogo().setAlignment(
                new Alignment(HorizontalAlignment.RIGHT, VerticalAlignment.TOP));
        mapView.getMap().getMapObjects().addPlacemark(currentLocation,
                ImageProvider.fromResource(this, R.drawable.location_color));
        requestLocationPermission();

        MapKit mapKit = MapKitFactory.getInstance();
        mapKit.resetLocationManagerToDefault();
        userLocationLayer = mapKit.createUserLocationLayer(mapView.getMapWindow());
        mapView.getMap().move(new CameraPosition(currentLocation, 14.0f, 0.0f, 0.0f));

        findViewById(R.id.btnAuth).setOnClickListener(view ->
                startActivity(new Intent(this, LoginActivity.class)));

        findViewById(R.id.btnSelect).setOnClickListener(view -> {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference("/Orders");
            reference.child(Long.toString(currentItem.Id))
                    .child("Status").setValue((long)2);
            reference.child(Long.toString(currentItem.Id))
                    .child("CompleteDate").setValue(Calendar.getInstance().getTime().toString());
            findViewById(R.id.btnAuth).setEnabled(true);
            findViewById(R.id.data_items).setVisibility(View.VISIBLE);
            findViewById(R.id.confirmMenu).setVisibility(View.GONE);
            Toast.makeText(this, "Заказ завершен", Toast.LENGTH_SHORT).show();
            mapView.getMap().getMapObjects().clear();
            mapView.getMap().getMapObjects().addPlacemark(currentLocation,
                    ImageProvider.fromResource(this, R.drawable.location_color));
        });
    }

    /**
     * Вызывается при начале работы activity.
     */
    @Override
    protected void onStart() {
        super.onStart();
        MapKitFactory.getInstance().onStart();
        mapView.onStart();

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if(user != null){
            findViewById(R.id.warnContainer).setVisibility(View.GONE);
            updateItemsList();
        }
        else {
            findViewById(R.id.warnContainer).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.txtWarn)).setText("Требуется авторизация");
        }
    }

    /**
     * Вызывается при окончании работы activity.
     */
    @Override
    protected void onStop() {
        mapView.onStop();
        MapKitFactory.getInstance().onStop();
        super.onStop();
    }

    /**
     * Вызывается при добавлении объекта слоя. Он вызывается один раз, когда значок местоположения пользователя появляется в первый раз.
     * @param userLocationView Ответ с экземпляром расположения пользовательского слоя
     */
    @Override
    public void onObjectAdded(UserLocationView userLocationView) {
        userLocationLayer.setAnchor(
                new PointF((float)(mapView.getWidth() * 0.5), (float)(mapView.getHeight() * 0.5)),
                new PointF((float)(mapView.getWidth() * 0.5), (float)(mapView.getHeight() * 0.83)));

        userLocationView.getArrow().setIcon(ImageProvider.fromResource(
                this, R.drawable.location_color));

        userLocationView.getPin().setIcon(ImageProvider.fromResource(
                this, R.drawable.location_color));

        userLocationView.getAccuracyCircle().setFillColor(Color.BLUE);
    }

    /**
     * Вызывается при удалении объекта слоя. Он никогда не вызывается для значка местоположения пользователя.
     * @param view Ответ с экземпляром расположения пользовательского слоя
     */
    @Override
    public void onObjectRemoved(@NonNull UserLocationView view) {
    }

    /**
     * Вызывается при обновлении объекта слоя.
     * @param view Ответ с экземпляром расположения пользовательского слоя
     * @param event Ответ с событием обновления
     */
    @Override
    public void onObjectUpdated(@NonNull UserLocationView view, @NonNull ObjectEvent event) {
    }

    /**
     * Обратный вызов для обработки результатов поиска.
     * @param response Ответ с результатом поиска.
     */
    @Override
    public void onSearchResponse(@NonNull Response response) {
        GeoObjectCollection.Item searchResult = response.getCollection().getChildren().get(0);
        Point resultLocation = Objects.requireNonNull(searchResult.getObj()).getGeometry().get(0).getPoint();
        if (resultLocation != null) {
            mapView.getMap().getMapObjects().addPlacemark(resultLocation,
                    ImageProvider.fromResource(this, R.mipmap.result_icon));
            currentItem.Distance = distance(currentLocation, resultLocation);
            mapView.getMap().move(new CameraPosition(resultLocation, 14.0f, 0.0f, 0.0f));
            ArrayList<RequestPoint> requestPoints = new ArrayList<>();
            requestPoints.add(new RequestPoint(
                    currentLocation,
                    RequestPointType.WAYPOINT,
                    null));
            requestPoints.add(new RequestPoint(
                    resultLocation,
                    RequestPointType.WAYPOINT,
                    null));
            drivingRouter.requestRoutes(requestPoints, new DrivingOptions(), new VehicleOptions(), this);
        }
    }

    /**
     * Обратный вызов для обработки ошибок.
     * @param error Информация об ошибке.
     */
    @Override
    public void onSearchError(@NonNull Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     * Вызывается при создании маршрутов движения.
     * @param list Список с точками маршрута.
     */
    @Override
    public void onDrivingRoutes(@NonNull List<DrivingRoute> list) {
        for (DrivingRoute route : list) {
            mapView.getMap().getMapObjects().addPolyline(route.getGeometry());
        }
    }

    /**
     * Обратный вызов для обработки ошибок.
     * @param error Информация об ошибке.
     */
    @Override
    public void onDrivingRoutesError(@NonNull Error error) {
        String errorMessage = getString(R.string.unknown_error_message);
        if (error instanceof RemoteError) {
            errorMessage = getString(R.string.remote_error_message);
        } else if (error instanceof NetworkError) {
            errorMessage = getString(R.string.network_error_message);
        }

        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
    }
}