package com.example.jordan.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.example.jordan.sunshine.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by jordan on 1/28/15.
 */
public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
    private final String DEFAULT_WEATHER_POSTAL = "92691";

    ArrayAdapter<String> mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            Log.d(LOG_TAG, "manually refreshing weather");

            //TODO reduce code duplicated here and in onCreateView
            //      get postal code from SharedPreferences
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String weatherPostal = settings.getString("location", "00000");
            String temperatureUnits = settings.getString("units", "metric");

//        fetch weather forecast with async task request to API
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute(weatherPostal, temperatureUnits);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //  api url for 7 days of weather data for 92691 in json format
//        http://api.openweathermap.org/data/2.5/forecast/daily?q=92691&mode=json&units=metric&cnt=7


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        String[] fakeWeatherArray = {};

//            String forecastJson = requestForecast();
//            fakeWeatherArray[0] = forecastJson;

        final List<String> weatherForecast = new ArrayList<String>(Arrays.asList(fakeWeatherArray));

        mForecastAdapter = new ArrayAdapter<String>(
                getActivity(),
                R.layout.list_item_forecast,
                R.id.list_item_forecast_textview,
                weatherForecast);

        ListView weatherListView = (ListView)rootView.findViewById(R.id.listview_forecast);
        weatherListView.setAdapter(mForecastAdapter);

        weatherListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String forecast = mForecastAdapter.getItem(position);
//                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, forecast);
                startActivity(detailIntent);
            }
        });

//      get postal code from SharedPreferences
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String weatherPostal = settings.getString("location", "00000");
        String temperatureUnits = settings.getString("units", "metric");

//        fetch weather forecast with async task request to API
        FetchWeatherTask weatherTask = new FetchWeatherTask();
        weatherTask.execute(weatherPostal, temperatureUnits);

        return rootView;
    }



    public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

        @Override
        protected void onPostExecute(String[] strings) {
            super.onPostExecute(strings);
//            Log.d(LOG_TAG, "Kickass! "+Arrays.toString(strings));
            if(strings != null) {
                mForecastAdapter.clear();
                mForecastAdapter.addAll(strings);
            }
            else {
                Log.d(LOG_TAG, "No weather data returned from async task. Expected if device is offline");
            }
        }

        @Override
        protected String[] doInBackground(String... params) {
            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            final String weatherAuthority = "api.openweathermap.org";
            final String weatherUriPrefix = "data/2.5/forecast/daily";
            final String postalKey = "q";
            final String modeKey = "mode"; //e.g. 'json', 'xml'
            final String unitsKey = "units";
            final String numDaysKey = "cnt";

            String postalValue = "00000";
            String modeValue = "json";
            String unitsValue = "metric"; //e.g. 'metric', 'imperial' //used for server request, not for displaying to user
            int numDaysValue = 7;

            if(params.length == 0) {
                return null;
            }
            String postalCode = params[0];
            String temperatureUnits = params[1]; //used for displaying returned data, not same as unitsValue for server request

            Log.d(LOG_TAG, "Requesting weather for postal code: "+postalCode);
            Log.d(LOG_TAG, "Temperature Units set to: "+temperatureUnits);

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
//                URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily?q=92691&mode=json&units=metric&cnt=7");

                Uri.Builder builder = new Uri.Builder();
                builder.scheme("http")
                        .authority(weatherAuthority)
                        .appendEncodedPath(weatherUriPrefix)
                        .appendQueryParameter(postalKey, postalCode)
                        .appendQueryParameter(modeKey, modeValue)
                        .appendQueryParameter(unitsKey, unitsValue)
                        .appendQueryParameter(numDaysKey, Integer.toString(numDaysValue));

                String urlString = builder.build().toString();
//                Log.d(LOG_TAG, "url: "+urlString);
                URL url = new URL(urlString);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    forecastJsonStr = null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
//            Log.d(LOG_TAG, forecastJsonStr);
            try {
                return getWeatherDataFromJson(forecastJsonStr, numDaysValue, temperatureUnits);
            }
            catch (JSONException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
            return null;
        }

        /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
        private String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            Date date = new Date(time * 1000);
            SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
            return format.format(date).toString();
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        private String formatHighLows(double high, double low, String units) {
            // convert to imperial if units variable is "imperial"
            //assuming temperature values passed in from "high" and "low" are in metric
//            Log.d(LOG_TAG, "Units for formatting are: "+units);
            if(units.equals("imperial")) {
//                Log.d(LOG_TAG, "Formatting temperature from metric to imperial");
                high = convertMetricToImperial(high);
                low = convertMetricToImperial(low);
            }

            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }

        /**
         * Convert double from metric to imperial temperature
         */
        private double convertMetricToImperial(double metricTemperature) {
            double imperialTemp = (metricTemperature*1.8)+32;
            return imperialTemp;
        }

        /**
         * Take the String representing the complete forecast in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays, String units)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DATETIME = "dt";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                long dateTime = dayForecast.getLong(OWM_DATETIME);
                day = getReadableDateString(dateTime);

                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low, units);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            return resultStrs;
        }
    }
}
