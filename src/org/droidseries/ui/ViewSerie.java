package org.droidseries.ui;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.droidseries.R;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;

import java.text.ParseException;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class ViewSerie extends Activity
{
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.view_serie);
		View view = findViewById(R.id.viewSerie);
		view.setOnTouchListener(new SwipeDetect());
		TextView seriename = (TextView) findViewById(R.id.seriename);
		seriename.setText(getIntent().getStringExtra("seriename"));
		setTitle(getIntent().getStringExtra("seriename") +" - "+ getString(R.string.messages_overview));
		TextView serieoverview = (TextView) findViewById(R.id.serieoverview);
		if (!getIntent().getStringExtra("poster").isEmpty()) {
			BitmapDrawable poster = (BitmapDrawable) BitmapDrawable.createFromPath(getIntent().getStringExtra("poster"));
			if (poster != null)  {
				poster.setTargetDensity(getResources().getDisplayMetrics().densityDpi);	// Don't auto-resize from mdpi to screen density
				serieoverview.setCompoundDrawablesWithIntrinsicBounds(null, null, poster, null);
			}
		}
		serieoverview.setText(getIntent().getStringExtra("serieoverview"));
		TextView status = (TextView) findViewById(R.id.status);
		String statusValue = translateStatus(getIntent().getStringExtra("status"));
		status.setText(getString(R.string.series_status) +" "+ statusValue);
		TextView firstaired = (TextView) findViewById(R.id.firstaired);
		String firstairedValue = getIntent().getStringExtra("firstaired");
		if (!firstairedValue.equals("")) {
			try {
				SimpleDateFormat SDF = new SimpleDateFormat("yyyy-MM-dd");
				Date epDate = SDF.parse(firstairedValue);
				Format formatter = SimpleDateFormat.getDateInstance();
				firstairedValue = formatter.format(epDate);
			} catch (ParseException e) {
				Log.e("DroidSeries", e.getMessage());
			}
		}
		firstaired.setText(getString(R.string.series_first_aired) + " " + firstairedValue);
		TextView airday = (TextView) findViewById(R.id.airday);
		String airdayValue = getIntent().getStringExtra("airday");
		if (airdayValue.equals("null")) {
			airdayValue = "";
		} else if (airdayValue.equalsIgnoreCase("Daily")) {
			airdayValue = getString(R.string.messages_daily);
		}
		airday.setText(getString(R.string.series_air_day) + " " + airdayValue);
		TextView airtime = (TextView) findViewById(R.id.airtime);
		String airtimeValue = getIntent().getStringExtra("airtime");
		if (airtimeValue.equals("null")) {
			airtimeValue = "";
		} else if (!airtimeValue.equals("")) {
			try {
				SimpleDateFormat SDF = new SimpleDateFormat("h:m a");
				Date epDate = SDF.parse(airtimeValue);
				Format formatter = SimpleDateFormat.getTimeInstance(SimpleDateFormat.SHORT);
				airtimeValue = formatter.format(epDate);
			} catch (ParseException e) {
				Log.e("DroidSeries", e.getMessage());
			}
		}
		airtime.setText(getString(R.string.series_air_time) + " " + airtimeValue);
		TextView runtime = (TextView) findViewById(R.id.runtime);
		runtime.setText(String.format(getString(R.string.series_runtime_minutes), getIntent().getStringExtra("runtime")));
		TextView network = (TextView) findViewById(R.id.network);
		network.setText(getString(R.string.series_network) + " "+ getIntent().getStringExtra("network"));
		TextView genre = (TextView) findViewById(R.id.genre);
		genre.setText(getString(R.string.series_genre) + " " + getIntent().getStringExtra("genre"));
		TextView rating = (TextView) findViewById(R.id.rating);
		rating.setText(getString(R.string.series_rating) + " " + getIntent().getStringExtra("rating"));
		TextView serieactors = (TextView) findViewById(R.id.serieactors);
		serieactors.setText(getString(R.string.series_actors) + " "+ getIntent().getStringExtra("serieactors"));
	}
	
	private String translateStatus(String statusValue) {
		if (statusValue.equalsIgnoreCase("Continuing")) {
			return getString(R.string.showstatus_continuing);
		} else if (statusValue.equalsIgnoreCase("Ended")) {
			return getString(R.string.showstatus_ended);
		} else {
			return statusValue;
		}
	}
}