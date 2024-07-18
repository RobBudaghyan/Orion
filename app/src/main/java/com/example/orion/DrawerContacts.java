package com.example.orion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DrawerContacts implements ContactAdapter.OnContactActionListener {
    private DrawerLayout drawerLayout;
    private TextView connectionIdTextView;
    private ImageView openDrawerButton;
    private ImageView copyConnectionId;
    private ImageView shareConnectionId;
    private Button addContactButton;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<Contact> contactList;
    private Context context;
    private MainActivity mainActivity;

    // Constructor
    public DrawerContacts(Activity activity) {
        this.context = activity;
        this.mainActivity = (MainActivity) activity;

        initializeViews(activity);
        initializeContactList();
        setListeners(activity);
    }

    // Initialize views
    private void initializeViews(Activity activity) {
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        openDrawerButton = activity.findViewById(R.id.contacts_btn);
        connectionIdTextView = activity.findViewById(R.id.connectionId_textview);
        copyConnectionId = activity.findViewById(R.id.copy_connectionId_button);
        shareConnectionId = activity.findViewById(R.id.share_connectionId_button);
        addContactButton = activity.findViewById(R.id.button_add_contact);
        recyclerView = activity.findViewById(R.id.recycler_view_contacts);
    }

    // Initialize contact list and set up RecyclerView
    private void initializeContactList() {
        contactList = SharedPreferencesHelper.loadContactsFromSharedPreferences(context);
        adapter = new ContactAdapter(contactList, context, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        connectionIdTextView.setText(mainActivity.getConnectionIdValue());
    }

    // Set listeners for buttons and views
    private void setListeners(Activity activity) {
        openDrawerButton.setOnClickListener(v -> drawerLayout.openDrawer(activity.findViewById(R.id.nav_view)));

        copyConnectionId.setOnClickListener(v -> {
            MainActivity.playAffirmativeButtonSound(context);
            copyToClipboard(connectionIdTextView.getText());
        });

        shareConnectionId.setOnClickListener(v -> {
            MainActivity.playAffirmativeButtonSound(context);
            shareConnectionId();
        });

        addContactButton.setOnClickListener(v -> {
            MainActivity.playButtonSound(context);
            showAddContactDialog();
        });
    }

    // Copy connection ID to clipboard
    private void copyToClipboard(CharSequence textToCopy) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clip);
    }

    // Share connection ID via an Intent
    private void shareConnectionId() {
        CharSequence textToShare = "ID# " + connectionIdTextView.getText();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Connection ID");
        shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare.toString());
        context.startActivity(Intent.createChooser(shareIntent, "Share Connection ID"));
    }

    // Add new contact to the list
    public void addNewContact(String name, int id) {
        contactList.add(new Contact(name, id));
        adapter.notifyItemInserted(contactList.size() - 1);
        SharedPreferencesHelper.saveContactsToSharedPreferences(context, contactList);
    }

    @Override
    public void onMessageClick(int contactId) {
        drawerLayout.closeDrawer(drawerLayout.findViewById(R.id.nav_view));
        mainActivity.setIdOfReceiver(String.valueOf(contactId));
    }

    @Override
    public void onDeleteClick(int position) {
        showConfirmationDialog(context, position);
    }

    // Show confirmation dialog for deleting a contact
    private void showConfirmationDialog(Context activity, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_delete_contact_confirmation, null);
        builder.setView(dialogView);
        Button buttonYes = dialogView.findViewById(R.id.dialog_button_yes);
        Button buttonNo = dialogView.findViewById(R.id.dialog_button_no);
        AlertDialog dialog = builder.create();

        buttonYes.setOnClickListener(v -> {
            MainActivity.playAffirmativeButtonSound(context);
            contactList.remove(position);
            adapter.notifyItemRemoved(position);
            SharedPreferencesHelper.saveContactsToSharedPreferences(context, contactList);
            dialog.dismiss();
        });

        buttonNo.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    // Show dialog to add a new contact
    private void showAddContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_add_contact, null);
        builder.setView(dialogView);
        final EditText editTextName = dialogView.findViewById(R.id.edit_text_contact_name);
        final EditText editTextNumber = dialogView.findViewById(R.id.edit_text_contact_number);
        Button buttonCancel = dialogView.findViewById(R.id.button_cancel);
        Button buttonAdd = dialogView.findViewById(R.id.button_add);
        AlertDialog dialog = builder.create();

        setDialogTextWatchers(editTextName, editTextNumber);
        setDialogButtons(dialog, editTextName, editTextNumber, buttonCancel, buttonAdd);

        dialog.show();
    }

    // Set text watchers for dialog EditTexts
    private void setDialogTextWatchers(EditText editTextName, EditText editTextNumber) {
        TextWatcher textWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.playKeyboardClickSound(context);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        editTextName.addTextChangedListener(textWatcher);
        editTextNumber.addTextChangedListener(textWatcher);
    }

    // Set button listeners for dialog
    private void setDialogButtons(AlertDialog dialog, EditText editTextName, EditText editTextNumber, Button buttonCancel, Button buttonAdd) {
        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        buttonAdd.setOnClickListener(v -> {
            MainActivity.playAffirmativeButtonSound(context);
            String name = editTextName.getText().toString().trim();
            String numberStr = editTextNumber.getText().toString().trim();

            if (name.length() <= 7 && !name.isEmpty() && numberStr.length() == 4) {
                try {
                    int number = Integer.parseInt(numberStr);
                    addNewContact(name, number);
                    dialog.dismiss();
                } catch (Exception ignored) {}
            }
        });
    }
}
