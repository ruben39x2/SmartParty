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

package es.udc.fic.tfg.smartparty.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.ImageConversor;

import static android.content.Context.MODE_PRIVATE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_BLUE_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_GREEN_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_NAV;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_RED_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_STATUS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_BLUE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_GREEN;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_RED;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * A class that sends a request (through JSON in TCP) to another user and request their profile
 * information. Also, gives our info if should be so.
 *
 * @author Rubén Montero Vázquez
  */
public class ProfileClient {
   private String myUsername, myStatus;
   private int myTheme;
   private boolean giveMyInfo;
   private int myServerPort;
   private String myImage;

   public ProfileClient(Context context, boolean giveMyInfo, int myServerPort) {
      SharedPreferences prefs = context.getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
      myUsername = prefs.getString(PREFERENCES_COMPLEX_USERNAME, context.getString(R.string.default_name));
      myStatus = prefs.getString(PREFERENCES_STATUS, context.getString(R.string.default_status));
      int red = prefs.getInt(PREFERENCES_THEME_COLOR_RED, PREFERENCES_RED_DEFAULT);
      int green = prefs.getInt(PREFERENCES_THEME_COLOR_GREEN, PREFERENCES_GREEN_DEFAULT);
      int blue = prefs.getInt(PREFERENCES_THEME_COLOR_BLUE, PREFERENCES_BLUE_DEFAULT);
      myTheme = Color.rgb(red, green, blue);
      myImage = prefs.getString(PREFERENCES_PHOTO_NAV, "None");
      this.giveMyInfo = giveMyInfo;
      this.myServerPort = myServerPort;
   }

   /**
    * Sends the request
    *
    * @param host ip address of the target
    * @param port tcp port of the target
    * @return the user object containing the info or null
    */
   public User requestProfileInfo(InetAddress host, int port) {
      String username, status, image;
      Integer theme;
      boolean isMe, allowCalls;

      try {
         Socket client = new Socket(host, port);
         client.setSoTimeout(8000);

         // We send the request.
         OutputStream os = client.getOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(os);
         JSONObject message = new JSONObject();
         message.put("protocol", "SmartParty Protocol v1.0");
         message.put("messageType", "Profile Information Solicitation");
         message.put("name", myUsername);
         if (giveMyInfo) {
            message.put("status", myStatus);
            message.put("theme", myTheme);
            message.put("image", myImage);
            message.put("serverPort", myServerPort);
            message.put("allowCalls", (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1));
         }
         oos.writeObject(message.toString());

         // And now wait for the answer.
         InputStream is = client.getInputStream();
         ObjectInputStream ois = new ObjectInputStream(is);

         JSONObject reply = new JSONObject(ois.readObject().toString());
         if (reply.getString("messageType").equals("Profile Information Solicitation Acceptance")) {
            username = reply.getString("name");
            status = reply.getString("status");
            theme = reply.getInt("theme");
            image = reply.getString("image");
            isMe = client.getInetAddress().equals(client.getLocalAddress());
            allowCalls = reply.getBoolean("allowCalls");
            client.close();
            return new User(username, status, theme, ImageConversor.StringToBitMap(image),
                    host, port, isMe, allowCalls);
         } else {
            client.close();
            return null;
         }
      } catch (IOException | ClassNotFoundException | JSONException  | OutOfMemoryError e) {
         e.printStackTrace();
         return null;
      }
   }
}
