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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Class that represent an AudioCall being received. This only contains ONE audiostream. In case
 * of this is a multiconference, we will receive all the audios mixed from it.
 *
 * @author Rubén Montero Vázquez
 */
public class SlaveAudioCall extends AudioCall {
   private AudioManager audioManager;
   private Socket socket;
   private int remoteRtpPort;
   private AudioStream audioStream;
   private AudioGroup audioGroup;
   private AudioCallListener listener;
   private Context context;

   public SlaveAudioCall(AudioCallListener listener, Socket socket,
                         int remoteRtpPort, Context context) {
      audioGroup = new AudioGroup();
      audioGroup.setMode(AudioGroup.MODE_ECHO_SUPPRESSION);
      audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      this.listener = listener;
      this.socket = socket;
      this.remoteRtpPort = remoteRtpPort;
      this.context = context;
      listenMessages();
   }

   public void setRinging() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               JSONObject callRinging = new JSONObject();
               callRinging.put("protocol", "SmartParty Protocol v1.0");
               callRinging.put("messageType", "Ringing");
               PrintWriter pw = new PrintWriter(socket.getOutputStream());
               pw.println(callRinging.toString());
               pw.flush();
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   private void listenMessages() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            // Start input stream.
            try {
               boolean finish = false;
               BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
               do {
                  JSONObject reply = new JSONObject(br.readLine());
                  String messageType = reply.getString("messageType");
                  switch (messageType) {
                     case "Participant Joined":
                        String joinedAddress = reply.getString("inetAddress");
                        String joinedName = reply.getString("name");
                        listener.onParticipantJoined(joinedName);
                        break;
                     case "Hangup":
                        String finisher = reply.getString("finisher");
                        listener.onHangup(finisher);
                        finish = true;
                        break;
                  }
               } while (!finish);

               audioManager.setMode(AudioManager.MODE_NORMAL);
               socket.close();
               audioStream.join(null);
               audioGroup.clear();

            } catch (IOException | JSONException | NullPointerException e) {
               if (audioStream != null) audioStream.join(null);
               audioGroup.clear();
               audioManager.setMode(AudioManager.MODE_NORMAL);
               e.printStackTrace();
            }
         }
      }).start();
   }

   public void acceptAudioCall() {
      final AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
      audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
      audioManager.setMicrophoneMute(false);

      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               boolean finish = false;
               audioStream = new AudioStream(socket.getLocalAddress());
               audioStream.setCodec(AudioCodec.PCMU);
               audioStream.setMode(RtpStream.MODE_NORMAL);
               audioStream.associate(socket.getInetAddress(), remoteRtpPort);
               audioStream.join(audioGroup);

               // Say that we accept.
               JSONObject callAccepted = new JSONObject();
               callAccepted.put("protocol", "SmartParty Protocol v1.0");
               callAccepted.put("messageType", "Call Accepted");
               callAccepted.put("rtpPort", audioStream.getLocalPort());
               PrintWriter pw = new PrintWriter(socket.getOutputStream());
               pw.println(callAccepted.toString());
               pw.flush();
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   public void rejectAudioCall() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               JSONObject callRejected = new JSONObject();
               callRejected.put("protocol", "SmartParty Protocol v1.0");
               callRejected.put("messageType", "Call Rejected");
               PrintWriter pw = new PrintWriter(socket.getOutputStream());
               pw.println(callRejected.toString());
               pw.flush();
               socket.close();
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
   }

   @Override
   public void callParticipant(final InetAddress inetAddress, final int remotePort,
                               final String participantName) {
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               JSONObject add = new JSONObject();
               add.put("protocol", "SmartParty Protocol v1.0");
               add.put("messageType", "Add Participant");
               add.put("name", participantName);
               add.put("inetAddress", inetAddress.toString());
               add.put("port", remotePort);
               PrintWriter pw = new PrintWriter(socket.getOutputStream());
               pw.println(add.toString());
               pw.flush();
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
      listener.onRingingBack(participantName);
   }

   @Override
   public void hangUp() {
      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               JSONObject goodbye = new JSONObject();
               goodbye.put("protocol", "SmartParty Protocol v1.0");
               goodbye.put("messageType", "Hangup");
               PrintWriter pw = new PrintWriter(socket.getOutputStream());
               pw.println(goodbye.toString());
               pw.flush();
               socket.close();
               audioStream.join(null);
               audioGroup.clear();
               ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE)).setMode(AudioManager.MODE_NORMAL);
            } catch (IOException | JSONException e) {
               e.printStackTrace();
            }
         }
      }).start();
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
   public boolean isSlave() {
      return true;
   }
}
