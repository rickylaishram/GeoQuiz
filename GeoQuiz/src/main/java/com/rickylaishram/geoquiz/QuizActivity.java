package com.rickylaishram.geoquiz;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.ResponseHandlerInterface;
import com.rickylaishram.geoquiz.data.QuestionAnswer;
import com.rickylaishram.geoquiz.util.Questions;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Random;

public class QuizActivity extends Activity {

    private GoogleMap mMap;
    private TextView tvQuestion;
    private TextView tvQNo;
    private TextView tvPoints;
    private ImageButton btnSubmit;
    private Context ctx;
    private ArrayList<Questions> data = new ArrayList<Questions>();
    private ArrayList<Integer> asked = new ArrayList<Integer>();
    private int TOTAL_COUNT = 0;
    private int POINTS = 0;
    private int CURRENT = 0;
    private int INDEX = 0;
    private LatLng LOCATION;
    private Boolean CORRECT;
    private int MAX_QUESTIONS = 5; // Max no of questions per quiz

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ctx = this;

        setContentView(R.layout.activity_quiz);
        getActionBar().hide();

        setUi();
        initilizeMap();
        initializeQuestions();

        setQuestion();
    }

    private void initilizeMap() {
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.setOnMapClickListener(mapClick);
    }

    private void setUi() {
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        tvQNo = (TextView) findViewById(R.id.tv_qno);
        tvPoints = (TextView) findViewById(R.id.tv_points);
        tvQuestion = (TextView) findViewById(R.id.tv_question);
        btnSubmit = (ImageButton) findViewById(R.id.btn_submit);

        btnSubmit.setOnClickListener(submitHandler);
    }

    private void initializeQuestions() {
        String[] questions = ctx.getResources().getStringArray(R.array.questions);
        String[] answers = ctx.getResources().getStringArray(R.array.answers);

        TOTAL_COUNT = questions.length;

        for (int i=0; i<TOTAL_COUNT; i++) {
            Questions q = new Questions(questions[i], "", answers[i]);
            data.add(q);
        }
    }

    private int getRandomIndex() {
        Double rand = (Math.random()*TOTAL_COUNT);
        int index = 0;

        if(asked.contains(rand.intValue())) {
            getRandomIndex();
        } else {
            index = rand.intValue();
            asked.add(index);
        }

        return  index;
    }

    private void setQuestion(){
        INDEX = getRandomIndex();
        Questions d = data.get(INDEX);

        mMap.clear();
        buttonToggle(false);
        tvQuestion.setText(d.question);
        tvPoints.setText("Points: "+POINTS+"/"+CURRENT);
        tvQNo.setText("Question: "+(CURRENT+1));
    }

    private void buttonToggle(Boolean state) {
        if(state) {
            btnSubmit.setClickable(true);
            btnSubmit.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
        } else {
            btnSubmit.setClickable(false);
            btnSubmit.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }
    }


    // Handler for map click
    private GoogleMap.OnMapClickListener mapClick = new GoogleMap.OnMapClickListener() {
        @Override
        public void onMapClick(LatLng latLng) {
            LOCATION = latLng;
            // Remove existing markers
            mMap.clear();
            // Drop new marker
            mMap.addMarker(new MarkerOptions().position(LOCATION));

            // Set submit button enable
            buttonToggle(true);
        }
    };

    // Handler for submit button
    private View.OnClickListener submitHandler = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ReverseGeocode r = new ReverseGeocode();
            r.check(LOCATION, data.get(INDEX).answer);
        }
    };

    private void nextQuestion(){
        CURRENT++;
        if(CORRECT) {
            POINTS ++;
        }
        if(CURRENT < MAX_QUESTIONS) {
            setQuestion();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle("Score");
            builder.setMessage(POINTS+"/"+MAX_QUESTIONS);
            builder.setPositiveButton("Restart?", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    asked.clear();
                    CURRENT = 0;
                    POINTS = 0;
                    setQuestion();
                }
            });
            builder.create().show();
        }
    }

    public class ReverseGeocode {
        private String URL = "https://maps.googleapis.com/maps/api/geocode/json?sensor=false&latlng=";
        private Boolean MATCH = false;

        public Boolean check(LatLng location, final String answer) {
            ArrayList<String> addresses = new ArrayList<String>();

            AsyncHttpClient client = new AsyncHttpClient();
            client.get(URL+location.latitude+","+location.longitude, new AsyncHttpResponseHandler(){

                ProgressDialog progressDialog = new ProgressDialog(ctx);

                @Override
                public void onStart() {
                    progressDialog.setMessage("Checking your answer");
                    progressDialog.show();
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody){
                    Boolean match = false;

                    try {
                        JSONObject resp = new JSONObject(new String(responseBody));
                        JSONArray results = resp.getJSONArray("results");

                        for(int i = 0; i<results.length(); i++) {
                            JSONArray addresses = results.getJSONObject(i).getJSONArray("address_components");

                            //Log.v("Address Components", addresses.getJSONObject(j).getString("long_name"));

                            for(int j = 0; i< addresses.length(); j++) {
                                Log.v("Address Components j", addresses.getJSONObject(j).getString("long_name"));
                                if(addresses.getJSONObject(j).getString("long_name").equals(answer)) {
                                    match = true;
                                    break;
                                }
                            }
                        }

                    } catch (Exception e) {

                    }

                    progressDialog.hide();

                    if(match) {
                        CORRECT = true;
                    } else {
                        CORRECT = false;
                    }

                    nextQuestion();
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

}