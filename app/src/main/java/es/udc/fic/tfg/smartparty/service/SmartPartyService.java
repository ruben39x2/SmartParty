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
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.activity.MainActivity;
import es.udc.fic.tfg.smartparty.util.FilterListName;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_DEFAULT_EVERYONE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_EXHAUSTIVE_SCAN;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_EXHAUSTIVE_SCAN_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LISTS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LIST_EVERYONE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LOGS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_NEW_USER;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_NEW_USER_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.SERVER_PORT;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * The heart of Smart Party. This class represent a background service
 * that realizes a host discovery of the network every time that the service
 * is created or that there is a change in the network configuration, which is
 * listened via a Broadcast Receiver.
 * <p/>
 * This class maintains a list of {@link User} and updates the recycler view
 * in the main activity every time that the list changes.
 * <p/>
 * {@link MainActivity} and {@link es.udc.fic.tfg.smartparty.activity.InCallActivity} do
 * link to this service in order to access the users list.
 * <p/>
 * This class is also responsible of starting a Server which receives connections
 * from other Smart Party Users and handles them.
 */
public class SmartPartyService extends Service {
   private final int MAX_CONNECTIONS = 30;
   private final int MAX_LOGS_LENGTH = 500; //Max string length to store as logs
   private final IBinder mBinder = new LocalBinder();
   private ServerSocket mServerSocket;
   private String roomName;
   private BroadcastReceiver networkStateReceiver;
   private List<User> usersList = new ArrayList<>();
   private MainActivity mainActivity;
   private boolean serviceIsStarted = false;
   private boolean networkNotCompatible = false;
   private boolean generalError = false;
   private boolean waitingForTriggerNetworkChange = false;
   private boolean performingScan = false;

   @Override
   public IBinder onBind(Intent intent) {
      return mBinder;
   }

   @Override
   public boolean onUnbind(Intent intent) {
      mainActivity = null;
      return super.onUnbind(intent);
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      unregisterReceiver(networkStateReceiver);
      try {
         mServerSocket.close();
      } catch (IOException e) {
         e.printStackTrace();
         logEvent("ServerSocket failed to stop: " + e);
      }
      logEvent("Service stopped");
   }

   @Override
   public void onCreate() {
      super.onCreate();
      try {
         mServerSocket = new ServerSocket(SERVER_PORT, MAX_CONNECTIONS);
      } catch (IOException e) {
         logEvent("ServerSocket failed to start: " + e);
         generalError = true;
         updateMainIfAvailable();
      }
      logEvent("Service started");
   }

