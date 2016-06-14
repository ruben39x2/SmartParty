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

package es.udc.fic.tfg.smartparty.audiocall;

import android.content.Context;
import android.media.AudioManager;
import android.net.rtp.AudioCodec;
import android.net.rtp.AudioGroup;
import android.net.rtp.AudioStream;
import android.net.rtp.RtpStream;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * This class is used to determine the behaviour of an AudioCall which has been started by
 * the user. This is, the current user stores a list with the rest of participants in the
 * AudioCall, because WE are the responsible of mixing the audiostreams and providing them
 * back to the other participants.
 *
 * @author Rubén Montero Vázquez
 */
public class MasterAudioCall extends AudioCall {
   private AudioCallListener listener;
   private List<Participant> participants = new ArrayList<>();
   private String myUsername;
   private AudioGroup audioGroup;
   private Context context;

   public MasterAudioCall(AudioCallListener listener, Context context) {
      audioGroup = new AudioGroup();
      audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
      this.listener = listener;
      this.myUsername = context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
              .getString(PREFERENCES_COMPLEX_USERNAME, context.getString(R.string.default_name));
      this.context = context;
   }

   // Here we send a JSON throw a socket to a callee and partially start an audio stream.
   @Override
   public void callParticipant(final InetAddress remoteAddress, final int remotePort,
                               final String name) {
      final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
      audioManager.setMicrophoneMute(false);

      final Socket[] socket = new Socket[1];

      // Let's manage each participant in a separate thread.
      Thread thread = new Thread(new Runnable() {
         @Override
         public void run() {
            AudioStream audioStream = null;
            try {
               boolean finish = false;
               socket[0] = new Socket(remoteAddress, remotePort);
               // Partially start the audio stream (in order to know the local RTP port).
               audioStream = new AudioStream(socket[0].getLocalAddress());
               audioStream.setCodec(AudioCodec.PCMU);
               audioStream.setMode(RtpStream.MODE_NORMAL);

               // Build the request and send it.
               JSONObject callRequest = new JSONObject();
               callRequest.put("protocol", "SmartParty Protocol v1.0");
               callRequest.put("messageType", "Call Solicitation");
               callRequest.put("name", myUsername);
               callRequest.put("rtpPort", audioStream.getLocalPort());
               if (!participants.isEmpty()) {
                  JSONArray guests = new JSONArray();
                  for (Participant p : participants) {
                     guests.put(p.getName());
                  }
                  callRequest.put("extraParticipants", guests.toString());
               }
               OutputStream os = socket[0].getOutputStream();
               ObjectOutputStream oos = new ObjectOutputStream(os);
               oos.writeObject(callRequest.toString());

               participants.add(new Participant(remoteAddress, socket[0], name, audioStream));

               // And start a loop to listen for what happens.
               InputStream is = socket[0].getInputStream();
               BufferedReader br = new BufferedReader(new InputStreamReader(is));
               do {
                  JSONObject reply = new JSONObject(br.readLine());
                  String messageType = reply.getString("messageType");
                  Log.i("THE MASTER", "got this message type: " + messageType);
                  switch (messageType) {
                     // If we receive "ringing", let's just show that it is ringing.
                     case "Ringing":
                        listener.onRingingBack(name);
                        break;
                     // If we receive "call accepted", let's associate the audio stream with
                     // the remote RTP port and start the audio.
                     case "Call Accepted":
                        int remotePort = reply.getInt("rtpPort");
                        audioStream.associate(remoteAddress, remotePort);
                        audioStream.join(audioGroup);
                        listener.onParticipantJoined(name);
                        // And inform other participants that one more has joined.
                        for (Participant participant : participants) {
                           informParticipantJoined(participant.getSocket(), remoteAddress, name);
                        }
                        break;
                     case "Call Rejected":
                        removeParticipantFromList(remoteAddress);
                        listener.onParticipantRefused(name);
                        socket[0].close();
                        finish = true;
                        break;
                     case "Add Participant":
                        String inetAddress = reply.getString("inetAddress");
                        String newName = reply.getString("name");
                        String port = reply.getString("port");
                        if (inetAddress.startsWith("/")) inetAddress = inetAddress.substring(1);
                        callParticipant(InetAddress.getByName(inetAddress), Integer.parseInt(port),
                                newName);
                        break;

                     // If we receive "hangup", then let's finish this stream.
                     case "Hangup":
                        listener.onHangup(name);
                        removeParticipantFromList(remoteAddress);
                        hangUp(name);
                        finish = true;
                        break;
                  }
               } while (!finish);

               // And here, finish audio stream, socket...
               audioStream.join(null);
               if (audioGroup.getStreams().length == 0) {
                  audioGroup.clear();
                  audioManager.setMode(AudioManager.MODE_NORMAL);
               }
            } catch (IOException | JSONException | NullPointerException e) {
               if (audioStream != null) audioStream.join(null);
               e.printStackTrace();
            }
         }
      });

      thread.start();
   }

   private void removeParticipantFromList(InetAddress address) {
      Iterator<Participant> iterator = participants.iterator();
      while (iterator.hasNext()) {
         Participant participant = iterator.next();
         if (participant.getAddress().equals(address)) {
            iterator.remove();
            break;
         }
      }
   }

   private void informParticipantJoined(Socket socket, InetAddress joinedAddress, String name) {
      try {
         PrintWriter pw = new PrintWriter(socket.getOutputStream());
         JSONObject joined = new JSONObject();
         joined.put("protocol", "SmartParty Protocol v1.0");
         joined.put("messageType", "Participant Joined");
         joined.put("name", name);
         joined.put("inetAddress", joinedAddress.toString());
         pw.println(joined.toString());
         pw.flush();
      } catch (IOException | JSONException e) {
         e.printStackTrace();
      }
   }

   private void informHangup(final Socket socket, final String finisherName) {
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               PrintWriter pw = new PrintWriter(socket.getOutputStream());
               JSONObject gone = new JSONObject();
               gone.put("protocol", "SmartParty Protocol v1.0");
               gone.put("messageType", "Hangup");
               gone.put("finisher", finisherName);
               pw.println(gone.toString());
               pw.flush();
               socket.close();
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   private void hangUp(String finisher) {
      for (Participant participant : participants)
         informHangup(participant.getSocket(), finisher);
   }

   @Override
   public void toggleSilence(boolean silence) {
      if (silence) {
         audioGroup.setMode(AudioGroup.MODE_MUTED);
      } else {
         audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
      }
   }

   @Override
   public void hangUp() {
      hangUp(myUsername);
   }

   @Override
   public boolean isSlave() {
      return false;
   }
}
