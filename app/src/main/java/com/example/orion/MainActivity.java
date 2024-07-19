package com.example.orion;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;
import com.google.firebase.firestore.PersistentCacheSettings;
import com.google.firebase.firestore.QuerySnapshot;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements OnDialogDismissedListener {


    // User's public and private keys
    protected PublicKey publicKey;
    protected static PrivateKey privateKey;

    // Receiver's public key
    private static PublicKey publicKeyOfReceiver;

    // Preferences values
    public static boolean soundsEnabled;
    public static boolean biometricVerificationEnabled;

    // Constant Values
    private static final String TAG = "TAG_LOG";
    private static final int MESSAGES_UPDATE_TIME = 2000;
    private static final int NETWORK_CHECK_TIME = 5000;
    private static final int TOAST_MESSAGE_SHOW_TIME = 2000;

    // UI elements
    private final FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
    private FirebaseAuth mAuth;
    public static MediaPlayer mediaPlayer;
    private ProgressBar progressBar;

    private TextView connectionId;
    private TextView emptyTv;
    private TextView messageLengthTv;
    private TextView toastsTv;
    private EditText inputId;
    private EditText inputMessage;
    ImageView contactsBtn;
    ImageView settingsBtn;
    Button retryButton;
    Button sendBtn;

    // Handlers and Others
    private List<String> displayedDocumentIds = new ArrayList<>();
    private final Handler mainhandler = new Handler();
    private final Handler networkCheckHandler = new Handler(Looper.getMainLooper());
    private final Handler toastsHandler = new Handler();
    private final Handler progressbarHandler = new Handler(Looper.getMainLooper());
    private DrawerContacts drawerContacts;
    private DrawerSettings drawerSettings;
    private DrawerLayout drawerLayout;
    private int progressStatus = 0;
    private static Random random;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        connectionId = findViewById(R.id.connection_id);
        inputId = findViewById(R.id.input_id);
        inputMessage = findViewById(R.id.input_message);
        sendBtn = findViewById(R.id.send_button);
        emptyTv = findViewById(R.id.empty_Tv);
        messageLengthTv = findViewById(R.id.message_length_Tv);
        toastsTv = findViewById(R.id.toasts_Tv);
        contactsBtn = findViewById(R.id.contacts_btn);
        settingsBtn = findViewById(R.id.settings_btn);
        progressBar = findViewById(R.id.progressBar);
        drawerLayout = findViewById(R.id.drawer_layout);
        random = new Random();

        // Check if user launched app first time
        if (SharedPreferencesHelper.isFirstLaunch(this)) {
            FirebaseAuth.getInstance().signOut();
            SharedPreferencesHelper.setFirstLaunch(MainActivity.this, false);
        }

        // Disable offline persistence
        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder(dataBase.getFirestoreSettings())
                        .setLocalCacheSettings(MemoryCacheSettings.newBuilder().build())
                        .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                                .build())
                        .build();
        dataBase.setFirestoreSettings(settings);



        // Ensure secure window flag
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        // Initialize keys
        try {
            mAuth = FirebaseAuth.getInstance();
            if (mAuth.getCurrentUser() == null) {
                signInAnonymously();
            } else {
                FirebaseUser user = mAuth.getCurrentUser();
                String userId = user.getUid();
                fetchConnectionIdFromFirestore(userId);
                privateKey = SharedPreferencesHelper.getPrivateKeyFromEncryptedSharedPreferences(this);
                if (privateKey == null || connectionId.getText() == "- - - -")
                    throw new Exception();
            }
        } catch (Exception e) {
            Toast.makeText(this, "ERROR signing in, please reinstall the app!", Toast.LENGTH_SHORT).show();
            throw new RuntimeException(e);
        }

        // Get the values of sound and biometric verification
        soundsEnabled = SharedPreferencesHelper.getSoundsPreference(this);
        biometricVerificationEnabled = SharedPreferencesHelper.getBioVerificationPreference(this);
        if (biometricVerificationEnabled && !checkBiometricPass()){
            Intent i = new Intent(MainActivity.this, BiometricPrompt.class);
            startActivity(i);
        }

        inputId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                playKeyboardClickSound(getApplicationContext());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Add text changed listener to update message length TextView
        inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                playKeyboardClickSound(getApplicationContext());
            }
            @SuppressLint("SetTextI18n")
            @Override
            public void afterTextChanged(Editable s) {
                messageLengthTv.setText(s.length() + "/210");
            }
        });

        // Send button click listener
        sendBtn.setOnClickListener(v -> {
            playButtonSound(getApplicationContext());
            String inputIdText = inputId.getText().toString().trim();
            String inputMessageText = inputMessage.getText().toString().trim();
            if (inputIdText.isEmpty() || inputMessageText.isEmpty()) {
                showToast("Both input fields must be filled!", TOAST_MESSAGE_SHOW_TIME);
                return;
            } else {
                inputIdText = inputId.getText().toString().trim();
            }
            // Check for the document in the "Users" collection
            String finalInputIdText = inputIdText;
            dataBase.collection("Users").document(inputIdText).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        encryptAndStoreMessageInUserCollection(finalInputIdText, inputMessageText);
                    } else {
                        showToast("No such user exists!", TOAST_MESSAGE_SHOW_TIME);
                    }
                } else {
                    showToast("ERROR checking user ID!", TOAST_MESSAGE_SHOW_TIME);
                }
            });
        });

        // Add DrawerListener to handle drawer events
        drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            boolean flag = true;
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                if (flag){
                    MainActivity.playSlideSound(getApplicationContext());
                    flag = false;
                }
                if(slideOffset == 1 || slideOffset == 0)
                    flag = true;
            }
            @Override
            public void onDrawerOpened(View drawerView) {}
            @Override
            public void onDrawerClosed(View drawerView) {}
            @Override
            public void onDrawerStateChanged(int newState) {}
        });

        // Restoring Document IDs in Android Lifecycle
        if (savedInstanceState != null) {
            displayedDocumentIds = savedInstanceState.getStringArrayList("displayedDocumentIds");
            if (displayedDocumentIds == null) {
                displayedDocumentIds = new ArrayList<>();
            }
        }

        // Contacts button click listener
        contactsBtn.setOnClickListener(v -> {
            drawerContacts = new DrawerContacts(MainActivity.this);
            FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
            transaction1.addToBackStack(null);
            transaction1.commit();
            drawerLayout.openDrawer(GravityCompat.START);
        });

        // Settings button click listener
        settingsBtn.setOnClickListener(v -> {
            drawerSettings = new DrawerSettings(MainActivity.this);
            FragmentTransaction transaction1 = getSupportFragmentManager().beginTransaction();
            transaction1.addToBackStack(null);
            transaction1.commit();
            drawerLayout.openDrawer(GravityCompat.END);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainhandler.post(checkFirestoreRunnable);
        networkCheckHandler.post(networkCheckRunnable);
        progressbarHandler.post(progressbarRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mainhandler.removeCallbacks(checkFirestoreRunnable);
        networkCheckHandler.removeCallbacks(networkCheckRunnable);
        progressbarHandler.removeCallbacks(progressbarRunnable);
    }

    @Override
    public void onDialogDismissed() {
        checkFirestoreCollection();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("displayedDocumentIds", new ArrayList<>(displayedDocumentIds));
    }

// *****************************************************************************************
// >>>>>>>>>>>>>>>>>>>>> User Authentication and Connection Management <<<<<<<<<<<<<<<<<<<<<
// *****************************************************************************************

    static class UserConnection {
        private String connectionId;
        UserConnection(String connectionId) {
            this.connectionId = connectionId;
        }
        public String getConnectionId() {
            return connectionId;
        }
    }

    // Sign In Anonymously
    private void signInAnonymously() {
        mAuth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String userId = user.getUid();
                        Log.d(TAG, "signInAnonymously:success, UID: " + userId);
                        generateAndCheckConnectionId(userId);
                        showToast("Welcome to Orion!",TOAST_MESSAGE_SHOW_TIME);
                    } else {
                        Log.w(TAG, "signInAnonymously:failure", task.getException());
                        showToast("ERROR: Failed to create ID#!",TOAST_MESSAGE_SHOW_TIME);
                    }
                });
    }

    // Generate a unique connection ID
    private String generateConnectionId() {
        Random random = new Random();
        int generatedNumber = 1000 + random.nextInt(9000);
        //pub-private key generation
        generateNewKeys();
        return String.valueOf(generatedNumber);
    }

    // Generate and Check Connection ID
    private void generateAndCheckConnectionId(String userId) {
        String generatedConnectionId = generateConnectionId();
        // Check if this connectionId already exists
        dataBase.collection("Users")
                .document(generatedConnectionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        generateAndCheckConnectionId(userId);
                    } else {
                        saveUserToFirestore(generatedConnectionId, userId);
                        saveConnectionIdToUserLinker(userId, generatedConnectionId);
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error checking connectionId uniqueness", e));
    }

    // Save Connection ID to UserLinker
    private void saveConnectionIdToUserLinker(String userId, String connId) {
        dataBase.collection("UserLinker")
                .document(userId)
                .set(new UserConnection(connId))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User UID added with connectionId: " + connId);
                    connectionId.setText(connId); // Use the renamed variable here
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error adding user UID", e));
    }

    // Fetch Connection ID from Firestore
    private void fetchConnectionIdFromFirestore(String userId) {
        dataBase.collection("UserLinker")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String retrievedConnectionId = documentSnapshot.getString("connectionId");
                        connectionId.setText(retrievedConnectionId);  // Set the connectionId TextView here
                    } else {
                        Log.d(TAG, "No such document for user in UserLinker");
                        signInAnonymously();
                    }
                })
                .addOnFailureListener(e -> Log.w(TAG, "Error getting connectionId", e));
    }


