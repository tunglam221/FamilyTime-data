package com.example.admin.familytime2;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Admin on 12/8/2015.
 */


public class ReportFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "TimelineFragment";

    // Constants
    private static final long DRAWING_GRAPH_INTERVAL = 5*1000;
    private static final long DRAWING_GRAPH_DELAY = 3*1000;
    private static final long STORE_DATA_TO_DB_INTERVAL = 5000;
    private static final long STORE_DATA_TO_DB_DELAY = 3000;


    public static final int REPORT_TYPE_YEAR = 1;
    public static final int REPORT_TYPE_MONTH = 2;
    public static final int REPORT_TYPE_DAY = 3;
    public static final int REPORT_TYPE_HOUR = 4;

    private static int mThisYear = -1;

    private static int mThisMonth = -1;	// (in the range [0,11])
    private static int[] mMonthArray = new int[12];

    private static int mThisDay = -1;		// (in the range [0,30])
    private static int[] mDayArray = new int[31];

    private static int mThisHour = -1;		// (in the range [0,23])
    private static int[] mHourArray = new int[24];

    private static int mThisMinute = -1;	// (in the range [0,59])
    private static int[] mMinuteArray = new int[60];


    // System
    private Context mContext = null;
    private DBHelper mDB;



    // Contents


    // View
    private RenderingStatistics mRenderStatistics;
    private TextView mStatisticsText = null;
    private TextView mCalorieText = null;
    private TextView mWalksText = null;
    //private ListView mTimelineList = null;
    //private TimelineAdapter mTimelineListAdapter = null;
    private Button mButtonTimeInterval = null;

    // Parameters
    private int mStatisticsType = REPORT_TYPE_HOUR;

    // Auto-refresh timer
    private Timer mRefreshTimer = null;
    private Timer mReloadDataTimer = null;


    public ReportFragment() {
        Calendar cal = Calendar.getInstance();
        mThisYear = cal.get(Calendar.YEAR);
        mThisMonth = cal.get(Calendar.MONTH);
        mThisDay = cal.get(Calendar.DAY_OF_MONTH);
        mThisHour = cal.get(Calendar.HOUR_OF_DAY);
    }

/*
    public ReportFragment(Context c, IFragmentListener l, Handler h) {
        mContext = c;
        mFragmentListener = l;
        mHandler = h;
    }
*/


    /*****************************************************
     *	Overrided methods
     ******************************************************/

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "# MessageListFragment - onCreateView()");
        mDB = new DBHelper(container.getContext()).openWritable();
        mContext = container.getContext();

        View rootView = inflater.inflate(R.layout.fragment_report, container, false);

        mRenderStatistics = (RenderingStatistics)rootView.findViewById(R.id.render_statistics);

        mStatisticsText = (TextView) rootView.findViewById(R.id.text_title_statistics);
        mCalorieText = (TextView) rootView.findViewById(R.id.text_content_calorie);
        mCalorieText.setText(String.format("%,.0f", MainActivity.calorieToday));
        mWalksText = (TextView) rootView.findViewById(R.id.text_content_walks);
        mWalksText.setText(MainActivity.stepToday + "");

        mStatisticsType = REPORT_TYPE_HOUR;
        mButtonTimeInterval = (Button) rootView.findViewById(R.id.button_time_interval);
        mButtonTimeInterval.setOnClickListener(this);
        setTimeIntervalString(mStatisticsType);

        getCurrentReportsFromDB();

        // TODO: If you need to show activity data as list, use below code
		/*
		mTimelineList = (ListView) rootView.findViewById(R.id.list_timeline);
		if(mTimelineListAdapter == null)
			mTimelineListAdapter = new TimelineAdapter(mContext, R.layout.list_item_timeline, null);
		mTimelineListAdapter.setAdapterParams(this);
		mTimelineList.setAdapter(mTimelineListAdapter);
		*/

        return rootView;
    }

