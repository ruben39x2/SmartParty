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

import android.net.rtp.AudioStream;

import java.net.InetAddress;
import java.net.Socket;

/**
 * POJO representing a participant in an AudioCall.
 *
 * @author Rubén Montero Vázquez
 */
public class Participant {
   private InetAddress address;
   private Socket socket;
   private String name;
   private AudioStream audioStream;

   public Participant(InetAddress address, Socket socket, String name, AudioStream audioStream) {
      this.address = address;
      this.socket = socket;
      this.name = name;
      this.audioStream = audioStream;
   }

   public InetAddress getAddress() {
      return address;
   }

   public Socket getSocket() {
      return socket;
   }

   public String getName() {
      return name;
   }

   public AudioStream getAudioStream() {
      return audioStream;
   }
}
