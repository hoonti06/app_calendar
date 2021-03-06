package com.example.younghyeon.mycalendar3;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends Activity {
    public static final String TAG = "CalendarMonthViewActivity";

    GridView monthView;
    CalendarMonthAdapter monthViewAdapter;

    TextView monthText;


    int curYear;
    int curMonth;
    int selectedDay;

    int curPosition;
    EditText scheduleInput;
    Button saveButton;
    ImageButton plusbutton;
    Button monthButton;

    ListView scheduleList;
    ScheduleListAdapter scheduleAdapter;
    ArrayList outScheduleList;

    public static final int REQUEST_CODE_SCHEDULE_INPUT = 1001;
    public static final int WEATHER_PROGRESS_DIALOG = 1002;
    public static final int WEATHER_SAVED_DIALOG = 1003;
    public static final int REQUEST_CODE_SCHEDULE_UPDATE = 1004;


    private static final String BASE_URL = "http://www.google.com";
    private static String WEATHER_URL = "http://www.google.com/ig/api?weather=";

    private static boolean weatherCanceled;

    WeatherCurrentCondition weather = null;

    Handler handler = new Handler();
    static ProgressDialog progressDialog;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        progressDialog = ProgressDialog.show(MainActivity.this, "", "데이터를 가져오는 중입니다", true, true);
        monthView = (GridView) findViewById(R.id.monthView);
        monthViewAdapter = new CalendarMonthAdapter(this);
        monthView.setAdapter(monthViewAdapter);

        // set listener
        monthView.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MonthItem curItem = (MonthItem) monthViewAdapter.getItem(position);
                int day = curItem.getDay();
                selectedDay = curItem.getDay();

                //Toast.makeText(getApplicationContext(), day + "���� ���õǾ����ϴ�.", 1000).show();
                monthViewAdapter.setSelectedPosition(position);
                monthViewAdapter.notifyDataSetChanged();

                outScheduleList = monthViewAdapter.getSchedule(position);
                if (outScheduleList == null) {
                    outScheduleList = new ArrayList<ScheduleListItem>();
                }

                scheduleAdapter.scheduleList = outScheduleList;

                scheduleAdapter.notifyDataSetChanged();

                // show ScheduleInputActivity if the position is already selected
                if (position == curPosition && selectedDay != 0) {
                    if (outScheduleList.size() == 0) {
                        showScheduleInput();
                        // 일정이 없을시에는 일정 추가
                    } else {
                        // showScheduleInput();

                        Intent intent = new Intent(getApplicationContext(), ScheduleShowActivity.class);
//                        intent.putExtra("_outScheduleList", monthViewAdapter.getSchedule(position));
                        intent.putExtra("year", curYear);
                        intent.putExtra("month", curMonth);
                        intent.putExtra("position", position);
                        intent.putExtra("day", day);
                        startActivityForResult(intent, REQUEST_CODE_SCHEDULE_UPDATE);
                        // 일정이 있을시에는 일정 보여주는 리스트 액티비티 띄워야 될 듯
                    }
                }
                // set schedule to the TextView
                curPosition = position;
            }
        });

        plusbutton = (ImageButton) findViewById(R.id.plusButton);
        plusbutton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (selectedDay == 0) {
                    Toast.makeText(MainActivity.this, "날짜를 선택해주세요.", Toast.LENGTH_SHORT).show();
                    /*
                    int position = monthViewAdapter.getTodayPosition();
                    MonthItem curItem = (MonthItem) monthViewAdapter.getItem(position);
                    int day = curItem.getDay();
                    selectedDay = curItem.getDay();*/
                } else showScheduleInput();
            }
        });

        monthView.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
