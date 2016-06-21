package com.example.user.seemarket;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslError;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.ValueCallback;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xwalk.core.XWalkCookieManager;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkView;
import org.xwalk.core.XWalkWebResourceRequest;
import org.xwalk.core.XWalkWebResourceResponse;
import org.xwalk.core.JavascriptInterface;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;


public class FullscreenActivity extends AppCompatActivity {

    private XWalkView mXWalkView;
    private WifiManager mWifiManager;
    private XWalkCookieManager mCookieManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_fullscreen);
        //wifi
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if(!mWifiManager.isWifiEnabled()){
            mWifiManager.setWifiEnabled(true);
        }

        //accept cookie
        mCookieManager = new XWalkCookieManager();
        mCookieManager.setAcceptCookie(true);
        //crosswalk view
        mXWalkView = (XWalkView)findViewById(R.id.webView);
        mXWalkView.setResourceClient(new XWalkResource(mXWalkView));
        mXWalkView.addJavascriptInterface(new AndroidAPI(), "android");
        mXWalkView.load("https://luffy.ee.ncku.edu.tw:8013/", null);

    }



    private class AndroidAPI{

        private List<ScanResult> scanResult;
        private JSONObject GPSData;
        private HashMap<Integer, HashMap<String, Integer>> WifiMap = new HashMap<Integer, HashMap<String, Integer>>();
        private HashMap<Integer, HashMap<Integer, Integer>> fixedTable = new HashMap<Integer,HashMap<Integer, Integer>>();

        private int bulidPointTable(){
            if(!WifiMap.isEmpty()){

                fixedTable = new HashMap<Integer, HashMap<Integer, Integer>>();
                for(int i=0;i<WifiMap.size();i++){
                    fixedTable.put(i, new HashMap<Integer, Integer>());
                }
            }
            return fixedTable.size();
        }

        private int searchMinmumError(int range){
            int id = -1;
            boolean[] err;
            for(int i = 0;i<fixedTable.size();i++){
                if(fixedTable.get(i).size() < 3 && fixedTable.get(i).size() > 0){
                    err = new boolean[fixedTable.get(i).size()];
                }
                else if(fixedTable.get(i).size() >= 3){
                    err = new boolean[3];
                }
                else
                    continue;

                for(int j = 0; j<err.length;j++){
                    err[j] = false;
                }
                int head = 0;
                for(int j=0;j<fixedTable.get(i).size();j++){
                    if(fixedTable.get(i).get(j) <= range ){
                        if(head < err.length) {
                            err[head] = true;
                            head++;
                        }
                    }
                }
                if(err[err.length-1]==true){
                    id = i;
                    break;
                }
            }
            return id;
        }


        @org.xwalk.core.JavascriptInterface
        public int threePointFix(int range){
            mWifiManager.startScan();
            scanResult = mWifiManager.getScanResults();
            bulidPointTable();
            for(int i=0;i<WifiMap.size();i++){
                HashMap<String, Integer> SSIDLevel = WifiMap.get(i);
                int index = 0;
                for(int j=0;j<scanResult.size();j++){
                    if(SSIDLevel.get(scanResult.get(j).SSID) != null){
                        int err = Math.abs(SSIDLevel.get(scanResult.get(j).SSID) - scanResult.get(j).level);
                        fixedTable.get(i).put(index++, err);

                    }
                }
            }
            int locationId = searchMinmumError(range);
            return  locationId;
        }

        @org.xwalk.core.JavascriptInterface
        public String saveGPSData(String jsonStr){
            try {
                GPSData = new JSONObject(jsonStr);
                JSONArray data = GPSData.getJSONArray("data");
                for(int i = 0;i < data.length();i++){
                    HashMap<String, Integer> wifi = new HashMap<String, Integer>();
                    JSONObject wifiLevel = data.getJSONObject(i).getJSONObject("wifiLevel");
                    Iterator<String> wifiKey = wifiLevel.keys();
                    while(wifiKey.hasNext()){
                        String key = wifiKey.next();
                        wifi.put(key, wifiLevel.getInt(key));
                    }
                    WifiMap.put(i, wifi);
                }
                return  "Data mapping";
            }
            catch (JSONException e){
                return "Json error";
            }
            catch(Exception e){
                return  "Other error";
            }
        }

    }

    class XWalkResource extends XWalkResourceClient {
        public XWalkResource(XWalkView xwalkView) {
            super(xwalkView);
        }
        public XWalkWebResourceResponse shouldInterceptLoadRequest(
                XWalkView view, XWalkWebResourceRequest request) {
            return super.shouldInterceptLoadRequest(view, request);
        }

        public void onReceivedLoadError(XWalkView view, int errorCode, String description,
                                        String failingUrl) {
            super.onReceivedLoadError(view, errorCode, description, failingUrl);
        }
        public void onReceivedSslError(XWalkView view, ValueCallback<Boolean> callback, SslError error) {
            callback.onReceiveValue(true);
        }

    }

}
