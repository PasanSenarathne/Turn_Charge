package com.isprid.turnchargelk.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.api.Status;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.isprid.turnchargelk.R;
import com.isprid.turnchargelk.model.EvPlace;
import com.isprid.turnchargelk.model.MyDestination;
import com.isprid.turnchargelk.service.BatterySimulateService;
import com.isprid.turnchargelk.util.AppConfig;
import com.isprid.turnchargelk.util.Constant;
import com.isprid.turnchargelk.util.EVDataProvider;
import com.isprid.turnchargelk.util.ObdHelper;
import com.isprid.turnchargelk.util.PrefUtils;
import com.sohrab.obd.reader.application.ObdPreferences;
import com.sohrab.obd.reader.obdCommand.ObdCommand;
import com.sohrab.obd.reader.obdCommand.ObdConfiguration;
import com.sohrab.obd.reader.obdCommand.SpeedCommand;
import com.sohrab.obd.reader.obdCommand.engine.RPMCommand;
import com.sohrab.obd.reader.service.ObdReaderService;
import com.sohrab.obd.reader.trip.TripRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.isprid.turnchargelk.service.BatterySimulateService.ACTION_BATTERY_DRAIN;
import static com.sohrab.obd.reader.constants.DefineObdReader.ACTION_OBD_CONNECTION_STATUS;
import static com.sohrab.obd.reader.constants.DefineObdReader.ACTION_READ_OBD_REAL_TIME_DATA;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int AUTOCOMPLETE_REQUEST_CODE = 2;
    Intent mIntent = new Intent(ACTION_BATTERY_DRAIN);
    private PrefUtils pref;
    private TextView text_connection;
    private TextView textSpeed, textRpm;
    private TextView textBatteryLevel, textDrivingDistance;
    private Button btn_connect;
    private ProgressDialog progress;
    private EditText edt_destination;
    private Place place;
    private boolean isObdActive = false;

    /**
     * Broadcast Receiver to receive OBD connection status and real time data
     */
    private final BroadcastReceiver mObdReaderReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(ACTION_OBD_CONNECTION_STATUS)) {

                String connectionStatusMsg = intent.getStringExtra(ObdReaderService.INTENT_OBD_EXTRA_DATA);
                text_connection.setText(connectionStatusMsg);

                if (connectionStatusMsg.equals(getString(R.string.obd_connected))) {
                    //OBD connected  do what want after OBD connection
                    isObdActive = true;
                    dismissLoadingDialog();
                    btn_connect.setVisibility(View.GONE);
                    if (ObdHelper.getBatteryLevel() != -1) {
                        initBatteryIndicator();
                        startService(new Intent(MainActivity.this, BatterySimulateService.class));
                    }
                    Toast.makeText(MainActivity.this, "OBD Connected", Toast.LENGTH_SHORT).show();
                } else if (connectionStatusMsg.equals(getString(R.string.connect_lost))) {
                    //OBD disconnected  do what want after OBD disconnection
                    btn_connect.setVisibility(View.VISIBLE);
                    isObdActive = false;
                    dismissLoadingDialog();
                    resetFields();
                    stopService(new Intent(MainActivity.this, ObdReaderService.class));
                    stopService(new Intent(MainActivity.this, BatterySimulateService.class));
                } else {
                    // here you could check OBD connection and pairing status
                    btn_connect.setVisibility(View.VISIBLE);
                    isObdActive = false;
                    dismissLoadingDialog();
                    resetFields();
                    stopService(new Intent(MainActivity.this, ObdReaderService.class));
                    stopService(new Intent(MainActivity.this, BatterySimulateService.class));
                }

            } else if (action.equals(ACTION_READ_OBD_REAL_TIME_DATA)) {
                TripRecord tripRecord = TripRecord.getTripRecode(MainActivity.this);
                // here you can fetch real time data from TripRecord using getter methods like
                textSpeed.setText(tripRecord.getSpeed().toString());
                textRpm.setText(tripRecord.getEngineRpm());

                isObdActive = true;
                dismissLoadingDialog();

                try {
                    if (ObdHelper.getBatteryLevel() != -1) {
                        if (Integer.parseInt(tripRecord.getEngineRpm()) > 3500 && Integer.parseInt(tripRecord.getEngineRpm()) <= 7200) {
                            mIntent.putExtra(Constant.EXTRA_DRAIN, 2);
                            sendBroadcast(mIntent);
                        }

                        if (tripRecord.getSpeed() > 120) {
                            mIntent.putExtra(Constant.EXTRA_DRAIN, 2);
                            sendBroadcast(mIntent);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    };
    private final BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //update your GUI fields
            updateGUI(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Places.
        Places.initialize(getApplicationContext(), AppConfig.MAP_API_KEY);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        pref = new PrefUtils(this);
        text_connection = findViewById(R.id.text_connection);

        textSpeed = findViewById(R.id.text_speed);
        textRpm = findViewById(R.id.text_rpm);
        textBatteryLevel = findViewById(R.id.text_battery_level);
        textDrivingDistance = findViewById(R.id.text_driving_distance);
        btn_connect = findViewById(R.id.btn_connect);


        //configure obd: add required command in arrayList and set to ObdConfiguration.
        //If you don't set any command or passing null, then all command OBD command will be requested.
        //here, it is recommended to set command that is required only like belows commented line.
        ArrayList<ObdCommand> obdCommands = new ArrayList<>();
        obdCommands.add(new SpeedCommand());
        obdCommands.add(new RPMCommand());
        ObdConfiguration.setmObdCommands(this, obdCommands);

        // passing null means we are executing all OBD command for now, but you should add required command for fast retrieval like above commented lines.
        ObdConfiguration.setmObdCommands(this, null);

        // Register receiver with some action related to OBD connection status
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_READ_OBD_REAL_TIME_DATA);
        intentFilter.addAction(ACTION_OBD_CONNECTION_STATUS);
        registerReceiver(mObdReaderReceiver, intentFilter);

        btn_connect.setOnClickListener(v -> {
            //start service which will execute in background for connecting and execute command until you stop
            startService(new Intent(MainActivity.this, ObdReaderService.class));
            showLoadingDialog();
        });


        textBatteryLevel.setOnClickListener(v -> setBatteryLevel());

//        registerChargingStations();
    }

    private void registerChargingStations() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference(Constant.EV_PLACES);
        databaseReference.removeValue();

        for (EvPlace place : EVDataProvider.getChargingStationList()) {
            String tagKey = databaseReference.push().getKey();
            assert tagKey != null;
            databaseReference.child(tagKey).setValue(place);
        }
    }

    private void resetFields() {
        textSpeed.setText("N/A");
        textRpm.setText("N/A");
        textBatteryLevel.setText("N/A");
        textDrivingDistance.setText("N/A");
    }

    private void initBatteryIndicator() {
        textBatteryLevel.setText(ObdHelper.getBatteryLevel() + "%");
        textDrivingDistance.setText(ObdHelper.getDrivingDistance() + "Km");
    }

    private void setBatteryLevel() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("Battery Level");
        LayoutInflater li = LayoutInflater.from(getApplicationContext());
        View promptsView = li.inflate(R.layout.alert_dialog, null);
        alertDialog.setView(promptsView);

        final EditText userInput = (EditText) promptsView.findViewById(R.id.etUserInput);

        alertDialog.setPositiveButton("SET",
                (dialog, which) -> {
                    int batteryLevel = Integer.parseInt(userInput.getText().toString());
                    if (batteryLevel >= 0 && batteryLevel <= 100) {
                        ObdHelper.setBatteryLevel(batteryLevel);
                        ObdHelper.setDrivingDistance(ObdHelper.getDrivingDistance(ObdHelper.getBatteryLevel()));

                        if (isObdActive) {
                            initBatteryIndicator();

                            stopService(new Intent(MainActivity.this, BatterySimulateService.class));
                            startService(new Intent(this, BatterySimulateService.class));
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid battery level. The battery level should be between 0 and 100", Toast.LENGTH_SHORT).show();
                    }
                });

        alertDialog.setNegativeButton("NO",
                (dialog, which) -> dialog.cancel());

        alertDialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_logout) {
            new android.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.logout))
                    .setMessage(getString(R.string.exit_warning))

                    // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(R.string.logout, (dialog1, which) -> {
                        pref.removeKeyFromPrefs(MainActivity.this, Constant.UUID);
                        pref.removeKeyFromPrefs(MainActivity.this, Constant.PHONE);
                        pref.removeKeyFromPrefs(MainActivity.this, Constant.IS_LOGGED_IN);
                        startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                        finish();
                    })

                    // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();

            return true;
        }

        if (id == R.id.action_map) {
            showAlertDialog();
            return true;
        }

        if (id == R.id.action_ev_location) {
            startActivity(new Intent(MainActivity.this, EvPlacesActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void showAlertDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
        final View dialogView = getLayoutInflater().inflate(R.layout.custom_dialog_layout, null);
        alertDialog.setView(dialogView);

        edt_destination = (EditText) dialogView.findViewById(R.id.edt_destination);
        Button btn_next = (Button) dialogView.findViewById(R.id.btn_next);
        edt_destination.setOnClickListener(v -> openPlacePickerView());
        btn_next.setOnClickListener(v -> {
            if (place != null) {
                MyDestination m = new MyDestination(place.getName(), place.getLatLng());

                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                i.putExtra(Constant.EXTRA_DESTINATION, m);
                startActivity(i);
            } else {
                edt_destination.setError("Please select destination");
                edt_destination.requestFocus();
                Toast.makeText(MainActivity.this, "Please select destination", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog alert = alertDialog.create();
        alert.setCanceledOnTouchOutside(true);
        alert.show();
    }

    private void openPlacePickerView() {
        // Create a new Places client instance.
        PlacesClient placesClient = Places.createClient(this);

        // Set the fields to specify which types of place data to return.
        List<Place.Field> fields = Arrays.asList(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG);
        // Start the autocomplete intent.
        Intent intent = new Autocomplete.IntentBuilder(
                AutocompleteActivityMode.FULLSCREEN, fields)
                .build(this);
        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                place = Autocomplete.getPlaceFromIntent(data);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getLatLng());
                edt_destination.setText(place.getName());

                MyDestination m = new MyDestination(place.getName(), place.getLatLng());

                Intent i = new Intent(MainActivity.this, MapsActivity.class);
                i.putExtra(Constant.EXTRA_DESTINATION, m);
                startActivity(i);

            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                // TODO: Handle the error.
                Status status = Autocomplete.getStatusFromIntent(data);
                Log.i(TAG, status.getStatusMessage());
            } else if (resultCode == RESULT_CANCELED) {
                // The user canceled the operation.
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //unregister receiver
        unregisterReceiver(mObdReaderReceiver);
        //stop service
        stopService(new Intent(this, ObdReaderService.class));
        // This will stop background thread if any running immediately.
        ObdPreferences.get(this).setServiceRunningStatus(false);

        stopService(new Intent(this, BatterySimulateService.class));
        Log.i(TAG, "Stopped service");
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(br, new IntentFilter(BatterySimulateService.ACTION_BATTERY_VALUE));
        Log.i(TAG, "Registered Battery receiver");
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(br);
        Log.i(TAG, "Unregistered Battery receiver");
    }

    @Override
    public void onStop() {
        try {
            unregisterReceiver(br);
        } catch (Exception e) {
            // Receiver was probably already stopped in onPause()
        }
        super.onStop();
    }

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            int batteryValue = intent.getIntExtra(Constant.EXTRA_BATTERY, 0);
            Log.i(TAG, "Battery remaining: " + batteryValue);

            ObdHelper.setBatteryLevel(batteryValue);
            ObdHelper.setDrivingDistance(ObdHelper.getDrivingDistance(ObdHelper.getBatteryLevel()));
            initBatteryIndicator();
        }
    }

    public void showLoadingDialog() {

        if (progress == null) {
            progress = new ProgressDialog(this);
            progress.setTitle(getString(R.string.loading_title));
            progress.setMessage(getString(R.string.loading_message));
            progress.setCancelable(false);
        }
        progress.show();
    }

    public void dismissLoadingDialog() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }
}