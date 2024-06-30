package com.example.orion;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;

public class DrawerSettings {

    private androidx.appcompat.widget.SwitchCompat switchSounds;
    private androidx.appcompat.widget.SwitchCompat switchBioVerification;
    private Button changeIdBtn;


    private Context context;
    private MainActivity mainActivity;

    public DrawerSettings(Activity activity){
        this.context = activity;
        this.mainActivity = (MainActivity) activity;

        switchSounds = activity.findViewById(R.id.switch_sounds);
        switchBioVerification = activity.findViewById(R.id.switch_bio_verification);
        changeIdBtn = activity.findViewById(R.id.change_id_btn);

        switchSounds.setChecked(SharedPreferencesHelper.getSoundsPreference(activity));
        switchBioVerification.setChecked(SharedPreferencesHelper.getBioVerificationPreference(activity));

        switchSounds.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesHelper.saveSoundsPreference(activity,isChecked);
                MainActivity.soundsEnabled = isChecked;
            }
        });

        switchBioVerification.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferencesHelper.saveBioVerificationPreference(activity,isChecked);
            }
        });

        changeIdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConfirmationDialog(activity);
            }
        });


    }



    private void showConfirmationDialog(Context activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_change_id_confirmation, null);
        builder.setView(dialogView);


        Button buttonYes = dialogView.findViewById(R.id.dialog_button_yes);
        Button buttonNo = dialogView.findViewById(R.id.dialog_button_no);

        AlertDialog dialog = builder.create();

        buttonYes.setOnClickListener(v -> {
            SharedPreferencesHelper.setFirstLaunch(activity, true);
            activity.startActivity(new Intent(activity, MainActivity.class));
            dialog.dismiss();
        });

        buttonNo.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }




}
