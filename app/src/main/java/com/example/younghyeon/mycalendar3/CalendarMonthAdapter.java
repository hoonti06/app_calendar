package com.example.younghyeon.mycalendar3;

import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseAdapter;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CalendarMonthAdapter extends BaseAdapter {

	public static final String TAG = "CalendarMonthAdapter";

	Context mContext;

	public static int oddColor = Color.rgb(225, 225, 225);
	public static int headColor = Color.rgb(12, 32, 158);

	private int selectedPosition = -1;

	private MonthItem[] items;

	private int countColumn = 7;

	int mStartDay;
	int startDay;
	int curYear;
	int curMonth;
	int tmpScheduleSize;
	int firstDay;
	int lastDay;

	Calendar mCalendar;
	boolean recreateItems = false;
	ArrayList scheduleMsg;

	// schedule Hash
	static HashMap<String,ArrayList<ScheduleListItem>> scheduleHash;

	// weather Hash
	HashMap<String,WeatherCurrentCondition> weatherHash;

	// today
	public int todayYear;
	public int todayMonth;
	public int todayDay;

	//Json 파싱
	String myJSON;

	private static final String TAG_RESULTS="result";
	private static final String TAG_ID = "id";
	private static final String TAG_DATE = "date";
	private static final String TAG_MEMO = "memo";

	JSONArray peoples = null;

	ArrayList<HashMap<String, String>> personList;
	ArrayList<String> memoList;
	ArrayList<String> dateList;
	int checkGetData = 0;
	int checkForMonth = 0;

	ArrayList outScheduleList;

	public CalendarMonthAdapter(Context context) {
		super();

		mContext = context;

		init();
	}

	public CalendarMonthAdapter(Context context, AttributeSet attrs) {
		super();

		mContext = context;

		init();
	}

	private void init() {
		items = new MonthItem[7 * 6];

		mCalendar = Calendar.getInstance();
		recalculate();
		resetDayNumbers();

		scheduleHash = new HashMap<String,ArrayList<ScheduleListItem>>();
		weatherHash = new HashMap<String,WeatherCurrentCondition>();

		// calculate today
		calculateToday();

		if (checkGetData == 0){
			personList = new ArrayList<HashMap<String,String>>();
			memoList = new ArrayList<String>();
			dateList = new ArrayList<String>();

			getDataFromPHP("http://52.78.88.182/getdata.php");
		}
	}

	private void calculateToday() {
		Date curDate = new Date();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(curDate);

		todayYear = calendar.get(Calendar.YEAR);
		todayMonth = calendar.get(Calendar.MONTH);
		todayDay = calendar.get(Calendar.DAY_OF_MONTH);
	}

	public int calculatePosition(int year, int month, int day) {
		int position = 0;
		int tempFirstDay = 0;

		Calendar tempCalendar = Calendar.getInstance();
		tempCalendar.set(year, month, 1);

		int dayOfWeek = tempCalendar.get(Calendar.DAY_OF_WEEK);
		tempFirstDay = getFirstDay(dayOfWeek);

		position = day + tempFirstDay - 1;

		return position;
	}

	public void recalculate() {

		// set to the first day of the month
		mCalendar.set(Calendar.DAY_OF_MONTH, 1);

		// get week day
		int dayOfWeek = mCalendar.get(Calendar.DAY_OF_WEEK);
		firstDay = getFirstDay(dayOfWeek);
		Log.d(TAG, "firstDay : " + firstDay);

		mStartDay = mCalendar.getFirstDayOfWeek();
		curYear = mCalendar.get(Calendar.YEAR);
		curMonth = mCalendar.get(Calendar.MONTH);
		lastDay = getMonthLastDay(curYear, curMonth);

		Log.d(TAG, "curYear : " + curYear + ", curMonth : " + curMonth + ", lastDay : " + lastDay);

		int diff = mStartDay - Calendar.SUNDAY - 1;
        startDay = getFirstDayOfWeek();
		Log.d(TAG, "mStartDay : " + mStartDay + ", startDay : " + startDay);

	}

	public void setPreviousMonth() {
		mCalendar.add(Calendar.MONTH, -1);
        recalculate();

		checkForMonth = -1;
        resetDayNumbers();
        selectedPosition = -1;


	}

	public void setNextMonth() {
		mCalendar.add(Calendar.MONTH, 1);
        recalculate();

		checkForMonth = 1;
        resetDayNumbers();
        selectedPosition = -1;
	}

	public void setInputMonth(int year, int month) {
		int temp1 = 0;
		int temp2 = 0;
		temp1 = year - curYear;
		temp2 = month - curMonth - 1;
		int index = temp1 * 12 + temp2;
		Log.e("hi", "wow1 " + temp1 + " "  +year + " "+ curYear);
		Log.e("hi", "wow2 " + temp2 + " "  +month + " "+ curMonth);
		mCalendar.add(Calendar.MONTH, index);
		recalculate();

		checkForMonth = 1;
		resetDayNumbers();
		selectedPosition = -1;
		notifyDataSetChanged();

		Log.e("hi", "wow3 " + temp1 + " "  +temp2 + " "+ index);
	}

	public void resetDayNumbers() {
		for (int i = 0; i < 42; i++) {
			// calculate day number
			int dayNumber = (i+1) - firstDay;
			if (dayNumber < 1 || dayNumber > lastDay) {
				dayNumber = 0;
			}

	        // save as a data item
	        items[i] = new MonthItem(dayNumber);
		}
	}

	/**
	 * get the position of today
	 *
	 * @return
	 */
	public int getTodayPosition() {
		Date curDate = new Date();
		Calendar curCalendar = Calendar.getInstance();
		curCalendar.setTime(curDate);

		int curDay = curCalendar.get(Calendar.DAY_OF_MONTH);

		// get week day
		curCalendar.set(Calendar.DAY_OF_MONTH, 1);
		int dayOfWeek = curCalendar.get(Calendar.DAY_OF_WEEK);
		int curFirstDay = getFirstDay(dayOfWeek);
		Log.d(TAG, "current day of week : " + curFirstDay);

		return curDay + curFirstDay - 1;
	}

	private int getFirstDay(int dayOfWeek) {
		int result = 0;
		if (dayOfWeek == Calendar.SUNDAY) {
			result = 0;
		} else if (dayOfWeek == Calendar.MONDAY) {
			result = 1;
		} else if (dayOfWeek == Calendar.TUESDAY) {
			result = 2;
		} else if (dayOfWeek == Calendar.WEDNESDAY) {
			result = 3;
		} else if (dayOfWeek == Calendar.THURSDAY) {
			result = 4;
		} else if (dayOfWeek == Calendar.FRIDAY) {
			result = 5;
		} else if (dayOfWeek == Calendar.SATURDAY) {
			result = 6;
		}

		return result;
	}


	public int getCurYear() {
		return curYear;
	}

	public int getCurMonth() {
		return curMonth;
	}

	public int getNumColumns() {
		return 7;
	}

	public int getCount() {
		return 7 * 6;
	}

	public Object getItem(int position) {
		return items[position];
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		Log.d(TAG, "getView(" + position + ") called.");

		MonthItemView itemView;
		if (convertView == null) {
			itemView = new MonthItemView(mContext);
		} else {
			itemView = (MonthItemView) convertView;
		}

		// create a params
		GridView.LayoutParams params = new GridView.LayoutParams(
				GridView.LayoutParams.FILL_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT);
		// 120이 반차는거
		// 250이 다차는거

		// calculate row and column
		int rowIndex = position / countColumn;
		int columnIndex = position % countColumn;

		Log.d(TAG, "Index : " + rowIndex + ", " + columnIndex);

		// set item data and properties
		itemView.setItem(items[position]);
		itemView.setLayoutParams(params);

		// set properties
		itemView.setGravity(Gravity.LEFT);

		if (columnIndex == 0) {
			itemView.setTextColor(Color.RED);
		} else {
			itemView.setTextColor(Color.BLACK);
		}

		// set background color

		ArrayList outList = getSchedule(position);
		// 누른 포지션의 size를 가져오는데
		int outListSize = 0;
		boolean scheduleExist = false;
		if (outList != null) {
			outListSize = outList.size();
			Log.e("hi", "outListSize " + outListSize);
		}
		if (outList != null && outList.size() > 0) {
			scheduleExist = true;
			// 일정 추가 했으면 트루
		}

		if (position == getSelectedPosition()) {
			// 누른 곳은 노랗게 칠하고
        	itemView.setBackgroundColor(Color.YELLOW);
        } else {
        	if (scheduleExist) {
        		itemView.setBackgroundColor(Color.WHITE);
				// 일정추가한거 스케줄 존재하면 밑에다가 일정 써주기
        	} else {
        		itemView.setBackgroundColor(Color.WHITE);
        	}

        	// display today color
        	int curDay = items[position].getDay();
        	if (curYear == todayYear && curMonth == todayMonth && curDay == todayDay) {
        		itemView.setBackgroundColor(Color.CYAN);
        	}
        }

		// 메인달력에 일정 보여주는 코드
		if (scheduleExist == false){
			itemView.setMsg(outList);
		}
		else {
			itemView.setMsg(items[position], outList, outListSize);
			// 일정이 있으면 사이즈 넘겨서
		}

		if(checkForMonth == 1) {
			Animation animation = AnimationUtils.loadAnimation(mContext.getApplicationContext(), R.anim.translate_right);
			itemView.startAnimation(animation);
		}
		else if(checkForMonth == -1) {
			Animation animation = AnimationUtils.loadAnimation(mContext.getApplicationContext(), R.anim.translate_left);
			itemView.startAnimation(animation);
		}
		if (position == 41) // 마지막 포지션의 아이템이 왔을때 checkForMonth 0으로 하면 애니메이션 없지롱
			checkForMonth = 0;

		// set weather
		/*
		WeatherCurrentCondition outWeather = getWeather(position);
		if (outWeather != null) {
			String weatherIconUrl = outWeather.getIconURL();
			File iconFile = new File(weatherIconUrl);
			String iconFileName = iconFile.getName();
			Log.d(TAG, "weather icon file name : " + iconFileName);

			if (iconFileName != null) {
				if (iconFileName.equals("sunny.gif")) {
					itemView.setWeatherImage(R.drawable.sunny);
				} else if (iconFileName.equals("cloudy.gif")) {
					itemView.setWeatherImage(R.drawable.cloudy);
				} else if (iconFileName.equals("rain.gif")) {
					itemView.setWeatherImage(R.drawable.rain);
				} else if (iconFileName.equals("snow.gif")) {
					itemView.setWeatherImage(R.drawable.snow);
				} else {
					Log.d(TAG, "weather icon image is not found in the resources.");
					itemView.setWeatherImage(0);
				}
			} else {
				itemView.setWeatherImage(0);
			}
		} else {
			itemView.setWeatherImage(0);
		}

*/
		return itemView;
	}




    /**
     * Get first day of week as android.text.format.Time constant.
     * @return the first day of week in android.text.format.Time
     */
    public static int getFirstDayOfWeek() {
        int startDay = Calendar.getInstance().getFirstDayOfWeek();
        if (startDay == Calendar.SATURDAY) {
            return Time.SATURDAY;
        } else if (startDay == Calendar.MONDAY) {
            return Time.MONDAY;
        } else {
            return Time.SUNDAY;
        }
    }


    /**
     * get day count for each month
     *
     * @param year
     * @param month
     * @return
     */
    private int getMonthLastDay(int year, int month){
    	switch (month) {
 	   		case 0:
      		case 2:
      		case 4:
      		case 6:
      		case 7:
      		case 9:
      		case 11:
      			return (31);

      		case 3:
      		case 5:
      		case 8:
      		case 10:
      			return (30);

      		default:
      			if(((year%4==0)&&(year%100!=0)) || (year%400==0) ) {
      				return (29);   // 2�� ������
      			} else {
      				return (28);
      			}
 	   	}
 	}

	/**
	 * set selected row
	 *

	 */
	public void setSelectedPosition(int selectedPosition) {
		this.selectedPosition = selectedPosition;
	}

	/**
	 * get selected row
	 *
	 * @return
	 */
	public int getSelectedPosition() {
		return selectedPosition;
	}

	public static void removeAllSchedule(int year, int month, int position) {
		String keyStr = year + "-" + month + "-" + position;
		ArrayList<ScheduleListItem> outList = scheduleHash.get(keyStr);
		scheduleHash.remove(keyStr);
	}

	public static void removeSchedule(int year, int month, int position, int index) {
		String keyStr = year + "-" + month + "-" + position;
		ArrayList<ScheduleListItem> outList = scheduleHash.get(keyStr);
		String str = outList.get(index).getMessage();
		outList.remove(index);

		Log.e("CalendarMonthAdapter", "oustList.size " + outList.size());
		Log.e("CalendarMonthAdapter", "index " + index);
		Log.e("CalendarMonthAdapter", "str " + str);
		// 이거 다 지우고 다시 넣는 방법밖에 없는 걸까요
		scheduleHash.remove(keyStr);
		scheduleHash.put(keyStr, outList);
	}

	public static void updateSchedule(int year, int month, int position, int index, ArrayList<ScheduleListItem> aList) {
		String keyStr = year + "-" + month + "-" + position;
		ArrayList<ScheduleListItem> outList = scheduleHash.get(keyStr);

		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("HH시mm분");
		outList.remove(index);
		ScheduleListItem tempItem = new ScheduleListItem(aList.get(0).getTime(), aList.get(0).getMessage());
		String curItemTime = aList.get(0).getTime();
		int intCurHour = 0;
		int intCurMin = 0;
		int intCompareHour = 0;
		int intCompareMin = 0;
		try {
			c.setTime(sdf.parse(curItemTime));
			intCurHour = c.get(Calendar.HOUR_OF_DAY);
			intCurMin = c.get(Calendar.MINUTE);
			Log.e("CalendarMonthAdapter", "intCurHour " + intCurHour);
			Log.e("CalendarMonthAdapter", "intCurMin " + intCurMin);
		} catch (java.text.ParseException ex) {
			ex.printStackTrace();
		}
		int here = 0;
		for (int i = 0; i < outList.size(); i++){
			String time = outList.get(i).getTime();
			/*
			Log.e("CalendarMonthAdapter", "curItemTime " + curItemTime);
			Log.e("CalendarMonthAdapter", "time " + time);
			Log.e("CalendarMonthAdapter", "compareTo " + time.compareTo(curItemTime));*/
			try {
				c.setTime(sdf.parse(time));
				intCompareHour = c.get(Calendar.HOUR_OF_DAY);
				intCompareMin = c.get(Calendar.MINUTE);
				Log.e("CalendarMonthAdapter", "intCompareHour " + intCompareHour);
				Log.e("CalendarMonthAdapter", "intCompareMin " + intCompareMin);

			} catch (java.text.ParseException ex) {
				ex.printStackTrace();
			}

			if (intCurHour > intCompareHour){
				here = i + 1;
			}
			else if (intCurHour == intCompareHour) {
				if (intCurMin > intCompareMin)
					here = i + 1;
			}
			else break;
		}
		Log.e("CalendarMonthAdapter", "here " + here);

		outList.add(here, tempItem);

		scheduleHash.remove(keyStr);
		scheduleHash.put(keyStr, outList);
	}

	/**
	 * get schedule
	 *
	 * @param year
	 * @param month
	 * @param position
	 * @return
	 */
	public static ArrayList<ScheduleListItem> getSchedule(int year, int month, int position) {
		String keyStr = year + "-" + month + "-" + position;
		ArrayList<ScheduleListItem> outList = scheduleHash.get(keyStr);

		return outList;
	}

	public ArrayList<ScheduleListItem> getSchedule(int position) {
		String keyStr = curYear + "-" + curMonth + "-" + position;
		ArrayList<ScheduleListItem> outList = scheduleHash.get(keyStr);

		return outList;
	}

	public static void putSchedule(int year, int month, int position, ArrayList<ScheduleListItem> aList) {
		String keyStr = year + "-" + month + "-" + position;
		scheduleHash.put(keyStr, aList);
	}

	public void putSchedule(int position, ArrayList<ScheduleListItem> aList) {
		String keyStr = curYear + "-" + curMonth + "-" + position;
		scheduleHash.put(keyStr, aList);
	}

	public void putScheduleFromParameter (int year, int month, int position, ArrayList<ScheduleListItem> aList) {
		String keyStr = year + "-" + month + "-" + position;
		scheduleHash.put(keyStr, aList);
	}

	public WeatherCurrentCondition getWeather(int position) {
		String keyStr = curYear + "-" + curMonth + "-" + position;
		WeatherCurrentCondition outWeather = weatherHash.get(keyStr);

		return outWeather;
	}

	public WeatherCurrentCondition getWeather(int year, int month, int position) {
		String keyStr = year + "-" + month + "-" + position;
		WeatherCurrentCondition outWeather = weatherHash.get(keyStr);

		return outWeather;
	}

	public void putWeather(int year, int month, int position, WeatherCurrentCondition aWeather) {
		String keyStr = year + "-" + month + "-" + position;
		weatherHash.put(keyStr, aWeather);
	}

	public void putWeather(int position, WeatherCurrentCondition aWeather) {
		String keyStr = curYear + "-" + curMonth + "-" + position;
		weatherHash.put(keyStr, aWeather);
	}


	protected void showDataFromPHP() {
		try {
			JSONObject jsonObj = new JSONObject(myJSON);
			peoples = jsonObj.getJSONArray(TAG_RESULTS);
			for(int i=0;i<peoples.length();i++){
				JSONObject c = peoples.getJSONObject(i);
				String id = c.getString(TAG_ID);
				String date = c.getString(TAG_DATE);
				String memo = c.getString(TAG_MEMO);

				HashMap<String,String> persons = new HashMap<String,String>();

				persons.put(TAG_ID,id);
				persons.put(TAG_DATE,date);
				persons.put(TAG_MEMO, memo);

				dateList.add(date);
				memoList.add(memo);
			//	personList.add(persons);
			}
			// 데이터 읽어와서 초기화 작업을 진행하는 곳

		//	java.util.Date date = new java.util.Date();
			Calendar c = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyy-MM-dd HH:mm:ss");
			if (outScheduleList == null) {
				outScheduleList = new ArrayList();
			}
			String tempDateString = "";
			String tempMemoString = "";
			int tempYear = 0;
			int tempMonth = 0;
			int tempDay = 0;
			int prevDay = 0;
			Integer tempHour = 0;
			Integer tempMin = 0;
			for (int i = 0; i < memoList.size(); i++){
				tempDateString = dateList.get(i);
				tempMemoString = memoList.get(i);
				Log.e("CalendarMonthAdapter", "tempDateString : "+tempDateString);
				Log.e("CalendarMonthAdapter", "tempMemoString : "+tempMemoString);
				try {
					c.setTime(sdf.parse(tempDateString));
					tempYear = c.get(Calendar.YEAR);
					tempMonth = c.get(Calendar.MONTH);
					tempDay = c.get(Calendar.DAY_OF_MONTH);
					Log.e("CalendarMonthAdapter", "tempYear : "+tempYear);
					Log.e("CalendarMonthAdapter", "tempMonth : "+tempMonth);
					Log.e("CalendarMonthAdapter", "tempDay : "+tempDay);
					tempHour = c.get(Calendar.HOUR_OF_DAY);
					tempMin = c.get(Calendar.MINUTE);
					tempDateString = tempHour.toString() + "시" + tempMin.toString() + "분";
				} catch (java.text.ParseException ex) {
					ex.printStackTrace();
				}
				if (prevDay != tempDay){
					outScheduleList = new ArrayList();
				}

				// 이거 시간이 고정인데 이것도 바꿔줘야 되는구만

				ScheduleListItem aItem = new ScheduleListItem(tempDateString, tempMemoString);
				//mCalendar.

				int position = calculatePosition(tempYear, tempMonth, tempDay);
				// 가장 처음에 데이터 아무것도 없을때 데이터 파싱해서 읽어와서 자리에 넣기 위해서는
				// pos를 모르기 때문에 구해야 함 day를 이용해서


				outScheduleList.add(aItem);
				putScheduleFromParameter(tempYear, tempMonth, position, outScheduleList);
				// 이렇게 하면 들어가 신기함
				notifyDataSetChanged();

				prevDay = tempDay;
			}
			checkGetData = 1;

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	protected void getDataFromPHP (String url) {

		class GetDataJSON extends AsyncTask<String, Void, String> {
			@Override
			protected String doInBackground(String... params) {

				String uri = params[0];
				BufferedReader bufferedReader = null;
				try {
					URL url = new URL(uri);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					StringBuilder sb = new StringBuilder();

					bufferedReader = new BufferedReader(new InputStreamReader(con.getInputStream()));

					String json;
					while ((json = bufferedReader.readLine()) != null) {
						sb.append(json + "\n");
					}

					return sb.toString().trim();

				} catch (Exception e) {
					return null;
				}
			}

			@Override
			protected void onPostExecute(String result) {
				myJSON = result;
				showDataFromPHP();

				MainActivity.progressDialog.dismiss();
				// 여기 위치가 맞는 듯
			}
		}

		GetDataJSON g = new GetDataJSON();
		g.execute(url);

		checkGetData = 1;

	}
}

