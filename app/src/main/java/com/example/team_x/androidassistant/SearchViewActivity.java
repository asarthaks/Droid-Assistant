package com.example.team_x.androidassistant;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.app.SearchableInfo;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class SearchViewActivity extends ActionBarActivity {

    TextView resultText;
    RelativeLayout rl;
    LinearLayout ll1;
    LinearLayout ll2;
    SearchView searchView ;
    TextToSpeech textToSpeech;
    SpeechRecognizer speechRecognizer;
    EditText writeText;
    Button submit;
    TextView spokenSpeech;
    String searchQuery;
    int requestForSearch;
    Button speakButton;

    private final int REQ_CODE_SPEECH_INPUT = 100;
    private final int REQ_CODE_CONTACT_SEARCH = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_view);
        searchView = (SearchView)findViewById(R.id.searchView);
        resultText = (TextView) findViewById(R.id.searchViewResult);
        rl = (RelativeLayout)findViewById(R.id.rl);
        ll1 = (LinearLayout)findViewById(R.id.ll1);
        ll2 = (LinearLayout)findViewById(R.id.ll2);
        ll1.removeView(searchView);
        speakButton = (Button)findViewById(R.id.speakButton);




        //Permissions for Marshmallow and above
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            allowMarshmallowPermissions();
        }



        writeText = (EditText) findViewById(R.id.writeText);
        submit = (Button) findViewById(R.id.submit);
        spokenSpeech = (TextView) findViewById(R.id.spokenSpeech);




        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    textToSpeech.setLanguage(Locale.US);
                }
            }
        });

//        intro();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String toSpeak = writeText.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);
            }
        });



    }


