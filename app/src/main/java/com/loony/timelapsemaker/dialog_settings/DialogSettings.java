package com.loony.timelapsemaker.dialog_settings;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.loony.timelapsemaker.MySharedPreferences;
import com.loony.timelapsemaker.R;
import com.loony.timelapsemaker.Util;
import com.loony.timelapsemaker.camera.Resolution;

import java.util.ArrayList;

/**
 * Created by Kamil on 7/26/2017.
 */

public class DialogSettings {
    private Context context;
    private FloatingActionButton fab;

    private OnDialogSettingChangeListener onDialogSettingChangeListener;

    private @Nullable Resolution[] supportedResolutions;
    private @Nullable Resolution choosenSize;
    private int intervalMiliseconds;
    private int photosLimit;
    private boolean webEnabled;

    public DialogSettings(Context context, FloatingActionButton fab) {
        this.context = context;
        this.fab = fab;
    }

    public void setOnDialogSettingChangeListener(OnDialogSettingChangeListener onDialogSettingChangeListener) {
        this.onDialogSettingChangeListener = onDialogSettingChangeListener;
    }

    public void giveSupportedResolutions(@Nullable Resolution[] supportedResolutions, @Nullable Resolution choosenSize) {
        this.supportedResolutions = supportedResolutions;
        this.choosenSize = choosenSize;
    }

    public void setInterval(int intervalMiliseconds) {
        this.intervalMiliseconds = intervalMiliseconds;
    }

    public void setPhotosLimit(int photosLimit) {
        this.photosLimit = photosLimit;
    }

    public void setWebEnabled(boolean enabled) {
        this.webEnabled = enabled;
    }

