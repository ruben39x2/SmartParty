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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * This class is a simple TCP client that sends a message in JSON requesting a "Hello".
 *
 * @author Rubén Montero Vázquez
 */
public class HelloClient {
   private String myUsername;

   public HelloClient(String myUsername) {
      this.myUsername = myUsername;
   }

   public boolean sendHello(InetAddress host, int port) {
      try {
         Socket client = new Socket();
         client.connect(new InetSocketAddress(host, port), 2000);

         // We send the request.
         OutputStream os = client.getOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(os);
         JSONObject message = new JSONObject();
         message.put("protocol", "SmartParty Protocol v1.0");
         message.put("messageType", "Hello");
         message.put("name", myUsername);
         oos.writeObject(message.toString());

         // And now wait for the answer.
         InputStream is = client.getInputStream();
         ObjectInputStream ois = new ObjectInputStream(is);
         JSONObject reply = new JSONObject(ois.readObject().toString());
         client.close();
         return reply.getString("messageType").equals("Hello Acceptance");
      } catch (IOException | JSONException | ClassNotFoundException e) {
         e.printStackTrace();
         return false;
      }
   }
}