/*    @Override
    public void OnAdapterCallback(int msgType, int arg0, int arg1, String arg2, String arg3, Object arg4) {
        switch(msgType) {
            case IAdapterListener.CALLBACK_xxx:
                // TODO:
                //if(arg4 != null)
                //	mFragmentListener.OnFragmentCallback(IFragmentListener.CALLBACK_REQUEST_ADD_FILTER, 0, 0, null, null, arg4);
                break;
        }
    }*/

    @Override
    public void onResume() {
        super.onResume();
        mRefreshTimer = new Timer();
        mRefreshTimer.schedule(new RefreshTimerTask(), DRAWING_GRAPH_DELAY, DRAWING_GRAPH_INTERVAL);
    }

    @Override
    public void onStop() {
        super.onStop();
        if(mRefreshTimer != null) {
            mRefreshTimer.cancel();
            mRefreshTimer = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.button_time_interval:
                changeTimeInterval();
                setTimeIntervalString(mStatisticsType);
                drawStatistics();
                break;
            default:
                break;
        }
        //TODO: delete this part after testing
        Log.d("Test database", mDB.getReportCount() + "");
    }


    /*****************************************************
     *	Private methods
     ******************************************************/
    /**
     * Initialize rendering view
     * @return	boolean		is initialized or not
     */
    private boolean checkRenderView() {
        if(mRenderStatistics != null) {
            mRenderStatistics.initializeGraphics(0);
            return true;
        }

        return false;
    }

    /**
     * Draw stacked calorie data
     */
    private void drawStatistics() {
        if(mRenderStatistics == null)
            return;

        // Set data type (month or day or hour)
        setStatisticsString(mStatisticsType);
        // Initialize view
        checkRenderView();

        int[] arrays = getCurrentActivityData(mStatisticsType);
        if(arrays != null) {
            mRenderStatistics.drawGraph(mStatisticsType, arrays);
            mRenderStatistics.invalidate();
        }
    }

    /**
     * Show graph title string
     * @param type	REPORT_TYPE_MONTH or REPORT_TYPE_DAY or REPORT_TYPE_HOUR
     */
    private void setStatisticsString(int type) {
        mStatisticsText.setText(R.string.title_statistics);

        switch(type) {
            case REPORT_TYPE_MONTH:
                mStatisticsText.append(" (");
                mStatisticsText.append(mContext.getString(R.string.title_month));
                mStatisticsText.append(")");
                break;
            case REPORT_TYPE_DAY:
                mStatisticsText.append(" (");
                mStatisticsText.append(mContext.getString(R.string.title_day));
                mStatisticsText.append(")");
                break;
            case REPORT_TYPE_HOUR:
                mStatisticsText.append(" (");
                mStatisticsText.append(mContext.getString(R.string.title_hour));
                mStatisticsText.append(")");
                break;
        }
    }

    /**
     * Set time interval string.
     * @param type		REPORT_TYPE_MONTH or REPORT_TYPE_DAY or REPORT_TYPE_HOUR
     */
    private void setTimeIntervalString(int type) {
        switch(type) {
            case REPORT_TYPE_MONTH:
                mButtonTimeInterval.setText(mContext.getString(R.string.title_month));
                break;
            case REPORT_TYPE_DAY:
                mButtonTimeInterval.setText(mContext.getString(R.string.title_day));
                break;
            case REPORT_TYPE_HOUR:
                mButtonTimeInterval.setText(mContext.getString(R.string.title_hour));
                break;
        }
    }

    /**
     * Change time interval
     * Repeats REPORT_TYPE_MONTH => REPORT_TYPE_DAY => REPORT_TYPE_HOUR
     */
    private void changeTimeInterval() {
        switch(mStatisticsType) {
            case REPORT_TYPE_MONTH:
                mStatisticsType = REPORT_TYPE_DAY;
                break;
            case REPORT_TYPE_DAY:
                mStatisticsType = REPORT_TYPE_HOUR;
                break;
            case REPORT_TYPE_HOUR:
                mStatisticsType = REPORT_TYPE_MONTH;
                break;
        }
    }


    protected void addNewData() {
        boolean isTimeChanged = false;
        double calories = MainActivity.calorieCount;
        Calendar cal = Calendar.getInstance();
        int prevYear = mThisYear;
        int prevMonth = mThisMonth;
        int prevDay = mThisDay;
        int prevHour = mThisHour;

        // Add calorie to buffer
        if(mThisMonth != cal.get(Calendar.MONTH)) {
            // Push monthly report to DB
            Time tempTime = new Time();
            tempTime.set(1, 0, 0, 1, prevMonth, prevYear);	// convert day: in the range [1,31], month: in the range [0,11]
            long millis = tempTime.toMillis(true);
            pushReportToDB(REPORT_TYPE_MONTH, millis, prevYear, prevMonth, 1, 0);

            // Set new date
            mThisYear = cal.get(Calendar.YEAR);
            mThisMonth = cal.get(Calendar.MONTH);
            if(mThisMonth == Calendar.JANUARY)
                Arrays.fill(mMonthArray, 0x00000000);
            isTimeChanged = true;
        }
        mMonthArray[mThisMonth] += calories;

        if(mThisDay != cal.get(Calendar.DAY_OF_MONTH) - 1 || isTimeChanged) {
            // Push daily report to DB
            Time tempTime = new Time();
            tempTime.set(1, 0, 0, prevDay + 1, prevMonth, prevYear);	// convert day: in the range [1,31], month: in the range [0,11]
            long millis = tempTime.toMillis(true);
            pushReportToDB(REPORT_TYPE_DAY, millis, prevYear, prevMonth, prevDay, 0);

            if(isTimeChanged) {
                // Month changed !!
                Arrays.fill(mDayArray, 0x00000000);
            } else {
                // Month is not changed but day changed
            }

            mThisDay = cal.get(Calendar.DAY_OF_MONTH) - 1;
            isTimeChanged = true;
        }
        mDayArray[mThisDay] += calories;

        if(mThisHour != cal.get(Calendar.HOUR_OF_DAY) || isTimeChanged) {
            // Push hourly report to DB
            Time tempTime = new Time();
            tempTime.set(1, 0, prevHour, prevDay + 1, prevMonth, prevYear);	// convert day: in the range [1,31], month: in the range [0,11]
            long millis = tempTime.toMillis(true);
            pushReportToDB(REPORT_TYPE_HOUR, millis, prevYear, prevMonth, prevDay, prevHour);

            if(isTimeChanged) {
                // Day changed !!
                Arrays.fill(mHourArray, 0x00000000);
            } else {
                // Day is not changed but hour changed
            }

            mThisHour = cal.get(Calendar.HOUR_OF_DAY);
            isTimeChanged = true;
        }
        mHourArray[mThisHour] += calories;

        if(isTimeChanged || mThisMinute != cal.get(Calendar.MINUTE)) {
            if(isTimeChanged) {
                // Hour changed !!
                Arrays.fill(mMinuteArray, 0x00000000);
            } else {
                // Hour is not changed but minute changed
            }

            // Add to new minute buffer
            mThisMinute = cal.get(Calendar.MINUTE);
        }
        mMinuteArray[mThisMinute] += calories;

    }

    private void pushReportToDB(int type, long time, int year, int month, int day, int hour) {
        int calorie = 0;

        switch(type) {
            case REPORT_TYPE_YEAR:
                // Not available
                return;

            case REPORT_TYPE_MONTH:
                if(month > -1 && month < mMonthArray.length) {
                    calorie = mMonthArray[month];
                } else {
                    return;
                }
                break;

            case REPORT_TYPE_DAY:
                if(day > -1 && day < mDayArray.length) {
                    calorie = mDayArray[day];
                } else {
                    return;
                }
                break;

            case REPORT_TYPE_HOUR:
                if(hour > -1 && hour < mHourArray.length) {
                    calorie = mHourArray[hour];
                } else {
                    return;
                }
                break;
        }

/*        if(calorie < 1)
            return;*/

        // Make data array to save
        // We use only one information, calorie.
        int[] dataArray = new int[5];
        Arrays.fill(dataArray, 0x00000000);
        dataArray[0] = calorie;

        mDB.insertActivityReport(type, time, year, month, day, hour, dataArray, null);
    }

    /**
     * Returns cached activity data
     * @param type		time period type
     * @return			array of activity data
     */
    public static int[] getCurrentActivityData(int type) {
        int[] activityData = null;

        switch(type) {
            case REPORT_TYPE_MONTH:
                activityData = mMonthArray;
                break;

            case REPORT_TYPE_DAY:
                activityData = mDayArray;
                break;

            case REPORT_TYPE_HOUR:
                activityData = mHourArray;
                break;

            default:
                break;
        }	// End of switch

        return activityData;
    }


    /*****************************************************
     *	Public methods
     ******************************************************/
    /**
     * Show sum of calorie and sum of walk count
     * Service triggers this at every sync
     */
    public void showActivityReport() {
        String str = String.format("%,.0f", MainActivity.calorieToday);
        mCalorieText.setText(str);
        mWalksText.setText(Integer.toString(MainActivity.stepToday));
    }