//                Toast.makeText(MainActivity.this, "top", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeRight() {
                selectedDay = 0;
                curPosition = 0;
                monthViewAdapter.setPreviousMonth();
                monthViewAdapter.notifyDataSetChanged();
                setMonthText();

                //   Toast.makeText(MainActivity.this, "right", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeLeft() {
                selectedDay = 0;
                curPosition = 0;
                monthViewAdapter.setNextMonth();
                monthViewAdapter.notifyDataSetChanged();
                setMonthText();
                //   Toast.makeText(MainActivity.this, "left", Toast.LENGTH_SHORT).show();
            }

            public void onSwipeBottom() {
//                Toast.makeText(MainActivity.this, "bottom", Toast.LENGTH_SHORT).show();
            }
        });


        monthButton = (Button) findViewById(R.id.monthText);
        monthButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Dialog dia = null;
                final Calendar c = Calendar.getInstance();
                MonthYearPickerDialog pd = new MonthYearPickerDialog();
                pd.setListener(mDateSetListener);
                pd.show(getFragmentManager(), "MonthYearPickerDialog");
//                createDialogWithoutDateField().show();
            }
        });

        monthText = (TextView) findViewById(R.id.monthText);
        setMonthText();
        // 2016년 8월 이거 설정

        /*
        Button monthPrevious = (Button) findViewById(R.id.monthPrevious);
        monthPrevious.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                monthViewAdapter.setPreviousMonth();
                monthViewAdapter.notifyDataSetChanged();

                setMonthText();
            }
        });

        Button monthNext = (Button) findViewById(R.id.monthNext);
        monthNext.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                monthViewAdapter.setNextMonth();
                monthViewAdapter.notifyDataSetChanged();

                setMonthText();
            }
        });
*/

        curPosition = -1;

        scheduleList = (ListView) findViewById(R.id.scheduleList);
        scheduleAdapter = new ScheduleListAdapter(this);
        scheduleList.setAdapter(scheduleAdapter);
    }

    private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            monthViewAdapter.setInputMonth(year, monthOfYear);
            setMonthText();
        }
    };

    private void setMonthText() {
        curYear = monthViewAdapter.getCurYear();
        curMonth = monthViewAdapter.getCurMonth();

        monthText.setText(curYear + "." + (curMonth+1) + "\n" + "    ﹀    ");
        // curMonth에 +1은 왜함?
        //monthText.setText(curYear + "년  " + (curMonth+1) + "월");
    }


    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        addOptionMenuItems(menu);

        return true;
    }

    private void addOptionMenuItems(Menu menu) {
        int id = Menu.FIRST;
        menu.clear();

        menu.add(id, id, Menu.NONE, "일정 추가");

        id = Menu.FIRST+1;
        menu.add(id, id, Menu.NONE, "현재 날씨 가져오기");
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case Menu.FIRST:
                showScheduleInput();

                return true;
            case Menu.FIRST+1:
          //      getCurrentWeather();

                return true;
            default:
                break;
        }

        return false;
    }

    /**
     * get current weather
     */

    /*
    private void getCurrentWeather() {
        weatherCanceled = false;

        showDialog(WEATHER_PROGRESS_DIALOG);

        CurrentWeatherSaveThread thread = new CurrentWeatherSaveThread();
        thread.start();

    }
*/
/*
    class CurrentWeatherSaveThread extends Thread {
        public CurrentWeatherSaveThread() {

        }

        public void run() {
            try {
                SAXParserFactory parserFactory = SAXParserFactory.newInstance();
                SAXParser parser = parserFactory.newSAXParser();
                XMLReader reader = parser.getXMLReader();

                WeatherHandler whandler = new WeatherHandler();
                reader.setContentHandler(whandler);

                String queryStr = WEATHER_URL + "Seoul,Korea";
                URL urlForHttp = new URL(queryStr);

                InputStream instream = getInputStreamUsingHTTP(urlForHttp);

                if (instream != null) {
                    reader.parse(new InputSource(instream));

                    weather = whandler.getWeather();

                    handler.post(completedRunnable);
                } else {
                    removeDialog(WEATHER_PROGRESS_DIALOG);
                }

            } catch(Exception ex) {
                ex.printStackTrace();
            }

        }
    }*/
