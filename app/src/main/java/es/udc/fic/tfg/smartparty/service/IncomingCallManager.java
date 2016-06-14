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

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Singleton used to store a socket, a boolean indicating that there is an active call in
 * the system and a list of additional guests of an AudioCall (in case of multiconference).
 *
 * We need this to store the socket because if we receive an AudioCall, the same socket
 * with the request must be used to signalize the call. But sockets cannot be passed between
 * activities, so, how do we pass it to the InCallActivity?
 *
 * Through this :)
 *
 * @author Rubén Montero Vázquez
 */
public class IncomingCallManager {
   private static IncomingCallManager manager;
   private Socket socket;
   private List<String> guests = new ArrayList<>();
   private boolean callActive = false;
   private int notificationID = 1;

   private IncomingCallManager() {
   }

   private static synchronized void initialize() {
      if (manager == null) {
         manager = new IncomingCallManager();
      }
   }

   public static IncomingCallManager getInstance() {
      if (manager == null) {
         initialize();
      }
      return manager;
   }

   public Socket getSocket() {
      return this.socket;
   }

   public void setSocket(Socket socket) {
      this.socket = socket;
   }

   public boolean getCallActive() {
      return callActive;
   }

   public void setCallActive(boolean active) {
      callActive = active;
      if (!active) {
         guests = new ArrayList<>();
      }
   }

   public void addGuest(String name) {
      guests.add(name);
   }

   public List<String> getGuests() {
      return guests;
   }

   public int getNotificationID() {
      return notificationID++;
   }
}