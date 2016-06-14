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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import es.udc.fic.tfg.smartparty.R;
import es.udc.fic.tfg.smartparty.activity.InCallActivity;
import es.udc.fic.tfg.smartparty.activity.MainActivity;
import es.udc.fic.tfg.smartparty.service.HelloClient;
import es.udc.fic.tfg.smartparty.service.User;

import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_ADDRESS;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_PEER_NAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_RECEIVE_CALL;
import static es.udc.fic.tfg.smartparty.util.Preferences.INTENT_SERVER_PORT;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_LISTS;
import static es.udc.fic.tfg.smartparty.util.Preferences.PREFERENCES_COMPLEX_USERNAME;
import static es.udc.fic.tfg.smartparty.util.Preferences.SHARED_PREFERENCES_NAME;

/**
 * This class controls the behaviour of the RecyclerView displayed in the {@link MainActivity}
 * that shows the users list.
 *
 * @author Rubén Montero Vázquez
 */
public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.UserViewHolder> {
   private List<User> users;
   private Context context;
   private String displayList;

   public RecyclerViewAdapter(List<User> users, String displayList, Context context) {
      this.users = users;
      this.context = context;
      this.displayList = displayList;
   }

   public void setDisplayList(String displayList){
      this.displayList = displayList;
   }

   @Override
   public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_user, parent, false);
      return new UserViewHolder(v);
   }

   @Override
   public void onBindViewHolder(UserViewHolder holder, int position) {
      // If we are displaying "all" list, then... OK, bind the viewholder.
      if (displayList.equals(context.getString(R.string.list_all))){
         holder.unHide();
         attachViewHolder(holder, position);
      } else { // If not, check the list we must display:
         String userTag = getAssociatedUserTag(Utils.getListsFromPreferences(displayList, context),
                 Utils.getIDName(users.get(position).getUsername()));
         if (userTag != null){ // Does it contain this user? Bind it.
            holder.unHide();
            attachViewHolder(holder, position, userTag);
         } else { // Does not? Hide it.
            holder.hide();
         }
      }
   }

   private String getAssociatedUserTag(List<String> list, String userID){
      for(String complexName : list){
         if (Utils.getIDName(complexName).equals(userID)){
            return Utils.getSimpleName(complexName);
         }
      }
      return null;
   }

   private void attachViewHolder(UserViewHolder holder, int position){
      if (users.get(position).getImage() == null) {
         holder.image.setImageResource(R.drawable.default_profile);
      } else {
         holder.image.setImageBitmap(users.get(position).getImage());
      }
      holder.title.setBackgroundColor(users.get(position).getThemeColor());
      holder.user.setText(Utils.getSimpleName(users.get(position).getUsername()));
      holder.status.setText(users.get(position).getStatus());
      holder.buttonCall.setOnClickListener(new OnClickCallListener(position));
      holder.buttonMenu.setOnClickListener(new OnClickMenuListener(position));

      Animation animation = AnimationUtils.loadAnimation(context, android.R.anim.slide_in_left);
      holder.cardView.startAnimation(animation);
   }

   private void attachViewHolder(UserViewHolder holder, int position, String userTag){
      attachViewHolder(holder, position);
      holder.user.setText(Utils.getSimpleName(users.get(position)
              .getUsername()) + " (" + userTag + ")");
   }

   @Override
   public int getItemCount() {
      return users.size();
   }

   public static class UserViewHolder extends RecyclerView.ViewHolder {
      private CardView cardView;
      private RelativeLayout title;
      private TextView user, status;
      private ImageView image;
      private ImageButton buttonCall, buttonMenu;
      private View itemView;

      public UserViewHolder(View itemView) {
         super(itemView);
         this.itemView = itemView;
         cardView = (CardView) itemView.findViewById(R.id.card_user);
         title = (RelativeLayout) itemView.findViewById(R.id.layout_user_card_title);
         user = (TextView) itemView.findViewById(R.id.text_user_card_name);
         status = (TextView) itemView.findViewById(R.id.text_user_card_status);
         image = (ImageView) itemView.findViewById(R.id.image_user);
         buttonCall = (ImageButton) itemView.findViewById(R.id.button_call);
         buttonMenu = (ImageButton) itemView.findViewById(R.id.button_user_menu);
      }

      public void hide(){
         ViewGroup.LayoutParams params = itemView.getLayoutParams();
         params.height = 0;
         itemView.setLayoutParams(params);
         itemView.setVisibility(View.INVISIBLE);
      }

      public void unHide(){
         ViewGroup.LayoutParams params = itemView.getLayoutParams();
         params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
         itemView.setLayoutParams(params);
         itemView.setVisibility(View.VISIBLE);
      }
   }

   private class OnClickCallListener implements View.OnClickListener {
      private int position;

      OnClickCallListener(int position) {
         this.position = position;
      }

      @Override
      public void onClick(View v) {
         if (users.get(position).isMe()) {
            Toast.makeText(context, context.getString(R.string.you_cannot_call_yourself),
                    Toast.LENGTH_SHORT).show();
            return;
         }
         if (!users.get(position).allowsAudioCalls()) {
            Toast.makeText(context, context.getString(R.string.old_device_doesnt_support_calls),
                    Toast.LENGTH_SHORT).show();
            return;
         }
         Intent intent = new Intent(context, InCallActivity.class);
         intent.putExtra(INTENT_ADDRESS, users.get(position).getInetAddress());
         intent.putExtra(INTENT_SERVER_PORT, users.get(position).getServerPort());
         intent.putExtra(INTENT_PEER_NAME, users.get(position).getUsername());
         intent.putExtra(INTENT_RECEIVE_CALL, false);
         context.startActivity(intent);
      }
   }

   private class OnClickMenuListener implements View.OnClickListener {
      private int userPosition;
      private AlertDialog dialog;

      OnClickMenuListener(int position) {
         this.userPosition = position;
      }

      @Override
      public void onClick(View v) {
         CharSequence items[] = new CharSequence[]{
                 context.getString(R.string.popup_menu_add_to_list),
                 context.getString(R.string.say_hello)};
         AlertDialog.Builder builder = new AlertDialog.Builder(context);
         builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
               switch (which) {
                  case 0:
                     showAddMenu();
                     break;
                  case 1:
                     if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB_MR1) {
                        new HelloTask().execute(users.get(userPosition));
                     } else {
                        new HelloTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                users.get(userPosition));
                     }

                     break;
                  default:
                     Toast.makeText(context, context.getString(R.string.unexpected_error),
                             Toast.LENGTH_SHORT).show();
               }
            }
         });
         builder.show();
      }

      private void showAddMenu() {
         // Here we initialize the data (which lists the user have defined).
         List<String> lists = Utils.getListsFromPreferences(PREFERENCES_LISTS, context);
         if (lists.isEmpty()) { // Exit if user doesn't have any list.
            Toast.makeText(context, context.getString(R.string.you_dont_have_lists),
                    Toast.LENGTH_SHORT).show();
            return;
         }
         final List<String> decodedLists = new ArrayList<>();
         for (String s : lists) {
            decodedLists.add((new FilterListName(s)).getName());
         }
         Collections.sort(decodedLists);
         decodedLists.add(context.getString(R.string.create_new_list));

         // Now we initialize the layout with a listView.
         View listViewLayout = View.inflate(context, R.layout.dialog_select_list, null);
         ListView listView = (ListView) listViewLayout.findViewById(R.id.list_view_add_to_filter);
         listView.setAdapter(
                 new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, decodedLists));

         // Initialize the onClickListener for the list.
         listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
               dialog.dismiss();
               if (!decodedLists.get(position).equals(context.getString(R.string.create_new_list))) {
                  addUser(decodedLists.get(position));
               } else {
                  showAddListDialog(users.get(userPosition).getUsername());
               }

            }
         });

         // And show the dialog.
         AlertDialog.Builder builder = new AlertDialog.Builder(context);
         builder.setView(listViewLayout);
         dialog = builder.create();
         dialog.show();
      }

      private void addUser(String listName){
         List<String> thatList = Utils.getListsFromPreferences(listName, context);
         String userToAdd = users.get(userPosition).getUsername();
         if (userToAdd.equals(context.getString(R.string.default_name))) {
            Toast.makeText(context, R.string.that_user_has_not_configured_name,
                    Toast.LENGTH_LONG).show();
            return;
         }
         // If the list does not contain already the ID of that user...
         if (getAssociatedUserTag(thatList, Utils.getIDName(userToAdd)) == null) {
            showAddUserTagDialog(userToAdd, thatList, listName);
         } else {
            Toast.makeText(context,
                    Utils.getSimpleName(userToAdd) + context.getString(R.string.is_already_in) +
                            "'" + listName + "'", Toast.LENGTH_SHORT).show();
         }
      }

         private boolean checkListExists(String listName){
            for(String myList : Utils.getListsFromPreferences(PREFERENCES_LISTS, context)){
               if ((new FilterListName(myList)).getName().toLowerCase()
                       .equals(listName.toLowerCase())) return true;
            }
            return false;
         }

      private void addList(FilterListName filterListName) {
         List<String> currentList = Utils.getListsFromPreferences(PREFERENCES_LISTS, context);
         currentList.add(filterListName.getCompoundName());
         Utils.storeListInPreferences(PREFERENCES_LISTS, currentList, context);
         ((MainActivity) context).refreshNavigationDrawerIcons();
      }

      private void showAddListDialog(final String userToFinallyAdd){
         /**
          * Called when we click on the floating action button used to add
          * a new list. It shows a dialog with an edit text for the input.
          */
         View addListView = View.inflate(context, R.layout.dialog_add_list, null);
         AlertDialog.Builder builder = new AlertDialog.Builder(context);
         final EditText userInput = (EditText) addListView.findViewById(R.id.edit_text_add_list);
         builder.setTitle(R.string.create_list_dialog_title)
                 .setView(addListView)
                 .setPositiveButton(R.string.ok_dialog,
                         new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                               String listName = String.valueOf(userInput.getText()).replace('\n', ' ');
                               if ((listName.length() != 0) && (!listName.equals(context.getString(R.string.list_all))) &&
                                       (!listName.equals(context.getString(R.string.create_new_list)))) {
                                  if (!checkListExists(listName)) {
                                     addList(new FilterListName(listName, true));
                                     showAddUserTagDialog(userToFinallyAdd,
                                             Utils.getListsFromPreferences(listName, context),
                                             listName);
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

      private void showAddUserTagDialog(final String userToAdd, final List<String> listToAdd,
                                        final String listName){
         View view = View.inflate(context, R.layout.dialog_add_user_to_list, null);
         AlertDialog.Builder builder = new AlertDialog.Builder(context);
         final EditText userInput = (EditText) view.findViewById(R.id.edit_text_add_user);
         userInput.setText(Utils.getSimpleName(userToAdd));
         builder.setTitle(R.string.configure_tag_dialog_title)
                 .setView(view)
                 .setCancelable(false)
                 .setPositiveButton(R.string.ok_dialog,
                         new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                               String tag = String.valueOf(userInput.getText())
                                       .replace('\n', ' ').replace('-', ' ');
                               if (tag.length() != 0) {
                                  listToAdd.add(Utils.getIDName(userToAdd) + "-" + tag);
                                  Utils.storeListInPreferences(listName, listToAdd, context);
                                  Toast.makeText(context, Utils.getSimpleName(userToAdd) +
                                          context.getString(R.string.was_successfully_added_to)
                                          + "'" + listName + "'", Toast.LENGTH_SHORT).show();
                               }
                            }
                         });
         builder.create().show();
      }

      // This private class represents an asynchronous task used to ping an user and check
      // if he/she is still available on the network.
      private class HelloTask extends AsyncTask<User, Void, Boolean> {
         private AlertDialog alertDialog;

         @Override
         protected void onPreExecute() {
            super.onPreExecute();
            View dialog = View.inflate(context, R.layout.dialog_processing, null);
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(dialog)
                    .setCancelable(true);
            alertDialog = builder.create();
            alertDialog.show();
         }

         @Override
         protected void onPostExecute(Boolean reachable) {
            super.onPostExecute(reachable);
            if (reachable) {
               alertDialog.dismiss();
               Toast.makeText(context, context.getString(R.string.hello_was_said_to)
                               + Utils.getSimpleName(users.get(userPosition).getUsername()),
                       Toast.LENGTH_SHORT).show();
            } else {
               alertDialog.dismiss();
               showRemoveDialog();
            }
         }

         @Override
         protected Boolean doInBackground(User... params) {
            HelloClient helloClient = new HelloClient(context
                    .getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE)
                    .getString(PREFERENCES_COMPLEX_USERNAME, context.getString(R.string.default_name)));
            return helloClient.sendHello(users.get(userPosition)
                    .getInetAddress(), users.get(userPosition).getServerPort());
         }

         private void showRemoveDialog() {
            // Build a dialog.
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.remove_user_dialog_title)
                    .setMessage(Utils.getSimpleName(users.get(userPosition).getUsername()) +
                            context.getString(R.string.remove_user_dialog_text))
                    .setPositiveButton(R.string.yes,
                            new DialogInterface.OnClickListener() {
                               @Override
                               public void onClick(DialogInterface dialog, int id) {
                                  ((MainActivity) context).getSmartPartyService().removeUserFromList(
                                          users.get(userPosition).getInetAddress()
                                  );
                               }
                            })
                    .setNegativeButton(R.string.no,
                            new DialogInterface.OnClickListener() {
                               public void onClick(DialogInterface dialog, int id) {
                                  dialog.cancel();
                               }
                            });

            // Create alert dialog and show it.
            builder.create().show();
         }
      }
   }
}
