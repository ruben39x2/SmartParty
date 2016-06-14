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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.util.FilterListName;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_LIST_NAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_DEFAULT_EVERYONE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LISTS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LIST_EVERYONE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_NEED_TO_EXPLAIN_LISTS;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Class for the Activity used to manage the lists. Lists are
 * like groups of users. User can manage (create, edit and delete)
 * them. Lists act like filters. If one list is checked, then users included
 * in that list will be responded by {@link es.udc.fic.tfg.smartparty.service.SmartPartyService}
 * when they ask for our profile information.
 * There is a special list, "Everyone else". It represents all users
 * not included in other lists. This list can be checked or not.
 * So lists, can be used to:
 * <ul>
 * <li>allow certain people to know our presence</li>
 * <li>control if unknown people are able to know our presence</li>
 * <li>don't allow certain people to know our presence</li>
 * </ul>
 * <p/>
 * This activity has a java list of {@link FilterListName}. It contains
 * the updated set of lists (filter lists) stored in the format [T|F]listName.
 * T means list is enabled. F means list is disabled. We need to know whether
 * each list is activated or not. We store them in that special format to know it.
 * If one list changes the state, then it's erased and re-added.
 * The content of each list is stored as a JSON-encoded list in preferences with the
 * <code>key</code> of the list name (without the [T|F] part, obviously).
 * <p/>
 * This activity only allows user to manage the lists, but not the content of
 * each list. That functionality is provided by {@link ListActivity}
 *
 * @author Rubén Montero Vázquez
 */
public class ListManagerActivity extends AppCompatActivity {
   private List<FilterListName> lists;
   private LinearLayout listsContainer;
   private Context context = this;

   /**
    * Called when the activity is first created. Initializes the data.
    * Lists are represented with a card view. We don't use a recycler view
    * to show them, we just add those card views programmatically to a
    * linear layout.
    *
    * @param savedInstanceState not used
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_list_manager);
      Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
      setSupportActionBar(toolbar);
      Utils.customizeToolbar(toolbar, this);
      listsContainer = (LinearLayout) findViewById(R.id.layout_lists);
      initializePrivateLists();
      initializeListsContainer();
      updateEveryoneList();
      showTutorialIfFirstStart();
   }

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu_lists, menu);
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      switch (item.getItemId()) {
         case R.id.menu_add_list:
            showAddListDialog();
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   /**
    * Initializes the private variable "lists" that represents
    * the configured lists of the user.
    */
   private void initializePrivateLists() {
      List<String> currentList = Utils.getListsFromPreferences(PREFERENCES_LISTS, this);
      lists = new ArrayList<>();
      for (String s : currentList) {
         this.lists.add(new FilterListName(s));
      }
      Collections.sort(lists);
   }

   /**
    * For every list {@link FilterListName} in our "lists" variable,
    * add programmatically a card view.
    */
   private void initializeListsContainer() {
      if (!lists.isEmpty()) findViewById(R.id.text_no_lists_yet).setVisibility(View.GONE);
      for (FilterListName filterListName : lists) {
         addListCard(filterListName.getName(), filterListName.isActive());
      }
   }

   /**
    * Called multiple times when the linear layout with the card views is being
    * initialized. It adds a card view to it.
    *
    * @param name   name of the list
    * @param active whether is active of not
    */
   private void addListCard(String name, boolean active) {
      View v = View.inflate(this, R.layout.card_list, null);
      ((TextView) v.findViewById(R.id.text_list_item)).setText(name);
      ((CheckBox) v.findViewById(R.id.checkbox_list_item)).setChecked(active);
      listsContainer.addView(v);
   }

   /**
    * Resets the linear layout with the card views (that represents
    * the lists). Used when there is a change and we need to restart
    * the layout.
    */
   private void resetListsContainer() {
      listsContainer.removeAllViewsInLayout();
      initializeListsContainer();
   }

   /**
    * Called when we add a new list from the dialog. It stores it in preferences
    * and resets the layout.
    *
    * @param filterListName the name of the list
    */
   private void addList(FilterListName filterListName) {
      List<String> currentList = Utils.getListsFromPreferences(PREFERENCES_LISTS, this);
      currentList.add(filterListName.getCompoundName());
      Utils.storeListInPreferences(PREFERENCES_LISTS, currentList, this);
      lists.add(filterListName);
      Collections.sort(lists);
      resetListsContainer();
   }

   /**
    * Called when we delete a list by pressing the button. Removes the list
    * from preferences and restarts the layout.
    *
    * @param listName the name of the list
    */
   private void deleteList(String listName) {
      List<String> currentList = Utils.getListsFromPreferences(PREFERENCES_LISTS, this);
      List<String> toStoreList = new ArrayList<>();
      for (String compoundName : currentList) {
         if (!compoundName.substring(1).equals(listName)) toStoreList.add(compoundName);
      }
      Utils.storeListInPreferences(PREFERENCES_LISTS, toStoreList, this);
      Utils.removeKeyFromPreferences(listName, this);
      lists.remove(new FilterListName(listName, false));
      if (lists.isEmpty()) {
         listsContainer.setVisibility(View.GONE);
         findViewById(R.id.text_no_lists_yet).setVisibility(View.VISIBLE);
      } else {
         resetListsContainer();
      }
   }

