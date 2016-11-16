package com.daniel.bikehours;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.regex.Pattern;


/*TODO
- New entry --> käyttäjä voi syöttää oman päivämäärän.
                Ajopaikkaan ehdotuksena/valikkona aikaisemmat paikat.
                gps location
-virheenkäsittelyt
-tallennuspaikka datalle
-tabs for different vehicles  pyörän nimi vaihtaa pyörien välillä pyyhkäisemällä
-action bar

 */

public class MainActivity extends Activity {

    LinkedList<LogEntry> list = new LinkedList<>();
    private String titteli = "";
    int initialMinutes;
    int hour;
    int minute;
    private boolean doubleBackToExitPressedOnce;
    public Handler mHandler;
    private final Runnable mRunnable;

    {
        mRunnable = new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mHandler==null) mHandler = new Handler();
        readFile();
        loadMain();

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

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_settings:
                Toast.makeText(getApplicationContext(), "Settings pressed...", Toast.LENGTH_SHORT).show();
                loadSettings();
                break;
            case R.id.help:
                Toast.makeText(getApplicationContext(), "No help for you...", Toast.LENGTH_SHORT).show();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadSettings() {
        setContentView(R.layout.settings_layout);
        Button deleteData = (Button) findViewById(R.id.deletedata);
        deleteData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteFile();
                list.clear();
                titteli = "";
                initialMinutes = 0;
                loadMain();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.doubleBackToExitPressedOnce = false;
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        if (mHandler != null) { mHandler.removeCallbacks(mRunnable); }
    }

    @Override
    public void onBackPressed() {
        loadMain();
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.exit_press_back_twice_message, Toast.LENGTH_SHORT).show();
        mHandler.postDelayed(mRunnable, 2000);
    }

    private void loadMain() {
        setContentView(R.layout.main_layout);
        TextView title = (TextView) findViewById(R.id.title);

        if (titteli.isEmpty()) {
            AlertDialog.Builder alert1 = new AlertDialog.Builder(this);
            alert1.setTitle(getString(R.string.vehicle_name_query));
            final EditText input1 = new EditText(this);
            alert1.setView(input1);
            alert1.setPositiveButton(getString(R.string.positive_answer), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    titteli = input1.getText().toString();
                    saveFile();
                    if (initialMinutes == 0)buildInitialMinutesDialog();
                }
            });

            alert1.show();
       }

        title.setText(titteli);

        Button addNew = (Button) findViewById(R.id.add_new);
        addNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInsertData();
            }
        });
        update();
    }


    private void buildInitialMinutesDialog(){
        final AlertDialog.Builder alert2 = new AlertDialog.Builder(this);
        alert2.setTitle(getString(R.string.initial_hours_query));
        final EditText input2 = new EditText(this);
        alert2.setView(input2);
        input2.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
        alert2.setPositiveButton(getString(R.string.positive_answer), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                try {
                    String koe = input2.getText().toString();
                    if (!koe.isEmpty()) {
                        float initialMinutesFloat = Float.parseFloat(koe);
                        initialMinutes = (int) (initialMinutesFloat * 60);
                    }
                    saveFile();
                    loadMain();
                } catch (Exception e) {
                    Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    buildInitialMinutesDialog();
                }
            }
        });
        alert2.show();
    }

    private void update(){
        int summaMinuutteina = initialMinutes;
        LinearLayout layout = (LinearLayout) findViewById(R.id.content);
        layout.removeAllViews();
        if (initialMinutes != 0) {
            TextView initialHours = new TextView(this);
            initialHours.setText("Initial Hours: " + summaMinuutteina/60 + "h " + summaMinuutteina%60 + "min");
            layout.addView(initialHours);
        }
        for(LogEntry e : list){
            TextView item1 = new TextView(this);
            item1.setText(getDate(e.getTime(),"dd/MM/yyyy")  + " " + e.getLocation() + " " + e.getRidingTime() + "min");
            layout.addView(item1);
            summaMinuutteina += e.getRidingTime(); //summaMinuutteina = summaMinuutteina +  e.getRidingTime();
        }
        TextView hours = (TextView) findViewById(R.id.hourcount);
        hours.setText("Ajettu yhteensä: " + summaMinuutteina/60 + "h " + summaMinuutteina%60 + "min");
    }

    private void loadInsertData(){
        setContentView(R.layout.insertdata_layout);
        final TimePicker timePicker = (TimePicker) findViewById(R.id.timepicker2);
        timePicker.setIs24HourView(true);
        timePicker.setCurrentHour(0);
        timePicker.setCurrentMinute(0);


        EditText ajoMin = ((EditText) findViewById(R.id.ajoaikavastausminuutit));
        EditText ajoTun = ((EditText) findViewById(R.id.ajoaikavastaustunnit));

        ajoMin.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                            @Override
                                            public void onFocusChange(View v, boolean hasFocus) {
                                                if (hasFocus == false) {
                                                    EditText ajoMin = ((EditText) findViewById(R.id.ajoaikavastausminuutit));
                                                    try {
                                                        timePicker.setCurrentMinute(Integer.parseInt(ajoMin.getText().toString()));
                                                    } catch (Exception e) {
                                                        Toast.makeText(getBaseContext(), e.getMessage(),
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }
                                        });
        //ajoTun.addTextChangedListener(new TextWatcher);
        ajoTun.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    EditText ajoTun = ((EditText) findViewById(R.id.ajoaikavastaustunnit));
                    try {
                        timePicker.setCurrentHour(Integer.parseInt(ajoTun.getText().toString()));
                    } catch (Exception e) {
                        Toast.makeText(getBaseContext(), e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        timePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
            public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                EditText ajoMin = ((EditText) findViewById(R.id.ajoaikavastausminuutit));
                EditText ajoTun = ((EditText) findViewById(R.id.ajoaikavastaustunnit));
                StringBuilder minuutit = new StringBuilder();
                minuutit.append(minute);
                StringBuilder tunnit = new StringBuilder();
                tunnit.append(hourOfDay);
                if (!ajoTun.hasFocus()) {
                    ajoTun.setText(tunnit);
                }
                if (!ajoMin.hasFocus()){
                    ajoMin.setText(minuutit);
                }
        }
        });

        Button save = (Button) findViewById(R.id.save);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TimePicker tp = (TimePicker) findViewById(R.id.timepicker2);
                final Calendar c = Calendar.getInstance();
                hour = c.get(Calendar.HOUR_OF_DAY);
                minute = c.get(Calendar.MINUTE);
                EditText ajoMin = ((EditText) findViewById(R.id.ajoaikavastausminuutit));
                EditText ajoTun = ((EditText) findViewById(R.id.ajoaikavastaustunnit));
                int ajoaika = 0;
                int ajoaika1 = tp.getCurrentHour()*60 + tp.getCurrentMinute();
                String ajoaikaMinuutit = ((EditText) findViewById(R.id.ajoaikavastausminuutit)).getText().toString();
                String ajoaikaTunnit = ((EditText) findViewById(R.id.ajoaikavastaustunnit)).getText().toString();
                if (ajoaikaMinuutit.matches(".*\\d.*") && ajoaikaTunnit.matches(".*\\d.*")) {ajoaika = Integer.parseInt(ajoaikaMinuutit) + Integer.parseInt(ajoaikaTunnit)*60;}
                int oikeaAika = ajoaikaValinta(ajoaika1, ajoaika);
                if (oikeaAika == -1){
                    Toast.makeText(getBaseContext(), getString(R.string.insert_ridingtime),
                    Toast.LENGTH_SHORT).show();
                    //loadInsertData();
                }
                else {
                    try {
                        list.add(new LogEntry(System.currentTimeMillis()
                                , ((EditText) findViewById(R.id.ajopaikkavastaus)).getText().toString()
                                , oikeaAika));
                        StringBuilder newLogEntryMessage = new StringBuilder();
                        newLogEntryMessage.append("Added LogEntry: ");
                        newLogEntryMessage.append(getDate(list.getLast().getTime(),"dd/MM/yyyy") + " " + list.getLast().getLocation() + " " + list.getLast().getRidingTime() + "min");

                        Toast.makeText(getBaseContext(),
                                newLogEntryMessage,
                                Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                Toast.makeText(getBaseContext(), e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                    }
                    InputMethodManager imm = (InputMethodManager)getSystemService(
                            Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(ajoMin.getWindowToken(), 0);
                    imm.hideSoftInputFromWindow(ajoTun.getWindowToken(), 0);
                    saveFile();
                    loadMain();
                }
            }
        });

        Button cancel = (Button) findViewById(R.id.cancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadMain();
            }
        });

