package com.namgil.map.mapservicememo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.ClusterManager;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity {

    private final int MY_PERMISSION_REQUEST_SMSSEND = 100;
    private GoogleMap mMap;
    private Context mContext = this;
    private LocationManager mLocMan;
    private String mProvider;

    private Spinner selectProviderSpinner;
    //마커 텍스트
    private TextView tv_marker;
    //클릭한 마커
    private Marker selectedMarker;
    //커스텀 마커뷰
    private View marker_root_view;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission(); // 퍼미션 체크
        }
        setContentView(R.layout.activity_maps);
        mLocMan = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        lnitMap(); // 맵 가져오기

        Handler mHandler = new Handler();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showCurrentPosition(); //현재 위치
            }
        });

        selectProvider(); //스피너 초기화 및 위치제공자 가져오기
    }

    /**
     * 맵 초기화
     */
    public void lnitMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mMap = googleMap;
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mMap.setMyLocationEnabled(true);
                final ClusterManager<MarkerItem> mClusterManager = new ClusterManager<>(mContext,mMap);
                mMap.setOnCameraIdleListener(mClusterManager);

                mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
                    @Override
                    public boolean onMarkerClick(Marker marker) {
                        CameraUpdate center = CameraUpdateFactory.newLatLng(marker.getPosition());
                        mMap.animateCamera(center);
                        changeSelectedMarker(marker);
                        return true;

                    }

                });
                mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                    @Override
                    public void onMapClick(LatLng latLng) {
                        changeSelectedMarker(null);

                        final double lat = latLng.latitude;
                        final double lon = latLng.longitude;

                        showAlert(lat, lon);
//                        mClusterManager.addItem(new MarkerItem(latLng));
                    }
                });

                setCustomMarkerView();



            }
        });
        TextView providers = (TextView) findViewById(R.id.providers);
        providers.setText(mLocMan.getProviders(true).toString());
    }

    /**
     * 맵에서 마커 찍을떄 보여주는 메모 다이얼로그창
     *
     * @param lat
     * @param lon
     */
    public void showAlert(final double lat, final double lon) {
        AlertDialog.Builder alert = new AlertDialog.Builder(mContext);
        View innerView = getLayoutInflater().inflate(R.layout.dialog_input, null);
        alert.setTitle("메모 입력").setView(innerView);

        final EditText memo = (EditText) innerView.findViewById(R.id.memo);

        final AlertDialog alertDialog = alert.create();
        Button btnCancel = (Button) innerView.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        //확인버튼
        Button btnSuccess = (Button) innerView.findViewById(R.id.btnSuccess);
        btnSuccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addMarker(new MarkerItem(lat, lon, memo.getText().toString()), false);
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    /**
     * 프로바이더 선택
     */
    public void selectProvider() {
        final List<String> poviders = mLocMan.getAllProviders();
        selectProviderSpinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext, R.array.provider, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        selectProviderSpinner.setAdapter(adapter);

        selectProviderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        changeProvider(poviders.get(position));
                        Toast.makeText(mContext, poviders.get(position) + "를 선택하였습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case 1:
                        changeProvider(poviders.get(position));
                        Toast.makeText(mContext, poviders.get(position) + "를 선택하였습니다", Toast.LENGTH_SHORT).show();
                        break;
                    case 2:
                        changeProvider(poviders.get(position));
                        Toast.makeText(mContext, poviders.get(position) + "를 선택하였습니다", Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * 위치 제공자 선택하기
     *
     * @param name
     */
    public void changeProvider(String name) {
        mLocMan.removeUpdates(mListener);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        String provider = "";
        switch (name) {
            case "passive":
                provider = LocationManager.PASSIVE_PROVIDER;
                break;
            case "gps":
                provider = LocationManager.GPS_PROVIDER;
                break;
            case "network":
                provider = LocationManager.NETWORK_PROVIDER;
                break;
            default:
                break;
        }

        mLocMan.requestLocationUpdates(provider, 100, 1, mListener);
    }


    /**
     * 현재 위치 가져오기
     */
    public void showCurrentPosition() {
        mProvider = mLocMan.getBestProvider(new Criteria(), true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
//        mLocMan.requestLocationUpdates(mProvider, 100, 1, mListener);
    }


    /**
     * 퍼미션 체크
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        if (checkSelfPermission(Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(Manifest.permission.SEND_SMS)) {
                // Explain to the user why we need to write the permission.
                Toast.makeText(this, "Location", Toast.LENGTH_SHORT).show();
            }

            requestPermissions(new String[]{Manifest.permission.INTERNET, Manifest.permission.ACCESS_NETWORK_STATE
                    , Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_SMSSEND);


        } else {
            // 다음 부분은 항상 허용일 경우에 해당이 됩니다.
        }
    }


    /**
     * location 리스너
     */
    LocationListener mListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());
            CameraPosition cp = new CameraPosition.Builder().target(currentPosition).zoom(18).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cp));
//            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPosition,16));

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Toast.makeText(mContext, provider + "로 변경되었습니다", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };


    /**
     * 마커 클릭 제어 메소드
     * @param marker
     */
    private void changeSelectedMarker(Marker marker) {
        // 선택했던 마커 되돌리기
        if (selectedMarker != null) {
            addMarker(selectedMarker, false);
            selectedMarker.remove();
        } // 선택한 마커 표시
        if (marker != null) {
            selectedMarker = addMarker(marker, true);
            marker.remove();
        }
    }

    /**
     * 마커 커스텀
     */
    private void setCustomMarkerView() {
        marker_root_view = LayoutInflater.from(this).inflate(R.layout.marker_layout, null);
        tv_marker = (TextView) marker_root_view.findViewById(R.id.tv_marker);
    }

    /**
     *선택된 마커 추가
     * @param marker
     * @param isSelectedMarker
     * @return
     */
    private Marker addMarker(Marker marker, boolean isSelectedMarker) {
        double lat = marker.getPosition().latitude;
        double lon = marker.getPosition().longitude;
        String memo = marker.getTitle();
        MarkerItem temp = new MarkerItem(lat, lon, memo);
        return addMarker(temp, isSelectedMarker);
    }


    /**
     * 마커 정보 가져와서 맵에 반환하는 메소드
     *
     * @param markerItem
     * @param isSelectedMarker
     * @return
     */
    private Marker addMarker(MarkerItem markerItem, boolean isSelectedMarker) {

        LatLng position = new LatLng(markerItem.getLat(), markerItem.getLon());
        String memo = markerItem.getMemo();

        tv_marker.setText(memo);

        if (isSelectedMarker) {
            tv_marker.setBackgroundResource(R.drawable.ic_marker_phone_blue);
            tv_marker.setTextColor(Color.WHITE);
        } else {
            tv_marker.setBackgroundResource(R.drawable.ic_marker_phone);
            tv_marker.setTextColor(Color.BLACK);
        }

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.title(memo);
        markerOptions.position(position);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(createDrawableFromView(this, marker_root_view)));

        return mMap.addMarker(markerOptions);

    }
    /**
     * view를 bitmap으로 변환
     *
     * @param context
     * @param view
     * @return
     */
    private Bitmap createDrawableFromView(Context context, View view) {
        //해상도 구하기
        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //

        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        view.measure(displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.layout(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels);
        view.buildDrawingCache(); //뷰 이미지를 Drawing cache에 저장

        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888); // ARGB_8888 = Bitmap이 투명도를 가짐
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }
}