   /**
    * Updates the checkBox of the generic list "everyone else".
    */
   private void updateEveryoneList() {
      boolean active =
              getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                      .getBoolean(PREFERENCES_LIST_EVERYONE, PREFERENCES_DEFAULT_EVERYONE);
      ((CheckBox) findViewById(R.id.checkbox_list_everyone)).setChecked(active);
   }

   /**
    * Called when we click on "delete list". It shows a dialog.
    *
    * @param v the view
    */
   public void onClickDeleteItem(View v) {
      TextView textView =
              (TextView) ((RelativeLayout) v.getParent()).findViewById(R.id.text_list_item);
      final String listName = (String) textView.getText();
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setTitle(R.string.delete_list_dialog_title)
              .setMessage(getString(R.string.are_you_sure_want_to_delete_list) + listName + "?")
              .setPositiveButton(R.string.yes,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            deleteList(listName);
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

   private boolean checkListExists(String listName){
      for(FilterListName myList : lists){
         if (myList.getName().toLowerCase().equals(listName.toLowerCase())) return true;
      }
      return false;
   }
   /**
    * Called when we click on the floating action button used to add
    * a new list. It shows a dialog with an edit text for the input.
    */
   public void showAddListDialog() {
      View addListView = getLayoutInflater().inflate(R.layout.dialog_add_list, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      final EditText userInput = (EditText) addListView.findViewById(R.id.edit_text_add_list);
      builder.setTitle(R.string.add_list_dialog_title)
              .setView(addListView)
              .setPositiveButton(R.string.ok_dialog,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            String listName = String.valueOf(userInput.getText()).replace('\n', ' ');
                            if ((listName.length() != 0) && (!listName.equals(getString(R.string.list_all))) &&
                                    (!listName.equals(getString(R.string.create_new_list)))) {
                               if (!checkListExists(listName)) {
                                  addList(new FilterListName(listName, true));
                               } else {
                                  Toast.makeText(context, R.string.that_list_exists, Toast.LENGTH_SHORT).show();
                               }
                            } else {
                               Toast.makeText(context, R.string.invalid_list_name, Toast.LENGTH_SHORT).show();
                            }
                         }
                      });
      builder.create().show();
   }



   /**
    * Called when we click on "edit list". It starts {@link ListActivity}.
    *
    * @param v the view
    */
   public void onClickEditItem(View v) {
      TextView textView =
              (TextView) ((RelativeLayout) v.getParent()).findViewById(R.id.text_list_item);
      String listName = (String) textView.getText();
      Intent intent = new Intent(this, ListActivity.class);
      intent.putExtra(INTENT_LIST_NAME, listName);
      startActivity(intent);
   }

   /**
    * Called when we click on a check box. It changes the state
    * of the related list (including the "everyone else" list).
    *
    * @param v the view
    */
   public void onClickChangeListCheckBox(View v) {
      boolean isChecked = ((CheckBox) v).isChecked();
      if (v.getId() == R.id.checkbox_list_everyone) {
         SharedPreferences.Editor editor =
                 getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
         editor.putBoolean(PREFERENCES_LIST_EVERYONE, isChecked);
         editor.apply();
         return;
      }
      TextView textView =
              (TextView) ((RelativeLayout) v.getParent()).findViewById(R.id.text_list_item);
      String listName = (String) textView.getText();
      setListStatus(listName, isChecked);
   }

   /**
    * Updates the status of a list. It iterates over the private
    * variable "lists" which contains the {@link FilterListName} of the
    * lists, and adds each of them using the <code>getCompoundName</code>
    * method (which returns a string like [T|F]listName) to a temporal list.
    * It also checks if it must update the status of a list.
    * At the end, it stores the temporal list.
    *
    * @param listName the name of the list
    * @param active   enabled or not
    */
   private void setListStatus(String listName, boolean active) {
      List<String> toStoreList = new ArrayList<>();
      for (FilterListName filterListName : lists) {
         if (filterListName.getName().equals(listName)) {
            filterListName.setActive(active);
         }
         toStoreList.add(filterListName.getCompoundName());
      }
      Utils.storeListInPreferences(PREFERENCES_LISTS, toStoreList, this);
   }

   private void showTutorialIfFirstStart(){
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
      if (prefs.getBoolean(PREFERENCES_NEED_TO_EXPLAIN_LISTS, true)){
         SharedPreferences.Editor editor = prefs.edit();
         editor.putBoolean(PREFERENCES_NEED_TO_EXPLAIN_LISTS, false);
         editor.apply();

         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setMessage(R.string.lists_explanation)
                 .setPositiveButton(R.string.got_it,
                         new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                            }
                         });
         builder.create().show();
      }
   }
}
