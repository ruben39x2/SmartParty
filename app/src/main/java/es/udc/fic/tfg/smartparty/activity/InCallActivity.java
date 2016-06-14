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

import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.audiocall.AudioCall;
import es.udc.fic.tfg.smartparty.audiocall.AudioCallListener;
import es.udc.fic.tfg.smartparty.audiocall.MasterAudioCall;
import es.udc.fic.tfg.smartparty.audiocall.SlaveAudioCall;
import es.udc.fic.tfg.smartparty.service.IncomingCallManager;
import es.udc.fic.tfg.smartparty.service.SmartPartyService;
import es.udc.fic.tfg.smartparty.service.User;
import es.udc.fic.tfg.smartparty.util.Call;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.INCOMING_CALL_VIBRATE_OFF;
import static es.udc.fic.tfg.smartparty.util.Preferences.INCOMING_CALL_VIBRATE_ON;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_ADDRESS;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_PEER_NAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_RECEIVE_CALL;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_REMOTE_RTP_PORT;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_SERVER_PORT;
import static es.udc.fic.tfg.smartparty.util.Preferences.MAX_REGISTER_CALLS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_CALLS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_MISSED_CALL;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NOTIFY_MISSED_CALL_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Class that represent the activity used for an ongoing call.
 * If there is an incoming call, then it starts a {@link SlaveAudioCall}.
 * If it's an outgoing call, it starts a {@link MasterAudioCall}.
 * Anyway, this is stored as a generic {@link AudioCall} and the events
 * that happen into the call are listened via a {@link AudioCallListener}
 *
 * @author Rubén Montero Vázquez
 */
