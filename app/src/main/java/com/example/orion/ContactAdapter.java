package com.example.orion;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private List<Contact> contactList;
    private Context context;
    private OnContactActionListener listener;

    public interface OnContactActionListener {
        void onMessageClick(int contactId);
        void onDeleteClick(int position);
    }

    public ContactAdapter(List<Contact> contactList, Context context, OnContactActionListener listener) {
        this.contactList = contactList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Contact contact = contactList.get(position);
        holder.contactName.setText(contact.getName());
        holder.contactId.setText(String.valueOf(contact.getId()));

        holder.copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Contact ID", String.valueOf(contact.getId()));
            clipboard.setPrimaryClip(clip);
        });

        holder.messageButton.setOnClickListener(v -> listener.onMessageClick(contact.getId()));

        holder.deleteButton.setOnClickListener(v -> listener.onDeleteClick(position));
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView contactName, contactId;
        ImageView copyButton, messageButton, deleteButton;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            contactName = itemView.findViewById(R.id.contact_name);
            contactId = itemView.findViewById(R.id.contact_id);
            copyButton = itemView.findViewById(R.id.contact_copy_button);
            messageButton = itemView.findViewById(R.id.contact_message_button);
            deleteButton = itemView.findViewById(R.id.contact_delete_button);
        }
    }

}

class Contact {
    private String name;
    private int id;

    public Contact(String name, int id) {
        this.name = name;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }
}