/*
    private InputStream getInputStreamUsingHTTP(URL url) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setUseCaches(false);
        conn.setAllowUserInteraction(false);

        int resCode = conn.getResponseCode();
        //Log.d(TAG, "Response Code : " + resCode);

        if (weatherCanceled) {
            return null;
        }

        InputStream instream = conn.getInputStream();
        return instream;
    }

    Runnable completedRunnable = new Runnable() {
        public void run() {
            removeDialog(WEATHER_PROGRESS_DIALOG);

            if (weatherCanceled) {
                weather = null;
            } else {
                Toast.makeText(getApplicationContext(), "Current Weather : " + weather.getCondition() + ", " + weather.getIconURL(), Toast.LENGTH_LONG).show();

                // set today weather
                int todayPosition = monthViewAdapter.getTodayPosition();
               // Log.d(TAG, "today position : " + todayPosition);

                monthViewAdapter.putWeather(monthViewAdapter.todayYear, monthViewAdapter.todayMonth, todayPosition, weather);
                monthViewAdapter.notifyDataSetChanged();

                showDialog(WEATHER_SAVED_DIALOG);
            }
        }
    };

*/
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case WEATHER_PROGRESS_DIALOG:
                ProgressDialog progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("�������� �������� ��...");
                progressDialog.setCancelable(true);
                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        weatherCanceled = true;
                    }
                });

                return progressDialog;
            case WEATHER_SAVED_DIALOG:
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setMessage("���������� �����Ͽ����ϴ�.");
                alertBuilder.setPositiveButton("Ȯ��", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                AlertDialog alertDialog = alertBuilder.create();
                return alertDialog;
        }
        return null;
    }


    private void showScheduleInput() {
        Intent intent = new Intent(this, ScheduleInputActivity.class);
        intent.putExtra("year", curYear);
        intent.putExtra("month", curMonth);
        intent.putExtra("day", selectedDay);
        Log.e("jsonerr", ""+selectedDay);

        int todayPosition = monthViewAdapter.getTodayPosition();


        WeatherCurrentCondition weather = monthViewAdapter.getWeather(monthViewAdapter.todayYear, monthViewAdapter.todayMonth, todayPosition);
        if (weather != null) {
            String weatherIconUrl = weather.getIconURL();
            intent.putExtra("weatherIconUrl", weatherIconUrl);
        }

        startActivityForResult(intent, REQUEST_CODE_SCHEDULE_INPUT);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (requestCode == REQUEST_CODE_SCHEDULE_INPUT) {
            if (intent == null) {
                return;
            }

            String time = intent.getStringExtra("time");
            String message = intent.getStringExtra("message");
            int selectedWeather = intent.getIntExtra("weather", 0);

            if (message != null) {
//                Toast toast = Toast.makeText(getBaseContext(), "time : " + time + ", message : " + message + ", selectedWeather : " + selectedWeather, Toast.LENGTH_LONG);
//                toast.show();
                // 일정 추가 저장시 토스트 메시지 띄우는 거


                ScheduleListItem aItem = new ScheduleListItem(time, message);

                if (outScheduleList == null) {
                    outScheduleList = new ArrayList();
                }
                outScheduleList.add(aItem);

                monthViewAdapter.putSchedule(curPosition, outScheduleList);

                scheduleAdapter.scheduleList = outScheduleList;
                scheduleAdapter.notifyDataSetChanged();
                Toast.makeText(getApplicationContext(), "일정이 추가되었습니다.", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_CODE_SCHEDULE_UPDATE){
            if (intent == null) {
                return;
            }

            // 이걸 하면 Calendar getView가 콜이 되는건가 암튼 화면 업데이트 할 수 있다.
            monthViewAdapter.notifyDataSetChanged();
            scheduleAdapter.notifyDataSetChanged();
        }

    }

}