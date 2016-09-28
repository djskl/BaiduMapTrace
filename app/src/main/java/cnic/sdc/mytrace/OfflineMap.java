package cnic.sdc.mytrace;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.baidu.mapapi.map.offline.MKOLSearchRecord;
import com.baidu.mapapi.map.offline.MKOLUpdateElement;
import com.baidu.mapapi.map.offline.MKOfflineMap;
import com.baidu.mapapi.map.offline.MKOfflineMapListener;

import java.util.ArrayList;

/**
 * 离线地图的下载和显示
 * 如果检测本地有离线地图可用，优先使用本地的离线地图
 * 离线的地图的下载过程(指定下载目录，更新下载进度等)不需要用户参与
 */
public class OfflineMap extends Activity implements MKOfflineMapListener {

    /**
     * 离线城市
     */
    private class CityItem{
        public int ID = -1;             //城市ID,百度地图为每个城市都编了唯一的ID
        public String name = null;      //城市名称
        public boolean update = false;  //当前的离线地图是否需要更新
        public double ratio = 0;        //下载进度
    }

    private MKOfflineMap mOffline = null;   //离线地图服务,用于管理离线地图.

    private ArrayList<CityItem> localMapList = new ArrayList<>();   //已下载的离线地图

    private LocalMapAdapter lAdapter = null;    //绑定视图与localMapList的列表适配器

    private SearchView mSearchView;     //android自带的搜索控件

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_offline);

        //初始化离线地图服务
        mOffline = new MKOfflineMap();
        mOffline.init(this);

        //初始化当前页面
        initView();

    }

    private void initView() {
        mSearchView = (SearchView) findViewById(R.id.search_city);

        //监听来自mSearchView的事件
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            /**
             * 点击键盘上的“搜索”按钮时触发该方法
             */
            @Override
            public boolean onQueryTextSubmit(String query) {
                ArrayList<MKOLSearchRecord> records = mOffline.searchCity(query);
                if (records == null || records.size() != 1) {
                    return false;
                }
                MKOLSearchRecord record = records.get(0);

                CityItem ct = new CityItem();
                ct.ID = record.cityID;
                ct.name = record.cityName;

                localMapList.add(0, ct);
                lAdapter.notifyDataSetChanged();

                return false;
            }

            // 当搜索内容改变时触发该方法
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        // 获取已下过的离线地图信息
        ArrayList<MKOLUpdateElement> locals = mOffline.getAllUpdateInfo();
        if(locals != null){
            for(int idx=0;idx<locals.size();idx++){
                MKOLUpdateElement mke = locals.get(idx);
                CityItem ct = new CityItem();
                ct.ID = mke.cityID;
                ct.name = mke.cityName;
                ct.ratio = mke.ratio;
                ct.update = mke.update;
                localMapList.add(ct);
            }
        }

        ListView localMapListView = (ListView) findViewById(R.id.localmaplist);
        lAdapter = new LocalMapAdapter();
        localMapListView.setAdapter(lAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        mOffline.destroy(); //退出时，销毁离线地图模块
        super.onDestroy();
    }

    @Override
    public void onGetOfflineMapState(int type, int state) {
        switch (type) {
            case MKOfflineMap.TYPE_DOWNLOAD_UPDATE: {
                MKOLUpdateElement update = mOffline.getUpdateInfo(state);
                // 处理下载进度更新提示
                if (update != null) {
                    updateView();
                }
            }
                break;
            case MKOfflineMap.TYPE_NEW_OFFLINE:
                // 有新离线地图安装
                Log.d("OfflineDemo", String.format("add offlinemap num:%d", state));
                break;
            case MKOfflineMap.TYPE_VER_UPDATE:
                // 版本更新提示
                // MKOLUpdateElement e = mOffline.getUpdateInfo(state);

                break;
            default:
                break;
        }
    }

    /**
     * 更新状态显示
     */
    public void updateView() {
        ArrayList<MKOLUpdateElement> locals = mOffline.getAllUpdateInfo();
        for(int idx=0;idx<locals.size();idx++){
            MKOLUpdateElement mke = locals.get(idx);
            for(int idy=0;idy<localMapList.size();idy++){
                CityItem ct = localMapList.get(idy);
                if(ct.ID == mke.cityID){
                    ct.ratio = mke.ratio;
                    ct.update = mke.update;
                }
            }
        }
        lAdapter.notifyDataSetChanged();
    }

    /**
     * 离线地图管理列表适配器
     */
    public class LocalMapAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return localMapList.size();
        }

        @Override
        public Object getItem(int index) {
            return localMapList.get(index);
        }

        @Override
        public long getItemId(int index) {
            return index;
        }

        @Override
        public View getView(int index, View view, ViewGroup arg2) {
            CityItem e = (CityItem) getItem(index);
            view = View.inflate(OfflineMap.this, R.layout.offline_localmap_list, null);
            initViewItem(view, e);
            return view;
        }

        void initViewItem(View view, final CityItem e) {

            TextView title = (TextView) view.findViewById(R.id.cityName);
            title.setText(e.name);

            TextView ratio = (TextView) view.findViewById(R.id.cityRatio);
            ratio.setText(e.ratio + "%");

            TextView update = (TextView) view.findViewById(R.id.cityVersion);
            if (e.update) {
                update.setText("可更新");
            } else {
                update.setText("最新版");
            }

            final Button stop_btn = (Button) view.findViewById(R.id.stop_download);
            stop_btn.setTag(e.ID);

            final Button start_btn = (Button) view.findViewById(R.id.start_download);
            start_btn.setTag(e.ID);


            stop_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int cityID = (int) view.getTag();
                    mOffline.pause(cityID);
                    updateView();
                }
            });

            start_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int cityID = (int) view.getTag();
                    mOffline.start(cityID);
                    updateView();
                }
            });

            MKOLUpdateElement item = mOffline.getUpdateInfo(e.ID);
            if(item != null){
                if(item.status == MKOLUpdateElement.DOWNLOADING){
                    start_btn.setVisibility(View.GONE);
                    stop_btn.setVisibility(View.VISIBLE);
                }
                if(item.status == MKOLUpdateElement.SUSPENDED){
                    start_btn.setVisibility(View.VISIBLE);
                    stop_btn.setVisibility(View.GONE);
                }
                if(item.status == MKOLUpdateElement.FINISHED){
                    start_btn.setVisibility(View.GONE);
                    stop_btn.setVisibility(View.GONE);
                }
            }

            if(e.ratio == 100){
                start_btn.setVisibility(View.GONE);
                stop_btn.setVisibility(View.GONE);
            }

            Button remove_btn = (Button) view.findViewById(R.id.cityRemoveBtn);
            remove_btn.setTag(e.ID);
            remove_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {    //删除离线地图
                    int cityID = (int) arg0.getTag();
                    mOffline.remove(cityID);
                    int idx = 0;
                    for(;idx<localMapList.size();idx++){
                        CityItem ct = localMapList.get(idx);
                        if(ct.ID == cityID){
                            break;
                        }
                    }
                    if(idx < localMapList.size()){
                        localMapList.remove(idx);
                        lAdapter.notifyDataSetChanged();
                    }
                }
            });

        }
    }

}
