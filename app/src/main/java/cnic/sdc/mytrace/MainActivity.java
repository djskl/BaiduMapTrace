package cnic.sdc.mytrace;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    MapView mMapView = null;
    BaiduMap mBaiduMap = null;
    Overlay last_track = null;

    LocationService locationService;
    LatLng lastPnt = null;
    List<LatLng> pointList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //在使用SDK各组件之前初始化context信息，传入ApplicationContext
        //注意该方法要再setContentView方法之前实现
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();

        //开启定位图层
        mBaiduMap.setMyLocationEnabled(true);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //填充选项菜单（读取XML文件、解析、加载到Menu组件上）
        getMenuInflater().inflate(R.menu.map_btn, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.get_offline_map:  //管理离线地图
                Intent offmap = new Intent(MainActivity.this, OfflineMap.class);
                startActivity(offmap);
                break;

            default:
                break;
        }
        return true;
    }

    @Override
    protected void onStop() {
        locationService.unregisterListener(mListener); //注销掉监听
        locationService.stop(); //停止定位服务
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //获取locationservice实例，建议应用中只初始化1个location实例，然后使用
        //可以参考其他示例的activity，都是通过此种方式获取locationservice实例的
        locationService = ((LocationApplication) getApplication()).locationService;

        locationService.registerListener(mListener); //注册监听

        int type = getIntent().getIntExtra("from", 0);
        if (type == 0) {
            locationService.setLocationOption(locationService.getDefaultLocationClientOption());
        } else if (type == 1) {
            locationService.setLocationOption(locationService.getOption());
        }

        //启动定位服务,此时百度地图开始每隔一定时间(setScanSpan)就发起一次定位请求
        locationService.start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationService.stop(); //停止定位服务
        mBaiduMap.setMyLocationEnabled(false);  // 关闭定位图层
        mMapView.onDestroy();
        mMapView = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume(); //在activity执行onResume时执行mMapView.onResume()
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause(); //在activity执行onPause时执行mMapView.onPause ()
    }

    private BDLocationListener mListener = new BDLocationListener() {

        @Override
        public void onReceiveLocation(BDLocation location) {
            if (null != location && location.getLocType() != BDLocation.TypeServerError) {

                MyLocationData locData = new MyLocationData.Builder()
                        .accuracy(location.getRadius())
                        // 此处设置开发者获取到的方向信息，顺时针0-360
                        .direction(100).latitude(location.getLatitude())
                        .longitude(location.getLongitude()).build();

                mBaiduMap.setMyLocationData(locData);
                LatLng ll = new LatLng(location.getLatitude(),
                        location.getLongitude());
                MapStatus.Builder builder = new MapStatus.Builder();
                //builder.target(ll).zoom(18.0f);
                builder.target(ll);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

                LatLng pnt = new LatLng(location.getLatitude(), location.getLongitude());

                if(lastPnt == null){
                    lastPnt = pnt;
                    pointList.add(pnt);
                    return;
                }

                pointList.add(pnt);
                PolylineOptions polyline = new PolylineOptions().width(10).color(Color.RED).points(pointList);
                Overlay track = mBaiduMap.addOverlay(polyline);
                if(last_track != null){ //每增加一个点就新绘制一条路径, 然后删除上一个点对应的路径
                    last_track.remove();
                }
                last_track = track;

                if (location.getLocType() == BDLocation.TypeServerError) {
                    Toast.makeText(getApplicationContext(), "服务端网络定位失败，可以反馈IMEI号和大体定位时间到loc-bugs@baidu.com，会有人追查原因", Toast.LENGTH_LONG).show();
                } else if (location.getLocType() == BDLocation.TypeNetWorkException) {
                    Toast.makeText(getApplicationContext(), "网络不同导致定位失败，请检查网络是否通畅", Toast.LENGTH_LONG).show();
                } else if (location.getLocType() == BDLocation.TypeCriteriaException) {
                    Toast.makeText(getApplicationContext(), "无法获取有效定位依据导致定位失败，一般是由于手机的原因，处于飞行模式下一般会造成这种结果，可以试着重启手机", Toast.LENGTH_LONG).show();
                }
            }
        }
    };

}
