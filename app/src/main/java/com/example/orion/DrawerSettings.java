package com.example.orion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

public class DrawerSettings {

    private androidx.appcompat.widget.SwitchCompat switchSounds;
    private androidx.appcompat.widget.SwitchCompat switchBioVerification;
    private TextView githubLink;
    private Button changeIdBtn;
    private Context context;
    private MainActivity mainActivity;

    // Constructor
    public DrawerSettings(Activity activity) {
        this.context = activity;
        this.mainActivity = (MainActivity) activity;


        initializeViews(activity);
        initializeSwitchStates(activity);
        setSwitchListeners(activity);
        setChangeIdButtonListener(activity);
    }

    // Initialize views
    private void initializeViews(Activity activity) {
        switchSounds = activity.findViewById(R.id.switch_sounds);
        switchBioVerification = activity.findViewById(R.id.switch_bio_verification);
        changeIdBtn = activity.findViewById(R.id.change_id_btn);
        githubLink = activity.findViewById(R.id.githubLink);
    }

    // Initialize switch states from SharedPreferences
    private void initializeSwitchStates(Activity activity) {
        switchSounds.setChecked(SharedPreferencesHelper.getSoundsPreference(activity));
        switchBioVerification.setChecked(SharedPreferencesHelper.getBioVerificationPreference(activity));
    }

    // Set listeners for switches
    private void setSwitchListeners(Activity activity) {
        switchSounds.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferencesHelper.saveSoundsPreference(activity, isChecked);
            MainActivity.soundsEnabled = isChecked;
        });

        switchBioVerification.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferencesHelper.saveBioVerificationPreference(activity, isChecked);
            MainActivity.playSwitchSound(context);
        });
        githubLink.setOnClickListener(v -> {
            String url = "https://github.com/RobBudaghyan/Orion.git";
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            activity.startActivity(intent);
        });
    }

    // Set listener for the Change ID button
    private void setChangeIdButtonListener(Activity activity) {
        changeIdBtn.setOnClickListener(v -> {
            MainActivity.playButtonSound(context);
            showConfirmationDialog(activity);
        });
    }

    // Show confirmation dialog for changing ID
    private void showConfirmationDialog(Context activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_change_id_confirmation, null);
        builder.setView(dialogView);

        Button buttonYes = dialogView.findViewById(R.id.dialog_button_yes);
        Button buttonNo = dialogView.findViewById(R.id.dialog_button_no);
        AlertDialog dialog = builder.create();

        buttonYes.setOnClickListener(v -> {
            MainActivity.playAffirmativeButtonSound(context);
            SharedPreferencesHelper.setFirstLaunch(activity, true);
            activity.startActivity(new Intent(activity, MainActivity.class));
            dialog.dismiss();
        });

        buttonNo.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.show();
    }
}
