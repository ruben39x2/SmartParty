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

import android.os.AsyncTask;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * A simple class running in a separate thread that executes a {@link ConnectionHandler} if we
 * receive a TCP connection.
 *
 * @author Rubén Montero Vázquez
 */
public class Server extends AsyncTask<Void, Void, Void> {
   ServerSocket serverSocket;
   SmartPartyService service;

   public Server(ServerSocket serverSocket, SmartPartyService service) {
      this.serverSocket = serverSocket;
      this.service = service;
   }

   @Override
   protected Void doInBackground(Void... params) {
      while (true) {
         try {
            Socket socket = serverSocket.accept();
            Runnable connectionHandler = new ConnectionHandler(socket, service);
            new Thread(connectionHandler).start();
         } catch (IOException e) {
            e.printStackTrace();
            return null;
         }
      }
   }
}