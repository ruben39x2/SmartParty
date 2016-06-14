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

import android.graphics.Bitmap;
import android.support.annotation.NonNull;

import java.net.InetAddress;

import es.udc.fic.tfg.smartparty.util.Utils;

/**
 * POJO representing a Smart Party User.
 *
 * @author Rubén Montero Vázquez
 */
public class User implements Comparable<User>{
   private String username;
   private String status;
   private int themeColor;
   private Bitmap image;
   private InetAddress inetAddress;
   private int serverPort;
   private boolean isMe;
   private boolean allowsAudioCalls;

   public User(String username, String status, int themeColor, Bitmap image,
               InetAddress inetAddress, int serverPort, boolean isMe, boolean allowsAudioCalls) {
      this.username = username;
      this.status = status;
      this.themeColor = themeColor;
      this.image = image;
      this.inetAddress = inetAddress;
      this.serverPort = serverPort;
      this.isMe = isMe;
      this.allowsAudioCalls = allowsAudioCalls;
   }

   public String getUsername() {
      return username;
   }

   public String getStatus() {
      return status;
   }

   public int getThemeColor() {
      return themeColor;
   }

   public Bitmap getImage() {
      return image;
   }

   public void setImage(Bitmap image) {
      this.image = image;
   }

   public InetAddress getInetAddress() {
      return inetAddress;
   }

   public int getServerPort() {
      return serverPort;
   }

   public boolean isMe() {
      return isMe;
   }

   public boolean allowsAudioCalls() {
      return allowsAudioCalls;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof User)) return false;

      User user = (User) o;

      if (getThemeColor() != user.getThemeColor()) return false;
      if (getUsername() != null ? !getUsername().equals(user.getUsername()) : user.getUsername() != null)
         return false;
      if (getStatus() != null ? !getStatus().equals(user.getStatus()) : user.getStatus() != null)
         return false;
      if (getImage() != null ? !getImage().sameAs(user.getImage()) : user.getImage() != null)
         return false;
      return !(getInetAddress() != null ? !getInetAddress().equals(user.getInetAddress()) : user.getInetAddress() != null);

   }

   @Override
   public int compareTo(@NonNull User another) {
      return Utils.getSimpleName(this.getUsername())
              .compareTo(Utils.getSimpleName(another.getUsername()));
   }
}
