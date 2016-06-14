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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.service.IncomingCallManager;
import es.udc.fic.tfg.smartparty.service.SmartPartyService;
import es.udc.fic.tfg.smartparty.util.FilterListName;
import es.udc.fic.tfg.smartparty.util.RecyclerViewAdapter;
import es.udc.fic.tfg.smartparty.util.Utils;

import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_FIRST_START;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LISTS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_RECYCLER_SIZE;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_RECYCLER_SIZE_DEFAULT;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * Class that represents the main activity of the app.
 * It contains a navigation drawer that leads to other activities and
 * also shows a list of users connected to the same network. This list
 * of users is provided by a background service {@link SmartPartyService}.
 *
 * @author Rubén Montero Vázquez
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
   // Used to improve performance by avoiding calls to findViewById()
   private Toolbar toolbar;
   private DrawerLayout drawer;
   private RelativeLayout header;
   private RecyclerView recyclerView;
   private TextView messageText, displayListText;
   private ImageView imageViewMessage;
   private NavigationView navigationView;
   private CardView cardTitle;
   // Other variables
   private SmartPartyService smartPartyService;
   private boolean isBound = false;
   private RecyclerViewAdapter adapter;
   private String displayList;
   private Context context = this;
   // The service connection object that will listen to when
   // we are connected to the service or disconnected
   private ServiceConnection mConnection = new ServiceConnection() {

      @Override
      public void onServiceConnected(ComponentName className,
                                     IBinder service) {
         // We've bound to SmartPartyService, cast the IBinder and get SmartPartyService instance
         SmartPartyService.LocalBinder binder = (SmartPartyService.LocalBinder) service;
         smartPartyService = binder.getService();
         isBound = true;
         startList();
         updateMainView();
      }

      @Override
      public void onServiceDisconnected(ComponentName arg0) {
         isBound = false;
      }
   };

   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.menu_main, menu);
      if (isBound){
         if (smartPartyService.isPerformingScan()) {
            showRefreshingIcon(menu, true);
         } else {
            showRefreshingIcon(menu, false);
         }
      }
      return true;
   }

   @Override
   public boolean onOptionsItemSelected(final MenuItem item) {
      // Handle item selection
      switch (item.getItemId()) {
         case R.id.menu_refresh:
            if (!isBound) {
               Toast.makeText(this, R.string.there_was_an_error_pls_restart_service, Toast.LENGTH_LONG).show();
            } else {
               if (!smartPartyService.isPerformingScan()) {
                  smartPartyService.runDiscovery();
               }
            }
            return true;
         default:
            return super.onOptionsItemSelected(item);
      }
   }

   /**
    * Initializes the app. It stores some views and initializes the
    * navigation drawer. This method is called when the activity is first created.
    *
    * @param savedInstanceState not used
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_main);
      displayList = getString(R.string.list_all);
      configIfFirstStart();
      initializeViews();
      initializeNavigationDrawer();
      updateRoomName(Utils.getRoomName(this));
   }

   /**
    * Starts / binds to the {@link SmartPartyService} if necessary and
    * customizes the interface. This method is called every time that we
    * access to the activity.
    */
   @Override
   protected void onStart() {
      super.onStart();
      if (IncomingCallManager.getInstance().getCallActive()){
         final Intent intent = new Intent(this, InCallActivity.class);
         startActivity(intent);
         finish();
      } else {
         makeSureCurrentListStillExists();
         customizeInterface();
         runService();
         updateMainView();
         refreshNavigationDrawerIcons();
      }
   }

   /**
    * If we are viewing the "favorites" list, and then we go to settings -> lists -> delete
    * favorites lists; favs list will be deleted. OK. But when we press the back button
    * to get to main activity again... We'll be viewing "favorites" again!
    * This method ensures that the current displayList is valid and changes to list_all if not.
    */
   private void makeSureCurrentListStillExists() {
      if (!getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).contains(displayList)){
         displayList = getString(R.string.list_all);
      }
   }

   /**
    * Unbinds from the {@link SmartPartyService} if it's bound.
    */
   @Override
   protected void onStop() {
      super.onStop();
      if (isBound) {
         unbindService(mConnection);
         isBound = false;
      }
   }

   /**
    * Closes the navigation drawer if it's open and overrides default
    * behaviour otherwise.
    */
   @Override
   public void onBackPressed() {
      if (drawer.isDrawerOpen(GravityCompat.START)) {
         drawer.closeDrawer(GravityCompat.START);
      } else {
         super.onBackPressed();
      }
   }

   /**
    * Open the navigation drawer with the menu physical key
    *
    * @param keyCode we will only listen for menu keycode, and ignore otherwise
    * @param e see stackoverflow pls
    * @return not relevant
    */
   @Override
   public boolean onKeyDown(int keyCode, KeyEvent e) {
      if (keyCode == KeyEvent.KEYCODE_MENU) {
         if (!drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.openDrawer(GravityCompat.START);
         }
         return true;
      }
      return super.onKeyDown(keyCode, e);
   }

   /**
    * Handles navigation view item clicks.
    *
    * @param item the menu item
    * @return true to display the item as the selected item
    */
   @Override
   public boolean onNavigationItemSelected(MenuItem item) {
      int id = item.getItemId();
      String title = item.getTitle().toString();
      if (id == R.id.nav_configuration){
         final Intent intent = new Intent(this, ConfigurationActivity.class);
         startActivity(intent);
      } else {
         // The adapter might be null if service is disabled
         if (adapter != null){
            adapter.setDisplayList(title);
            adapter.notifyDataSetChanged();
         }
         displayList = title;
         updateMainView();
      }
      drawer.closeDrawer(GravityCompat.START);
      return true;
   }

   /**
    * Getter for the recyclerViewAdapter of the activity. This method is called
    * by the background {@link SmartPartyService} in order to notify the adapter
    * that an item has been added or removed.
    *
    * @return the adapter of recycler view
    */
   public RecyclerViewAdapter getAdapter() {
      return adapter;
   }

   /**
    * Getter for the background service object. This method is called by the
    * RecyclerViewAdapter class in order to access to the list of users.
    *
    * @return the background service
    */
   public SmartPartyService getSmartPartyService() {
      return smartPartyService;
   }

   /**
    * Obtains the current wifi SSID and puts it into the title of the action bar.
    */
   public void updateRoomName(String roomName) {
      ActionBar supportActionBar = getSupportActionBar();
      if (supportActionBar != null) {
         supportActionBar.setTitle(roomName);
      }
   }

   /**
    * Makes repeated calls to findViewById and stores the results.
    * FindViewById is a heavy operation, so we call it only once and save
    * the results in private variables.
    */
   private void initializeViews() {
      toolbar = (Toolbar) findViewById(R.id.toolbar);
      drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
      header = (RelativeLayout) findViewById(R.id.layout_nav_header);
      recyclerView = (RecyclerView) findViewById(R.id.recycler_view_main);
      messageText = (TextView) findViewById(R.id.text_main_message);
      imageViewMessage = (ImageView) findViewById(R.id.image_message);
      navigationView = (NavigationView) findViewById(R.id.nav_view);
      cardTitle = (CardView) findViewById(R.id.card_title_list);
      displayListText = (TextView) findViewById(R.id.text_title_list);
   }

   /**
    * Initializes the navigation drawer view. First sets the action bar,
    * and the starts the navigation drawer.
    */
   private void initializeNavigationDrawer() {
      // First we set the toolbar.
      setSupportActionBar(toolbar);

      // Now we setup this to listen for the state changes in the Navigation Drawer.
      ActionBarDrawerToggle toggle =
              new ActionBarDrawerToggle(this, drawer, toolbar,
                      R.string.navigation_drawer_open, R.string.navigation_drawer_close);
      drawer.setDrawerListener(toggle);
      toggle.syncState();

      // No need to store navigationView as a local variable
      navigationView.setNavigationItemSelectedListener(this);
      refreshNavigationDrawerIcons();
   }

   public void refreshNavigationDrawerIcons(){
      SubMenu listsGroup = navigationView.getMenu().findItem(R.id.submenu_1).getSubMenu();
      listsGroup.clear();
      listsGroup.add(getString(R.string.list_all)).setIcon(R.drawable.ic_menu_cc_am);
      for(String list : Utils.getListsFromPreferences(PREFERENCES_LISTS, this)) {
         listsGroup.add((new FilterListName(list)).getName()).setIcon(R.drawable.ic_menu_friend);
      }
   }

   /**
    * Change the color of the toolbar and set the data on the Navigation Drawer header.
    */
   private void customizeInterface() {
      Utils.customizeNavHeader(header, this);
      Utils.customizeToolbar(toolbar, cardTitle, this);
   }

   /**
    * Starts the service if it is enabled.
    *
    * @see Utils
    */
   private void runService() {
      if (Utils.isServiceEnabled(this)) {
         final Intent intent = new Intent(this, SmartPartyService.class);
         startService(intent);
         bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
      }
   }

   /**
    * Starts the list by initializing the adapter and calls the
    * background {@link SmartPartyService} passing the activity as
    * an object so that the service can update the adapter in the
    * UI thread. This method is called when the service is connected.
    */
   private void startList() {
      int size = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
              .getInt(PREFERENCES_RECYCLER_SIZE, PREFERENCES_RECYCLER_SIZE_DEFAULT);
      recyclerView.setLayoutManager(new GridLayoutManager(this, size));
      adapter = new RecyclerViewAdapter(smartPartyService.getListOfUsers(), displayList, this);
      recyclerView.setAdapter(adapter);
      smartPartyService.makeListAutoUpdatable(this);
   }

   /**
    * Changes the number of elements displayed horizontally in the
    * recycler view. By default, it's 1. This method changes it in
    * preferences and changes from 1 to 2 and vice-versa.
    *
    * Currently this is not used, but we maintain it in case we want to display
    * 2 users per row.
    */
   private void changeRecyclerSize() {
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
      SharedPreferences.Editor editor = prefs.edit();
      int currentSize = prefs.getInt(PREFERENCES_RECYCLER_SIZE, PREFERENCES_RECYCLER_SIZE_DEFAULT);
      int newSize;
      if (currentSize == 1)
         newSize = 2;
      else
         newSize = 1;
      recyclerView.setLayoutManager(new GridLayoutManager(this, newSize));
      editor.putInt(PREFERENCES_RECYCLER_SIZE, newSize);
      editor.apply();
   }

   /**
    * Updates the main view according to the status of the app.
    * First, this method hides all the views and then checks:
    * <ul>
    * <li>If the service is disabled</li>
    * <li>If the service is bound</li>
    * <li>If the discovery is running</li>
    * <li>If there was a general error</li>
    * <li>If the network is compatible</li>
    * <li>If there are no users connected which belong to the currently selected list</li>
    * <li>If the list of users is empty</li>
    * </ul>
    */
   public void updateMainView() {
      hideAllViews();
      invalidateOptionsMenu();
      displayListText.setText(displayList);
      messageText.setOnClickListener(null);
      if (!Utils.isServiceEnabled(this)) {
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(R.string.discovery_service_disabled);
         return;
      }
      if (!isBound) {
         imageViewMessage.setVisibility(View.VISIBLE);
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(R.string.not_connected_to_service);
         return;
      }
      if (smartPartyService.isPerformingScan()){
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(R.string.performing_scan);
         return;
      }
      if (smartPartyService.isGeneralError()) {
         imageViewMessage.setVisibility(View.VISIBLE);
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(R.string.general_error);
         return;
      }
      if (smartPartyService.isNetworkNotCompatible()) {
         imageViewMessage.setVisibility(View.VISIBLE);
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(R.string.not_connected_to_wifi);
         return;
      }
      if (!displayList.equals(getString(R.string.list_all)) &&
              !Utils.isThereAnyUser(smartPartyService.getListOfUsers(), displayList, this)){
         imageViewMessage.setVisibility(View.VISIBLE);
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(Html.fromHtml(getString(R.string.it_seems_that_none_of_your_friends_in) + displayList + getString(R.string.is_now_connected_click_here)));
         messageText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               final Intent intent = new Intent(context, ListManagerActivity.class);
               startActivity(intent);
            }
         });
         return;
      }
      if (smartPartyService.getListOfUsers().isEmpty()) {
         imageViewMessage.setVisibility(View.VISIBLE);
         messageText.setVisibility(View.VISIBLE);
         messageText.setText(getString(R.string.no_users_in_room));
         return;
      }
      recyclerView.setVisibility(View.VISIBLE);
   }

   /**
    * Hides all the message texts and the recycler view.
    */
   private void hideAllViews() {
      recyclerView.setVisibility(View.GONE);
      messageText.setVisibility(View.GONE);
      imageViewMessage.setVisibility(View.GONE);
   }

   /**
    * This shows the "Change name" dialog if the users starts the app for the first time.
    */
   private void configIfFirstStart(){
      SharedPreferences prefs = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE);
      if (prefs.getBoolean(PREFERENCES_FIRST_START, true)){
         SharedPreferences.Editor editor = prefs.edit();
         editor.putBoolean(PREFERENCES_FIRST_START, false);

         editor.putString(PREFERENCES_COMPLEX_USERNAME, Utils.getUniqueDeviceID(context) + '-' +
                 Utils.getSimpleName(getString(R.string.default_name)));
         editor.apply();

         List<String> myUnconfiguredLists = new ArrayList<>();
         myUnconfiguredLists.add((new FilterListName(getString(R.string.list_favorites), true)
                 .getCompoundName()));
         Utils.storeListInPreferences(PREFERENCES_LISTS, myUnconfiguredLists, this);
         Toast.makeText(this, R.string.pls_configure_username, Toast.LENGTH_SHORT).show();

         showChangeNameDialog();
      }
   }

   private void showChangeNameDialog(){
      View changeUsernameView = View.inflate(this, R.layout.dialog_username, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      final EditText userInput = (EditText) changeUsernameView.findViewById(R.id.edit_text_username);
      builder.setTitle(R.string.change_username_dialog_title)
              .setView(changeUsernameView)
              .setCancelable(false)
              .setPositiveButton(R.string.ok_dialog,
                      new DialogInterface.OnClickListener() {
                         @Override
                         public void onClick(DialogInterface dialog, int id) {
                            String username = String.valueOf(userInput.getText()).replace('\n', ' ')
                                    .replace('-', ' ');
                            if (username.length() != 0) {
                               SharedPreferences.Editor editor =
                                       getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE)
                                               .edit();
                               String complexUsername = Utils.getUniqueDeviceID(context) + '-' + username;
                               editor.putString(PREFERENCES_COMPLEX_USERNAME, complexUsername);
                               editor.commit();
                               customizeInterface();
                            }
                         }
                      });
      builder.create().show();
   }

   /**
    * Shows the progress bar in the menu. Used when a scan is being performed.
    *
    * @param menu the item containing the current menu of the activity
    * @param isRefreshing display / hide progress bar
    */
   private void showRefreshingIcon(Menu menu, boolean isRefreshing){
      final MenuItem item = menu.findItem(R.id.menu_refresh);
      if (isRefreshing){
         item.setActionView(new ProgressBar(this));
         item.getActionView().postDelayed(new Runnable() {
            @Override
            public void run() {
               item.setActionView(null);
            }
         }, 100000);
      } else {
         item.setActionView(null);
      }
   }

   /**
    * A shortcut to the edit profile menu.
    *
    * @param v not used
    */
   public void onClickEditProfile(View v){
      final Intent intent = new Intent(this, ProfileActivity.class);
      startActivity(intent);
   }
}