    public void show() {
        final View dialogView = View.inflate(context, R.layout.dialog_settings, null);
        final Dialog dialog = new Dialog(context, R.style.MyAlertDialogStyle);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(dialogView);
        dialog.show();

        ListView listView = (ListView) dialogView.findViewById(R.id.optionsList);
        setListViewClickListenerForBasicCategory(listView);


        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Util.log("CZY TO SIE POKAZUJE ?");
                revealShow(dialogView, true, null);
            }
        });

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                if (i == KeyEvent.KEYCODE_BACK){

                    revealShow(dialogView, false, dialog);
                    return true;
                }
                return false;
            }
        });

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
    }

    private String getIntervalDescription() {
        return String.format(context.getResources().getString(R.string.dialog_interval_description), String.format("%.1f", intervalMiliseconds/1000f));
    }

    private String getPhotosLimitDescription() {
        return String.format(context.getResources().getString(R.string.dialog_photos_limit_description), photosLimit);
    }

    private String getPhotoResolutionDescription() {
        return String.format(context.getResources().getString(R.string.dialog_resolution_description), choosenSize.getWidth(), choosenSize.getHeight());
    }

    private void setListViewClickListenerForBasicCategory(ListView listView) {

        final ArrayList<DialogOption> options = new ArrayList<>();
        options.add(new DialogOption(R.drawable.ic_photo_size_select, "Photo resolution", getPhotoResolutionDescription()));
        options.add(new DialogOption(R.drawable.ic_interval, "Interval", getIntervalDescription()));
        options.add(new DialogOption(R.drawable.ic_amount, "Limit", getPhotosLimitDescription()));
        options.add(new DialogOption(R.drawable.ic_sd_storage, "Storage", "Storage location for your timalapses"));
        options.add(new DialogOption(R.drawable.ic_remote, "WebAccess", "Access your timelapse progress through a website",
                webEnabled ? DialogOption.Switch.ENABLED : DialogOption.Switch.DISABLED, new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Toast.makeText(DialogSettings.this.context, b ? R.string.text_dialog_webserver_toggleOn : R.string.text_dialog_webserver_toggleOff, Toast.LENGTH_LONG).show();
                MySharedPreferences p = new MySharedPreferences(DialogSettings.this.context);
                p.setWebEnabled(b);
                onDialogSettingChangeListener.onToggleWebServer(b);
            }
        }));


        final DialogSettingsAdapter adapter = new DialogSettingsAdapter(context, options);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                switch(position) {
                    case 0: { // resolution
                        final String[] resOptions = new String[supportedResolutions.length];
                        for(int i=0; i<resOptions.length; i++)
                            resOptions[i] = String.format("%dx%d", supportedResolutions[i].getWidth(), supportedResolutions[i].getHeight());

                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.dialog_choose_resolution)
                                .setItems(resOptions, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        choosenSize = supportedResolutions[which];
                                        options.get(0).description = getPhotoResolutionDescription();
                                        adapter.notifyDataSetChanged();
                                        DialogSettings.this.onDialogSettingChangeListener.onChangePhotoResolution(supportedResolutions[which]);
                                        Util.log("Dialog::ListView::onItemClick -> resolution change " + choosenSize.getWidth() + "x" + choosenSize.getHeight());
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                    }
                    case 1: { // interval
                        final NumberPicker numberPicker = new NumberPicker(context);
                        numberPicker.setMinValue(3);
                        numberPicker.setMaxValue(60 * 5);
                        numberPicker.setValue(intervalMiliseconds / 1000);

                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.dialog_choose_interval)
                                .setView(numberPicker)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        intervalMiliseconds = numberPicker.getValue() * 1000;
                                        options.get(1).description = getIntervalDescription();
                                        adapter.notifyDataSetChanged();
                                        DialogSettings.this.onDialogSettingChangeListener.onChangeInterval(intervalMiliseconds);
                                        Util.log("Dialog::ListView::onItemClick -> interval change [ms] " + intervalMiliseconds);
                                    }
                                })
                                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                    }
                    case 2: { // photos limit
                        final NumberPicker numberPicker = new NumberPicker(context);
                        numberPicker.setMinValue(3);
                        numberPicker.setMaxValue(1000);
                        numberPicker.setValue(photosLimit);

                        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle(R.string.dialog_choose_amount_photos)
                                .setView(numberPicker)
                                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        photosLimit = numberPicker.getValue();
                                        options.get(2).description = getPhotosLimitDescription();
                                        adapter.notifyDataSetChanged();
                                        DialogSettings.this.onDialogSettingChangeListener.onChangePhotosLimit(photosLimit);
                                        Util.log("Dialog::ListView::onItemClick -> photos limit change " + photosLimit);
                                    }
                                })
                                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener(){
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        break;
                    }
                    case 3: { // storage
                        break;
                    }
                    case 4: { // web access
                        final MySharedPreferences p = new MySharedPreferences(context);
                        final String webPassword = p.readWebPassword();

                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setTitle("Set web password");
                        final EditText input = new EditText(context);
                        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        input.setText(webPassword);
                        builder.setView(input);
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String newPassword = input.getText().toString();
                                String finalPass = Util.makeBasicAuthPassword("admin", newPassword);
                                p.setWebPassword(finalPass);

//                                Util.log("[Changing pass] Pass source: '%s'; finalPass: '%s'", newPassword, finalPass);
                                Toast.makeText(context, R.string.text_dialog_change_password_success, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                            }
                        });
                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });

                        builder.show();
                        break;
                    }
                }
            }
        });


    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void revealShow(View dialogView, boolean b, final Dialog dialog) {
        Util.log("revealShow executes");
        final View view = dialogView.findViewById(R.id.dialog);

        int w = view.getWidth();
        int h = view.getHeight();

        int endRadius = (int) Math.hypot(w, h);

        int cx = (int) (fab.getX() + (fab.getWidth()/2));
        int cy = (int) (fab.getY())+ fab.getHeight() + 56;

        if(b){
            Animator revealAnimator = ViewAnimationUtils.createCircularReveal(view, cx,cy, 0, endRadius);

            view.setVisibility(View.VISIBLE);
            revealAnimator.setDuration(700);
            revealAnimator.start();

        } else {
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(view, cx, cy, endRadius, 0);

            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    dialog.dismiss();
                    view.setVisibility(View.INVISIBLE);
                    DialogSettings.this.onDialogSettingChangeListener.onDialogExit();

                }
            });
            anim.setDuration(700);
            anim.start();
        }

    }

    public interface OnDialogSettingChangeListener {
        void onChangePhotoResolution(Resolution resolution);
        void onChangeInterval(int intervalMiliseconds);
        void onChangePhotosLimit(int amount);
        void onToggleWebServer(boolean toggle);

        void onDialogExit();
    }
}
