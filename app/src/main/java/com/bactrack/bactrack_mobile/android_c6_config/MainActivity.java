package com.bactrack.bactrack_mobile.android_c6_config;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import BreathalyzerSDK.API.BACtrackAPI;
import BreathalyzerSDK.API.BACtrackAPICallbacks;
import BreathalyzerSDK.API.BACtrackDevice;
import BreathalyzerSDK.Constants.BACTrackDeviceType;
import BreathalyzerSDK.Constants.BACtrackUnit;
import BreathalyzerSDK.Constants.Errors;
import BreathalyzerSDK.Exceptions.BluetoothLENotSupportedException;
import BreathalyzerSDK.Exceptions.BluetoothNotEnabledException;
import BreathalyzerSDK.Exceptions.LocationServicesNotEnabledException;

public class MainActivity extends Activity implements BACtrackAPICallbacks {

    private static final byte PERMISSIONS_FOR_SCAN = 100;

    private static String TAG = "MainActivity";

    private TextView breathalyzerStateTextView;
    private TextView connectionTextView;
    private Button getUnitsButton;
    private Button setUnits1Button;
    private Button disconnectButton;
    private Button connectButton;

    private BACtrackAPI mAPI;

    private final BACtrackUnit selected = BACtrackUnit.BACtrackUnit_permille_2100_1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        this.breathalyzerStateTextView = this.findViewById(R.id.breathalyzerStateTextView);
        this.connectionTextView = this.findViewById(R.id.connectionStateTextView);
        this.connectButton = findViewById(R.id.connect_nearest_button_id);
        this.getUnitsButton = findViewById(R.id.get_units_of_measure_button_id);
        this.setUnits1Button = findViewById(R.id.set_units_of_measure_button_id);
        this.disconnectButton = findViewById(R.id.disconnect_button_id);

        connectButton.setOnClickListener(v -> {
            connectNearestClicked();
        });

        getUnitsButton.setOnClickListener(v -> {
            getUnitsClicked();
        });

        setUnits1Button.setOnClickListener(v -> {
            setUnitsClicked();
        });

        disconnectButton.setOnClickListener(v -> {
            disconnectClicked();
        });

