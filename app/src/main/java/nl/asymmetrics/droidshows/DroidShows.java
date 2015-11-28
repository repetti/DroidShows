package nl.asymmetrics.droidshows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.io.FileUtils;

import nl.asymmetrics.droidshows.thetvdb.TheTVDB;
import nl.asymmetrics.droidshows.thetvdb.model.Serie;
import nl.asymmetrics.droidshows.thetvdb.model.TVShowItem;
import nl.asymmetrics.droidshows.ui.IconView;
import nl.asymmetrics.droidshows.ui.SerieSeasons;
import nl.asymmetrics.droidshows.ui.ViewEpisode;
import nl.asymmetrics.droidshows.ui.ViewSerie;
import nl.asymmetrics.droidshows.utils.SQLiteStore;
import nl.asymmetrics.droidshows.utils.SwipeDetect;
import nl.asymmetrics.droidshows.utils.Update;
import nl.asymmetrics.droidshows.utils.Utils;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class DroidShows extends ListActivity
{
	/* Menus */
	private static final int UNDO_MENU_ITEM = Menu.FIRST;
	private static final int ADD_SERIE_MENU_ITEM = UNDO_MENU_ITEM + 1;
	private static final int SEARCH_MENU_ITEM = ADD_SERIE_MENU_ITEM + 1;
	private static final int TOGGLE_ARCHIVE_MENU_ITEM = SEARCH_MENU_ITEM + 1;
	private static final int SORT_MENU_ITEM = TOGGLE_ARCHIVE_MENU_ITEM + 1;
	private static final int UPDATEALL_MENU_ITEM = SORT_MENU_ITEM + 1;
	private static final int OPTIONS_MENU_ITEM = UPDATEALL_MENU_ITEM + 1;
	private static final int EXIT_MENU_ITEM = OPTIONS_MENU_ITEM + 1;
	/* Context Menus */
	private static final int MARK_NEXT_EPISODE_AS_SEEN_CONTEXT = Menu.FIRST;
	private static final int TOGGLE_ARCHIVED = MARK_NEXT_EPISODE_AS_SEEN_CONTEXT + 1;
	private static final int VIEW_SERIEDETAILS_CONTEXT = TOGGLE_ARCHIVED + 1;
	private static final int VIEW_IMDB_CONTEXT = VIEW_SERIEDETAILS_CONTEXT + 1;
	private static final int VIEW_EP_IMDB_CONTEXT = VIEW_IMDB_CONTEXT + 1;
	private static final int UPDATE_CONTEXT = VIEW_EP_IMDB_CONTEXT + 1;
	private static final int DELETE_CONTEXT = UPDATE_CONTEXT + 1;
	public static String on;
	private static AlertDialog m_AlertDlg;
	private static ProgressDialog m_ProgressDialog = null;
	private static ProgressDialog updateAllSeriesPD = null;
	public static SeriesAdapter seriesAdapter;
	private static ListView listView = null;
	private static String backFromSeasonSerieId = null;
	private static int oldListPosition = -1;
	private static TheTVDB theTVDB;
	private Utils utils = new Utils();
	private Update updateDS;
	private static final String PREF_NAME = "DroidShowsPref";
	private SharedPreferences sharedPrefs;
	private static final String SORT_PREF_NAME = "sort";
	private static final int SORT_BY_NAME = 0;
	private static final int SORT_BY_LAST_UNSEEN = 1;
	private static int sortOption;
	private static final String LAST_SEASON_PREF_NAME = "last_season";
	private static final int UPDATE_ALL_SEASONS = 0;
	private static final int UPDATE_LAST_SEASON_ONLY = 1;
	private static int lastSeasonOption;
	private static final String INCLUDE_SPECIALS_NAME = "include_specials";
	public static boolean includeSpecialsOption;
	private static final String FULL_LINE_CHECK_NAME = "full_line";
	public static boolean fullLineCheckOption;
	private static final String SWITCH_SWIPE_DIRECTION = "switch_swipe_direction";
	public static boolean switchSwipeDirection;
	private static final String LAST_STATS_UPDATE_NAME = "last_stats_update";
	private static String lastStatsUpdate;
	private static final String LANGUAGE_CODE_NAME = "language";
	public static String langCode;
	public static Thread deleteTh = null;
	public static Thread updateShowTh = null;
	public static Thread updateAllShowsTh = null;
	private String toastMessage;
	public static SQLiteStore db;
	public static List<TVShowItem> series = null;
	private static List<String[]> undo = new ArrayList<String[]>();
	private SwipeDetect swipeDetect = new SwipeDetect();
	private static AsyncInfo asyncInfo;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	private EditText searchV;
	private InputMethodManager keyboard;
	private int padding;
	public static int showArchive;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (!isTaskRoot()) {	// Prevent multiple instances: http://stackoverflow.com/a/11042163
			final Intent intent = getIntent();
			if (intent.hasCategory(Intent.CATEGORY_LAUNCHER) && Intent.ACTION_MAIN.equals(intent.getAction())) {
				finish();
				return;
			}
		}
		setContentView(R.layout.main);
		db = SQLiteStore.getInstance(this);

		updateDS = new Update(this);
		if(updateDS.updateDroidShows())
			db.updateShowStats();

		// Preferences
		sharedPrefs = getSharedPreferences(PREF_NAME, 0);
		sortOption = sharedPrefs.getInt(SORT_PREF_NAME, SORT_BY_NAME);
		lastSeasonOption = sharedPrefs.getInt(LAST_SEASON_PREF_NAME, UPDATE_ALL_SEASONS);
		includeSpecialsOption = sharedPrefs.getBoolean(INCLUDE_SPECIALS_NAME, false);
		fullLineCheckOption = sharedPrefs.getBoolean(FULL_LINE_CHECK_NAME, false);
		switchSwipeDirection = sharedPrefs.getBoolean(SWITCH_SWIPE_DIRECTION, false);
		lastStatsUpdate = sharedPrefs.getString(LAST_STATS_UPDATE_NAME, "");
		langCode = sharedPrefs.getString(LANGUAGE_CODE_NAME, getString(R.string.lang_code));

		series = new ArrayList<TVShowItem>();
		seriesAdapter = new SeriesAdapter(this, R.layout.row, series);
		setListAdapter(seriesAdapter);
		on = getString(R.string.messages_on);
		listView = getListView();
		getSeries();
		registerForContextMenu(listView);
		listView.setOnTouchListener(swipeDetect);
		setFastScroll();
		searchV = (EditText) findViewById(R.id.search_text);
		searchV.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				seriesAdapter.getFilter().filter(s);
			}
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			public void afterTextChanged(Editable s) {}
		});
		keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		padding = (int) (6 * (getApplicationContext().getResources().getDisplayMetrics().densityDpi / 160f));
	}
	
	private void setFastScroll() {
		if (seriesAdapter.getCount() > 20) {
			try {	// http://stackoverflow.com/a/26447004
				Drawable thumb = getResources().getDrawable(R.drawable.thumb);
				String fieldName = "mFastScroller";
				if (android.os.Build.VERSION.SDK_INT >= 22)	// 22 = Lollipop
		            fieldName = "mFastScroll";

				java.lang.reflect.Field fieldFastScroller = AbsListView.class.getDeclaredField(fieldName);
				fieldFastScroller.setAccessible(true);
				listView.setFastScrollEnabled(true);
				Object thisFastScroller = fieldFastScroller.get(listView);
				java.lang.reflect.Field fieldToChange;

				if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
					fieldToChange = fieldFastScroller.getType().getDeclaredField("mThumbImage");
					fieldToChange.setAccessible(true);
					ImageView iv = (ImageView) fieldToChange.get(thisFastScroller);
					fieldToChange.set(thisFastScroller, iv);
					iv.setMinimumWidth(thumb.getIntrinsicWidth());	//IS//THIS//NECESSARY//?//
					iv.setMaxWidth(thumb.getIntrinsicWidth());	//IS//THIS//NECESSARY//?//
					iv.setImageDrawable(thumb);

					fieldToChange = fieldFastScroller.getType().getDeclaredField("mTrackImage");
					fieldToChange.setAccessible(true);
					iv = (ImageView) fieldToChange.get(thisFastScroller);
					fieldToChange.set(thisFastScroller, iv);
					iv.setImageDrawable(null);	// getResources().getDrawable(R.drawable.div)
				} else {
					fieldToChange = fieldFastScroller.getType().getDeclaredField("mThumbDrawable");
					fieldToChange.setAccessible(true);
					fieldToChange.set(thisFastScroller, thumb);

					fieldToChange = fieldFastScroller.getType().getDeclaredField("mThumbW");
					fieldToChange.setAccessible(true);
					fieldToChange.setInt(thisFastScroller, thumb.getIntrinsicWidth());

					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
						fieldToChange = fieldFastScroller.getType().getDeclaredField("mTrackDrawable");
						fieldToChange.setAccessible(true);
						fieldToChange.set(thisFastScroller, null);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/* Options Menu */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		menu.add(0, UNDO_MENU_ITEM, 0, getString(R.string.menu_undo)).setIcon(android.R.drawable.ic_menu_revert);
		menu.add(0, ADD_SERIE_MENU_ITEM, 0, getString(R.string.menu_add_serie)).setIcon(android.R.drawable.ic_menu_add);
		menu.add(0, SEARCH_MENU_ITEM, 0, getString(R.string.menu_search)).setIcon(android.R.drawable.ic_menu_search);
		menu.add(0, TOGGLE_ARCHIVE_MENU_ITEM, 0, getString(R.string.menu_show_archive));
		menu.add(0, SORT_MENU_ITEM, 0, getString(R.string.menu_sort_last_unseen));
		menu.add(0, UPDATEALL_MENU_ITEM, 0, getString(R.string.menu_update)).setIcon(android.R.drawable.ic_menu_upload);
		menu.add(0, OPTIONS_MENU_ITEM, 0, getString(R.string.menu_about)).setIcon(android.R.drawable.ic_menu_manage);
		menu.add(0, EXIT_MENU_ITEM, 0, getString(R.string.menu_exit)).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (undo.size() > 0) {
			menu.findItem(UNDO_MENU_ITEM).setVisible(true);
		} else {
			menu.findItem(UNDO_MENU_ITEM).setVisible(false);
		}
		if (sortOption == SORT_BY_LAST_UNSEEN) {
			menu.findItem(SORT_MENU_ITEM).setIcon(android.R.drawable.ic_menu_sort_alphabetically)
				.setTitle(R.string.menu_sort_az);
		} else {
			menu.findItem(SORT_MENU_ITEM).setIcon(android.R.drawable.ic_menu_sort_by_size)
				.setTitle(R.string.menu_sort_last_unseen);
		}
		if (showArchive == 1) {
			menu.findItem(TOGGLE_ARCHIVE_MENU_ITEM).setTitle(R.string.menu_show_current)
				.setIcon(android.R.drawable.ic_menu_today);
		} else {
			menu.findItem(TOGGLE_ARCHIVE_MENU_ITEM).setTitle(R.string.menu_show_archive)
				.setIcon(android.R.drawable.ic_menu_recent_history);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case ADD_SERIE_MENU_ITEM :
				super.onSearchRequested();
				break;
			case SEARCH_MENU_ITEM :
				onSearchRequested();
				break;
			case TOGGLE_ARCHIVE_MENU_ITEM :
				toggleArchive();
				break;
			case SORT_MENU_ITEM :
				toggleSort();
				break;
			case UPDATEALL_MENU_ITEM :
				updateAllSeries();
				break;
			case OPTIONS_MENU_ITEM :
				aboutDialog();
				break;
			case UNDO_MENU_ITEM :
				markLastEpUnseen();
				break;
			case EXIT_MENU_ITEM :
				onPause();	// save options
				asyncInfo.cancel(true);
				db.close();
				this.finish();
				System.gc();
				System.exit(0);	// kill process
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void toggleArchive() {
		asyncInfo.cancel(true);
		listView.setEmptyView(findViewById(R.id.empty_notext));
		seriesAdapter.clear();
		lastStatsUpdate = "";
		showArchive ^= 1;
		getSeries();
		asyncInfo = new AsyncInfo();
		asyncInfo.execute();
		setTitle(getString(R.string.layout_app_name)
				+(showArchive == 1 ? " - "+ getString(R.string.archive) : ""));
	}

	public void toggleSort() {
		sortOption ^= 1;
		listView.post(updateListView);
	}

	private void aboutDialog() {
		if (m_AlertDlg != null) {
			m_AlertDlg.cancel();
		}
		View about = View.inflate(this, R.layout.alert_about, null);
		TextView changelog = (TextView) about.findViewById(R.id.copyright);
		try {
			changelog.setText(getString(R.string.copyright)
				.replace("{v}", getPackageManager().getPackageInfo(getPackageName(), 0).versionName)
				.replace("{y}", new Date().getYear()+1900 +""));
			changelog.setTextColor(changelog.getTextColors().getDefaultColor());
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		TextView changeLangB = (TextView) about.findViewById(R.id.change_language);
		changeLangB.setText(getString(R.string.dialog_change_language) +" ("+ langCode +")");
		CheckBox lastSeasonCheckbox = (CheckBox) about.findViewById(R.id.last_season);
		lastSeasonCheckbox.setChecked(lastSeasonOption == UPDATE_LAST_SEASON_ONLY);
		CheckBox includeSpecialsCheckbox = (CheckBox) about.findViewById(R.id.include_specials);
		includeSpecialsCheckbox.setChecked(includeSpecialsOption);
		CheckBox fullLineCheckbox = (CheckBox) about.findViewById(R.id.full_line_check);
		fullLineCheckbox.setChecked(fullLineCheckOption);
		CheckBox switchSwipeDirectionBox = (CheckBox) about.findViewById(R.id.switch_swipe_direction);
		switchSwipeDirectionBox.setChecked(switchSwipeDirection);
		m_AlertDlg = new AlertDialog.Builder(this)
			.setView(about)
			.setTitle(R.string.layout_app_name).setIcon(R.drawable.icon)
			.setPositiveButton(getString(R.string.dialog_backup), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					backup();
				}
			})
			.setNegativeButton(getString(R.string.dialog_restore), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					new AlertDialog.Builder(DroidShows.this)
					.setTitle(R.string.dialog_restore)
					.setMessage(R.string.dialog_restore_now)
					.setPositiveButton(R.string.dialog_OK, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int id) {
							restore();
						}
					})
					.setNegativeButton(R.string.dialog_Cancel, null)
					.show();
				}
			})
			.show();
		m_AlertDlg.setCanceledOnTouchOutside(true);
	}
	
	public void dialogOptions(View v) {
		switch(v.getId()) {
			case R.id.last_season:
				lastSeasonOption ^= 1;
				break;
			case R.id.include_specials:
				includeSpecialsOption ^= true;
				db.updateShowStats();
				getSeries();
				break;
			case R.id.full_line_check:
				fullLineCheckOption ^= true;
				break;
			case R.id.switch_swipe_direction:
				switchSwipeDirection ^= true;
				break;
			case R.id.change_language:
				AlertDialog.Builder changeLang = new AlertDialog.Builder(this);
				changeLang.setItems(R.array.languages, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						langCode = getResources().getStringArray(R.array.langcodes)[item];
						TextView changeLangB = (TextView) m_AlertDlg.findViewById(R.id.change_language);
						changeLangB.setText(getString(R.string.dialog_change_language) +" ("+ langCode +")");
					}
				});
				changeLang.show();
			break;
		}
	}
	
	private void backup() {
		int toastTxt = R.string.dialog_backup_done;
		File source = new File(getApplicationInfo().dataDir +"/databases/DroidShows.db");
		File destination = new File(Environment.getExternalStorageDirectory(), "DroidShows.db");
		if (destination.exists()) {
			try {
				copy(destination, new File(Environment.getExternalStorageDirectory(), "DroidShows.db.previous"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			copy(source, destination);
		} catch (IOException e) {
			toastTxt = R.string.dialog_backup_failed;
			e.printStackTrace();
		}
		Toast.makeText(getApplicationContext(), toastTxt, Toast.LENGTH_LONG).show();
	}
	
	private void restore() {
		int toastTxt = R.string.dialog_restore_done;
		File source = new File(Environment.getExternalStorageDirectory(), "DroidShows.db");
		if (!source.exists()) source = new File(Environment.getExternalStorageDirectory(), "droidseries.db");
		if (source.exists()) {
			File destination = new File(getApplicationInfo().dataDir +"/databases/DroidShows.db");
			try {
				copy(source, destination);
				updateDS.updateDroidShows();
				File thumbs[] = new File(getApplicationContext().getFilesDir().getAbsolutePath() +"/thumbs/banners/posters").listFiles();
				if (thumbs != null)
					for (File thumb : thumbs)
						thumb.delete();
				for (File file : new File(getApplicationInfo().dataDir +"/databases/").listFiles())
				    if (!file.getName().equalsIgnoreCase("DroidShows.db")) file.delete();
				if (showArchive == 1)
					setTitle(getString(R.string.layout_app_name));
				showArchive = 2;	// Get archived and current shows
				getSeries();
				updateAllSeries();
				undo.clear();
			} catch (IOException e) {
				toastTxt = R.string.dialog_restore_failed;
				e.printStackTrace();
			}
		} else {
			toastTxt = R.string.dialog_restore_notfound;
		}
		Toast.makeText(getApplicationContext(), toastTxt, Toast.LENGTH_LONG).show();
	}
		
	private void copy(File source, File destination) throws IOException {
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			asyncInfo.cancel(true);
			db.close();
			FileChannel sourceCh = null, destinationCh = null;
			try {
				sourceCh = new FileInputStream(source).getChannel();
				if (destination.exists()) destination.delete();
				destination.createNewFile();
				destinationCh = new FileOutputStream(destination).getChannel();
				destinationCh.transferFrom(sourceCh, 0, sourceCh.size());
				destination.setLastModified(source.lastModified());
			} finally {
				if (sourceCh != null) {
					sourceCh.close();
				}
				if (destinationCh != null) {
					destinationCh.close();
				}
			}
			db.openDataBase();
			asyncInfo = new AsyncInfo();
			asyncInfo.execute();
		}
	}

	/* context menu */
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		menu.add(0, MARK_NEXT_EPISODE_AS_SEEN_CONTEXT, 0, getString(R.string.menu_context_mark_next_episode_as_seen));
		menu.add(0, VIEW_SERIEDETAILS_CONTEXT, 0, getString(R.string.menu_context_view_serie_details));
		menu.add(0, VIEW_IMDB_CONTEXT, 0, getString(R.string.menu_context_view_imdb));
		menu.add(0, VIEW_EP_IMDB_CONTEXT, 0, getString(R.string.menu_context_view_ep_imdb));
		menu.add(0, UPDATE_CONTEXT, 0, getString(R.string.menu_context_update));
		menu.add(0, TOGGLE_ARCHIVED, 0, getString(R.string.menu_archive));
		menu.add(0, DELETE_CONTEXT, 0, getString(R.string.menu_context_delete));
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) menuInfo;
	    if (seriesAdapter.getItem(info.position).getUnwatchedAired() == 0)
	    	menu.findItem(MARK_NEXT_EPISODE_AS_SEEN_CONTEXT).setVisible(false);
	    if (seriesAdapter.getItem(info.position).getPassiveStatus())
	    	menu.findItem(TOGGLE_ARCHIVED).setTitle(R.string.menu_unarchive);
	}

	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
			case MARK_NEXT_EPISODE_AS_SEEN_CONTEXT :
				markNextEpSeen(info.position);
				return true;
			case VIEW_SERIEDETAILS_CONTEXT :
				showDetails(seriesAdapter.getItem(info.position).getSerieId());
				return true;
			case VIEW_IMDB_CONTEXT :
				IMDbDetails(seriesAdapter.getItem(info.position).getSerieId(), seriesAdapter.getItem(info.position).getName(), false);
				return true;
			case VIEW_EP_IMDB_CONTEXT :
				IMDbDetails(seriesAdapter.getItem(info.position).getSerieId(), seriesAdapter.getItem(info.position).getName(), true);
				return true;
			case UPDATE_CONTEXT :
				updateSerie(seriesAdapter.getItem(info.position).getSerieId(), info.position);
				return true;
			case TOGGLE_ARCHIVED :
				asyncInfo.cancel(true);
				String serieId = seriesAdapter.getItem(info.position).getSerieId();
				db.updateSerieStatus(serieId, showArchive ^ 1);
				String message = seriesAdapter.getItem(info.position).getName()
					+" "+ (showArchive == 1 ? getString(R.string.messages_context_unarchived) : getString(R.string.messages_context_archived));
				Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
				series.remove(seriesAdapter.getItem(info.position));
				listView.post(updateListView);
				asyncInfo = new AsyncInfo();
				asyncInfo.execute();
				return true;
			case DELETE_CONTEXT :
				asyncInfo.cancel(true);
				final int position = info.position;
				final Runnable deleteserie = new Runnable() {
					public void run() {
						TVShowItem serie = seriesAdapter.getItem(position);
						String sname = serie.getName();
						db.deleteSerie(serie.getSerieId());
						series.remove(series.indexOf(serie));
						listView.post(updateListView);
						Looper.prepare();	// Threads don't have a message loop
						Toast.makeText(getApplicationContext(), sname +" "+ getString(R.string.messages_deleted), Toast.LENGTH_LONG).show();
						asyncInfo = new AsyncInfo();
						asyncInfo.execute();
						Looper.loop();
					}
				};
				AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
				alertDialog.setTitle(R.string.dialog_title_delete);
				alertDialog.setMessage(String.format(getString(R.string.dialog_delete), seriesAdapter.getItem(info.position).getName()));
				alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
				alertDialog.setCancelable(false);
				alertDialog.setPositiveButton(getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						deleteTh = new Thread(deleteserie);
						deleteTh.start();
						clearFilter(null);
						return;
					}
				});
				alertDialog.setNegativeButton(getString(R.string.dialog_Cancel), new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						return;
					}
				});
				alertDialog.show();
				return true;
			default :
				return super.onContextItemSelected(item);
		}
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		keyboard.hideSoftInputFromWindow(searchV.getWindowToken(), 0);
		oldListPosition = position;
		if (swipeDetect.value == 1) {
			if (markNextEpSeen(position)) {
				Vibrator vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
				vib.vibrate(150);
			}
		} else if (swipeDetect.value == 0) {
			String serieId = seriesAdapter.getItem(position).getSerieId();
			backFromSeasonSerieId = serieId;
			Intent serieSeasons = new Intent(DroidShows.this, SerieSeasons.class);
			serieSeasons.putExtra("serieId", serieId);
			startActivity(serieSeasons);
		}
	}
	
	private boolean markNextEpSeen(int position) {
		oldListPosition = position;
		TVShowItem serie = seriesAdapter.getItem(position);
		String serieId = serie.getSerieId();
		String nextEpisode = db.getNextEpisodeId(serieId, -1, true);
		if (!nextEpisode.equals("-1")) {
			String episodeMarked = db.updateUnwatchedEpisode(serieId, nextEpisode);
			Toast.makeText(getApplicationContext(), serie.getName() +" "+ episodeMarked +" "+ getString(R.string.messages_marked_seen), Toast.LENGTH_SHORT).show();
			undo.add(new String[] {serieId, nextEpisode, serie.getName()});
			updateShowView(serie);
			return true;
		}
		return false;
	}
	
	private void markLastEpUnseen() {
		oldListPosition = -1;
		String[] episodeInfo = undo.get(undo.size()-1);
		String serieId = episodeInfo[0];
		String episodeId = episodeInfo[1];
		String serieName = episodeInfo[2];
		String episodeMarked = db.updateUnwatchedEpisode(serieId, episodeId);
		undo.remove(undo.size()-1);
		Toast.makeText(getApplicationContext(), serieName +" "+ episodeMarked +" "+ getString(R.string.messages_marked_unseen), Toast.LENGTH_SHORT).show();
		listView.post(updateShowView(serieId));
	}
	
	private Runnable updateShowView(final String serieId) {
		Runnable updateView = new Runnable(){
			public void run() {
				for (TVShowItem serie : series) {
					if (serie.getSerieId().equals(serieId)) {
						updateShowView(serie);
						break;
					}
				}
			}
		};
		return updateView;
	}
	
	private void updateShowView(final TVShowItem serie) {
		final TVShowItem newSerie = createTVShowItem(serie.getSerieId());
		series.set(series.indexOf(serie), newSerie);
		listView.post(updateListView);
		listView.post(new Runnable() {
			public void run() {
				if (seriesAdapter.getPosition(newSerie) != oldListPosition) {
					int pos = seriesAdapter.getPosition(newSerie);
					if (pos != series.indexOf(serie)) {
						listView.setSelection(pos);
						if (0 < pos && pos < listView.getCount() - 5)
							listView.smoothScrollBy(-padding, 500);
					}
				}
			}
		});
	}
	
	private void showDetails(String serieId) {
		Intent viewSerie = new Intent(DroidShows.this, ViewSerie.class);
		viewSerie.putExtra("serieId", serieId);
		startActivity(viewSerie);
	}
	
	private void IMDbDetails(String serieId, String serieName, boolean viewNextEpisode) {
		String nextEpisode = (viewNextEpisode ? db.getNextEpisodeId(serieId, -1, false) : "-1");
		String query;
		if (!nextEpisode.equals("-1"))
			query = "SELECT imdbId, episodeName FROM episodes WHERE id = '"+ nextEpisode +"' AND serieId='"+ serieId +"'";
		else
			query = "SELECT imdbId, serieName FROM series WHERE id = '" + serieId + "'";
		Cursor c = db.Query(query);
		c.moveToFirst();
		if (c != null && c.isFirst()) {
			String imdbId = c.getString(0);
	    if (!nextEpisode.equals("-1") && imdbId.equals(serieIMDbId(serieId)))	// Sometimes the given episode's IMDb id is that of the show's
	    	imdbId = "-1";	// So we want to search for the episode instead of go to the show's page 
	    String name = c.getString(1);
			c.close();
			String uri = "imdb:///";
			Intent testForApp = new Intent(Intent.ACTION_VIEW, Uri.parse("imdb:///find"));
	    if (getApplicationContext().getPackageManager().resolveActivity(testForApp, 0) == null)
	    	uri = "http://m.imdb.com/";
			if (imdbId.indexOf("tt") == 0)
				uri += "title/"+ imdbId;
			else
				uri += "find?q="+ (!nextEpisode.equals("-1") ? serieName.replaceAll(" \\(....\\)", "") +" " : "") + name;
			Intent imdb = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
			startActivity(imdb);
		}
	}
	
	private String serieIMDbId(String serieId) {
		String imdbId = "";
		Cursor c = db.Query("SELECT imdbId, serieName FROM series WHERE id = '" + serieId + "'");
		c.moveToFirst();
		if (c != null && c.isFirst()) {
			imdbId = c.getString(0);
			c.close();
		}
		return imdbId;
	}

	private void updateSerie(final String serieId, int position) {
		oldListPosition = position;
		final String serieName = seriesAdapter.getItem(position).getName();
		if (utils.isNetworkAvailable(DroidShows.this)) {
			Runnable updateserierun = new Runnable() {
				public void run() {
					theTVDB = new TheTVDB("8AC675886350B3C3");
					if (theTVDB.getMirror() != null) {
						Serie sToUpdate = theTVDB.getSerie(serieId, langCode);
						toastMessage = getString(R.string.messages_title_updating_db) + " - " + sToUpdate.getSerieName();
						runOnUiThread(changeMessage);
						db.updateSerie(sToUpdate, lastSeasonOption == UPDATE_LAST_SEASON_ONLY);
						updatePosterThumb(serieId, sToUpdate);
					} else {
						Looper.prepare();
						Toast.makeText(getApplicationContext(), "Could not connect to TheTVDb", Toast.LENGTH_LONG).show();
						Looper.loop();
						return;
					}
					m_ProgressDialog.dismiss();
					Looper.prepare();
					Toast.makeText(getApplicationContext(), serieName +" "+ getString(R.string.menu_context_updated), Toast.LENGTH_SHORT).show();
					listView.post(updateShowView(serieId));
					Looper.loop();
					theTVDB = null;
				}
			};
			m_ProgressDialog = ProgressDialog.show(DroidShows.this, serieName, getString(R.string.messages_update_serie), true);
			updateShowTh = new Thread(updateserierun);
			updateShowTh.start();
		} else {
			Toast.makeText(getApplicationContext(), R.string.messages_no_internet, Toast.LENGTH_LONG).show();
		}
	}
	
	public void updatePosterThumb(String serieId, Serie sToUpdate) {
		Cursor c = DroidShows.db.Query("SELECT posterInCache, poster, posterThumb FROM series WHERE id='"+ serieId +"'");
		c.moveToFirst();
		if (c != null && c.isFirst()) {
			String posterInCache = c.getString(0);
			String poster = c.getString(1);
			String posterThumbPath = c.getString(2);
			URL posterURL = null;
			if (!posterInCache.equals("true") || !(new File(posterThumbPath).exists())) {
				poster = sToUpdate.getPoster();
				try {
					posterURL = new URL(poster);
					new File(posterThumbPath).delete();
					posterThumbPath = getApplicationContext().getFilesDir().getAbsolutePath() +"/thumbs"+ posterURL.getFile().toString();
				} catch (MalformedURLException e) {
					Log.e(SQLiteStore.TAG, "Show "+ serieId +" doesn't have poster URL");
					e.printStackTrace();
					return;
				}
				File posterThumbFile = new File(posterThumbPath);
				try {
					FileUtils.copyURLToFile(posterURL, posterThumbFile);
				} catch (IOException e) {
					Log.e(SQLiteStore.TAG, "Could not download poster: "+ posterURL);
					e.printStackTrace();
					return;
				}
				Bitmap posterThumb = BitmapFactory.decodeFile(posterThumbPath);
				if (posterThumb == null) {
					Log.e(SQLiteStore.TAG, "Corrupt or unknown poster file type:"+ posterThumbPath);
					return;
				}
				int width = getWindowManager().getDefaultDisplay().getWidth();
				int height = getWindowManager().getDefaultDisplay().getHeight();
				int newHeight = (int) ((height > width ? height : width) * 0.265);
				int newWidth = (int) (1.0 * posterThumb.getWidth() / posterThumb.getHeight() * newHeight);
				Bitmap resizedBitmap = Bitmap.createScaledBitmap(posterThumb, newWidth, newHeight, true);
				OutputStream fOut = null;
				try {
					fOut = new FileOutputStream(posterThumbFile, false);
					resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fOut);
					fOut.flush();
					fOut.close();
					db.execQuery("UPDATE series SET posterInCache='true', poster='"+ poster
						+"', posterThumb='"+ posterThumbPath +"' WHERE id='"+ serieId +"'");
					Log.d(SQLiteStore.TAG, "Updated poster thumb for "+ sToUpdate.getSerieName());
				} catch (FileNotFoundException e) {
					Log.e(SQLiteStore.TAG, "File not found:"+ posterThumbFile);
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				posterThumb.recycle();
				resizedBitmap.recycle();
				System.gc();
				posterThumb = null;
				resizedBitmap = null;
			}
		}
		c.close();
	}

	private Runnable changeMessage = new Runnable() {
		public void run() {
			m_ProgressDialog.setMessage(toastMessage);
		}
	};
	
	public void clearFilter(View v) {
		keyboard.hideSoftInputFromWindow(searchV.getWindowToken(), 0);
		searchV.setText("");
		findViewById(R.id.search).setVisibility(View.GONE);		
	}

	private void updateAllSeries() {
		if (!utils.isNetworkAvailable(DroidShows.this)) {
			Toast.makeText(getApplicationContext(), R.string.messages_no_internet, Toast.LENGTH_LONG).show();
		} else {
			final Runnable updateMessage = new Runnable() {
				public void run() {
					updateAllSeriesPD.setMessage(toastMessage);
				}
			};
			final Runnable updateallseries = new Runnable() {
				public void run() {
					theTVDB = new TheTVDB("8AC675886350B3C3");
					for (int i = 0; i < series.size(); i++) {
						Log.d(SQLiteStore.TAG, "Getting updated info from TheTVDB for TV show " + series.get(i).getName() +" ["+ i +"/"+ (series.size()-1) +"]");
						toastMessage = series.get(i).getName() + "\u2026";
						runOnUiThread(updateMessage);
						Serie sToUpdate = theTVDB.getSerie(series.get(i).getSerieId(), langCode);
						if (sToUpdate != null) {
							Log.d(SQLiteStore.TAG, "Updating the database");
							try {
								db.updateSerie(sToUpdate, lastSeasonOption == UPDATE_LAST_SEASON_ONLY);
								updatePosterThumb(series.get(i).getSerieId(), sToUpdate);
							} catch (Exception e) {
								e.printStackTrace();
							}
							updateAllSeriesPD.incrementProgressBy(1);
						} else {
							Log.e(SQLiteStore.TAG, "Skipped this show (no data received)");
						}
					}
					if (showArchive == 2)	// If coming from restore
						showArchive = 0;
					getSeries();
					updateAllSeriesPD.dismiss();
					theTVDB = null;
				}
			};
			updateAllSeriesPD = new ProgressDialog(this);
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
			alertDialog.setTitle(R.string.messages_title_update_series);
			String updateMessageAD = getString(R.string.dialog_update_series) + (lastSeasonOption == UPDATE_ALL_SEASONS ? getString(R.string.dialog_update_speedup) : "");
			alertDialog.setMessage(updateMessageAD);
			alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
			alertDialog.setCancelable(false);
			alertDialog.setPositiveButton(getString(R.string.dialog_OK), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					updateAllSeriesPD.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					updateAllSeriesPD.setTitle(R.string.messages_title_updating_series);
					updateAllSeriesPD.setMessage(getString(R.string.messages_update_series));
					updateAllSeriesPD.setCancelable(false);
					updateAllSeriesPD.setMax(series.size());
					updateAllSeriesPD.setProgress(0);
					updateAllSeriesPD.show();
					updateAllShowsTh = new Thread(updateallseries);
					updateAllShowsTh.start();
					return;
				}
			});
			alertDialog.setNegativeButton(getString(R.string.dialog_Cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			});
			alertDialog.show();
		}
	}
	
	private static TVShowItem createTVShowItem(String serieId) {
		String name = "", tmpPoster = "", showStatus = "", tmpNextEpisode = "", nextEpisode = "", tmpNextAir = "";
		int tmpStatus = 0, seasonCount = 0, unwatched = 0, unwatchedAired = 0;
		Date nextAir = null;
		String query = "SELECT serieName, posterThumb, status, passiveStatus, seasonCount, unwatchedAired, unwatched, nextEpisode, nextAir FROM series WHERE id = '" + serieId + "'";
		Cursor c = db.Query(query);
		c.moveToFirst();
		if (c != null && c.isFirst()) {
			name = c.getString(c.getColumnIndex("serieName"));
			tmpPoster = c.getString(c.getColumnIndex("posterThumb"));
			showStatus = c.getString(c.getColumnIndex("status"));
			tmpStatus = c.getInt(c.getColumnIndex("passiveStatus"));
			seasonCount = c.getInt(c.getColumnIndex("seasonCount"));
			unwatchedAired = c.getInt(c.getColumnIndex("unwatchedAired"));
			unwatched = c.getInt(c.getColumnIndex("unwatched"));
			tmpNextEpisode = c.getString(c.getColumnIndex("nextEpisode"));
			tmpNextAir = c.getString(c.getColumnIndex("nextAir"));
		}
		c.close();
		if (!tmpNextEpisode.equals("-1"))
			nextEpisode = tmpNextEpisode.replace("[on]", on);
		if (!tmpNextAir.isEmpty()) {
			try {
				nextAir = dateFormat.parse(tmpNextAir);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		boolean status = (tmpStatus == 1);
		TVShowItem tvsi = new TVShowItem(serieId, tmpPoster, null, name, seasonCount, nextEpisode, nextAir, unwatchedAired, unwatched, status, showStatus);
		return tvsi;
	}

	private void getSeries() {
		if (series != null) series.clear();
		try {
			List<String> serieIds;
			serieIds = db.getSeries(showArchive);
			for (int i = 0; i < serieIds.size(); i++) {
				String serieId = serieIds.get(i);
				TVShowItem tvsi = createTVShowItem(serieId);
				series.add(tvsi);
			}
			listView.post(updateListView);
		} catch (Exception e) {
			Log.e(SQLiteStore.TAG, "Error populating TVShowItems or no shows added yet");
			e.printStackTrace();
		}
	}
	
	public static Runnable updateListView = new Runnable() {
		public void run() {
			seriesAdapter.notifyDataSetChanged();
			if (series != null && series.size() > 0) {
				if (seriesAdapter.isFiltered) {
					for (int i = 0; i < seriesAdapter.getCount(); i++) {
						String adapterSerie = seriesAdapter.getItem(i).getSerieId();
						for (TVShowItem serie : series)
							if (serie.getSerieId().equals(adapterSerie))
								seriesAdapter.setItem(i, serie);
					}
				} else {
					for (int i = 0; i < series.size(); i++) {
						if (series.get(i).equals(seriesAdapter.getItem(i)))
							seriesAdapter.setItem(i, series.get(i));
						else
							seriesAdapter.add(series.get(i));
					}
				}
			}
			
			Comparator<TVShowItem> comperator = new Comparator<TVShowItem>() {
				public int compare(TVShowItem object1, TVShowItem object2) {
					if (sortOption == SORT_BY_LAST_UNSEEN) {
						int unwatchedAired1 = object1.getUnwatchedAired();
						int unwatchedAired2 = object2.getUnwatchedAired();
						if (unwatchedAired1 == unwatchedAired2) {
							Date nextAir1 = object1.getNextAir();
							Date nextAir2 = object2.getNextAir();
							if (nextAir1 == null && nextAir2 == null)
								return object1.getName().compareToIgnoreCase(object2.getName());
							if (nextAir1 == null)
								return 1;
							if (nextAir2 == null)
								return -1;
							return nextAir1.compareTo(nextAir2);
						}
						if (unwatchedAired1 == 0)
							return 1;
						if (unwatchedAired2 == 0)
							return -1;
						return ((Integer) unwatchedAired2).compareTo(unwatchedAired1);
					} else {
						return object1.getName().compareToIgnoreCase(object2.getName());
					}
				}
			};
			
			seriesAdapter.sort(comperator);
			seriesAdapter.notifyDataSetChanged();
		}
	};
	
	@Override
	public void onPause() {
		super.onPause();
		SharedPreferences.Editor ed = sharedPrefs.edit();
		ed.putInt(SORT_PREF_NAME, sortOption);
		ed.putInt(LAST_SEASON_PREF_NAME, lastSeasonOption);
		ed.putBoolean(INCLUDE_SPECIALS_NAME, includeSpecialsOption);
		ed.putBoolean(FULL_LINE_CHECK_NAME, fullLineCheckOption);
		ed.putBoolean(SWITCH_SWIPE_DIRECTION, switchSwipeDirection);
		ed.putString(LAST_STATS_UPDATE_NAME, lastStatsUpdate);
		ed.putString(LANGUAGE_CODE_NAME, langCode);
		ed.commit();
	}
	
	@Override
	public void onRestart() {
		super.onRestart();
		listView.post(updateShowView(backFromSeasonSerieId));
		backFromSeasonSerieId = null;
		if (searchV.length() > 0) {
			findViewById(R.id.search).setVisibility(View.VISIBLE);
			listView.requestFocus();
		}
	}
	
	@Override
	public void onResume() {
		super.onResume();
		asyncInfo = new AsyncInfo();
		asyncInfo.execute();
	}
	
	private class AsyncInfo extends AsyncTask<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			String newAsync = dateFormat.format(new Date());
			if (!lastStatsUpdate.equals(newAsync)) {
				Iterator<TVShowItem> it = series.iterator();
				while(it.hasNext()) {
					if (isCancelled()) return null;
					TVShowItem serie = it.next();
					String serieId = serie.getSerieId();
					int unwatched = db.getEPUnwatched(serieId);
					int unwatchedAired = db.getEPUnwatchedAired(serieId);
					if (unwatched != serie.getUnwatched() || unwatchedAired != serie.getUnwatchedAired()) {
						if (isCancelled()) return null;
						serie.setUnwatched(unwatched);
						serie.setUnwatchedAired(unwatchedAired);
						listView.post(updateListView);
						if (isCancelled()) return null;
						db.execQuery("UPDATE series SET unwatched="+ unwatched +", unwatchedAired="+ unwatchedAired +" WHERE id="+ serieId);
					}
				}
				lastStatsUpdate = newAsync;
//				Log.d(SQLiteStore.TAG, "Updated show stats on "+ newAsync);
			}
			return null;
		}
	}

	@Override
	public boolean onSearchRequested() {
		findViewById(R.id.search).setVisibility(View.VISIBLE);
		searchV.requestFocus();
		searchV.selectAll();
		keyboard.toggleSoftInput(0, InputMethodManager.HIDE_IMPLICIT_ONLY);
		return true;
	}

	@Override
	public void onBackPressed() {
		if (findViewById(R.id.search).getVisibility() == View.VISIBLE)
			clearFilter(null);
		else if (showArchive == 1)
			toggleArchive();
		else
			super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (m_ProgressDialog != null)
			m_ProgressDialog.dismiss();
		super.onSaveInstanceState(outState);
	}
		
	public String translateStatus(String statusValue) {
		if (statusValue.equalsIgnoreCase("Continuing")) {
			return getString(R.string.showstatus_continuing);
		} else if (statusValue.equalsIgnoreCase("Ended")) {
			return getString(R.string.showstatus_ended);
		} else {
			return statusValue;
		}
	}

	public class SeriesAdapter extends ArrayAdapter<TVShowItem> {
		private List<TVShowItem> items;
		private ShowsFilter filter;
		public boolean isFiltered = false;

		public SeriesAdapter(Context context, int textViewResourceId, List<TVShowItem> series) {
			super(context, textViewResourceId, series);
			items = series;
		}
		
		@Override
		public int getCount() {
			return items.size();
		}
		
		@Override
		public Filter getFilter() {
			if (filter == null)
				filter = new ShowsFilter();
			return filter;
		}
		
		@Override
		public TVShowItem getItem(int position) {
			return items.get(position);
		}
		
		public void setItem(int location, TVShowItem serie) {
			items.set(location, serie);
			notifyDataSetChanged();
		}
		
		private class ShowsFilter extends Filter {
			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				if (constraint == null || constraint.length() == 0) {
					results.count = series.size();
					results.values = series;
					isFiltered = false;
				} else {
					constraint = constraint.toString().toLowerCase();
					ArrayList<TVShowItem> filteredSeries = new ArrayList<TVShowItem>();
					for (TVShowItem serie : series) {
						if (serie.getName().toLowerCase().contains(constraint))
							filteredSeries.add(serie);
					}
					results.count = filteredSeries.size();
					results.values = filteredSeries;
					isFiltered = true;
				}
				return results;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				items = (List<TVShowItem>) results.values;
				notifyDataSetChanged();
			}
		}

		public View getView(final int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			if (convertView == null) {
				LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = vi.inflate(R.layout.row, parent, false);
				holder = new ViewHolder();
				holder.sn = (TextView) convertView.findViewById(R.id.seriename);
				holder.si = (TextView) convertView.findViewById(R.id.serieinfo);
				holder.sne = (TextView) convertView.findViewById(R.id.serienextepisode);
				holder.icon = (IconView) convertView.findViewById(R.id.serieicon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
				holder.icon.setOnClickListener(null);
			}
			TVShowItem serie = items.get(position);
			int nunwatched = serie.getUnwatched();
			int nunwatchedAired = serie.getUnwatchedAired();
			String ended = (serie.getShowStatus().equalsIgnoreCase("Ended") ? " \u2020" : "");
			holder.sn.setText(serie.getName() + ended);
			if (holder.si != null) {
				String siText = "";
				int sNumber = serie.getSNumber();
				if (sNumber == 1) {
					siText = sNumber +" "+ getString(R.string.messages_season);
				} else {
					siText = sNumber +" "+ getString(R.string.messages_seasons);
				}
				String unwatched = "";
				if (nunwatched == 0) {
					unwatched = getString(R.string.messages_no_new_eps);
					if (!serie.getShowStatus().equalsIgnoreCase("null"))
						unwatched += " ("+ translateStatus(serie.getShowStatus()) +")";
					holder.si.setEnabled(false);
				} else {
					unwatched = nunwatched +" "+ (nunwatched > 1 ? getString(R.string.messages_new_episodes) : getString(R.string.messages_new_episode)) +" ";
					if (nunwatchedAired > 0) {
						unwatched = (nunwatchedAired == nunwatched ? "" : nunwatchedAired +" "+ getString(R.string.messages_of) +" ") + unwatched + getString(R.string.messages_ep_aired) + (nunwatchedAired == nunwatched && ended.isEmpty() ? " \u00b7" : "");
						holder.si.setEnabled(true);
					} else {
						unwatched += getString(R.string.messages_to_be_aired);
						holder.si.setEnabled(false);
					}
				}
				holder.si.setText(siText +" | "+ unwatched);
			}
			if (holder.sne != null) {
				if (nunwatched > 0) {
					holder.sne.setText(getString(R.string.messages_next_episode) +" "+ serie.getNextEpisode());
					holder.sne.setVisibility(View.VISIBLE);
					if (nunwatchedAired > 0) {
						holder.sne.setEnabled(true);
					} else {
						holder.sne.setEnabled(false);
					}
				} else {
					holder.sne.setText("");
				}
			}
			if (holder.icon != null) {
				Drawable icon = serie.getDIcon();
				if (icon == null)
					if (!serie.getIcon().equals(""))
						icon = Drawable.createFromPath(serie.getIcon());
				if (icon == null) {
					holder.icon.setImageResource(R.drawable.noposter);
				} else {
					holder.icon.setImageDrawable(icon);
					serie.setDIcon(icon);
				}
				holder.icon.setOnClickListener(detailsListener);
				holder.icon.setOnLongClickListener(episodeListener);
			}
			return convertView;
		}
	}
	static class ViewHolder
	{
		TextView sn;
		TextView si;
		TextView sne;
		IconView icon;
	}
	private OnClickListener detailsListener = new OnClickListener() {
		public void onClick(View v) {
	        final int position = listView.getPositionForView(v);
	        if (position != ListView.INVALID_POSITION) {
	        	keyboard.hideSoftInputFromWindow(searchV.getWindowToken(), 0);
				showDetails(seriesAdapter.getItem(position).getSerieId());
			}
		}
	};
	private OnLongClickListener episodeListener = new OnLongClickListener() {
		public boolean onLongClick(View v) {
			final int position = listView.getPositionForView(v);
			if (position != ListView.INVALID_POSITION) {
				String episodeId = db.getNextEpisodeId(seriesAdapter.getItem(position).getSerieId(), -1, false);
				if (!episodeId.equals("-1")) {
					keyboard.hideSoftInputFromWindow(searchV.getWindowToken(), 0);
					Intent viewEpisode = new Intent(DroidShows.this, ViewEpisode.class);
					viewEpisode.putExtra("serieId", seriesAdapter.getItem(position).getSerieId());
					viewEpisode.putExtra("serieName", seriesAdapter.getItem(position).getName());
					viewEpisode.putExtra("episodeId", episodeId);
					startActivity(viewEpisode);
				}
			}
			return true;
		}
	};
}