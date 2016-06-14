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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.activity.InCallActivity;
import es.udc.fic.tfg.smartparty.activity.MainActivity;
import es.udc.fic.tfg.smartparty.util.ImageConversor;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_PEER_NAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_RECEIVE_CALL;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_REMOTE_RTP_PORT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_BLUE_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_GREEN_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_PHOTO_NAV;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_RED_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_STATUS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_BLUE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_GREEN;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_THEME_COLOR_RED;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.SERVER_PORT;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Class used for handling an incoming connection to the Smart Party Service.
 *
 * @author Rubén Montero Vázquez
 */
public class ConnectionHandler implements Runnable {
   private Socket socket;
   private SmartPartyService service;
   private String username;
   private String status;
   private int theme;
   private String image;

   // Constructor. We get a socket that we will use to receive / send data and a reference
   // to the SmartPartyService in order to call "add user to list".
   public ConnectionHandler(Socket socket, SmartPartyService service) {
      this.service = service;
      this.socket = socket;
      SharedPreferences prefs = service.getSharedPreferences(SHARED_PREFERENCES_NAME,
              Context.MODE_PRIVATE);
      this.username = prefs.getString(PREFERENCES_COMPLEX_USERNAME, service.getString(R.string.default_name));
      this.status = prefs.getString(PREFERENCES_STATUS, service.getString(R.string.default_status));
      int red = prefs.getInt(PREFERENCES_THEME_COLOR_RED, PREFERENCES_RED_DEFAULT);
      int green = prefs.getInt(PREFERENCES_THEME_COLOR_GREEN, PREFERENCES_GREEN_DEFAULT);
      int blue = prefs.getInt(PREFERENCES_THEME_COLOR_BLUE, PREFERENCES_BLUE_DEFAULT);
      this.theme = Color.rgb(red, green, blue);
      this.image = prefs.getString(PREFERENCES_PHOTO_NAV, "None");
   }

   /**
    * Called when this class starts as a new thread. It decodes the string
    * as a JSON and checks the messageType value for:
    * <ul>
    * <li>Hi, I am here: User is announcing their presence. We send them
    * a profile information solicitation</li>
    * <li>Profile Information Solicitation: User is requesting info</li>
    * <li>Hello: User is checking our availability</li>
    * <li>Call Solicitation: User is calling us</li>
    * </ul>
    */
   @Override
   public void run() {
      try {
         InputStream is = socket.getInputStream();
         ObjectInputStream ois = new ObjectInputStream(is);

         // We are supposed to receive a JSON Object with the request.
         JSONObject message = new JSONObject((String) ois.readObject());

         // First we check the type of request.
         String messageType = message.getString("messageType");
         if (messageType.equals("Hi, I am here")) {
            String requesterId = message.getString("name");
            User user = new ProfileClient(service, service.isFriend(requesterId), SERVER_PORT)
                    .requestProfileInfo(socket.getInetAddress(), SERVER_PORT);
            if (user != null) {
               service.addUserToList(user);
            } else {
               service.logEvent("There was an error retrieving user data from " + requesterId);
            }
            socket.close();
            return;
         }
         if (messageType.equals("Profile Information Solicitation")) {
            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            JSONObject reply = new JSONObject();

            String requesterName = message.getString("name");
            if (service.isFriend(requesterName) ||
                    socket.getInetAddress().equals(socket.getLocalAddress())) {
               reply.put("protocol", "SmartParty Protocol v1.0");
               reply.put("messageType", "Profile Information Solicitation Acceptance");
               reply.put("name", username);
               reply.put("status", status);
               reply.put("theme", theme);
               reply.put("image", image);
               reply.put("allowCalls", (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1));
            } else {
               reply.put("protocol", "SmartParty Protocol v1.0");
               reply.put("messageType", "Profile Information Solicitation Denial");
            }
            oos.writeObject(reply.toString());

            // Now we check if the user sent their user information to us.
            if (message.has("status")) {
               service.addUserToList(new User(requesterName, message.getString("status"),
                       message.getInt("theme"),
                       ImageConversor.StringToBitMap(message.getString("image")),
                       socket.getInetAddress(), message.getInt("serverPort"),
                       socket.getInetAddress().equals(socket.getLocalAddress()),
                       message.getBoolean("allowCalls")));
            }
            socket.close();
            return;
         }
         if (messageType.equals("Hello")) {
            String requesterName = message.getString("name");

            OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            JSONObject reply = new JSONObject();

            if (service.isFriend(requesterName)) {
               reply.put("protocol", "SmartParty Protocol v1.0");
               reply.put("messageType", "Hello Acceptance");
               notifyHello(requesterName);
            } else {
               reply.put("protocol", "SmartParty Protocol v1.0");
               reply.put("messageType", "Hello Denial");
            }
            oos.writeObject(reply.toString());
            socket.close();
            return;
         }
         if (messageType.equals("Call Solicitation")) {
            String requesterName = message.getString("name");
            if (!service.isFriend(requesterName)) {
               socket.close();
               return;
            }
            if (!IncomingCallManager.getInstance().getCallActive()) {
               int remoteRtpPort = message.getInt("rtpPort");
               if (message.has("extraParticipants")) {
                  JSONArray extraParticipants = new JSONArray(message.getString("extraParticipants"));
                  for (int i = 0; i < extraParticipants.length(); i++) {
                     IncomingCallManager.getInstance().addGuest(extraParticipants.getString(i));
                  }
               }
               IncomingCallManager.getInstance().setSocket(socket);
               Intent intent = new Intent(service, InCallActivity.class);
               intent.putExtra(INTENT_PEER_NAME, requesterName);
               intent.putExtra(INTENT_RECEIVE_CALL, true);
               intent.putExtra(INTENT_REMOTE_RTP_PORT, remoteRtpPort);
               intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
               service.startActivity(intent);
               return;
            }
         }
         socket.close();
      } catch (IOException | JSONException | ClassNotFoundException e) {
         e.printStackTrace();
         service.logEvent("Server exception: " + e);
      }
   }

   /**
    * Send a notification if someone send us a hello request.
    *
    * @param requesterName the complex name of someone sending us a request.
    */
   private void notifyHello(String requesterName) {
      // Build the notification.
      NotificationCompat.Builder builder = new NotificationCompat.Builder(service)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentTitle(service.getString(R.string.app_name))
              .setContentText(Utils.getSimpleName(requesterName) +
                      service.getString(R.string.someone_says_hello))
              .setLights(this.theme, 400, 600);

      Intent targetIntent = new Intent(service, MainActivity.class);
      PendingIntent contentIntent = PendingIntent.getActivity(service, 0, targetIntent,
              PendingIntent.FLAG_UPDATE_CURRENT);
      builder.setContentIntent(contentIntent);
      // Create the notification.
      Notification notification = builder.build();
      notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;
      // And show the notification (always use the same ID, so that they can't spam).
      ((NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE))
              .notify(778, notification);
   }
}