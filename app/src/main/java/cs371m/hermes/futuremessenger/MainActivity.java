package cs371m.hermes.futuremessenger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

/**
 * Main activity of the app. Shows a list of scheduled messages and presets a menu
 * for creating new messages/presets.
 */
public class MainActivity extends AppCompatActivity {

    // Future Messenger's database helper.
    private MessengerDatabaseHelper mDb;

    // ID of the message in the ListView that was clicked last.
    private long mLast_clicked_message_id;

    /* When a message gets sent, the alarm receiver will broadcast this action to refresh
       the ListView. */
    private static final String REFRESH_LV_ACTION = "cs371m.hermes.futuremessenger.refreshlv";

    // Receiver for refresh ListView broadcasts
    BroadcastReceiver refreshLVReceiver =  new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            fillListView();
        }
    };

    // Inflate the individual message edit/delete menu.
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.message_menu, menu);
    }

    // Message context menu options.
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit:
                editScheduledMessage();
                return true;
            case R.id.delete:
                deleteScheduledMessage();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.currently_scheduled_tv);

        // Create our database.
        mDb = MessengerDatabaseHelper.getInstance(this);
        ListView scheduledListView = (ListView) findViewById(R.id.scheduled_messages_list);
        scheduledListView.setEmptyView(findViewById(R.id.empty_messages_list_tv));

        // Populate the ListView from the database.
        fillListView();

        initializeFloatingMenu();
        registerRefreshReceiver();
    }

    /* Registers a receiver to update the list view if a message gets sent off and deleted
       while the activity is open. */
    private void registerRefreshReceiver() {
        LocalBroadcastManager.getInstance(this).registerReceiver(
                refreshLVReceiver, new IntentFilter (REFRESH_LV_ACTION));
    }

    // Unregister the refreshLV receiver
    private void unregisterRefreshReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshLVReceiver);
    }

    // Initialize the floating actions menu and its buttons.
    private void initializeFloatingMenu() {
        final FloatingActionsMenu main_menu = (FloatingActionsMenu) findViewById(R.id.main_menu);

        // Initialize the preset button
        com.getbase.floatingactionbutton.FloatingActionButton preset_button =
                (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.create_preset_button);
        preset_button.setIconDrawable(getResources().getDrawable(R.drawable.preset_icon));
        preset_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main_menu.collapse();
                launchPresetActivity();
            }
        });

        // Initialize the text message button
        com.getbase.floatingactionbutton.FloatingActionButton text_button =
                (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.new_text_button);
        text_button.setIconDrawable(getResources().getDrawable(R.drawable.text_icon));
        text_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main_menu.collapse();
                createTextMessage();
            }
        });

        // Initialize the picture message button
        com.getbase.floatingactionbutton.FloatingActionButton pic_button =
                (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.new_pic_button);
        pic_button.setIconDrawable(getResources().getDrawable(R.drawable.picture_icon));
        pic_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                main_menu.collapse();
                createPictureMessage();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Close the database connection.
        mDb.close();
        unregisterRefreshReceiver();
    }


    private void launchPresetActivity(){
        Intent intent = new Intent(this, ManagePresets.class);
        startActivity(intent);
    }

    private void createTextMessage() {
        Intent intent = new Intent(this, EditTextMessageActivity.class);
        startActivityForResult(intent, 1);
    }

    private void createPictureMessage() {
        Intent intent = new Intent(this, MultimediaMessageActivity.class);
        startActivityForResult(intent, 1);
    }

    /* User requested to edit a currently scheduled message.
    *  Package the selected message's data into a bundle and start the
    *  edit activity. */
    private void editScheduledMessage() {
        // Get the message's data.
        Bundle message_info = mDb.getScheduledMessageData(mLast_clicked_message_id);
        mDb.close();
        if (message_info != null) {
            String recip_names = message_info.getString("recip_names");
            String recip_nums = message_info.getString("recip_nums");
            String message = message_info.getString("message");
            String image_path = message_info.getString("image_path");
            int group_flag = message_info.getInt("group_flag");
            String date = message_info.getString("date");
            String time = message_info.getString("time");
            String dateTime = message_info.getString("dateTime");

            // Place the data in an intent.
            Intent intent;
            if (image_path == null || image_path.equals("")) {
                // text message, because no image was specified
                Log.d("edit SMS", "image_path is null, or equals empty string");
                intent = new Intent(this, EditTextMessageActivity.class);
            } else {
                // picture message, because an image was specified
                Log.d("edit MMS", image_path);
                intent = new Intent(this, MultimediaMessageActivity.class);
            }
            intent.putExtra("recip_names", recip_names);
            intent.putExtra("recip_nums", recip_nums);
            intent.putExtra("message", message);
            intent.putExtra("image_path", image_path);
            intent.putExtra("group_flag", group_flag);
            intent.putExtra("date", date);
            intent.putExtra("time", time);
            intent.putExtra("message_datetime", dateTime);
            intent.putExtra("message_id", mLast_clicked_message_id);

            // Start the edit message activity through this intent.
            startActivityForResult(intent, 1);
        }
        else {
            Toast.makeText(MainActivity.this, "That message can't be edited.", Toast.LENGTH_SHORT).show();
        }
    }


    /* Delete a currently scheduled message. */
    private void deleteScheduledMessage() {
        stopAlarm(mLast_clicked_message_id);
        mDb.deleteMessage(mLast_clicked_message_id);
        mDb.close();
        // Force a refresh of the listView so that the changes will be reflected in the ListView.
        fillListView();
    }

    /* Delete a currently scheduled alarm (called from deleteScheduledMessage())
    * For alarm cancel to work, pending intent MUST match the
    * pending intent used to create the alarm */
    public void stopAlarm(long message_id){
        Intent alarmIntent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(),
                (int) message_id, alarmIntent, PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.d("MainActivity stopalarm", "Alarm canceled");
    }

    /* Populate the ListView from our database with all of the currently scheduled messages. */
    private void fillListView() {
        Cursor cursor = mDb.getAllScheduledMessages();
        ContactDatabaseAdapter adapter =
                new ContactDatabaseAdapter(getBaseContext(), cursor, R.layout.listed_message_layout);
        ListView messagesListView = (ListView) findViewById(R.id.scheduled_messages_list);
        messagesListView.setAdapter(adapter);

        /* Make the list items clickable for their context menu */
        registerForContextMenu(findViewById(R.id.scheduled_messages_list));

        // Allow short clicks to open the context menu
        messagesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int pos, long id) {
                mLast_clicked_message_id = id;
                openContextMenu(findViewById(R.id.scheduled_messages_list));
                Log.d("Short Click", "Last clicked message id just set to " + mLast_clicked_message_id);
            }
        });

        // Allow long clicks to open the context menu.
        messagesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
                                           int pos, long id) {
                mLast_clicked_message_id = id;
                Log.d("Long Click", "Last clicked message id just set to " + mLast_clicked_message_id);
                return false;
            }
        });
        mDb.close();
    }

    // An edit activity just returned after saving something, so we will refresh the ListView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if(resultCode == MainActivity.RESULT_OK) {
                fillListView();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Force a refresh on the ListView
        fillListView();
        registerRefreshReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterRefreshReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterRefreshReceiver();
    }
}
