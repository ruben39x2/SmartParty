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

package es.udc.fic.tfg.smartparty.boot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import es.udc.fic.tfg.smartparty.service.SmartPartyService;
import es.udc.fic.tfg.smartparty.util.Utils;

/**
 * Class registered in AndroidManifest.xml as a receiver. The
 * onReceive method will be called every time that
 * "android.intent.action.BOOT_COMPLETED" action is triggered.
 *
 * @author Rubén Montero Vázquez
 */
public class BootReceiver extends BroadcastReceiver {

   /**
    * Starts the service when the system boots.
    *
    * @param context got from system
    * @param intent  got from system
    */
   @Override
   public void onReceive(Context context, Intent intent) {
      Utils.setServiceStatus(context, true);
      final Intent serviceIntent = new Intent(context, SmartPartyService.class);
      context.startService(serviceIntent);
   }
}