// *****************************************************************************************
// >>>>>>>>>>>>>>>>>>>>> Message Encryption and Firestore Operations <<<<<<<<<<<<<<<<<<<<<<<
// *****************************************************************************************

    // Runnable to check Firestore and update time values
    private final Runnable checkFirestoreRunnable = new Runnable() {
        @Override
        public void run() {
            checkFirestoreCollection();
            updateAllTimeAgoValues();
            try {
                TextView connectionIdTextView = findViewById(R.id.connectionId_textview);
                if (connectionIdTextView.getText().toString().equals("- - - -"))
                    connectionIdTextView.setText(getConnectionIdValue());
            } catch (Exception ignored) {}
            mainhandler.postDelayed(this, MESSAGES_UPDATE_TIME); // Check every 3 seconds
        }
    };

    // Encrypt and Store Message in User Collection
    private void encryptAndStoreMessageInUserCollection(String inputIdText, String inputMessageText) {
        CollectionReference msCollection = dataBase.collection("Users").document(inputIdText).collection("MS");
        dataBase.collection("Users").document(inputIdText).get().addOnCompleteListener(innerTask -> {
            if (innerTask.isSuccessful()) {
                DocumentSnapshot document = innerTask.getResult();
                if (document.exists()) {
                    String publicKeyString = document.getString("publicKey");
                    if (publicKeyString != null) {
                        publicKeyOfReceiver = stringToPublicKey(publicKeyString);
                        Log.d("PPKey", "publicKeyOfReceiver :" + publicKeyOfReceiver);
                        String message = null;
                        try {
                            message = EncryptionModule.runEncrypt(publicKeyOfReceiver, inputMessageText);
                            inputMessage.setText("");
                        } catch (Exception e) {
                            showToast("Error occurred while sending",TOAST_MESSAGE_SHOW_TIME);
                            Log.d("PPKey", e.toString());
                            return;
                        }

                        String finalMessage = message;
                        msCollection.get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                QuerySnapshot querySnapshot = task.getResult();
                                if (querySnapshot != null) {
                                    int highestDocNumber = 0;

                                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                        String docId = doc.getId();
                                        if (docId.startsWith("MS")) {
                                            try {
                                                int docNum = Integer.parseInt(docId.substring(2));
                                                highestDocNumber = Math.max(highestDocNumber, docNum);
                                            } catch (NumberFormatException e) {
                                                Log.e(TAG, "Unexpected document ID format", e);
                                            }
                                        }
                                    }
                                    int nextDocNumber = highestDocNumber + 1;
                                    Map<String, Object> data = new HashMap<>();
                                    data.put("message", finalMessage);
                                    data.put("sender", connectionId.getText().toString());
                                    data.put("timestamp", FieldValue.serverTimestamp());
                                    msCollection.document("MS" + nextDocNumber).set(data).addOnCompleteListener(task1 -> {
                                        if (task1.isSuccessful()) {
                                            showToast("Message sent!", TOAST_MESSAGE_SHOW_TIME);
                                        } else {
                                            showToast("ERROR sending message!", TOAST_MESSAGE_SHOW_TIME);
                                        }
                                    });
                                }
                            } else {
                                showToast("ERROR finding user!", TOAST_MESSAGE_SHOW_TIME);
                            }
                        });

                    } else {
                        Log.d(TAG, "Public key not found for the user!");
                    }
                } else {
                    showToast("No such user exists!", TOAST_MESSAGE_SHOW_TIME);
                }
            } else {
                Log.d(TAG, "Error fetching public key!");
            }
        });
    }

    // Check Firestore Collection for Messages
    private void checkFirestoreCollection() {
        String currentConnectionId = connectionId.getText().toString();
        CollectionReference msCollection = dataBase.collection("Users").document(currentConnectionId).collection("MS");

        msCollection.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    emptyTv.setVisibility(View.GONE);
                    updateReceivedMessages(querySnapshot.getDocuments());
                } else {
                    emptyTv.setVisibility(View.VISIBLE);
                    removeAllReceivedMessageFragments();
                }
            } else {
                Log.e(TAG, "Error fetching sub-collection!", task.getException());
            }
        });
    }

    // Update Received Messages in UI
    private void updateReceivedMessages(List<DocumentSnapshot> documents) {
        List<String> fetchedDocumentIds = new ArrayList<>();
        for (DocumentSnapshot doc : documents) {
            fetchedDocumentIds.add(doc.getId());
        }
        // Remove fragments for deleted documents
        for (String displayedId : displayedDocumentIds) {
            if (!fetchedDocumentIds.contains(displayedId)) {
                removeReceivedMessageFragment(displayedId);
            }
        }
        // Add fragments for new documents
        for (DocumentSnapshot doc : documents) {
            if (!displayedDocumentIds.contains(doc.getId())) {
                addReceivedMessageFragment(doc);
            }
        }
        displayedDocumentIds = fetchedDocumentIds;
    }

    // Save user data to Firestore
    private void saveUserToFirestore(String connectionId, String userId) {
        String publicKeyString = publicKeyToString(publicKey);
        Map<String, Object> userData = new HashMap<>();
        userData.put("uid", userId);
        userData.put("publicKey", publicKeyString);

        dataBase.collection("Users")
                .document(connectionId)
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User added with ID: " + connectionId))
                .addOnFailureListener(e -> Log.w(TAG, "Error adding user", e));
    }


