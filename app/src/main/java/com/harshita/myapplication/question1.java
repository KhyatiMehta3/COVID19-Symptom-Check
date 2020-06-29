
package com.harshita.myapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.harshita.myapplication.models.ChatMessage;
import com.harshita.myapplication.views.ChatView;

import org.json.JSONArray;
import org.w3c.dom.Text;

import java.util.Objects;

import static com.harshita.myapplication.models.ChatMessage.Type.RECEIVED;
import static com.harshita.myapplication.models.ChatMessage.Type.SENT;

public class question1  extends AppCompatActivity implements View.OnClickListener {

    private ChatView chatView1;
    private JSONObject covidObject = new JSONObject();
    private JSONObject apiResponse = new JSONObject();
    private MutableLiveData<JSONObject> responseAlert = new MutableLiveData<>();
    //private String agebyuser;
    private  JSONArray evidence = new JSONArray(); //Array for storing id and choice
    private String singleQuestionChoiceId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.question1);

        chatView1 = findViewById(R.id.chat_view);

        chatView1.addMessage(new ChatMessage("Please Select Your Gender",System.currentTimeMillis(), RECEIVED));
        chatView1.addMessage(new ChatMessage(question_1(), System.currentTimeMillis(), SENT));
        chatView1.setOnClickListener(this);

