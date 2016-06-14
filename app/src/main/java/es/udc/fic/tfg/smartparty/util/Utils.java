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

package es.udc.fic.tfg.smartparty.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.service.User;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_BLUE_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_GREEN_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_NAV;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_RED_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_SERVICE_STATUS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_STATUS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_BLUE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_GREEN;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_RED;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Provides different utilities for Smart Party.
 *
 * @author Rubén Montero Vázquez
 */
public class Utils {

   public static List<String> getListsFromPreferences(String listName, Context context) {
      try {
         JSONArray jsonArray = new JSONArray(context
                 .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                 .getString(listName, "[]"));
         List<String> currentList = new ArrayList<>();
         for (int i = 0; i < jsonArray.length(); i++) {
            currentList.add(jsonArray.getString(i));
         }
         return currentList;
      } catch (JSONException e) {
         e.printStackTrace();
         return new ArrayList<>();
      }
   }

   public static void storeListInPreferences(String listName, List<String> list, Context context) {
      SharedPreferences.Editor editor = context.getSharedPreferences(SHARED_PREFERENCES_NAME,
              Context.MODE_PRIVATE).edit();
      JSONArray jsonArray = new JSONArray(list);
      editor.putString(listName, jsonArray.toString());
      editor.commit();
   }

   public static void removeKeyFromPreferences(String key, Context context){
      SharedPreferences.Editor editor =
              context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
      editor.remove(key);
      editor.apply();
   }

   public static String getRoomName(Context context) {
      WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
      if (wifiManager == null) {
         return context.getString(R.string.local_room);
      }
      WifiInfo info = wifiManager.getConnectionInfo();
      if (info == null) {
         return context.getString(R.string.local_room);
      } else {
         if (info.getSSID() == null) {
            return context.getString(R.string.local_room);
         } else {
            if (info.getSSID().startsWith("0x") || info.getSSID().equals("<unknown ssid>")) {
               return context.getString(R.string.local_room);
            } else {
               return info.getSSID().replace("\"", "");
            }
         }
      }
   }

   public static void customizeToolbar(Toolbar toolbar, CardView cardView, Context context) {
      int themeColor = getThemeColor(context
              .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE));
      toolbar.setBackgroundColor(themeColor);
      toolbar.setTitleTextColor(Color.WHITE);
      cardView.setCardBackgroundColor(themeColor);
   }

   public static void customizeToolbar(Toolbar toolbar, Context context) {
      int themeColor = getThemeColor(context
              .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE));
      toolbar.setBackgroundColor(themeColor);
      toolbar.setTitleTextColor(Color.WHITE);
   }

   public static void customizeToolbar(CollapsingToolbarLayout toolbarLayout, Context context) {
      SharedPreferences prefs =
              context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
      String image = prefs
              .getString(PREFERENCES_PHOTO_NAV, "None");

      toolbarLayout.setCollapsedTitleTextColor(Color.WHITE);
      toolbarLayout.setExpandedTitleColor(Color.WHITE);
      toolbarLayout.setContentScrimColor(getThemeColor(prefs));

      Bitmap bitmap = ImageConversor.StringToBitMap(image);

      if (bitmap != null) {
         ((ImageView) toolbarLayout.findViewById(R.id.image_background_toolbar))
                 .setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
      } else {
         ((ImageView) toolbarLayout.findViewById(R.id.image_background_toolbar))
                 .setImageResource(R.drawable.default_profile);
      }
   }

   public static void customizeNavHeader(RelativeLayout navHeader, Context context) {
      if (navHeader != null) {
         SharedPreferences prefs =
                 context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
         String username = Utils.getSimpleName(prefs
                 .getString(PREFERENCES_COMPLEX_USERNAME, context.getString(R.string.default_name)));
         String status = prefs
                 .getString(PREFERENCES_STATUS, context.getString(R.string.default_status));
         String image = prefs
                 .getString(PREFERENCES_PHOTO_NAV, "None");

         ((TextView) navHeader.findViewById(R.id.text_nav_name)).setText(username);
         ((TextView) navHeader.findViewById(R.id.text_nav_status)).setText(status);

         Bitmap bitmap = ImageConversor.StringToBitMap(image);
         if (bitmap != null) {
            ((ImageView) navHeader.findViewById(R.id.image_background_nav_header))
                    .setImageDrawable(new BitmapDrawable(context.getResources(), bitmap));
         } else {
            ((ImageView) navHeader.findViewById(R.id.image_background_nav_header))
                    .setImageResource(R.drawable.default_profile);
         }
      }
   }

   private static int getThemeColor(SharedPreferences prefs) {
      int red = prefs.getInt(PREFERENCES_THEME_COLOR_RED, PREFERENCES_RED_DEFAULT);
      int green = prefs.getInt(PREFERENCES_THEME_COLOR_GREEN, PREFERENCES_GREEN_DEFAULT);
      int blue = prefs.getInt(PREFERENCES_THEME_COLOR_BLUE, PREFERENCES_BLUE_DEFAULT);
      return Color.rgb(red, green, blue);
   }

   public static boolean isServiceEnabled(Context context) {
      return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
              .getBoolean(PREFERENCES_SERVICE_STATUS, true);
   }

   public static void setServiceStatus(Context context, boolean enabled) {
      SharedPreferences.Editor editor =
              context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
      editor.putBoolean(PREFERENCES_SERVICE_STATUS, enabled);
      editor.commit();
   }

   public static String getSimpleName(String complexUsername){
      if (complexUsername.lastIndexOf('-')>-1) {
         return complexUsername.substring(complexUsername.lastIndexOf('-')+1);
      } else {
         return complexUsername;
      }
   }

   public static String getIDName(String complexUsername){
      if (complexUsername.lastIndexOf('-')>-1) {
         return complexUsername.substring(0, complexUsername.lastIndexOf('-'));
      } else {
         return complexUsername;
      }
   }


   public static String getUniqueDeviceID(Context context){
      String uniqueID =
              Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
      // If no ID was returned, take a random number (current timestamp). Won't survive a
      // reinstallation of the app.
      if (uniqueID == null) {
         return ((Long)(new Date()).getTime()).toString();
      }
      // If ID is 9774d56d682e549c, then we have a BUGGED DEVICE which returns an ID that may be
      // repited. We return a random number instead (current timestamp). Won't survive a
      // reinstallation of the app.
      if (uniqueID.equals("9774d56d682e549c")){
         return ((Long)(new Date()).getTime()).toString();
      }
      return uniqueID;
   }

   public static boolean isThereAnyUser(List<User> smartPartyServiceList,
                                        String listName, Context context){
      for (String userName : Utils.getListsFromPreferences(listName, context)){
         for(User user : smartPartyServiceList){
            if (Utils.getIDName(user.getUsername()).equals(Utils.getIDName(userName))) return true;
         }
      }
      return false;
   }

}
