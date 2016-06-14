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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_LIST_NAME;

/**
 * Class for editing the content of a list. Allows user to remove / add
 * usernames to a list. This activity is shown like a dialog.
 *
 * @author Rubén Montero Vázquez
 * @see ListManagerActivity
 */
public class ListActivity extends AppCompatActivity {
   List<String> list = new ArrayList<>();
   ArrayAdapter adapter;
   String listName;

   /**
    * Called when the activity is first created. Gets the name of the
    * list from the intent and initializes it.
    *
    * @param savedInstanceState not used
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_list);
      listName = getIntent().getStringExtra(INTENT_LIST_NAME);
      setTitle(listName);
      initializeExplanationText();
      initializeList();
   }

   /**
    * Sets a small text in the down part of the dialog. The text is something like:
    * "To add any user to a list, you must see them connected." This way, users
    * won't get crazy looking for an "Add" button.
    */
   private void initializeExplanationText(){
      TextView textView = (TextView) findViewById(R.id.text_explanation);
      textView.setText(Html.fromHtml(getString(R.string.explanation_adding_users)));
      // Needed to make sure that the method is triggered (in some mobiles)
      textView.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            onClickExplanation(v);
         }
      });
   }

   /**
    * Takes the list data from preferences and shows it into a listView.
    */
   private void initializeList() {
      list = Utils.getListsFromPreferences(listName, this);
      if (!list.isEmpty()) findViewById(R.id.text_empty_list).setVisibility(View.INVISIBLE);
      ListView listView = (ListView) findViewById(R.id.list_view);
      adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list){

         // WHAT'S HAPPENING!?
         // Ok, let's see:
         // We have a list that contains complex user ids (like 17236482734-JohnDoe)
         // And we want it to display only JohnDoe (for instance)...
         // ...so we create a new Array Adapter overriding the "getItem" method. Easy.
         @Override
         public String getItem(int position) {
            return Utils.getSimpleName(super.getItem(position));
         }

      };
      listView.setAdapter(adapter);

      // Initialize the onClickListeners for the list
      listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
         @Override
         public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            showDeleteDialog(position);
         }
      });
      listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
         @Override
         public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
            showDeleteDialog(i);
            return true;
         }
      });
   }

   /**
    * Deletes an item and updates the adapter. If the list gets empty, it shows
    * a textView with "You have no users in this list" text.
    *
    * @param pos position in the array list variable
    */
   private void deleteUserFromList(int pos) {
      list.remove(pos);
      adapter.notifyDataSetChanged();
      updateList();
      if (list.isEmpty()) findViewById(R.id.text_empty_list).setVisibility(View.VISIBLE);
   }

   /**
    * Called every time that there is a modification on the list.
    * Saves the content of a private variable list to the preferences.
    */
   private void updateList() {
      Utils.storeListInPreferences(listName, list, this);
      if (!list.isEmpty()) findViewById(R.id.text_empty_list).setVisibility(View.INVISIBLE);
   }

    /**
    * Called when we click on an item from the list. Shows a dialog.
    *
    * @param pos the position of the user to delete in the private list
    */
   private void showDeleteDialog(final int pos) {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.delete_user_dialog_title)
              .setMessage(getString(R.string.are_you_sure_want_to_delete_the_user)
                      + Utils.getSimpleName(list.get(pos)) + "?")
              .setPositiveButton(R.string.yes,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            deleteUserFromList(pos);
                         }
                      })
              .setNegativeButton(R.string.no,
                      new DialogInterface.OnClickListener() {
                         public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                         }
                      });
      builder.create().show();
   }

   public void onClickExplanation(View v){
      final Intent intent = new Intent(this, AboutActivity.class);
      startActivity(intent);
   }
}
