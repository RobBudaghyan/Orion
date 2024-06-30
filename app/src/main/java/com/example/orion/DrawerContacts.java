package com.example.orion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DrawerContacts implements ContactAdapter.OnContactActionListener {
    private DrawerLayout drawerLayout;
    TextView connectionIdTextView;
    private ImageView openDrawerButton;
    private ImageView copyConnectionId;
    private ImageView shareConnectionId;
    private Button addContactButton;
    private RecyclerView recyclerView;
    private ContactAdapter adapter;
    private List<Contact> contactList;
    private Context context;
    private MainActivity mainActivity;
    static final String SHARED_PREFS_NAME = "contacts_prefs";
    static final String CONTACTS_KEY = "contacts_key";

    public DrawerContacts(Activity activity) {
        this.context = activity;
        this.mainActivity = (MainActivity) activity;
        drawerLayout = activity.findViewById(R.id.drawer_layout);
        openDrawerButton = activity.findViewById(R.id.contacts_btn);
        connectionIdTextView = activity.findViewById(R.id.connectionId_textview);
        copyConnectionId = activity.findViewById(R.id.copy_connectionId_button);
        shareConnectionId = activity.findViewById(R.id.share_connectionId_button);
        addContactButton = activity.findViewById(R.id.button_add_contact);
        recyclerView = activity.findViewById(R.id.recycler_view_contacts);

        contactList = loadContacts();
        adapter = new ContactAdapter(contactList, context, this);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);


        openDrawerButton.setOnClickListener(v -> drawerLayout.openDrawer(activity.findViewById(R.id.nav_view)));

        connectionIdTextView.setText(((MainActivity) activity).getConnectionIdValue());

        copyConnectionId.setOnClickListener(v -> {
            CharSequence textToCopy = connectionIdTextView.getText();
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", textToCopy);
            clipboard.setPrimaryClip(clip);
        });

        shareConnectionId.setOnClickListener(v -> {
            CharSequence textToShare = "ID# " + connectionIdTextView.getText();

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Connection ID");
            shareIntent.putExtra(Intent.EXTRA_TEXT, textToShare.toString());

            context.startActivity(Intent.createChooser(shareIntent, "Share Connection ID"));

        });

        addContactButton.setOnClickListener(v -> showAddContactDialog());
    }

    public void addNewContact(String name, int id) {
        contactList.add(new Contact(name, id));
        adapter.notifyItemInserted(contactList.size() - 1);
        saveContacts();
    }

    @Override
    public void onMessageClick(int contactId) {
        // Handle message click
        drawerLayout.closeDrawer(drawerLayout.findViewById(R.id.nav_view));
        //mainActivity.updateContactNumber(contactId);
        mainActivity.setIdOfReceiver(String.valueOf(contactId));
    }

    @Override
    public void onDeleteClick(int position) {
        showConfirmationDialog(context, position);

    }


    private void showConfirmationDialog(Context activity, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_delete_contact_confirmation, null);
        builder.setView(dialogView);


        Button buttonYes = dialogView.findViewById(R.id.dialog_button_yes);
        Button buttonNo = dialogView.findViewById(R.id.dialog_button_no);

        AlertDialog dialog = builder.create();

        buttonYes.setOnClickListener(v -> {
            contactList.remove(position);
            adapter.notifyItemRemoved(position);
            saveContacts();
            dialog.dismiss();
        });

        buttonNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

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

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        buttonAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = editTextName.getText().toString().trim();
                String numberStr = editTextNumber.getText().toString().trim();

                if (name.isEmpty() || numberStr.isEmpty()) {
                    Toast.makeText(context, "Please enter both name and number", Toast.LENGTH_SHORT).show();
                }
                else if (name.length() > 7){
                    Toast.makeText(context, "Please enter a shorter name", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        int number = Integer.parseInt(numberStr);
                        addNewContact(name, number);
                        dialog.dismiss();
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Please enter a valid number", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        dialog.show();
    }


    private void saveContacts() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(contactList);
        editor.putString(CONTACTS_KEY, json);
        editor.apply();
    }

    private List<Contact> loadContacts() {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = sharedPreferences.getString(CONTACTS_KEY, null);
        Type type = new TypeToken<ArrayList<Contact>>() {}.getType();
        List<Contact> contacts = gson.fromJson(json, type);
        if (contacts == null) {
            contacts = new ArrayList<>();
        }
        return contacts;
    }


}