//        TimePicker timePicker = (TimePicker)findViewById(R.id.timepicker2);
//        timePicker.setOnTimeChangedListener(){};

        }

    public int ajoaikaValinta(int a, int b){
        if (a>b) return a;
        if (a<=b) return b;
        return -1;

    }

    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @param dateFormat Date format
     * @return String representing date in specified format
     */
    public String getDate(long milliSeconds, String dateFormat) {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    private void deleteFile(){
        if(!isExternalStorageWritable()){
            Toast.makeText(getBaseContext(),
                    getString(R.string.unwriteable_storage),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if(!isExternalStorageReadable()){
            Toast.makeText(getBaseContext(),
                    getString(R.string.unreadable_storage),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        File myFile = new File("/sdcard/ajopaivakirja.txt");
        if(myFile.exists()) {
            myFile.delete();
            Toast.makeText(getBaseContext(),
                    "Deleted 'ajopaivakirja.txt'",
                    Toast.LENGTH_SHORT).show();
        }
        else {Toast.makeText(getBaseContext(),
                "Nothing to delete.",
                Toast.LENGTH_SHORT).show();}


    }

    private void saveFile(){
        if(!isExternalStorageWritable()){
            Toast.makeText(getBaseContext(),
                    getString(R.string.unwriteable_storage),
                    Toast.LENGTH_SHORT).show();
            return;

        }

        StringBuilder builder = new StringBuilder();

        builder.append(titteli).append('\n');
        builder.append(initialMinutes).append('\n');

        for(LogEntry e : list){
            builder.append(e.getData());
            builder.append('\n');
        }

        try {
            File myFile = new File("/sdcard/ajopaivakirja.txt");
            if(myFile.exists()) myFile.delete();
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter =
                    new OutputStreamWriter(fOut);
            myOutWriter.append(builder.toString());
            myOutWriter.close();
            fOut.close();
            Toast.makeText(getBaseContext(),
                    "Done writing SD 'ajopaivakirja.txt'",
                    Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(getBaseContext(), e.getMessage(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void readFile(){
        if(!isExternalStorageReadable()){
            Toast.makeText(getBaseContext(),
                    getString(R.string.unreadable_storage),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int lineNum = 0;
            File myFile = new File("/sdcard/ajopaivakirja.txt");
            if(!myFile.exists()) return;
            BufferedReader br = new BufferedReader(new FileReader(myFile));
            String line;

            while ((line = br.readLine()) != null) {

                if(!line.equals("")) {
                    if (lineNum == 0) this.titteli = line;
                    if (lineNum == 1) this.initialMinutes = Integer.parseInt(line);
                    if (lineNum > 1) list.add(new LogEntry(line));
                    lineNum += 1;
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




//Timepickertestitin under here http://www.mkyong.com/android/android-time-picker-example/
}