        startSDK();
    }

    private void startSDK() {
        String apiKey = BuildConfig.BACTRACK_API_KEY;
        try {
            mAPI = new BACtrackAPI(this, this, apiKey);
        } catch (BluetoothLENotSupportedException e) {
            e.printStackTrace();
            this.setStatus(getString(R.string.TEXT_ERR_BLE_NOT_SUPPORTED));
        } catch (BluetoothNotEnabledException e) {
            e.printStackTrace();
            this.setStatus(getString(R.string.TEXT_ERR_BT_NOT_ENABLED));
        } catch (LocationServicesNotEnabledException e) {
            e.printStackTrace();
            this.setStatus(getString(R.string.TEXT_ERR_LOCATIONS_NOT_ENABLED));
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            /**
             * Only start scan if permissions granted.
             */
            setStatus("Tap on Connect Breathalyzer");
            startSDK();
        }
    }

    @Override
    public void BACtrackAPIKeyDeclined(String errorMessage) {
        setStatus("API Key Declined");
        connectionTextView.setText("");
        Log.d(TAG, "BACtrackAPIKeyDeclined");
    }

    @Override
    public void BACtrackAPIKeyAuthorized() {
        Log.d(TAG, "BACtrackAPIKeyAuthorized");
    }

    @Override
    public void BACtrackConnected(BACTrackDeviceType bacTrackDeviceType) {
        runOnUiThread(() -> {
            String name = bacTrackDeviceType.getDisplayName();
            connectionTextView.setText("Connected to device:\n" + name);
            setStatus(getString(R.string.TEXT_CONNECTED));
        });
    }

    @Override
    public void BACtrackDidConnect(String s) {
        Log.d("DEBUG_TAG", "BACtrackDidConnect: s" + s);
        setStatus(getString(R.string.TEXT_DISCOVERING_SERVICES));
    }

    @Override
    public void BACtrackDisconnected() {
        setStatus(getString(R.string.TEXT_DISCONNECTED));
        connectionTextView.setText("");
    }

    @Override
    public void BACtrackConnectionTimeout() {
        Log.d(TAG, "BACtrackConnectionTimeout");
    }

    @SuppressLint("MissingPermission")
    @Override
    public void BACtrackFoundBreathalyzer(BACtrackDevice breathalyzer) {
        Log.d(TAG, "Found breathalyzer");
        this.connectionTextView.setText("Device found nearby");
        this.breathalyzerStateTextView.setText("Connecting..");
    }

    @Override
    public void BACtrackCountdown(int currentCountdownCount) {
    }

    @Override
    public void BACtrackStart() {
    }

    @Override
    public void BACtrackBlow(final float v) {
    }

    @Override
    public void BACtrackAnalyzing() {
    }

    @Override
    public void BACtrackResults(float measuredBac) {
    }

    @Override
    public void BACtrackFirmwareVersion(String version) {
    }

    @Override
    public void BACtrackSerial(String serialHex) {
    }

    @Override
    public void BACtrackUseCount(int useCount) {
        setStatus("Use Count: " + useCount);
    }

    @Override
    public void BACtrackBatteryVoltage(float voltage) {
    }

    @Override
    public void BACtrackBatteryLevel(int level) {
    }

    @Override
    public void BACtrackError(int errorCode) {
        if (errorCode == Errors.ERROR_BLOW_ERROR) {
            setStatus(getString(R.string.TEXT_ERR_BLOW_ERROR));
        }
    }

    @Override
    public void BACtrackUnits(final BACtrackUnit baCtrackUnit) {
        String actualString = unitsToString(baCtrackUnit);
        BACtrackUnit expectedUnit = selected;
        String expectedString = unitsToString(expectedUnit);
        if (baCtrackUnit == expectedUnit) {
            setStatus("Units set to expected value.\n" + expectedString, true);
        } else {
            setStatus("Units NOT set to expected value.\nActual: " + actualString, false);
        }
    }

    @NonNull
    private static String unitsToString(BACtrackUnit baCtrackUnit) {
        String units;
        switch (baCtrackUnit) {
            case BACtrackUnit_bac:
                units = "BAC";
                break;
            case BACtrackUnit_mgL:
                units = "mg/L";
                break;
            case BACtrackUnit_permille_2300_1:
                units = "Permille % - 2300:1 (3)";
                break;
            case BACtrackUnit_permille_2100_1:
                units = "Permille % - 2100:1 (4)";
                break;
            case BACtrackUnit_permille_2000_1:
                units = "Permille % - 2000:1 (5)";
                break;
            case BACtrackUnit_permilleByMass:
                units = "Permille by mass";
                break;
            case BACtrackUnit_mg:
                units = "mg";
                break;
            default:
                units = "UNKNOWN";
        }
        return units;
    }

    private void requestPermissions() {
        ArrayList<String> list = new ArrayList<>();
        list.add(Manifest.permission.BLUETOOTH_SCAN);
        list.add(Manifest.permission.BLUETOOTH_CONNECT);
        list.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        list.add(Manifest.permission.ACCESS_FINE_LOCATION);
        String[] array = list.toArray(new String[0]);
        ActivityCompat.requestPermissions(MainActivity.this, array, PERMISSIONS_FOR_SCAN);
    }

    public void connectNearestClicked() {
        if (mAPI != null) {
            setStatus(getString(R.string.TEXT_CONNECTING));
            mAPI.connectToNearestBreathalyzer();
        }
    }

    public void disconnectClicked() {
        if (mAPI != null) {
            mAPI.disconnect();
        }
    }

    private void getUnitsClicked() {
        if (mAPI != null) {
            boolean result = mAPI.readUnitsFromDevice();
            if (!result) {
                setStatus("Failed to read units", false);
            }
        }
    }

    private void setUnitsClicked() {
        if (mAPI != null) {
            boolean result = mAPI.writeUnitsToDevice(selected);
            if (result) {
                setStatus("Units set to\n" + unitsToString(selected));
            } else {
                setStatus("Failed to set units", false);
            }
        }
    }

    private void setStatus(final String message) {
        setStatus(message, null);
    }

    private void setStatus(final String message, @Nullable Boolean success) {
        Log.d(TAG, "Status: " + message);
        breathalyzerStateTextView.setText(message);
        if (success != null) {
            if (success) {
                breathalyzerStateTextView.setTextColor(ContextCompat.getColor(this, R.color.BACGreen));
            } else {
                breathalyzerStateTextView.setTextColor(ContextCompat.getColor(this, R.color.BACRed));
            }
        } else {
            breathalyzerStateTextView.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
    }
}