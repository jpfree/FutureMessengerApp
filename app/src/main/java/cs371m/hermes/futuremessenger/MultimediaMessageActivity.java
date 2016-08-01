package cs371m.hermes.futuremessenger;

import android.app.DialogFragment;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

//TODO: Have these activities extend an MessageActivity, to remove duplicate code
public class MultimediaMessageActivity extends AppCompatActivity implements DatePickerFragment.DatePickerListener, TimePickerFragment.TimePickerListener {

    private static final int SELECT_IMAGE = 100;

    private EditText _contact_field;
    private Button _date_button;
    private Button _time_button;
    private EditText _message_field;
    private Uri _image_uri;

    /* Calendar which by the buttons */
    private Calendar _calendar;
    private final DateFormat df_date     = new SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US); //TODO: replace with DateFormat.getDateInstance(DateFormat.MEDIUM) ?
    private final DateFormat df_time     = new SimpleDateFormat("h:mm a", Locale.US); //TODO: replace with DateFormat.getTimeInstance(DateFormat.SHORT) ?
    private final DateFormat df_datetime = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    /* Are we making a brand new message?
     * If we're editing/deleting an existing message, store the ID of it here.
     * Otherwise it will be -1. */
    private long last_clicked_message_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multimedia_message);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        _calendar = Calendar.getInstance();
        setDateButtonText();
        setTimeButtonText();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // sendMMS("5554;5556", "Hello MMS!");
//                Log.d("click", _image_uri.getPath().toString());
                if (_image_uri != null) {
                    copyImage();
                }

                //TODO: Keep as reference for now, but remove this later
//                _calendar.set(Calendar.SECOND, 0);
//                Log.d("Click", df_datetime.format(_calendar.getTime()));
//                String temp_datetime = "2020-12-31 03:59:00";
//                try {
//                    Date date = df_datetime.parse(temp_datetime);
//                    Calendar c = (Calendar) _calendar.clone();
//                    c.setTime(date);
//                    printCalendar(c);
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                }
            }
        });

    }

    //TODO: Move method to AlarmReciever
    public void sendMMS(String phonenum, String message) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mmsto:" + phonenum));
        intent.putExtra("sms_body", message);
        File sdcard = Environment.getExternalStorageDirectory();
        File gif = new File (sdcard, "Download/dancing-banana.gif");
        Log.d("sendMMS", gif.getPath());
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(gif));

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(Intent.createChooser(intent, "Send MMS"));
        }
    }

    public void showTimePickerDialog (View v) {
        DialogFragment newFragment = new TimePickerFragment();
        newFragment.show(getFragmentManager(), "timePicker");
    }

    @Override
    public void onTimeSelected (int hour, int minute) {

    }

    public void showDatePickerDialog (View v) {
        DialogFragment newFragment = new DatePickerFragment();
        newFragment.show(getFragmentManager(), "datePicker");
    }

    @Override
    public void onDateSelected(int year, int month, int dayOfMonth) {

    }

    public void selectAttachment(View v) {
        // https://stackoverflow.com/questions/2507898/how-to-pick-an-image-from-gallery-sd-card-for-my-app
        //TODO: make it so that we can attach a video, image, or other files?
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, SELECT_IMAGE);
    }

    //SOURCE: https://stackoverflow.com/questions/10854211/android-store-inputstream-in-file
    private void copyImage() {
        String state = Environment.getExternalStorageState();
        String rootExtDir = Environment.getExternalStorageDirectory().toString();
        Log.d("", rootExtDir);

        String fileName = getFileName(_image_uri);
        File dst_file = new File(getExternalFilesDir(null), fileName);
        try {
            InputStream input = getContentResolver().openInputStream(_image_uri);
            OutputStream output = new FileOutputStream(dst_file);
            try {
                try {
                    byte[] buffer = new byte[1024];
                    int read;

                    while ((read = input.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                    output.flush();
                } finally {
                    output.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                input.close();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);

        switch (requestCode) {
            case SELECT_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    _image_uri = selectedImage;
                    try {
                        InputStream imageStream = getContentResolver().openInputStream(selectedImage);
                        Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);
                        ImageButton ib = (ImageButton) findViewById(R.id.mms_image_button);
                        ib.setImageBitmap(yourSelectedImage);
                    } catch (FileNotFoundException e) {
                        Log.d("onActivityResult", "FILE NOT FOUND");
                    }

                }
        }
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public void setDateButtonText() {
        Button b = (Button) findViewById(R.id.mms_button_date);

        Log.d("Date", df_date.format(_calendar.getTime()) );
        b.setTransformationMethod(null);
        b.setText(df_date.format(_calendar.getTime()));
    }

    public void setTimeButtonText() {
        Button b = (Button) findViewById(R.id.mms_button_time);

        Log.d("Date", df_time.format(_calendar.getTime()) );
        b.setText(df_time.format(_calendar.getTime()));
    }

    private void printCalendar(Calendar c, DateFormat df) {
        Log.d("print", df.format(c.getTime()));
    }
}
