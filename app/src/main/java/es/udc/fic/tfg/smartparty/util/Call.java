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

import android.support.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

/**
 * POJO representing the information of a call, that will be displayed in the
 * {@link es.udc.fic.tfg.smartparty.activity.CallsActivity}
 *
 * This is not an {@link es.udc.fic.tfg.smartparty.audiocall.AudioCall}, don't get it wrong.
 *
 * @author Rubén Montero Vázquez
 */
public class Call implements Comparable<Call> {
   private String peers;
   private int duration;
   private Date date;
   private boolean received;

   public Call(String peers, int duration, Date date, boolean received) {
      this.peers = peers;
      this.duration = duration;
      this.date = date;
      this.received = received;
   }

   public Call(String jsonObject) {
      JSONObject object;
      try {
         object = new JSONObject(jsonObject);
         peers = object.getString("peers");
         duration = object.getInt("duration");
         date = new Date(object.getLong("date"));
         received = object.getBoolean("received");
      } catch (JSONException e) {
         e.printStackTrace();
      }
   }

   public String getPeers() {
      return peers;
   }

   public int getDuration() {
      return duration;
   }

   public Date getDate() {
      return date;
   }

   public boolean isReceived() {
      return received;
   }

   public String getCompoundName() {
      JSONObject object = new JSONObject();
      try {
         object.put("peers", peers);
         object.put("duration", duration);
         object.put("date", date.getTime());
         object.put("received", received);
      } catch (JSONException e) {
         e.printStackTrace();
         return "";
      }
      return object.toString();
   }

   @Override
   public int compareTo(@NonNull Call another) {
      return this.date.compareTo(another.getDate());
   }
}
