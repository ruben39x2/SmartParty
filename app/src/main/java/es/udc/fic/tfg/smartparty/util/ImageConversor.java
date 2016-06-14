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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import java.io.ByteArrayOutputStream;

/**
 * Converts an image to a string, in order to store it in preferences, and vice versa.
 */
public class ImageConversor {
   public static Bitmap StringToBitMap(String encodedString) {
      try {
         byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
         return BitmapFactory.decodeByteArray(encodeByte, 0,
                 encodeByte.length);
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static String BitMapToString(Bitmap bitmap) {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
      byte[] b = baos.toByteArray();
      return Base64.encodeToString(b, Base64.DEFAULT);
   }
}
