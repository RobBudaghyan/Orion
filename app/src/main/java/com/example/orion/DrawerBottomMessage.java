package com.example.orion;

import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.PrivateKey;

public class DrawerBottomMessage extends BottomSheetDialogFragment {

    private PrivateKey privateKey;
    private TextView messageTv, senderIdTv;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String docId;
    private String connectionId;
    private ImageView closeBtn;
    private OnDialogDismissedListener listener;
    private TextView copyBtn;

    // Constructor
    public DrawerBottomMessage(String docId, String connectionId, OnDialogDismissedListener listener) {
        this.docId = docId;
        this.connectionId = connectionId;
        this.listener = listener;
    }

    // Called when the fragment's view is created
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.drawer_bottom_message, container, false);
        initializeViews(v);
        setClickListeners();
        fetchMessage();
        return v;
    }

    // Initialize views
    private void initializeViews(View v) {
        closeBtn = v.findViewById(R.id.close_btn);
        messageTv = v.findViewById(R.id.message_tv);
        senderIdTv = v.findViewById(R.id.sender_Id_Tv);
        copyBtn = v.findViewById(R.id.copy_btn);
    }

    // Set click listeners for the buttons
    private void setClickListeners() {
        closeBtn.setOnClickListener(v -> {
            dismiss();
            MainActivity.playSlideSound(getContext());
        });

        copyBtn.setOnClickListener(view -> {
            copyMessageToClipboard();
            MainActivity.playAffirmativeButtonSound(getContext());
        });
    }

    // Copy message to clipboard
    private void copyMessageToClipboard() {
        CharSequence textToCopy = messageTv.getText();
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);
        copyBtn.setText("❐ COPIED!");
    }

    // Called when the fragment is created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false); // This will make the dialog non-cancellable
    }

    // Fetch message from Firestore
    private void fetchMessage() {
        db.collection("Users").document(connectionId).collection("MS").document(docId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String message = documentSnapshot.getString("message");
                        String sender = documentSnapshot.getString("sender");

                        privateKey = MainActivity.privateKey;
                        try {
                            message = EncryptionModule.runDecrypt(privateKey, message);
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Exception while decryption", Toast.LENGTH_SHORT).show();
                            message = "Error 404";
                        }

                        senderIdTv.setText("# " + sender);
                        messageTv.setText(message);
                        removeMessageFromFirestore();

                        if (listener != null) {
                            listener.onDialogDismissed();
                        }
                    } else {
                        messageTv.setText("Message not found.");
                    }
                })
                .addOnFailureListener(e -> {
                    messageTv.setText("Error fetching message.");
                });
    }

    // Remove message from Firestore
    private void removeMessageFromFirestore() {
        db.collection("Users").document(connectionId).collection("MS").document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d("Firestore", "Document successfully deleted!");
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error deleting document", e);
                });
    }

    // Called when the dialog is dismissed
    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
    }
}
