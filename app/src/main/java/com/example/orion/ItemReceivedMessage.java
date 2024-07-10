package com.example.orion;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.Timestamp;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ItemReceivedMessage extends Fragment {

    private TextView senderIdTv;
    private TextView messageReceivedTv;  // <-- Define it here
    private String senderId;
    private String docId; // add this to store the document id

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_received_message, container, false);

        senderIdTv = view.findViewById(R.id.sender_Id_Tv);
        messageReceivedTv = view.findViewById(R.id.message_received);

        if (getArguments() != null) {
            senderId = "#" + getArguments().getString("senderId") ;
            docId = getArguments().getString("docId"); // get the docId here

            Timestamp timestamp = getArguments().getParcelable("timestamp");

            if (timestamp != null) {
                Log.d("ReceivedTimestamp", "Timestamp: " + timestamp.toDate().toString());
                String timeAgo = getTimeAgo(timestamp);
                messageReceivedTv.setText(timeAgo);
            } else {
                Log.d("ReceivedTimestamp", "Timestamp is null");
            }

            senderIdTv.setText(senderId);
        }

        view.setOnClickListener(v -> openBottomSheetDialog());

        return view;
    }


    private void openBottomSheetDialog() {
        MainActivity.playSlideSound(getContext());
        String connectionId = ((MainActivity) getActivity()).getConnectionIdValue();
        DrawerBottomMessage dialog = new DrawerBottomMessage(docId, connectionId, (OnDialogDismissedListener) getActivity());
        dialog.show(getParentFragmentManager(), "MessageDialog");
    }

    public static String getTimeAgo(Timestamp timestamp) {
        Date date = timestamp.toDate();
        long currentTimeMillis = System.currentTimeMillis();
        long timeMillis = date.getTime();
        long diffMillis = currentTimeMillis - timeMillis;

        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMillis);
        long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
        long weeks = days / 7;
        long months = days / 30; // Approximation
        long years = days / 365; // Approximation

        if (seconds < 60) {
            return "Just Now";
        } else if (minutes == 1) {
            return "1 minute ago";
        } else if (minutes < 60) {
            return minutes + " minutes ago";
        } else if (hours == 1) {
            return "1 hour ago";
        } else if (hours < 24) {
            return hours + " hours ago";
        } else if (days == 1) {
            return "1 day ago";
        } else if (days < 7) {
            return days + " days ago";
        } else if (weeks == 1) {
            return "1 week ago";
        } else if (weeks < 4) { // Consider 4 weeks as a month
            return weeks + " weeks ago";
        } else if (months == 1) {
            return "1 month ago";
        } else if (months < 12) {
            return months + " months ago";
        } else if (years == 1) {
            return "1 year ago";
        } else {
            return years + " years ago";
        }
    }
    public void updateTimeAgo() {
        Timestamp timestamp = getArguments().getParcelable("timestamp");
        if (timestamp != null) {
            String timeAgo = getTimeAgo(timestamp);
            messageReceivedTv.setText(timeAgo);
        }
    }
}