//
//    public void addMessage(ActivityReport object) {
//        if(object != null && mTimelineListAdapter != null) {
//            mTimelineListAdapter.addObject(object);
//            mTimelineListAdapter.notifyDataSetChanged();
//        }
//    }
//
//    public void addMessageOnTop(ActivityReport object) {
//        if(object != null && mTimelineListAdapter != null) {
//            mTimelineListAdapter.addObjectOnTop(object);
//            mTimelineListAdapter.notifyDataSetChanged();
//        }
//    }
//
//    public void addMessageAll(ArrayList<ActivityReport> objList) {
//        if(objList != null && mTimelineListAdapter != null) {
//            mTimelineListAdapter.addObjectAll(objList);
//            mTimelineListAdapter.notifyDataSetChanged();
//        }
//    }
//
//    public void deleteMessage(int id) {
//        if(mTimelineListAdapter != null) {
//            mTimelineListAdapter.deleteObject(id);
//            mTimelineListAdapter.notifyDataSetChanged();
//        }
//    }
//
//    public void deleteMessageByType(int type) {
//        if(mTimelineListAdapter != null) {
//            mTimelineListAdapter.deleteObjectByType(type);
//            mTimelineListAdapter.notifyDataSetChanged();
//        }
//    }
//
//    public void deleteMessageAll() {
//        if(mTimelineListAdapter != null) {
//            mTimelineListAdapter.deleteObjectAll();
//            mTimelineListAdapter.notifyDataSetChanged();
//        }
//    }


    /*****************************************************
     *	Handler, Listener, Timer, Sub classes
     ******************************************************/
    /**
     * Auto-refresh Timer
     */
    private class RefreshTimerTask extends TimerTask {
        public RefreshTimerTask() {}

        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (MainActivity.ready) {
                        addNewData();    // Reload data into database
                        MainActivity.ready = false;
                    }
                    drawStatistics();    // Refresh graph periodically
                    showActivityReport();
                }
            });
        }
    }

    public static final int MODE_CURRENT_TIME = 1;
    public static final int MODE_SELECTED_TIME = 2;

    private void getCurrentReportsFromDB() {
        // Get month data in this year
        Cursor c = mDB.selectReportWithDate(REPORT_TYPE_MONTH, mThisYear, -1, -1, -1);
        if(c != null) {
            getDataFromCursor(MODE_CURRENT_TIME, REPORT_TYPE_MONTH, c);
            c.close();
        }

        // Get day data in this month
        c = mDB.selectReportWithDate(REPORT_TYPE_DAY, mThisYear, mThisMonth, -1, -1);
        if(c != null) {
            getDataFromCursor(MODE_CURRENT_TIME, REPORT_TYPE_DAY, c);
            c.close();
        }

        // Get hour data in this day
        c = mDB.selectReportWithDate(REPORT_TYPE_HOUR, mThisYear, mThisMonth, mThisDay, -1);
        if(c != null) {
            getDataFromCursor(MODE_CURRENT_TIME, REPORT_TYPE_HOUR, c);
            c.close();
        }
    }

    private int[] getDataFromCursor(int mode, int type, Cursor c) {
        int[] timeArray = null;
        int columnIndex = 0;

        switch(type) {
            case REPORT_TYPE_MONTH:
                if(mode == MODE_CURRENT_TIME) {
                    timeArray = mMonthArray;
                } else {
                    timeArray = new int[12];
                    Arrays.fill(timeArray, 0x00000000);
                }

                columnIndex = DBHelper.INDEX_ACCEL_MONTH;
                break;

            case REPORT_TYPE_DAY:
                if(mode == MODE_CURRENT_TIME) {
                    timeArray = mDayArray;
                } else {
                    timeArray = new int[31];
                    Arrays.fill(timeArray, 0x00000000);
                }

                columnIndex = DBHelper.INDEX_ACCEL_DAY;
                break;

            case REPORT_TYPE_HOUR:
                if(mode == MODE_CURRENT_TIME) {
                    timeArray = mHourArray;
                } else {
                    timeArray = new int[24];
                    Arrays.fill(timeArray, 0x00000000);
                }

                columnIndex = DBHelper.INDEX_ACCEL_HOUR;
                break;
            default:
                return null;
        }

        if(c != null && c.getCount() > 0) {
            c.moveToFirst();
            while(!c.isAfterLast()) {
                int index = c.getInt(columnIndex);
                int calorie = c.getInt(DBHelper.INDEX_ACCEL_DATA1);
                if(calorie > 0 && index > -1 && index < timeArray.length) {
                    timeArray[index] = calorie;
                }

                c.moveToNext();
            }
        }

        return timeArray;
    }

}