//        chatView1.setOnSentMessageListener(new ChatView.OnSentMessageListener() {
//            @Override
//            public boolean sendMessage(ChatMessage chatMessage) {
//                String message = chatMessage.getMessage().toLowerCase();
//                switch (message){
//                    case "yes":
//                    case "no":
//                        evidence.put()
//                        break;
//                    default:
//                        Toast.makeText(question1.this, "Please say yes or no", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        responseAlert.observe(this, new Observer<JSONObject>() {
            @Override
            public void onChanged(JSONObject jsonObject) {
                displayQuestions();
            }
        });
    }

    private View question_1(){
        //Sample view given here
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.sample_layout, null);
    }


    private void displayQuestions(){
        WorkManager workManager = WorkManager.getInstance(this);
        Data inputData = new Data.Builder().putString("apiResponse",apiResponse.toString()).build();

        @SuppressLint("RestrictedApi") OneTimeWorkRequest displayNextQuestionRequest =
                new OneTimeWorkRequest.Builder(IncomingMessageDisplayer.class)
                        .setInputData(inputData)
                        .build();
        workManager.enqueueUniqueWork("displayNextQuestion",ExistingWorkPolicy.REPLACE, displayNextQuestionRequest);

        workManager.getWorkInfoByIdLiveData(displayNextQuestionRequest.getId()).observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                if(workInfo!=null){
                    if(workInfo.getState().equals(WorkInfo.State.SUCCEEDED)){

                        Data outputDataFromWorker = workInfo.getOutputData();

                        chatView1.addMessage(new ChatMessage(outputDataFromWorker.getString("nextQuestion"), System.currentTimeMillis(), RECEIVED));
                        String items = outputDataFromWorker.getString("items");
                        switch(Objects.requireNonNull(outputDataFromWorker.getString("type"))){
                            case "group_multiple":
                                chatView1.addMessage(new ChatMessage(groupMultipleTypeView(items),System.currentTimeMillis(), SENT));
                                break;
                            case "single":
                                chatView1.addMessage(new ChatMessage(singleTypeView(items),System.currentTimeMillis(), SENT));
                                break;
                            case "group_single":
                                chatView1.addMessage(new ChatMessage(groupSingleTypeView(items),System.currentTimeMillis(), SENT));
                                break;
                        }
                    }
                    else{
                        getvr(); // calling triage
                    }
                }
            }
        });

    }
    public <name> void getvr()
    {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "https://api.infermedica.com/covid19/triage";
        final LinearLayout resultHolder = new LinearLayout(this);
        resultHolder.setOrientation(LinearLayout.VERTICAL);

        final TextView resultView = new TextView(this);
       // bgmp.setText("Next"); // Button for storing value in evidence[]
        JsonObjectRequest getRequest = new JsonObjectRequest(
                Request.Method.POST
                ,  url,covidObject, new com.android.volley.Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response){
                try {
                    Process process = Runtime.getRuntime().exec("logcat -d");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    StringBuilder log=new StringBuilder();
                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        log.append(line);
                    }
                    resultView.setText(response.getString("description"));
                    resultHolder.addView(resultView);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                Log.d("description", response.toString());
                //textView.setText(response);

                try {
                    String text = response.getString("text");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new com.android.volley.Response.ErrorListener()
        {
            @Override
            public void onErrorResponse(VolleyError error)
            {
                Log.d("ERROR","error => "+error.toString());
                error.printStackTrace();

            }
        }){


            @Override
            public Map<String, String> getHeaders() throws AuthFailureError
            {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("Content-Type","application/json");
                params.put("App-Id", "fd9740f0");
                params.put("App-Key", "369c63fe2ab752b87ac60189d368a7e1");

                return params;
            }
        };
        queue.add(getRequest);
    }

    private String idExtractor(JSONArray itemsArray, int index) throws Exception{
        return itemsArray.getJSONObject(index).getString("id");
    }
    private void getAPIJson(){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "https://api.infermedica.com/covid19/diagnosis";
            Log.wtf("covid obejct", covidObject.toString());

            JsonObjectRequest getRequest = new JsonObjectRequest(
                    Request.Method.POST
                    ,url,covidObject, new com.android.volley.Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response)
                {
                    apiResponse = response;
                    responseAlert.setValue(apiResponse);

                    Log.wtf("question", response.toString());

                }
            }, new com.android.volley.Response.ErrorListener()
            {
                @Override
                public void onErrorResponse(VolleyError error)
                {
                    Log.d("ERROR","error => "+error.toString());
                    error.printStackTrace();

                }
            }){
                @Override
                public Map<String, String> getHeaders()
                {
                    Map<String, String>  params = new HashMap<String, String>();
                    params.put("Content-Type","application/json");
                    params.put("App-Id", "fd9740f0");
                    params.put("App-Key", "369c63fe2ab752b87ac60189d368a7e1");

                    return params;
                }
            };
           //return getRequest;
            queue.add(getRequest);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private View groupMultipleTypeView(String items){
        try {
            final JSONArray itemsArray = new JSONArray(items);
            final LinearLayout checkboxHolder = new LinearLayout(this);
            checkboxHolder.setOrientation(LinearLayout.VERTICAL);

            Button bgmp = new Button(this);
            bgmp.setText("Next"); // Button for storing value in evidence[]

            for(int i=0; i<itemsArray.length();i++){
                CheckBox selectableCheckBox = new CheckBox(this);
                selectableCheckBox.setText(itemsArray.getJSONObject(i).getString("name"));
                checkboxHolder.addView(selectableCheckBox,i);
            }

            checkboxHolder.addView(bgmp,itemsArray.length()); // Adding button to view

            bgmp.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    for(int i=0;i<itemsArray.length();i++){
                        try{
                            evidence.put(getEvidenceSubJson(idExtractor(itemsArray,i),
                                    ((CheckBox)checkboxHolder.getChildAt(i)).isChecked()? "present": "absent"));
                        }catch (Exception e){e.printStackTrace();}
                    }
                    addEvidence();
                    getAPIJson();
                }
            });

            return checkboxHolder;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addEvidence(){
        // question4 line 209
        try {
            covidObject.put("evidence",evidence);
            Log.wtf("object here",evidence.toString());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private View singleTypeView(final String items){
        try{
            final JSONArray itemsArray = new JSONArray(items).getJSONObject(0).getJSONArray("choices");
            RadioGroup radiobuttonHolder = new RadioGroup(this);
            radiobuttonHolder.setOrientation(LinearLayout.VERTICAL);

            for(int i=0; i<itemsArray.length();i++){
                RadioButton selectableRadioButton = new RadioButton(this);
                selectableRadioButton.setId(i);
                selectableRadioButton.setText(itemsArray.getJSONObject(i).getString("label"));
                radiobuttonHolder.addView(selectableRadioButton,i);
            }
            radiobuttonHolder.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    //TODO:Take the selected item's id and append to the evidence json
                    try{
                        String choice_id = ((RadioButton)group.getChildAt(checkedId)).getText().toString().toLowerCase().equals("yes") ? "present" : "absent";
                        evidence.put(getEvidenceSubJson(idExtractor(new JSONArray(items),0),choice_id));

                        addEvidence();
                        getAPIJson();

                    }catch (Exception e){e.printStackTrace();}
                }
            });
            return radiobuttonHolder;
        }catch (Exception e){e.printStackTrace();}
        return null;
    }

    private View groupSingleTypeView(String items){
        try {
            final JSONArray itemsArray = new JSONArray(items);
            final RadioGroup radiobuttonHolder = new RadioGroup(this);
            radiobuttonHolder.setOrientation(LinearLayout.VERTICAL);
            Button bgmp = new Button(this);
            bgmp.setText("Next"); // Button for storing value in evidence[]


            for(int i=0; i<itemsArray.length();i++){
                RadioButton selectableRadioButton = new RadioButton(this);
                selectableRadioButton.setId(i);

                selectableRadioButton.setText(itemsArray.getJSONObject(i).getString("name"));
                radiobuttonHolder.addView(selectableRadioButton,i);
            }
            radiobuttonHolder.addView(bgmp,itemsArray.length()); // Adding button to view

            bgmp.setOnClickListener( new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    for(int i=0;i<itemsArray.length();i++){
                        try{
                            evidence.put(getEvidenceSubJson(idExtractor(itemsArray,i),
                                    ((RadioButton)radiobuttonHolder.getChildAt(i)).isChecked()? "present": "absent"));
                        }catch (Exception e){e.printStackTrace();}
                    }
                    addEvidence();
                    getAPIJson();
                }
            });

            return radiobuttonHolder;

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getEvidenceSubJson(String id, String choiceId) throws Exception{
        JSONObject evidence_subJson = new JSONObject();
        evidence_subJson.put("id",id);
        evidence_subJson.put("choice_id",choiceId);
        return evidence_subJson;
    }

    
    private void ageQuestion(){
        chatView1.addMessage(new ChatMessage("What is your age?", System.currentTimeMillis(), ChatMessage.Type.RECEIVED));
        chatView1.setTypingListener(new ChatView.TypingListener(){
            @Override
            public void userStartedTyping(){
                // will be called when the user starts typing
            }

            @Override
            public void userStoppedTyping(){
                // will be called when the user stops typing
            }
        });
    }




    @Override
    public void onClick(View v) {
        try{
            switch (v.getId()){
                case R.id.genderNextButton:
                    RadioButton m =findViewById(R.id.male);
                    RadioButton f =findViewById(R.id.female);
                    if(m.isChecked()){
                    covidObject.put("sex","male");
                    covidObject.put("age",21);
                        covidObject.put("evidence", new JSONArray());}
                    else if(f.isChecked()){
                        covidObject.put("sex","female");
                        covidObject.put("age",21);
                        covidObject.put("evidence", new JSONArray()); }
                    else{
                        Toast.makeText(question1.this, "Select male or female!", Toast.LENGTH_SHORT).show();

                    }
                    break;
            }
            getAPIJson();
        }catch (Exception e){e.printStackTrace();}
    }
}

