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

package es.udc.fic.tfg.smartparty.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.Utils;

/**
 * Class for the general configuration menu. This only displays 5 cardViews to access
 * the subconfiguration menus, which are:
 * - My Profile
 * - Calls
 * - Lists and privacy
 * - Settings
 * - About
 *
 * @author Rubén Montero Vázquez
 */
public class ConfigurationActivity extends AppCompatActivity {
   private Toolbar toolbar;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_configuration);
      toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
   }

   @Override
   protected void onStart() {
      super.onStart();
      Utils.customizeToolbar(toolbar, this);
   }

   public void onClickMyProfile(View v){
      final Intent intent = new Intent(this, ProfileActivity.class);
      startActivity(intent);
   }

   public void onClickCalls(View v){
      final Intent intent = new Intent(this, CallsActivity.class);
      startActivity(intent);
   }

   public void onClickLists(View v){
      final Intent intent = new Intent(this, ListManagerActivity.class);
      startActivity(intent);
   }

   public void onClickSettings(View v){
      final Intent intent = new Intent(this, SettingsActivity.class);
      startActivity(intent);
   }

   public void onClickAbout(View v){
      final Intent intent = new Intent(this, AboutActivity.class);
      startActivity(intent);
   }
}
