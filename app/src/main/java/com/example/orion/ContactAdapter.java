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

    /**
     * Interface for handling contact actions.
     */
    public interface OnContactActionListener {
        void onMessageClick(int contactId);
        void onDeleteClick(int position);
    }

    /**
     * Constructor for the ContactAdapter.
     *
     * @param contactList List of contacts.
     * @param context Context of the application.
     * @param listener Listener for contact actions.
     */
    public ContactAdapter(List<Contact> contactList, Context context, OnContactActionListener listener) {
        this.contactList = contactList;
        this.context = context;
        this.listener = listener;
    }

    /**
     * Creates and returns a new ContactViewHolder object.
     *
     * @param parent The ViewGroup into which the new View will be added after it is bound to an adapter position.
     * @param viewType The view type of the new View.
     * @return A new ContactViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    /**
     * Replaces the contents of a view (invoked by the layout manager).
     *
     * @param holder The ViewHolder which should be updated to represent the contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Contact contact = contactList.get(position);
        holder.contactName.setText(contact.getName());
        holder.contactId.setText(String.valueOf(contact.getId()));

        holder.copyButton.setOnClickListener(v -> {
            MainActivity.playAffirmativeButtonSound(context);
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Contact ID", contact.getName() + ": #" + contact.getId());
            clipboard.setPrimaryClip(clip);
        });

        holder.messageButton.setOnClickListener(v -> {
            listener.onMessageClick(contact.getId());
            MainActivity.playAffirmativeButtonSound(context);
        });

        holder.deleteButton.setOnClickListener(v -> {
            listener.onDeleteClick(position);
            MainActivity.playButtonSound(context);
        });
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return contactList.size();
    }

    /**
     * ViewHolder class for holding contact item views.
     */
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

    /**
     * Constructor for the Contact class.
     *
     * @param name The name of the contact.
     * @param id The ID of the contact.
     */
    public Contact(String name, int id) {
        this.name = name;
        this.id = id;
    }

    /**
     * Returns the name of the contact.
     *
     * @return The name of the contact.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the ID of the contact.
     *
     * @return The ID of the contact.
     */
    public int getId() {
        return id;
    }
}