// *****************************************************************************************
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>> RSA Key Management <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// *****************************************************************************************

    // Generate New RSA Key Pair
    private void generateNewKeys(){
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();
        publicKey = kp.getPublic();
        privateKey = kp.getPrivate();

        SharedPreferencesHelper.savePrivateKeyToEncryptedSharedPreferences(privateKey, this);
    }

    // Convert Public Key to Base64 String
    private String publicKeyToString(PublicKey publicKey){
        String str = "";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            str = Base64.getEncoder().encodeToString(publicKey.getEncoded());
        }
        return str;
    }

    // Convert Base64 String to Public Key
    public static PublicKey stringToPublicKey(String publicK) {
        PublicKey pubKey = null;
        try {
            byte[] publicBytes = new byte[0];
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                publicBytes = Base64.getDecoder().decode(publicK);
            }
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            pubKey = keyFactory.generatePublic(keySpec);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return pubKey;
    }


// *****************************************************************************************
// >>>>>>>>>>>>>>>>>>>>>>>>>>> UI and Connectivity Management <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// *****************************************************************************************

    // Runnable to check internet connection
    private final Runnable networkCheckRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isConnectedToInternet()) {
                showNoConnectionScreen();
            }
            networkCheckHandler.postDelayed(this, NETWORK_CHECK_TIME);
        }
    };

    // Runnable to show progress bar
    private final Runnable progressbarRunnable = new Runnable() {
        @Override
        public void run() {
            if(connectionId.getText().toString().equals("- - - -"))
                progressStatus = 0;
            progressStatus += 1;
            if (progressStatus > 100) {
                progressStatus = 0;
            }
            progressBar.setProgress(progressStatus);
            if (progressStatus == 100) {
                progressbarHandler.postDelayed(this, 0);
            } else {
                progressbarHandler.postDelayed(this, MESSAGES_UPDATE_TIME / 100);
            }

            if(progressStatus == 2)
                playProgressBarSound(getApplicationContext());
        }
    };


    // Shows text under send button for some time
    private void showToast(String text, int show_time) {
        toastsTv.setText(text);
        toastsHandler.postDelayed(() -> toastsTv.setText(""), show_time);
    }


    // Update UI with Received Message Fragment
    private void addReceivedMessageFragment(DocumentSnapshot doc) {
        playMessageReceivedSound(getApplicationContext());
        ItemReceivedMessage fragment = new ItemReceivedMessage();
        Bundle args = new Bundle();
        args.putString("senderId", doc.getString("sender"));
        args.putString("docId", doc.getId()); // pass the docId
        Timestamp timestamp = doc.getTimestamp("timestamp");
        if (timestamp != null) {
            args.putParcelable("timestamp", timestamp);
        }
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().add(R.id.message_list, fragment, doc.getId()).commit();
    }

    // Remove Received Message Fragment from UI
    private void removeReceivedMessageFragment(String docId) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(docId);
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    // Remove All Received Message Fragments from UI
    private void removeAllReceivedMessageFragments() {
        for (String displayedId : displayedDocumentIds) {
            removeReceivedMessageFragment(displayedId);
        }
        displayedDocumentIds.clear();
    }

    // Update time values for displayed messages
    private void updateAllTimeAgoValues() {
        for (String displayedId : displayedDocumentIds) {
            ItemReceivedMessage fragment = (ItemReceivedMessage) getSupportFragmentManager().findFragmentByTag(displayedId);
            if (fragment != null) {
                fragment.updateTimeAgo();
            }
        }
    }

    // Get Connection ID Value
    public String getConnectionIdValue() {
        return connectionId.getText().toString();
    }

    private boolean isConnectedToInternet() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    // Show No Connection Screen
    private void showNoConnectionScreen(){
        setContentView(R.layout.activity_no_connection);
        retryButton = findViewById(R.id.retryButton);
        retryButton.setOnClickListener(v -> {
            playButtonSound(getApplicationContext());
            if (isConnectedToInternet()) {
                startActivity(new Intent(MainActivity.this, MainActivity.class));
            }
        });
    }
    // Set ID of Message Receiver
    void setIdOfReceiver(String idOfReceiver){
        inputId.setText(idOfReceiver);
    }

    // Open biometric security check
    private boolean checkBiometricPass(){
        boolean pass = false;
        Intent intent = getIntent();
        if(intent.getExtras() != null) {
            pass = intent.getExtras().getBoolean("lock_passed");
        }
        return pass;
    }



// *****************************************************************************************
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> Other <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
// *****************************************************************************************

    // Play sounds
    public static void playSlideSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.slide_sound);
        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    public static void playSwitchSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.switch_sound);
        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    public static void playProgressBarSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.progressbar_sound);
        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    public static void playButtonSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.button_sound);
        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    public static void playAffirmativeButtonSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.button_affirmative_sound);
        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    public static void playMessageReceivedSound(Context context) {
        mediaPlayer = MediaPlayer.create(context, R.raw.message_received_sound);
        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();
        }
    }
    public static void playKeyboardClickSound(Context context) {
        int[] soundIds = {R.raw.keyboard_click_sound_1, R.raw.keyboard_click_sound_2, R.raw.keyboard_click_sound_3};
        int soundId = soundIds[random.nextInt(soundIds.length)];

        mediaPlayer = MediaPlayer.create(context, soundId);

        if (soundsEnabled && mediaPlayer!=null) {
            mediaPlayer.seekTo(0);
            mediaPlayer.start();

            // Set up a listener to release the MediaPlayer once playback is complete
            mediaPlayer.setOnCompletionListener(mp -> {
                if (mp != null) {
                    mp.release();
                    mediaPlayer = null;
                }
            });
        }
    }


}


