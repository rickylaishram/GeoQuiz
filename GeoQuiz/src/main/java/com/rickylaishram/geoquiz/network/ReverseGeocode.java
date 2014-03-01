package com.rickylaishram.geoquiz.network;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.JsonArray;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by rickylaishram on 3/1/14.
 */
public class ReverseGeocode {
    private String URL = "https://maps.googleapis.com/maps/api/geocode/json?sensor=false&latlng=";
    private Boolean MATCH = false;

    public Boolean check(LatLng location, final String answer) {
        ArrayList<String> addresses = new ArrayList<String>();

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(URL+location.latitude+","+location.longitude, new AsyncHttpResponseHandler(){

            @Override
            public void onStart() {
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody){
                try {
                    JSONObject resp = new JSONObject(new String(responseBody));
                    JSONArray results = resp.getJSONArray("results");

                    for(int i = 0; i<results.length(); i++) {
                        JSONArray addresses = results.getJSONObject(i).getJSONArray("address_components");

                        //Log.v("Address Components", addresses.getJSONObject(j).getString("long_name"));

                        for(int j = 0; i< addresses.length(); i++) {
                            Log.v("Address Components j", addresses.getJSONObject(j).getString("long_name"));
                            if(addresses.getJSONObject(j).getString("long_name").equals(answer)) {
                                MATCH = true;
                            }
                        }
                    }

                } catch (Exception e) {

                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error){
                Log.e("Error", "Error");
            }

            @Override
            public void onRetry() {
                // Request was retried
            }

            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                // Progress notification
            }

            @Override
            public void onFinish() {
            }
        });

        return MATCH;
    }
}
