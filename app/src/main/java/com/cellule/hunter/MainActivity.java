package com.cellule.hunter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.greenrobot.greendao.query.Query;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener {

    private static final int RC_SIGN_IN = 12;
    private static final int REQUEST_ENABLE_BT = 13;
    private GoogleMap gMap;
    private BottomSheetBehavior bottomSheetBehavior;
    private BottomSheetBehavior bottomSheetBehavior2;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private BluetoothAdapter bluetoothAdapter;
    private NoteDao noteDao;
    private Query<Note> notesQuery;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Load Map
        MapFragment mapFragment = (MapFragment) getFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // Initialize Cloud Firestore
        db = FirebaseFirestore.getInstance();

        LinearLayout layout = findViewById(R.id.main_bottom_sheet);
        bottomSheetBehavior = BottomSheetBehavior.from(layout);

        RelativeLayout layout2 = findViewById(R.id.profile_bottom_sheet);
        bottomSheetBehavior2 = BottomSheetBehavior.from(layout2);

        ImageView ivProfilePic = findViewById(R.id.iv_profile_pic);
        ivProfilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profileStuff();
                bottomSheetBehavior2.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        });

        RelativeLayout cvGoToAnss = findViewById(R.id.rl_go_to_anss);
        cvGoToAnss.setOnClickListener(MainActivity.this);


        startBluetooth();

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        //Prepare the DAO local DB
        // get the note DAO
        DaoSession daoSession = ((Application) getApplication()).getDaoSession();
        noteDao = daoSession.getNoteDao();

        getDailyStats();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        gMap.getUiSettings().setMyLocationButtonEnabled(false);
        gMap.setMyLocationEnabled(true);
        gMap.getUiSettings().setZoomControlsEnabled(false);
              // Updates the location and zoom of the MapView
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng(9.649121, -13.578395), 13f);
        //CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, 200, 200, padding);

        gMap.moveCamera(cameraUpdate);

    }

    private void profileStuff() {
        Button btnStartDiag = findViewById(R.id.btn_i_am_positive);
        Button btnSignIn = findViewById(R.id.btn_sign_in);

        btnSignIn.setOnClickListener(MainActivity.this);
        btnStartDiag.setOnClickListener(MainActivity.this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_sign_in:
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.FacebookBuilder().build(),
                        new AuthUI.IdpConfig.PhoneBuilder().build());

                startActivityForResult(
                        AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(providers)
                            .setTheme(R.style.AppTheme)
                            .setTosAndPrivacyPolicyUrls("https://www.condenast.com/user-agreement/", "https://www.condenast.com/user-agreement/")
                            .build(),
                        RC_SIGN_IN);
                break;

            case R.id.btn_i_am_positive:
                Toast.makeText(this, "Bon retablissement", Toast.LENGTH_SHORT).show();
                uploadLocalDbToCloud();
                break;

            case R.id.rl_go_to_anss:
                startActivity(new Intent(MainActivity.this, ANSSActivity.class));
                Toast.makeText(this, "TT", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void uploadLocalDbToCloud() {
        //add this users personal infos
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        //Get local db
        List<Note> notesList = noteDao.queryBuilder()
                .orderAsc(NoteDao.Properties.Date)
                .list();

        if (notesList.size() != 0) {
            //Convert ArrayList to Map
            Map<String, Object> notesMap = new HashMap<>();
            notesMap.put("uploaded_by", user.getUid());
            notesMap.put("timestamp", FieldValue.serverTimestamp());
            notesMap.put("mac_list", notesList);

            //upload
            db.collection("positives").document().set(notesMap);

        }}

    private void signAnonymous(){
        mAuth.signInAnonymously().addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success, update UI with the signed-in user's information
                    FirebaseUser user = mAuth.getCurrentUser();
                        updateDB(user);
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(MainActivity.this, "Authentication failed.",
                            Toast.LENGTH_SHORT).show();
//                            updateUI(null);
                }

                // ...
            }
            });
    }

    private void updateDB(FirebaseUser user) {
        String uid = user.getUid();
        // Create a new user with a first and last name
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", uid);
        userData.put("created", FieldValue.serverTimestamp());

// Add a new document with a generated ID
        db.collection("users").document(uid)
                .set(userData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                    }
                });

        //Add device token to db
        registerDeviceId();
    }

    private void linkRealToAnonymous(){
        AuthCredential credential = PhoneAuthProvider.getCredential("+15145140000", "000000");
        mAuth.getCurrentUser().linkWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = task.getResult().getUser();
                            Toast.makeText(MainActivity.this, "Authentication succeded", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    linkRealToAnonymous();
                }
                break;

            case REQUEST_ENABLE_BT:
                if (resultCode == RESULT_OK){
                    Toast.makeText(MainActivity.this, "BT is ON", Toast.LENGTH_SHORT).show();
                bluetoothAdapter.startDiscovery();
                addUserMacToDb(bluetoothAdapter.getAddress());
                }
                break;
        }
    }

    private void addUserMacToDb(String deviceMac) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        db.collection("users").document(user.getUid()).update("user_mac", deviceMac).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "Err: "+e, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            signAnonymous();
        }else {
            Toast.makeText(MainActivity.this, "Already in", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    @Override
    public void onBackPressed() {
        if (bottomSheetBehavior2.getState() != BottomSheetBehavior.STATE_COLLAPSED) {
            bottomSheetBehavior2.setState(BottomSheetBehavior.STATE_COLLAPSED);
            return;
        }

        super.onBackPressed();
    }


/***************************************BLUETOOTH*************************************
*-Check if device compatible
*-Request enable BT
*-Scan for devices
*
*************************************************************************************/
    private void startBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

//Check if device is compatible
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            new AlertDialog.Builder(this)
                    .setTitle("Pas compatible")
                    .setMessage("Votre appareil doit avoir le Bluetooth")
                    .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            System.exit(0);
                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
        }

//Enable Bluetooth and make this device discoverable
        if (!bluetoothAdapter.isEnabled()) {
/*
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
*/

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
            startActivityForResult(discoverableIntent, REQUEST_ENABLE_BT);

        }else{
            bluetoothAdapter.startDiscovery();
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                String mac = device.getAddress(); // MAC address

                //Add found device to local DB
                //TODO: get last know location and add it before calling
                addNote(mac, 12.3456, 12.6789, new Date());

            }
        }
    };

    private void addNote(String macAddress, double locationLat, double locationLon, Date date){
        Note note = new Note();

        note.setMacAddress(macAddress);
        note.setLocationLat(locationLat);
        note.setLocationLon(locationLon);
        note.setDate(new Date());
        noteDao.insert(note);         //Add new device to local DB.

        //Vibrate to lt me know if succeeded in bg
        vibrate();

        //Query total size
        List<Note> notesList = noteDao.queryBuilder()
                .orderAsc(NoteDao.Properties.Date)
                .list();

        if (notesList != null) {
            //Show me the numbers
            TextView textView = findViewById(R.id.tv_num_total_contact);
            String s = String.valueOf(notesList.size());
            textView.append(s);

        }
    }

    private void vibrate() {
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Vibrate for 500 milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            //deprecated in API 26
            v.vibrate(500);
        }
    }

    private void registerDeviceId() {
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w("Error", "getInstanceId failed", task.getException());
                            return;
                        }

                        // Get new Instance ID token
                        String token = task.getResult().getToken();

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        db.collection("users").document(user.getUid()).update("device_token", token);


                    }
        });
    }

    private void getDailyStats() {
        DocumentReference docRef = db.collection("stats").document("current_stats");
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        TextView tvDead = findViewById(R.id.tv_cases_died);
                        TextView tvRecovered = findViewById(R.id.tv_cases_recovered);
                        TextView tvTotal = findViewById(R.id.tv_cases_total);

                        tvDead.setText(document.get("dead").toString());
                        tvRecovered.setText(document.get("recovered").toString());
                        tvTotal.setText(document.get("total").toString());

                    } else {
                        Toast.makeText(MainActivity.this, "Err. [MAJ non disponible.]", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "failed to get" + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
