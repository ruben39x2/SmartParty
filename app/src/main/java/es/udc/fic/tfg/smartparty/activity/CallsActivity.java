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

import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.Call;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_CALLS;

/**
 * Class for the Call Register of the app. This class only reads the
 * preferences, parses a list of calls and shows it.
 *
 * @author Rubén Montero Vázquez
 */
public class CallsActivity extends AppCompatActivity {

   /**
    * Method called when the activity is first created. It only
    * customizes the toolbar and generates programmatically the
    * layout for the card views.
    *
    * @param savedInstanceState not used
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_calls);
      initializeCollapsingToolbar();
      initializeData();
   }

   /**
    * Starts the collapsing toolbar layout and customizes it.
    *
    * @see Utils
    */
   private void initializeCollapsingToolbar() {
      CollapsingToolbarLayout toolBarLayout =
              (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
      toolBarLayout.setTitle(getTitle());
      Utils.customizeToolbar(toolBarLayout, this);
   }

   /**
    * Reads the <code>PREFERENCES_CALLS</code> in preferences, which contains
    * a list of data structured in JSON format.
    * <p/>
    * This method adds a card view for each call contained in the preferences.
    *
    * @see Call
    */
   private void initializeData() {
      List<Call> calls = new ArrayList<>();
      LinearLayout callsContainer = (LinearLayout) findViewById(R.id.layout_calls);
      for (String call : Utils.getListsFromPreferences(PREFERENCES_CALLS, this)) {
         calls.add(new Call(call));
      }
      if (!calls.isEmpty()) findViewById(R.id.text_no_calls_yet).setVisibility(View.GONE);
      Collections.sort(calls);
      Collections.reverse(calls);
      for (Call call : calls) {
         View v = View.inflate(this, R.layout.card_call, null);
         DateFormat dateFormat = SimpleDateFormat.getDateInstance();
         String date = dateFormat.format(call.getDate());
         int seconds = call.getDuration() % 60;
         int minutes = (call.getDuration() / 60) % 60;
         ((TextView) v.findViewById(R.id.text_call_peers)).setText(call.getPeers());
         ((TextView) v.findViewById(R.id.text_call_duration))
                 .setText(String.format("%02d:%02d", minutes, seconds));
         ((TextView) v.findViewById(R.id.text_call_date)).setText(date);
         if (call.isReceived()) {
            ((ImageView) v.findViewById(R.id.image_call_type))
                    .setImageResource(R.drawable.sym_call_incoming);
         } else {
            ((ImageView) v.findViewById(R.id.image_call_type))
                    .setImageResource(R.drawable.sym_call_outgoing);
         }
         callsContainer.addView(v);
      }
   }
}