public class InCallActivity extends AppCompatActivity {
   private AudioCall audioCall;
   private Vibrator vibrator;
   private SecondsCounter counter;
   private Context context = this;
   private boolean isBound = false, isCallEstablished = false, callWasRejected = false;
   private SmartPartyService smartPartyService;
   private List<String> peerNames = new ArrayList<>();
   private int callSelection = 0;
   // The service connection object that will listen to when
   // we are connected to the service or disconnected
   private ServiceConnection mConnection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName className,
                                     IBinder service) {
         SmartPartyService.LocalBinder binder = (SmartPartyService.LocalBinder) service;
         smartPartyService = binder.getService();
         isBound = true;
         showPeerImage();
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
         isBound = false;
      }
   };

   /**
    * Called when the activity is first created. Sets the status
    * as "call active" into the {@link IncomingCallManager} singleton, gets
    * the peer names from the intent and binds to the background service.
    * Then, checks if it must make or receive an audio call.
    *
    * @param savedInstanceState not used
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_in_call);
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
              WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
              WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
              WindowManager.LayoutParams.FLAG_FULLSCREEN |
              WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

      IncomingCallManager.getInstance().setCallActive(true);

      peerNames.add(getIntent().getStringExtra(INTENT_PEER_NAME));

      final Intent intent = new Intent(this, SmartPartyService.class);
      bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

      counter = new SecondsCounter((TextView) findViewById(R.id.text_time));

      showOnGoingNotification("00:00");
      if (getIntent().getBooleanExtra(INTENT_RECEIVE_CALL, false)) {
         receiveCall();
      } else {
         makeCall();
      }
   }

   /**
    * Method called from the onCreate when the <code>INTENT_RECEIVE_CALL</code>
    * is true. It creates an {@link SlaveAudioCall} and sets it to "ringing".
    * Then shows the accept / reject buttons and vibrates the phone.
    */
   private void receiveCall() {
      audioCall = new SlaveAudioCall(getListener(), IncomingCallManager.getInstance().getSocket(),
              getIntent().getIntExtra(INTENT_REMOTE_RTP_PORT, 0), this);
      ((SlaveAudioCall) audioCall).setRinging();
      showAcceptRejectButtons();
      vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
      long pattern[] = new long[]{0, INCOMING_CALL_VIBRATE_ON, INCOMING_CALL_VIBRATE_OFF};
      vibrator.vibrate(pattern, 0);
      peerNames.addAll(IncomingCallManager.getInstance().getGuests());
      updatePeerName();
      setCallStatus(getString(R.string.incoming_call));
   }

   /**
    * Method called from the onCreate when the <code>INTENT_RECEIVE_CALL</code>
    * is false. It creates an {@link MasterAudioCall} and "calls a participant".
    * Also updates a few things in the UI.
    */
   private void makeCall() {
      final InetAddress host = (InetAddress) getIntent().getSerializableExtra(INTENT_ADDRESS);
      final int port = getIntent().getIntExtra(INTENT_SERVER_PORT, 0);
      final String calleeName = getIntent().getStringExtra(INTENT_PEER_NAME);
      audioCall = new MasterAudioCall(getListener(), this);
      audioCall.callParticipant(host, port, calleeName);
      showInCallButtons();
      updatePeerName();
      setCallStatus(getString(R.string.trying));
   }

   /**
    * Unbinds from the service.
    */
   @Override
   protected void onStop() {
      super.onStop();
      if (isBound) {
         unbindService(mConnection);
         isBound = false;
      }
   }

   /**
    * Overrides the behaviour of on back pressed, so that the final
    * user cannot finish the activity.
    */
   @Override
   public void onBackPressed() {
      // Don't allow the user to destroy the activity.
   }

   /**
    * Called when the hangUp button is pressed. Hangs up the call and
    * calls exitCall().
    *
    * @param v the button (not used)
    */
   public void onClickHangup(View v) {
      hideInCallButtons();
      audioCall.hangUp();
      exitCall();
   }

   /**
    * Called when the toggle speaker button is pressed.
    * Changes the speakerphone mode in the <code>AUDIO_SERVICE</code> of the system.
    *
    * @param v the button (not used)
    */
   public void onClickToggleSpeaker(View v) {
      boolean on = ((ToggleButton) v).isChecked();
      ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(on);
   }

   /**
    * Called when the toggle speaker button is pressed.
    * Changes the silence option in the audio call.
    *
    * @param v the button (not used)
    */
   public void onClickToggleSilence(View v) {
      boolean on = ((ToggleButton) v).isChecked();
      audioCall.toggleSilence(on);
   }

   /**
    * Called when the user clicks in "Add participant" button.
    * Takes the list of available users from the background {@link SmartPartyService}
    * and shows them in a dialog (it omits the users already in the call).
    * When pressing in one user, it calls the callParticipant() method
    * from the audio call.
    *
    * @param v not used
    */
   public void onClickAddParticipant(View v) {
      int size = 0;
      if (!isBound) {
         showToast(getString(R.string.not_possible_to_add_participant));
         return;
      }
      if (audioCall.isSlave()){
         showToast(getString(R.string.only_who_started_can_add));
         return;
      }
      final List<String> names = new ArrayList<>();
      final List<InetAddress> addresses = new ArrayList<>();
      final List<Integer> ports = new ArrayList<>();
      for (User u : smartPartyService.getListOfUsers()) {
         // If the user is not me and it's neither participating in the call...
         if (!u.isMe() && !peerNames.contains(u.getUsername())) {
            // ...then add them to our private variables list
            names.add(u.getUsername());
            addresses.add(u.getInetAddress());
            ports.add(u.getServerPort());
            size++;
         }
      }
      final String[] displayNames = new String[size];
      for (int i = 0; i < size; i++){
         displayNames[i] = Utils.getSimpleName(names.get(i));
      }
      if (size == 0) {
         showToast(getString(R.string.toast_not_possible_to_add_more_users));
      } else {
         // Build the dialog
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setSingleChoiceItems(displayNames, -1, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int item) {
               // Every time you click on a singleChoice Option, this is executed.
               setSelection(item);
            }
         }).setPositiveButton(context.getString(R.string.call_participant),
                 new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                       audioCall.callParticipant(addresses.get(getSelection()), ports.get(getSelection()),
                               names.get(getSelection()));
                       showToast(getString(R.string.calling) + displayNames[getSelection()]);
                    }
                 });
         builder.create().show();
      }
   }

   /**
    * Called when receiving a call and the user presses "accept call".
    * Sets the call status to established and accepts the call.
    *
    * @param v not used
    */
   public void onAcceptCall(View v) {
      vibrator.cancel();
      ((SlaveAudioCall) audioCall).acceptAudioCall();
      setCallStatus(getString(R.string.call_established));
      hideAcceptRejectButtons();
      showInCallButtons();
   }

   /**
    * Called when receiving a call and the user presses "reject call".
    * Finishes the activity.
    *
    * @param v not used
    */
   public void onRejectCall(View v) {
      ((SlaveAudioCall) audioCall).rejectAudioCall();
      callWasRejected = true;
      exitCall();
   }

   /**
    * Name is self-explicative.
    */
   private void showAcceptRejectButtons() {
      findViewById(R.id.button_accept_call).setVisibility(View.VISIBLE);
      findViewById(R.id.button_reject_call).setVisibility(View.VISIBLE);
   }

   /**
    * The same.
    */
   private void hideAcceptRejectButtons() {
      findViewById(R.id.button_accept_call).setVisibility(View.INVISIBLE);
      findViewById(R.id.button_reject_call).setVisibility(View.INVISIBLE);
   }

   /**
    * OK let's stop writing comments for this...
    */
   private void showInCallButtons() {
      findViewById(R.id.button_hangup).setVisibility(View.VISIBLE);
      findViewById(R.id.button_speaker).setVisibility(View.VISIBLE);
      findViewById(R.id.button_silence).setVisibility(View.VISIBLE);
      findViewById(R.id.button_add_participant).setVisibility(View.VISIBLE);
   }

   private void hideInCallButtons() {
      findViewById(R.id.button_hangup).setVisibility(View.INVISIBLE);
      findViewById(R.id.button_speaker).setVisibility(View.INVISIBLE);
      findViewById(R.id.button_silence).setVisibility(View.INVISIBLE);
      findViewById(R.id.button_add_participant).setVisibility(View.INVISIBLE);
   }

   private void showToast(final String msg) {
      this.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
         }
      });
   }

   /**
    * Returns a string containing a legible interpretation of the peer names.
    * E.g.:, the peer variable contains [0] Peter, [1] David and [2] Sarah,
    * then this method returns "Peter, David and Sarah".
    */
   private String getPeerNamesToDisplay() {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < peerNames.size(); i++) {
         if (i == peerNames.size() - 1) {
            builder.append(Utils.getSimpleName(peerNames.get(i)));
         } else {
            builder.append(Utils.getSimpleName(peerNames.get(i)));
            builder.append(", ");
         }
      }
      return builder.toString();
   }

   /**
    * Updates the peer name text according to the peers stored in
    * a private variable, which is updated when a participant joins
    * the call.
    */
   private void updatePeerName() {
      final String peers = getPeerNamesToDisplay();
      this.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            ((TextView) findViewById(R.id.text_peer)).setText(peers);
         }
      });
   }

   /**
    * Changes the call status text.
    *
    * @param status string containing the text to set
    */
   private void setCallStatus(final String status) {
      this.runOnUiThread(new Runnable() {
         @Override
         public void run() {
            ((TextView) findViewById(R.id.text_call_status)).setText(status);
         }
      });
   }

   /**
    * Shows the image of the first peer. Default image if it is not available.
    */
   private void showPeerImage() {
      String peer1 = peerNames.get(0);
      for (User u : smartPartyService.getListOfUsers()) {
         if (u.getUsername().equals(peer1)) {
            if (u.getImage() != null) {
               ((ImageView) findViewById(R.id.image_peer)).setImageBitmap(u.getImage());
               return;
            }
         }
      }
      ((ImageView) findViewById(R.id.image_peer)).setImageResource(R.drawable.default_profile);
   }

   /**
    * Called every time that a call is finalized. It turns off the speaker, stops the
    * ongoing notification, stores the call in the call register, sets the call
    * to not active in the {@link IncomingCallManager} singleton, stops the vibrator
    * and the counter (in case they were active), notifies the missed call
    * in case it's needed and calls the finish() method.
    */
   private void exitCall() {
      AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
      audioManager.setSpeakerphoneOn(false);
      audioManager.setMode(AudioManager.MODE_NORMAL);
      stopOnGoingNotification();
      storeCall();
      IncomingCallManager.getInstance().setCallActive(false);
      if (vibrator != null) vibrator.cancel();
      if (counter != null) counter.cancel(true);
      if (mustNotifyMissedCall()) {
         notifyMissedCall();
      }
      runOnUiThread(new Runnable() {
         @Override
         public void run() {
            finish();
         }
      });
   }

   /**
    * Stores the call in the preferences, so that this can be viewed
    * from the {@link CallsActivity}. Checks if <code>MAX_REGISTER_CALLS</code>
    * has been reached and deletes the oldest call in that case.
    */
   private void storeCall() {
      List<String> calls = Utils.getListsFromPreferences(PREFERENCES_CALLS, this);
      Queue<String> callsQueue = new PriorityQueue<>();
      callsQueue.addAll(calls);
      Call currentCall = new Call(getPeerNamesToDisplay(), counter.getDuration(), new Date(),
              getIntent().getBooleanExtra(INTENT_RECEIVE_CALL, false));
      callsQueue.offer(currentCall.getCompoundName());
      if (callsQueue.size() > MAX_REGISTER_CALLS) callsQueue.remove();
      List<String> callsToStore = new ArrayList<>(callsQueue);
      Utils.storeListInPreferences(PREFERENCES_CALLS, callsToStore, this);
   }

   /**
    * Checks if the call was being received, the duration is 0, it was not
    * manually rejected and the <code>NOTIFY_MISSED_CALL</code> is
    * true in preferences.
    *
    * @return boolean indicating whether a missed call must be notified
    */
   private boolean mustNotifyMissedCall() {
      return getIntent().getBooleanExtra(INTENT_RECEIVE_CALL, false) &&
              (counter.getDuration() == 0) && (!callWasRejected) &&
              getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                      .getBoolean(PREFERENCES_NOTIFY_MISSED_CALL,
                              PREFERENCES_NOTIFY_MISSED_CALL_DEFAULT);
   }

   /**
    * Notifies a missed call.
    */
   private void notifyMissedCall() {
      NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentTitle(getString(R.string.app_name))
              .setContentText(getPeerNamesToDisplay() + getString(R.string.called_you));

      Intent targetIntent = new Intent(this, MainActivity.class);
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent,
              PendingIntent.FLAG_UPDATE_CURRENT);
      builder.setContentIntent(contentIntent);
      // Create the notification.
      Notification notification = builder.build();
      notification.flags |= NotificationCompat.FLAG_AUTO_CANCEL;
      // And show the notification.
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
              .notify(IncomingCallManager.getInstance().getNotificationID(), notification);
   }

   /**
    * Shows an ongoing notification that allows the user to return to
    * the in call screen. This is called every second in order to
    * update the duration.
    *
    * @param time a formatted string containing the current duration of the call
    */
   private void showOnGoingNotification(String time) {
      NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
              .setSmallIcon(R.mipmap.ic_launcher)
              .setContentTitle(getString(R.string.app_name))
              .setContentText(getString(R.string.ongoing_call) + " (" + time + ")")
              .setOngoing(true);
      Intent targetIntent = new Intent(this, InCallActivity.class);

      targetIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
      targetIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, 0);

      builder.setContentIntent(contentIntent);
      // Create the notification.
      Notification notification = builder.build();
      // And show the notification.
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE))
              .notify(779, notification);
   }

   /**
    * Stops the ongoing notification.
    */
   private void stopOnGoingNotification() {
      ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(779);
   }

   /**
    * Returns an {@link AudioCallListener} used to listen to the changes of
    * the call.
    *
    * @return a listener object
    */
   private AudioCallListener getListener() {
      return new AudioCallListener() {
         @Override
         public void onRingingBack(String name) {
            setCallStatus(getString(R.string.connecting));
         }

         @Override
         public void onParticipantJoined(String name) {
            setCallStatus(getString(R.string.call_established));
            if (!isCallEstablished) {
               counter.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
               isCallEstablished = true;
            } else {
               peerNames.add(name);
               updatePeerName();
            }
         }

         @Override
         public void onParticipantRefused(String name) {
            if (!isCallEstablished) {
               exitCall();
            } else {
               showToast(name + getString(R.string.didnt_join_the_call));
            }
         }

         @Override
         public void onHangup(String name) {
            showToast(Utils.getSimpleName(name) + getString(R.string.finished_the_call));
            exitCall();
         }
      };
   }

   /**
    * Used when the add participant dialog is open in order to know
    * which user is selected.
    */
   private int getSelection() {
      return callSelection;
   }

   /**
    * Used when the add participant dialog is open in order to know
    * which user is selected.
    *
    * @param selection the selection in the list
    */
   private void setSelection(int selection) {
      callSelection = selection;
   }

   @Override
   protected void onDestroy() {
      AudioManager audioManager = ((AudioManager) getSystemService(Context.AUDIO_SERVICE));
      audioManager.setSpeakerphoneOn(false);
      audioManager.setMode(AudioManager.MODE_NORMAL);
      stopOnGoingNotification();
      IncomingCallManager.getInstance().setCallActive(false);
      if (vibrator != null) vibrator.cancel();
      if (counter != null) counter.cancel(true);
      super.onDestroy();
   }

   // Private class used to update the ongoing notification and the
   // upper textview every second.
   private class SecondsCounter extends AsyncTask<Void, Integer, Void> {
      private TextView textView;
      private int time = 0;

      SecondsCounter(TextView textView) {
         this.textView = textView;
      }

      int getDuration() {
         return time;
      }

      @Override
      protected void onProgressUpdate(Integer... values) {
         int seconds = time % 60;
         int minutes = (time / 60) % 60;
         String time = String.format("%02d:%02d", minutes, seconds);
         textView.setText(time);
         showOnGoingNotification(time);
      }

      @Override
      protected Void doInBackground(Void... params) {
         while (true) {
            if (isCancelled()) return null;
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
               e.printStackTrace();
               return null;
            }
            time++;
            publishProgress(time);
         }
      }
   }
}