   // This is called every time we start the main activity (if service is enabled).
   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      if (checkRoomChanged()) {
         logEvent("The room name has changed, new room: " + roomName);
         usersList.clear();
      }
      if (!serviceIsStarted) {
         serviceIsStarted = true;
         initializeReceiver();
         startServer();
         runDiscovery();
      }
      return START_NOT_STICKY;
   }

   /**
    * Checks if the room name has changed
    */
   private boolean checkRoomChanged() {
      String actualRoom = Utils.getRoomName(this);
      if (!actualRoom.equals(roomName)) {
         roomName = actualRoom;
         return true;
      } else {
         return false;
      }
   }

   /**
    * Initializes the broadcast received that will be triggered when changes in the
    * wifi state happen.
    */
   private void initializeReceiver() {
      networkStateReceiver = new BroadcastReceiver() {
         @Override
         public void onReceive(Context context, Intent intent) {
            if (!waitingForTriggerNetworkChange) {
               new Thread(new Runnable() {
                  @Override
                  public void run() {
                     waitingForTriggerNetworkChange = true;
                     try {
                        Thread.sleep(5000);
                     } catch (InterruptedException e) {
                        e.printStackTrace();
                     }
                     logEvent("Network change detected, refreshing...");
                     networkNotCompatible = false;
                     runDiscovery();
                     waitingForTriggerNetworkChange = false;
                  }
               }).start();
            }
         }
      };
      IntentFilter intentFilter = new IntentFilter("android.net.wifi.STATE_CHANGE");
      registerReceiver(networkStateReceiver, intentFilter);
   }

   /**
    * Starts the server that will listen to incoming TCP connections.
    */
   private void startServer() {
      if (mServerSocket != null) {
         Server server = new Server(mServerSocket, this);
         server.execute();
      } else {
         logEvent("Failed to start the local server.");
      }
   }

   /**
    * Generally update the UI.
    */
   private void updateMainIfAvailable() {
      // We can update the main view.
      if (mainActivity != null) {
         mainActivity.runOnUiThread(new Runnable() {
            public void run() {
               mainActivity.getAdapter().notifyDataSetChanged();
               mainActivity.updateMainView();
               mainActivity.updateRoomName(Utils.getRoomName(mainActivity));
               mainActivity.invalidateOptionsMenu();
            }
         });
      }
   }

   /**
    * Check if someone SHOULD be considered a friend (check if he belongs to a checked list, for
    * instance).
    *
    * @param name the complex name, which is, UNIQUE_ID+NAME
    * @return if we must be visible to them
    */
   protected boolean isFriend(String name) {
      // Check the lists.
      List<String> lists = Utils.getListsFromPreferences(PREFERENCES_LISTS, this);
      // In case that 'everyone else list' is NOT checked, we have to search for the username
      // in the checked lists.
      if (!getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
              .getBoolean(PREFERENCES_LIST_EVERYONE, PREFERENCES_DEFAULT_EVERYONE)) {
         for (String listName : lists) {
            FilterListName filterListName = new FilterListName(listName);
            if (filterListName.isActive()) {
               List<String> usersInTheList =
                       Utils.getListsFromPreferences(filterListName.getName(), this);
               if (usersInTheList.contains(name)) return true;
            }
         }
         return false;
         // In case that 'everyone else list' IS checked, we have to search for the username
         // in the UNchecked lists (let's see if we have to block the user).
      } else {
         for (String listName : lists) {
            FilterListName filterListName = new FilterListName(listName);
            if (!filterListName.isActive()) {
               List<String> usersInTheList =
                       Utils.getListsFromPreferences(filterListName.getName(), this);
               if (usersInTheList.contains(name)) return false;
            }
         }
         return true;
      }
   }

   /**
    * Store an event.
    *
    * @param event string containing something that happened here in the service
    */
   protected void logEvent(String event) {
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      String currentLogs = prefs.getString(PREFERENCES_LOGS, "");
      if (currentLogs.length() > MAX_LOGS_LENGTH) {
         currentLogs = "";
      }
      SimpleDateFormat sdf = new SimpleDateFormat("[HH:mm:ss]");
      String logToStore = sdf.format(Calendar.getInstance().getTime()) + " " + event + "\n";
      editor.putString(PREFERENCES_LOGS, currentLogs + logToStore);
      editor.apply();
   }

   /**
    * Add user to the users list.
    *
    * @param u the user to add.
    */
   protected void addUserToList(User u) {
      if (u.isMe()) return;
      if (!checkListContains(u.getInetAddress())) {
         notifyUserConnected();
         logEvent("Added user " + u.getUsername() + " from IP " + u.getInetAddress().toString());
      } else {
         removeUserFromList(u.getInetAddress());
         logEvent("Updated user " + u.getUsername() + " from IP " + u.getInetAddress().toString());
      }
      this.usersList.add(u);
      updateMainIfAvailable();
   }

   private boolean checkListContains(InetAddress inetAddress) {
      for (User u : usersList) {
         if (u.getInetAddress().equals(inetAddress)) return true;
      }
      return false;
   }

   private void notifyUserConnected() {
      // Check preferences. Maybe we don't have to notify "new user connected to wifi".
      if (getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
              .getBoolean(PREFERENCES_NOTIFY_NEW_USER, PREFERENCES_NOTIFY_NEW_USER_DEFAULT)) {
         // Build the notification.
         NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                 .setSmallIcon(R.mipmap.ic_launcher)
                 .setContentTitle(getString(R.string.app_name))
                 .setContentText(getString(R.string.new_user_connected));

         Intent targetIntent = new Intent(this, MainActivity.class);
         PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent,
                 PendingIntent.FLAG_UPDATE_CURRENT);
         builder.setContentIntent(contentIntent);
         // Create the notification.
         Notification notification = builder.build();
         notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;
         // And show the notification.
         ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
                 .notify(777, notification);
      }
   }

   /**
    * Public methods that the application will be able to call, in order to maintain
    * the user's list actualized in the main activity
    */

   // This can be called throw the recycler view.
   public void removeUserFromList(InetAddress inetAddress) {
      Iterator<User> iterator = usersList.iterator();
      while (iterator.hasNext()) {
         User user = iterator.next();
         if (user.getInetAddress().equals(inetAddress)) {
            iterator.remove();
            break;
         }
      }
      updateMainIfAvailable();
   }

   public List<User> getListOfUsers() {
      return this.usersList;
   }

   public void makeListAutoUpdatable(MainActivity mainActivity) {
      this.mainActivity = mainActivity;
   }

   public boolean isNetworkNotCompatible() {
      return networkNotCompatible;
   }

   public boolean isGeneralError() {
      return generalError;
   }

   /**
    * Makes general checks and performs an scan.
    */
   public void runDiscovery() {
      usersList.clear();
      logEvent("Starting discovery...");
      InetAddress currentAddress = getIpAddress();
      if (currentAddress == null || currentAddress.isLoopbackAddress()) {
         logEvent("Discovery couldn't start (not connected to network)");
         updateMainIfAvailable();
         return;
      }
      if (!isConnectedToWifi()) {
         logEvent("Current network is invalid (not WiFi). Aborting discovery...");
         networkNotCompatible = true;
         updateMainIfAvailable();
         return;
      }
      performScan(currentAddress);
   }

   /**
    * Sets the variable "performScan" to true and starts an scan, which can be
    * normal or exhaustive.
    *
    * @param currentAddress the current IP address of the device.
    */
   private void performScan(InetAddress currentAddress){
      if (!performingScan) {
         performingScan = true;
         updateMainIfAvailable();
         boolean doExhaustiveScan = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                 .getBoolean(PREFERENCES_EXHAUSTIVE_SCAN, PREFERENCES_EXHAUSTIVE_SCAN_DEFAULT);
         if (doExhaustiveScan) {
            performExhaustiveScan(currentAddress);
         } else {
            performNormalScan(currentAddress);
         }
         // updateMainIfAvailable();
         logEvent("Discovery finished");
      }
   }

   /**
    * Announces our presence to all devices within the network.
    * "Normal" scan means that we are assuming a subnetmask of 255.255.255.0,
    * this is, only 255 devices in our network.
    *
    * @param currentAddress the current ip address of the device
    */
   private void performNormalScan(final InetAddress currentAddress){
      new Thread(new Runnable() {
         @Override
         public void run() {
            // This prefix is, for instance, 192.168.6.
            String prefix = currentAddress.toString()
                    .substring(1, currentAddress.toString().lastIndexOf(".") + 1);
            logEvent("Performing normal scan...");
            logEvent("Scanning " + prefix + "x");
            for (int i = 1; i < 255; i++) {
               String address = prefix + i;
               announcePresence(address); // This will send a message
                                          // " Hi, I am here "
            }
            try {
               Thread.sleep(2100);
            } catch (InterruptedException e) {
               e.printStackTrace();
            }
            performingScan = false;
            updateMainIfAvailable();
         }
      }).start();
   }

   /**
    * Announces our presence to all devices within the network.
    * "Exhaustive" scan means that we are assuming a subnetmask of 255.255.252.0,
    * this is, 1020 devices in our network.
    *
    * @param currentAddress the current ip address of the device
    */
   private void performExhaustiveScan(final InetAddress currentAddress){
      new Thread(new Runnable() {
         @Override
         public void run() {
            // This prefix is, for instance, 192.168.6
            String prefix = currentAddress.toString()
                    .substring(1, currentAddress.toString().lastIndexOf("."));
            // This would be the "6"
            Integer thirdPartOfAddress = Integer.parseInt(prefix.substring(prefix.lastIndexOf(".") + 1));
            // Now we calculate where the subnetwork would start if it had a netmask of /21
            // For instance, if our IP address is:
            // 192.168.1.13 -> the prefix is 192.168.0 (so that network englobes 192.168.(0-4)
            // 10.10.37.9 -> the prefix is 10.10.32 (so that network englobes 10.10.(32-36)
            Integer bitWhereNetworkStarts = thirdPartOfAddress - (thirdPartOfAddress % 4);
            prefix = prefix.substring(0, prefix.lastIndexOf(".") + 1);
            logEvent("Performing exhaustive scan...");
            for (int i = bitWhereNetworkStarts; i < bitWhereNetworkStarts + 4; i++) {
               logEvent("Scanning " + prefix + i + ".x");
               for (int j = 0; j < 255; j++) {
                  String address = prefix + i + "." + j;
                  announcePresence(address);
               }
               try {
                  Thread.sleep(2100);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
            performingScan = false;
            updateMainIfAvailable();
         }
      }).start();
   }

   private boolean isConnectedToWifi() {
      ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
      if (manager == null) return false;
      NetworkInfo info = manager.getActiveNetworkInfo();
      return (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI);
   }

   /**
    * Starts a socket and sends a "Hi i am here" message. Servers, through ConnectionHandlers in
    * other devices will respond to this with an Profile Information Solicitation.
    *
    * @param address the ip address of the target
    */
   private void announcePresence(final String address) {
      final String myUsername = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
              .getString(PREFERENCES_COMPLEX_USERNAME, getString(R.string.default_name));
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               InetAddress inetAddress = InetAddress.getByName(address);
               Socket socket = new Socket();
               socket.connect(new InetSocketAddress(inetAddress, SERVER_PORT), 2000);
               ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
               JSONObject announce = new JSONObject();
               announce.put("protocol", "SmartParty Protocol v1.0");
               announce.put("messageType", "Hi, I am here");
               announce.put("name", myUsername);
               oos.writeObject(announce.toString());
               socket.close();
               logEvent("Our presence was announced to " + address);
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   private InetAddress getIpAddress() {
      WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
      if (manager == null) return null;
      if (manager.getConnectionInfo() == null) return null;
      int ipAddress = manager.getConnectionInfo().getIpAddress();
      try {
         return InetAddress.getByName(intToIp(ipAddress));
      } catch (UnknownHostException e) {
         logEvent("Error while getting our IP address: " + e.toString());
         e.printStackTrace();
         return null;
      }
   }

   private String intToIp(int i) {
      return (i & 0xFF) + "." +
              ((i >> 8 ) & 0xFF) + "." +
              ((i >> 16 ) & 0xFF) + "." +
              ((i >> 24 ) & 0xFF);
   }

   // Class used for the client Binder.
   public class LocalBinder extends Binder {
      public SmartPartyService getService() {
         // Return this instance of the Service so clients can call public methods
         return SmartPartyService.this;
      }
   }

   public boolean isPerformingScan(){
      return this.performingScan;
   }
}
