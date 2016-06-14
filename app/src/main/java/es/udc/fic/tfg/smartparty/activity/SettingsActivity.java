package es.udc.fic.tfg.smartparty.activity;

/**
 * Created by Rubén Montero Vázquez on 22/12/2015
 * This file is part of the degree's final project: Smart Party.
 * University of A Coruña (2015-2016)
 */

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.service.SmartPartyService;
import es.udc.fic.tfg.smartparty.util.ImageConversor;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LOGS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_MISSED_CALL;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_MISSED_CALL_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_NEW_USER;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_NEW_USER_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_DATE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_NAV;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_EXHAUSTIVE_SCAN;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_EXHAUSTIVE_SCAN_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Class for the Settings Activity. This is a basic activity with
 * a few card views that separates the configurable elements in
 * different categories. Basically this reads the Preferences and
 * sets the checked buttons or the text views according to them, and
 * implements a lot of listeners for when the settings are changed.
 * <p/>
 * An important part of the class is the seek bars. There are 3 seek
 * bars (red, green and blue) that user can move to customize a personal
 * theme color. This color will be the color of the toolbar and other things.
 * It's stored in preferences.
 * <p/>
 * Another important thing is that it has an inner class that process
 * an asynchronous task. It changes the size of an image and stores it
 * in preferences. It's the profile image.
 *
 * @author Rubén Montero Vázquez
 */
public class SettingsActivity extends AppCompatActivity {
   private Context context = this;
   // Listener used for when the seek bar of the theme color selector changes

   /**
    * Method called at the beginning of the lifecycle, when the activity
    * is first created. Customizes the toolbar and changes the state of
    * some views according to preferences (checkboxes, text views)
    *
    * @param savedInstanceState not used
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_settings);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      Utils.customizeToolbar(toolbar, this);
      initializeData();
   }



   /**
    * Simply reads preferences and starts some views. It initializes info of:
    * <ul>
    * <li>Username</li>
    * <li>Status (is a short message)</li>
    * <li>The date when the photo was last updated (we don't show the image itself)</li>
    * <li>Notification preferences</li>
    * <li>Service preferences</li>
    * </ul>
    */
   private void initializeData() {
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);

      boolean notifyNewUser = prefs.getBoolean(PREFERENCES_NOTIFY_NEW_USER,
              PREFERENCES_NOTIFY_NEW_USER_DEFAULT);
      boolean notifyMissedCall = prefs.getBoolean(PREFERENCES_NOTIFY_MISSED_CALL,
              PREFERENCES_NOTIFY_MISSED_CALL_DEFAULT);
      boolean exhaustiveScan = prefs.getBoolean(PREFERENCES_EXHAUSTIVE_SCAN,
              PREFERENCES_EXHAUSTIVE_SCAN_DEFAULT);

      ((CheckBox) findViewById(R.id.checkbox_notify_new_user)).setChecked(notifyNewUser);
      ((CheckBox) findViewById(R.id.checkbox_notify_missedcall)).setChecked(notifyMissedCall);
      ((CheckBox) findViewById(R.id.checkbox_exhaustive_scan)).setChecked(exhaustiveScan);
      updateServiceButton((Button) findViewById(R.id.button_service),
              Utils.isServiceEnabled(this));
   }


   /**
    * Method called by every checkbox of the layout. It looks for if
    * the checkbox is checked and also checks the id. Different checkboxes
    * have different ids so that we can now if it is the "Notify missed calls"
    * checkbox or the "Exhaustive Scan" one.
    *
    * @param v the checkbox that was checked or unchecked
    */
   public void onClickChangeCheckBox(View v) {
      boolean isChecked = ((CheckBox) v).isChecked();
      SharedPreferences.Editor editor =
              getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
      switch (v.getId()) {
         case R.id.checkbox_notify_new_user: {
            editor.putBoolean(PREFERENCES_NOTIFY_NEW_USER, isChecked);
            break;
         }
         case R.id.checkbox_notify_missedcall: {
            editor.putBoolean(PREFERENCES_NOTIFY_MISSED_CALL, isChecked);
            break;
         }
         case R.id.checkbox_exhaustive_scan: {
            editor.putBoolean(PREFERENCES_EXHAUSTIVE_SCAN, isChecked);
            break;
         }
         default:
            Toast.makeText(this, R.string.unexpected_error, Toast.LENGTH_SHORT).show();
      }
      editor.apply();
   }

   /**
    * Method called when the start / stop service button is pressed.
    * It changes the status of the service and the value of
    * <code>SERVICE_STATUS</code> in preferences.
    *
    * @param v not used
    */
   public void onClickService(View v) {
      final Intent intent = new Intent(this, SmartPartyService.class);
      if (Utils.isServiceEnabled(this)) {
         showStopDialog(intent);
      } else {
         startService(intent);
         Utils.setServiceStatus(this, true);
         updateServiceButton((Button) v, true);
      }
   }

   /**
    * Shows a dialog with the logs stored in preferences. This logs
    * are saved from {@link SmartPartyService}.
    *
    * @param v the button (not used)
    */
   public void onClickViewLog(View v) {
      String logs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
              .getString(PREFERENCES_LOGS, "");
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.logs);
      builder.setMessage(logs);
      builder.create().show();
   }

   /**
    * Removes the logs stored in preferences. This logs
    * are saved from {@link SmartPartyService}.
    *
    * @param v the button (not used)
    */
   public void onClickDeleteLog(View v) {
      SharedPreferences.Editor editor =
              getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
      editor.putString(PREFERENCES_LOGS, "");
      editor.apply();
      Toast.makeText(this, R.string.logs_deleted, Toast.LENGTH_SHORT).show();
   }

   /**
    * Receives a button and a boolean and sets the text "Start service"
    * or "Stop service" according to it.
    *
    * @param button         the view to update
    * @param serviceRunning <code>true</code> for enabled, <code>false</code> for disabled
    */
   private void updateServiceButton(Button button, boolean serviceRunning) {
      if (serviceRunning) button.setText(R.string.stop_service);
      else button.setText(R.string.start_service);
   }

   /**
    * Shows a dialog asking the user to stop the
    * background {@link SmartPartyService}.
    *
    * @param intent the intent with the service class
    */
   private void showStopDialog(final Intent intent) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.stop_service_dialog_title)
              .setMessage(R.string.are_you_sure_want_to_stop_service)
              .setPositiveButton(R.string.yes,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            stopService(intent);
                            Utils.setServiceStatus(context, false);
                            updateServiceButton((Button) findViewById(R.id.button_service), false);
                         }
                      })
              .setNegativeButton(R.string.no,
                      new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                         }
                      });
      builder.create().show();
   }

   public void onClickIgnoreBatterySaving(View v){
      Intent intent = new Intent();
      intent.setAction("android.settings.IGNORE_BATTERY_OPTIMIZATION_SETTINGS");
      startActivity(intent);
//      ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
   }

}
