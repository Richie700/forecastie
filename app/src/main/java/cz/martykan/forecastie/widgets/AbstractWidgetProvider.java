package cz.martykan.forecastie.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

import cz.martykan.forecastie.MainActivity;
import cz.martykan.forecastie.R;
import cz.martykan.forecastie.Weather;

public abstract class AbstractWidgetProvider extends AppWidgetProvider {

    protected Bitmap getWeatherIcon(String text, Context context) {
        Bitmap myBitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_4444);
        Canvas myCanvas = new Canvas(myBitmap);
        Paint paint = new Paint();
        Typeface clock = Typeface.createFromAsset(context.getAssets(), "fonts/weather.ttf");
        paint.setAntiAlias(true);
        paint.setSubpixelText(true);
        paint.setTypeface(clock);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        paint.setTextSize(150);
        paint.setTextAlign(Paint.Align.CENTER);
        myCanvas.drawText(text, 128, 180, paint);
        return myBitmap;
    }

    private String setWeatherIcon(int actualId, int hourOfDay, Context context) {
        int id = actualId / 100;
        String icon = "";
        if (actualId == 800) {
            if (hourOfDay >= 7 && hourOfDay < 20) {
                icon = context.getString(R.string.weather_sunny);
            } else {
                icon = context.getString(R.string.weather_clear_night);
            }
        } else {
            switch (id) {
                case 2:
                    icon = context.getString(R.string.weather_thunder);
                    break;
                case 3:
                    icon = context.getString(R.string.weather_drizzle);
                    break;
                case 7:
                    icon = context.getString(R.string.weather_foggy);
                    break;
                case 8:
                    icon = context.getString(R.string.weather_cloudy);
                    break;
                case 6:
                    icon = context.getString(R.string.weather_snowy);
                    break;
                case 5:
                    icon = context.getString(R.string.weather_rainy);
                    break;
            }
        }
        return icon;
    }

    protected Weather parseWidgetJson(String result, Context context) {
        try {
            MainActivity.initMappings();

            JSONObject reader = new JSONObject(result);
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

            String temperature = reader.optJSONObject("main").getString("temp");
            if (sp.getString("unit", "C").equals("C")) {
                temperature = Float.parseFloat(temperature) - 273.15 + "";
            } else if (sp.getString("unit", "C").equals("F")) {
                temperature = (((9 * (Float.parseFloat(temperature) - 273.15)) / 5) + 32) + "";
            }

            double wind = Double.parseDouble(reader.optJSONObject("wind").getString("speed"));
            if (sp.getString("speedUnit", "m/s").equals("kph")) {
                wind = wind * 3.59999999712;
            } else if (sp.getString("speedUnit", "m/s").equals("mph")) {
                wind = wind * 2.23693629205;
            }

            double pressure = Double.parseDouble(reader.optJSONObject("main").getString("pressure"));
            if (sp.getString("pressureUnit", "hPa").equals("kPa")) {
                pressure = pressure / 10;
            } else if (sp.getString("pressureUnit", "hPa").equals("mm Hg")) {
                pressure = pressure * 0.750061561303;
            }

            long lastUpdateTimeInMillis = sp.getLong("lastUpdate", -1);
            String lastUpdate;
            if (lastUpdateTimeInMillis < 0) {
                // No time
                lastUpdate = "";
            } else {
                lastUpdate = context.getString(R.string.last_update_widget, MainActivity.formatTimeWithDayIfNotToday(context, lastUpdateTimeInMillis));
            }

            String description = reader.optJSONArray("weather").getJSONObject(0).getString("description");
            description = description.substring(0,1).toUpperCase() + description.substring(1).toLowerCase();

            Weather widgetWeather = new Weather();
            widgetWeather.setCity(reader.getString("name"));
            widgetWeather.setCountry(reader.optJSONObject("sys").getString("country"));
            widgetWeather.setTemperature(temperature.substring(0, temperature.indexOf(".")) + "°" + localize(sp, context, "unit", "C"));
            widgetWeather.setDescription(description);
            widgetWeather.setWind(context.getString(R.string.wind) + ": " + (wind + "").substring(0, (wind + "").indexOf(".") + 2) + " " + localize(sp, context, "speedUnit", "m/s")
                    + (widgetWeather.isWindDirectionAvailable() ? " " + MainActivity.getWindDirectionString(sp, context, widgetWeather) : ""));
            widgetWeather.setPressure(context.getString(R.string.pressure) + ": " + (pressure + "").substring(0, (pressure + "").indexOf(".") + 2) + " " + localize(sp, context, "pressureUnit", "hPa"));
            widgetWeather.setHumidity(reader.optJSONObject("main").getString("humidity"));
            widgetWeather.setSunrise(reader.optJSONObject("sys").getString("sunrise"));
            widgetWeather.setSunset(reader.optJSONObject("sys").getString("sunset"));
            widgetWeather.setIcon(setWeatherIcon(Integer.parseInt(reader.optJSONArray("weather").getJSONObject(0).getString("id")), Calendar.getInstance().get(Calendar.HOUR_OF_DAY), context));
            widgetWeather.setLastUpdated(lastUpdate);

            return widgetWeather;
        } catch (JSONException e) {
            Log.e("JSONException Data", result);
            e.printStackTrace();
            return new Weather();
        }
    }

    protected String localize(SharedPreferences sp, Context context, String preferenceKey,
                              String defaultValueKey) {
        return MainActivity.localize(sp, context, preferenceKey, defaultValueKey);
    }

    public static void updateWidgets(Context context) {
        updateWidgets(context, ExtensiveWidgetProvider.class);
        updateWidgets(context, TimeWidgetProvider.class);
    }

    private static void updateWidgets(Context context, Class widgetClass) {
        Intent intent = new Intent(context.getApplicationContext(), widgetClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] ids = AppWidgetManager.getInstance(context.getApplicationContext())
                .getAppWidgetIds(new ComponentName(context.getApplicationContext(), widgetClass));
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.getApplicationContext().sendBroadcast(intent);
    }
}