//    private void intro(){
//        textToSpeech.speak("Welcome , Click the speak button to enter commands", TextToSpeech.QUEUE_FLUSH, null);
//    }

    private void setupSearchView() {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView.setQuery(searchQuery , true);
        SearchableInfo searchableInfo = searchManager.getSearchableInfo(getComponentName());
        searchView.setSearchableInfo(searchableInfo);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (ContactsContract.Intents.SEARCH_SUGGESTION_CLICKED.equals(intent.getAction())) {
            //handles suggestion clicked query

            String displayName = getDisplayNameForContact(intent);
            String displayNumber = getNumberContact(intent ,displayName);
            String contactNum = displayNumber.substring(displayNumber.length() - 10);
            resultText.setText(contactNum);

            //Cases for whatsapp msg/calling/sms
            switch (requestForSearch){
                case 1:


                    Toast.makeText(this, contactNum, Toast.LENGTH_SHORT).show();
                    String smsNumber = ("91" + contactNum);


                    try {

                        Uri uri = Uri.parse("smsto:" + contactNum);
                        Intent sendIntent = new Intent(Intent.ACTION_SENDTO, uri);
                        sendIntent.setPackage("com.whatsapp");
                        textToSpeech.speak("opening whatsapp chat with " + displayName.toString(), TextToSpeech.QUEUE_FLUSH, null);
//                        sendIntent.putExtra(Intent.EXTRA_TEXT, "This is my text to send.");           //not working for some reason
                        startActivity(sendIntent);
                        finish();

                    } catch (Exception e) {
                        Log.e("error while sending", e.toString());
                        Toast.makeText(this, "Error/n" + e.toString(), Toast.LENGTH_SHORT).show();
                    }
                break;

                case 2:

                    Intent my_callIntent = new Intent(Intent.ACTION_CALL);
                    my_callIntent.setData(Uri.parse("tel:" + contactNum));
                    //here the word 'tel' is important for making a call...
                    startActivity(my_callIntent);
                    Toast.makeText(this, "Calling " + contactNum.toString(), Toast.LENGTH_SHORT).show();
                    textToSpeech.speak("Calling " + displayName.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    finish();
                    break;


                case 3:

                    try {
                        sendSMS(contactNum, writeText.getText().toString());
                        Toast.makeText(this, "Message Sent to " + displayName.toString(), Toast.LENGTH_LONG).show();
                        textToSpeech.speak("Message Sent to " + displayName.toString(), TextToSpeech.QUEUE_FLUSH, null);
                    } catch (Exception e) {
                        Log.e("error while sending", e.toString());
                        Toast.makeText(this, "Error/n" + e.toString(), Toast.LENGTH_SHORT).show();
                    }
                    finish();
                    break;
            }

        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // handles a search query
            String query = intent.getStringExtra(SearchManager.QUERY);
            resultText.setText("should search for query: '" + query + "'...");
        }
    }
    //return Contact Name
    private String getDisplayNameForContact(Intent intent) {
        Cursor phoneCursor = getContentResolver().query(intent.getData(), null, null, null, null);
        phoneCursor.moveToFirst();
        int idDisplayName = phoneCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
        String name = phoneCursor.getString(idDisplayName);
        phoneCursor.close();
        return name;
    }

    //return Phone Number
    private String getNumberContact(Intent intent , String name){
        //
        //  Find contact based on name.
        //
        String number;
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, null,
                "DISPLAY_NAME = '" + name + "'", null, null);
        if (cursor.moveToFirst()) {
            String contactId =
                    cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
            //
            //  Get all phone numbers.
            //
            Cursor phones = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contactId, null, null);
            while (phones.moveToNext()) {
                number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                int type = phones.getInt(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                switch (type) {
                    case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
                        // do something with the Home number here...
                        break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
                        // do something with the Mobile number here...
                        break;
                    case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
                        // do something with the Work number here...
                        break;
                }
                name = number;
            }

            phones.close();

        }
        cursor.close();
        return name;
    }


    //Speak Button Clicked
    public void speakButtonClicked(View view) {
        promptSpeechInput();
    }


    //setting up voice typing
    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say Something");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(),"Sorry! Your device doesn\\'t support speech input",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT:
                if (resultCode == RESULT_OK && null != data) {

                    ArrayList<String> result = data
                            .getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    spokenSpeech.setText(result.get(0));
                    if (result.get(0).equalsIgnoreCase("open whatsapp")) {
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
                        startActivity(launchIntent);
                    }

                        //whatsapp chat
                    if (result.get(0).contains("send free message to")) {
                        String[] split = result.get(0).split("to");
                        String num = split[1];

                        ll1.addView(searchView);
                        ll2.removeView(speakButton);
                        resultText.setText("Click to see results !");
                        textToSpeech.speak("Click the magnifying glass to see results matching your query", TextToSpeech.QUEUE_FLUSH, null);
                        Toast.makeText(this,"Click the magnifying glass to see results matching your query", Toast.LENGTH_SHORT).show();
                        searchQuery = num;
                        requestForSearch = 1;
                        setupSearchView();

                    }

                        //calling
                    if (result.get(0).contains("call")) {
                        String[] split = result.get(0).split("call");
                        String num = split[1];

                        ll1.addView(searchView);
                        ll2.removeView(speakButton);
                        resultText.setText("Click to see results !");
                        textToSpeech.speak("Click the magnifying glass to see results matching your query", TextToSpeech.QUEUE_FLUSH, null);
                        Toast.makeText(this,"Click the magnifying glass to see results matching your query", Toast.LENGTH_SHORT).show();
                        searchQuery = num;
                        requestForSearch = 2;
                        setupSearchView();

                    }

                        //sms
                    if (result.get(0).contains("send simple message to")) {
                        String[] split = result.get(0).split("to");
                        String num = split[1];

                        ll1.addView(searchView);
                        ll2.removeView(speakButton);
                        resultText.setText("Click to see results !");
                        textToSpeech.speak("Click the magnifying glass to see results matching your query", TextToSpeech.QUEUE_FLUSH, null);
                        Toast.makeText(this,"Click the magnifying glass to see results matching your query", Toast.LENGTH_SHORT).show();
                        searchQuery = num;
                        requestForSearch = 3;
                        setupSearchView();

                    }

                        //sms body
                    if (result.get(0).contains("set message to")) {
                        String[] split = result.get(0).split("to");
                        String body = split[1];
                        writeText.setText(body);
                        textToSpeech.speak("message body set !", TextToSpeech.QUEUE_FLUSH, null);
                        Toast.makeText(this, "message body set !", Toast.LENGTH_SHORT).show();
                    }

                        //camera
                    if (result.get(0).contains("open camera")) {
                        Intent launchIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivity(launchIntent);
                        textToSpeech.speak("Opening Camera", TextToSpeech.QUEUE_FLUSH, null);
                        Toast.makeText(this, "Opening Camera", Toast.LENGTH_SHORT).show();
                    }

                        //music
                    if (result.get(0).contains("open music")) {
                        Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.musixmatch.android.lyrify");
                        startActivity(launchIntent);
                        textToSpeech.speak("Opening MusixMatch", TextToSpeech.QUEUE_FLUSH, null);
                        Toast.makeText(this, "Opening MusixMatch", Toast.LENGTH_SHORT).show();
                    }

                    else{
                        Toast.makeText(this, "Command not recognized , try again      ", Toast.LENGTH_SHORT).show();
                    }


                }






                break;

        }


    }




    private void sendSMS(String phoneNumber, String message)
    {
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);

    }






    public void allowMarshmallowPermissions() {
        // The request code used in ActivityCompat.requestPermissions()
        // and returned in the Activity's onRequestPermissionsResult()
        int PERMISSION_ALL = 1;
        String[] PERMISSIONS = {Manifest.permission.READ_CONTACTS,Manifest.permission.CAMERA,
                Manifest.permission.SEND_SMS,Manifest.permission.CALL_PHONE}; //Add permission in manifest and here also. Code will take care of asking for permission

        if (!hasPermissions(SearchViewActivity.this, PERMISSIONS)) {
            try {
                ActivityCompat.requestPermissions(SearchViewActivity.this, PERMISSIONS, PERMISSION_ALL);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else {
            return;
        }
    }

    public static boolean hasPermissions(Context context, String... permissions) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
                for (String permission : permissions) {
                    if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                        return false;
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return true;
    }


}

