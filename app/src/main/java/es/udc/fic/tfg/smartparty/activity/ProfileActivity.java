/**
 * Copyright 2016 Rubén Montero Vázquez
 *
 * This file is part of Smart Party.
 *
 * Smart Party is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Smart Party is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Smart Party.  If not, see <http://www.gnu.org/licenses/>.
 */

package es.udc.fic.tfg.smartparty.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.ImageConversor;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_BLUE_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_GREEN_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_DATE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_NAV;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_RED_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_STATUS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_BLUE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_GREEN;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_RED;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Class that allows users to change their profile information. This just
 * displays some options and stores changed values into preferences.
 *
 * @author Rubén Montero Vázquez
 */
public class ProfileActivity extends AppCompatActivity {
   // Constant used for the result of the activity that picked a photo
   private final Context context = this;
   private final int REQUEST_CODE = 1;
   private final int RESULT_OK = 1;
   private final int RESULT_IMAGE_SMALL = 2;
   private final int RESULT_UNKNOWN_ERROR = 4;
   // Other variables
   private Toolbar toolbar;
   private int red, green, blue;

   // Listener used for when the seek bar of the theme color selector changes
   private SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
         // Update the variables each step
         if (seekBar.getId() == R.id.red_seek_bar) {
            red = progress;
         } else if (seekBar.getId() == R.id.green_seek_bar) {
            green = progress;
         } else if (seekBar.getId() == R.id.blue_seek_bar) {
            blue = progress;
         }
         // Update the toolbar
         toolbar.setBackgroundColor(Color.rgb(red, green, blue));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
         // Commit the changes to the preferences only when the tracking stops
         SharedPreferences.Editor editor =
                 getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
         if (seekBar.getId() == R.id.red_seek_bar) {
            editor.putInt(PREFERENCES_THEME_COLOR_RED, red);
         } else if (seekBar.getId() == R.id.green_seek_bar) {
            editor.putInt(PREFERENCES_THEME_COLOR_GREEN, green);
         } else if (seekBar.getId() == R.id.blue_seek_bar) {
            editor.putInt(PREFERENCES_THEME_COLOR_BLUE, blue);
         }
         editor.apply();
      }
   };

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
      setContentView(R.layout.activity_profile);
      toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      Utils.customizeToolbar(toolbar, this);
      initializeData();
      initializeSeekBars();
   }

   /**
    * Method from superclass that is called after startActivityForResult.
    *
    * @param requestCode the code for identifying the request
    * @param resultCode  response code usually -1 for errors
    * @param data        the intent
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
         new ProcessProfileImageTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, data);
      }

      super.onActivityResult(requestCode, resultCode, data);
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
      String username = prefs.getString(PREFERENCES_COMPLEX_USERNAME, getString(R.string.default_name));
      String status = prefs.getString(PREFERENCES_STATUS, getString(R.string.default_status));
      String currentDate = prefs.getString(PREFERENCES_PHOTO_DATE,
              getString(R.string.default_photo_date));
      updateUsername(Utils.getSimpleName(username));
      updateStatus(status);
      updateProfilePhotoDate(currentDate);
   }

   /**
    * Simply reads preferences and starts the seek bars. The 3 colors are stored
    * separated in the preferences.
    */
   private void initializeSeekBars() {
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
      red = prefs.getInt(PREFERENCES_THEME_COLOR_RED, PREFERENCES_RED_DEFAULT);
      green = prefs.getInt(PREFERENCES_THEME_COLOR_GREEN, PREFERENCES_GREEN_DEFAULT);
      blue = prefs.getInt(PREFERENCES_THEME_COLOR_BLUE, PREFERENCES_BLUE_DEFAULT);
      SeekBar seekBarRed = (SeekBar) findViewById(R.id.red_seek_bar);
      SeekBar seekBarGreen = (SeekBar) findViewById(R.id.green_seek_bar);
      SeekBar seekBarBlue = (SeekBar) findViewById(R.id.blue_seek_bar);
      seekBarRed.setOnSeekBarChangeListener(listener);
      seekBarGreen.setOnSeekBarChangeListener(listener);
      seekBarBlue.setOnSeekBarChangeListener(listener);
      seekBarRed.setProgress(red);
      seekBarGreen.setProgress(green);
      seekBarBlue.setProgress(blue);
   }

   /**
    * Method called when user wants to edit the username. It shows
    * a dialog.
    *
    * @param v the button
    */
   public void onClickChangeUsername(View v) {
      View changeUsernameView = View.inflate(this, R.layout.dialog_username, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      final EditText userInput = (EditText) changeUsernameView.findViewById(R.id.edit_text_username);
      builder.setTitle(R.string.change_username_dialog_title)
              .setView(changeUsernameView)
              .setPositiveButton(R.string.ok_dialog,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            String username = String.valueOf(userInput.getText()).replace('\n', ' ')
                                    .replace('-', ' ');
                            if (username.length() != 0) {
                               SharedPreferences.Editor editor =
                                       getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                                               .edit();
                               String complexUsername = Utils.getUniqueDeviceID(context) + '-' + username;
                               editor.putString(PREFERENCES_COMPLEX_USERNAME, complexUsername);
                               editor.apply();
                               updateUsername(username);
                            }
                         }
                      });
      builder.create().show();
   }

   /**
    * Method called when user wants to edit the status. It shows
    * a dialog.
    *
    * @param v the button
    */
   public void onClickChangeStatus(View v) {
      View changeUsernameView = View.inflate(this, R.layout.dialog_status, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      final EditText userInput = (EditText) changeUsernameView.findViewById(R.id.edit_text_status);
      builder.setTitle(R.string.change_status_dialog_title)
              .setView(changeUsernameView)
              .setPositiveButton(R.string.ok_dialog,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            String status = String.valueOf(userInput.getText()).replace('\n', ' ');
                            if (status.length() != 0) {
                               SharedPreferences.Editor editor =
                                       getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                                               .edit();
                               editor.putString(PREFERENCES_STATUS, status);
                               editor.apply();
                               updateStatus(status);
                            }
                         }
                      });
      builder.create().show();
   }

   /**
    * Updates the textView username with the given string.
    *
    * @param username the string to use
    */
   private void updateUsername(String username) {
      // We don't store the TextView in a private variable of the activity because this method
      // isn't going to be called a lot of times during the activity lifetime
      TextView textView = (TextView) findViewById(R.id.text_name);
      String toShow = getString(R.string.username_is) + username;
      textView.setText(toShow);
   }

   /**
    * Updates the textView status with the given string.
    *
    * @param status the string to use
    */
   private void updateStatus(String status) {
      // We don't store the TextView in a private variable of the activity because this method
      // isn't going to be called a lot of times during the activity lifetime
      TextView textView = (TextView) findViewById(R.id.text_settings_status);
      String toShow = getString(R.string.status_is) + status;
      textView.setText(toShow);
   }

   /**
    * Updates the textView for the last modified date of
    * the profile picture with the given string.
    *
    * @param updatedDate the string to use
    */
   private void updateProfilePhotoDate(String updatedDate) {
      TextView textView = (TextView) findViewById(R.id.text_settings_photo);
      String toShow = getString(R.string.profile_photo) + updatedDate;
      textView.setText(toShow);
   }

   /**
    * Called when the user wants to change the profile picture. It
    * starts and activity for result, with action <code>ACTION_GET_CONTENT</code>,
    * category <code>CATEGORY_OPENABLE</code> and type "image/*".
    *
    * @param v the button "change photo"
    */
   public void onClickChangePhoto(View v) {
      Intent intent = new Intent();
      intent.setType("image/*");
      intent.setAction(Intent.ACTION_GET_CONTENT);
      intent.addCategory(Intent.CATEGORY_OPENABLE);
      startActivityForResult(intent, REQUEST_CODE);
   }

   // This private class represents an asynchronous task used to process the profile image
   // after we receive it from another intent
   private class ProcessProfileImageTask extends AsyncTask<Intent, String, Integer> {
      private ProfileActivity activity;
      private AlertDialog alertDialog;

      // Constructor
      private ProcessProfileImageTask(ProfileActivity activity) {
         this.activity = activity;
      }

      // Called before the asynchronous thread starts (here we setup a dialog)
      @Override
      protected void onPreExecute() {
         super.onPreExecute();
         View dialog = getLayoutInflater().inflate(R.layout.dialog_processing, null);
         AlertDialog.Builder builder = new AlertDialog.Builder(activity);
         builder.setView(dialog)
                 .setCancelable(true);
         alertDialog = builder.create();
         alertDialog.show();
      }

      // Called after the asynchronous thread finishes (here we check the result)
      @Override
      protected void onPostExecute(Integer resultCode) {
         super.onPostExecute(resultCode);
         switch (resultCode) {
            case RESULT_OK: {
               Toast.makeText(activity, R.string.photo_updated_successfully,
                       Toast.LENGTH_LONG).show();
               // And save the day of the update (update the current textView too)
               SharedPreferences.Editor editor =
                       getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
               DateFormat df = SimpleDateFormat.getDateInstance();
               String currentDate = df.format(Calendar.getInstance().getTime());
               editor.putString(PREFERENCES_PHOTO_DATE, currentDate);
               editor.apply();
               updateProfilePhotoDate(currentDate);
               break;
            }
            case RESULT_IMAGE_SMALL: {
               Toast.makeText(activity, getString(R.string.image_small), Toast.LENGTH_LONG).show();
               break;
            }
            case RESULT_UNKNOWN_ERROR: {
               Toast.makeText(activity, getString(R.string.something_went_wrong_loading_photo),
                       Toast.LENGTH_LONG).show();
               break;
            }
         }
         alertDialog.dismiss();
      }

      // This will be executed asynchronously. Here we process the image
      @Override
      protected Integer doInBackground(Intent... params) {
         int widthPx = 320, heightPx = 320;

         try {
            // Decode the result
            InputStream stream = getContentResolver().openInputStream(
                    params[0].getData());
            Bitmap original = BitmapFactory.decodeStream(stream);
            stream.close(); // NullPointer already captured

            // Calculate minimum width and height of the image
            DisplayMetrics displaymetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
            // Check image size.
            if ((original.getHeight() < heightPx) || (original.getWidth() < widthPx)) {
               return RESULT_IMAGE_SMALL;
            }

            Matrix m = new Matrix();
            if (!m.setRectToRect(new RectF(0, 0, original.getWidth(), original.getHeight()),
                    new RectF(0, 0, widthPx, heightPx), Matrix.ScaleToFit.CENTER)) {
               return RESULT_UNKNOWN_ERROR;
            }
            Bitmap toStore = Bitmap.createBitmap(original, 0, 0,
                    original.getWidth(), original.getHeight(), m, true);

            // Store it
            SharedPreferences.Editor editor =
                    getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
            editor.putString(PREFERENCES_PHOTO_NAV,
                    ImageConversor.BitMapToString(toStore));
            editor.apply();
            return RESULT_OK;

         } catch (Exception e) {
            e.printStackTrace();
            return RESULT_UNKNOWN_ERROR;
         }
      }
   }
}
