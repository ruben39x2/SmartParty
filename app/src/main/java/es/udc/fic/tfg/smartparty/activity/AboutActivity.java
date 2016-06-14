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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.FAQ;
import es.udc.fic.tfg.smartparty.util.Questions;
import es.udc.fic.tfg.smartparty.util.Utils;

/**
 * Class for the About of the app. This activity only shows a
 * list of card views, each of them with the format Question / Answer.
 *
 * @author Rubén Montero Vázquez
 */
public class AboutActivity extends AppCompatActivity {

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
      setContentView(R.layout.activity_about);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      Utils.customizeToolbar(toolbar, this);
      startFAQsList();
      showRateThisApp();
   }

   /**
    * Starts a linear layout and adds views programmatically. This
    * method iterates over the {@link Questions} list and inflates a
    * card view for each of them.
    *
    * @see FAQ
    */
   private void startFAQsList() {
      LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout_about);
      for (FAQ faq : Questions.get(this)) {
         View v = View.inflate(this, R.layout.card_faq, null);
         ((TextView) v.findViewById(R.id.text_question)).setText(faq.getQuestion());
         ((TextView) v.findViewById(R.id.text_answer)).setText(faq.getAnswer());
         linearLayout.addView(v);
      }
   }

   private void showRateThisApp(){
      final Context context = this;
      LinearLayout linearLayout = (LinearLayout) findViewById(R.id.layout_about);
      View v = View.inflate(this, R.layout.card_faq, null);
      TextView textAnwer = ((TextView) v.findViewById(R.id.text_answer));
      ((TextView) v.findViewById(R.id.text_question)).setText(R.string.question_rate);
      textAnwer.setText(R.string.answer_rate);
      textAnwer.setTextColor(Color.BLUE);
      textAnwer.setOnClickListener(new View.OnClickListener() {
                 @Override
                 public void onClick(View v) {
                    Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
                    Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
                    // To count with Play market backstack, After pressing back button,
                    // to taken back to our application, we need to add following flags to intent.
                    goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                              Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                    try {
                       startActivity(goToMarket);
                    } catch (ActivityNotFoundException e) {
                       startActivity(new Intent(Intent.ACTION_VIEW,
                               Uri.parse("http://play.google.com/store/apps/details?id="
                                       + context.getPackageName())));
                    }
                 }
              });
      linearLayout.addView(v);
   }
}