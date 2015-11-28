package nl.asymmetrics.droidshows.utils;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import nl.asymmetrics.droidshows.DroidShows;
import nl.asymmetrics.droidshows.thetvdb.model.Episode;
import nl.asymmetrics.droidshows.thetvdb.model.Serie;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.text.TextUtils;
import android.util.Log;

public class SQLiteStore extends SQLiteOpenHelper
{
	public static final String TAG = "DroidShows";
	private static SQLiteStore instance = null;
	private static String DB_PATH = "";
	private static String DB_NAME = "DroidShows.db";
	private SQLiteDatabase db;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

	public static SQLiteStore getInstance(Context context) {
		if (instance == null)
			instance = new SQLiteStore(context.getApplicationContext());
		return instance;
	}
	
	private SQLiteStore(Context context) {
		super(context, DB_NAME, null, 1);
		DB_PATH = context.getApplicationInfo().dataDir +"/databases/";
		try {
			openDataBase();
		} catch (SQLException sqle) {
			try {
				createDataBase();
				close();
				try {
					openDataBase();
				} catch (SQLException sqle2) {
					Log.e(TAG, sqle2.getMessage());
				}
			} catch (IOException e) {
				Log.e(TAG, "Unable to create database");
			}
		}
	}

	public void createDataBase() throws IOException {
		boolean dbExist = checkDataBase();
		if (!dbExist) {
			this.getWritableDatabase();
		}
	}

	private boolean checkDataBase() {
		SQLiteDatabase checkDB = null;
		try {
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		} catch (SQLiteException e) {
			Log.d(TAG, "Database does't exist yet.");
		}
		if (checkDB != null) {
			checkDB.close();
		}
		return checkDB != null ? true : false;
	}

	public void openDataBase() throws SQLException {
		// Open the database
		String myPath = DB_PATH + DB_NAME;
		db = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READWRITE);
	}

	/* Insert Methods */
	public boolean execQuery(String query) {
		try {
			db.execSQL(query);
		} catch (SQLiteException e) {
			return false;
		}
		return true;
	}

	public Cursor Query(String query) {
		Cursor c = null;
		try {
			c = db.rawQuery(query, null);
		} catch (SQLiteException e) {
			return null;
		}
		return c;
	}

	/* Get Methods */
	public List<Episode> getEpisodes(String serieId) {
		List<Episode> episodes = null;
		episodes = new ArrayList<Episode>();
		Cursor c = Query("SELECT * FROM episodes WHERE serieId = '"+ serieId +"'");
		try {
			if (c != null) {
				int idCol = c.getColumnIndex("id");
				int combinedEpisodeNumberCol = c.getColumnIndex("combinedEpisodeNumber");
				int combinedSeasonCol = c.getColumnIndex("combinedSeason");
				int dvdChapterCol = c.getColumnIndex("dvdChapter");
				int dvdDiscIdCol = c.getColumnIndex("dvdDiscId");
				int dvdEpisodeNumberCol = c.getColumnIndex("dvdEpisodeNumber");
				int dvdSeasonCol = c.getColumnIndex("dvdSeason");
				int epImgFlagCol = c.getColumnIndex("epImgFlag");
				int episodeNameCol = c.getColumnIndex("episodeName");
				int episodeNumberCol = c.getColumnIndex("episodeNumber");
				int firstAiredCol = c.getColumnIndex("firstAired");
				int imdbIdCol = c.getColumnIndex("imdbId");
				int languageCol = c.getColumnIndex("language");
				int overviewCol = c.getColumnIndex("overview");
				int productionCodeCol = c.getColumnIndex("productionCode");
				int ratingCol = c.getColumnIndex("rating");
				int seasonNumberCol = c.getColumnIndex("seasonNumber");
				int absoluteNumberCol = c.getColumnIndex("absoluteNumber");
				int filenameCol = c.getColumnIndex("filename");
				int lastUpdatedCol = c.getColumnIndex("lastUpdated");
				int seriesIdCol = c.getColumnIndex("serieId");
				int seasonIdCol = c.getColumnIndex("seasonId");
				int seenCol = c.getColumnIndex("seen");
				c.moveToFirst();
				if (c.isFirst()) {
					do {
						Episode eTmp = new Episode();
						List<String> directors = new ArrayList<String>();
						Cursor cdirectors = Query("SELECT director FROM directors WHERE serieId='"+ serieId
							+"' AND episodeId='"+ c.getString(idCol) +"'");
						cdirectors.moveToFirst();
						int directorCol = cdirectors.getColumnIndex("director");
						if (cdirectors != null && cdirectors.isFirst()) {
							do {
								directors.add(cdirectors.getString(directorCol));
							} while (cdirectors.moveToNext());
						}
						cdirectors.close();
						List<String> guestStars = new ArrayList<String>();
						Cursor cguestStars = Query("SELECT guestStar FROM guestStars WHERE serieId='"+ serieId
							+"' AND episodeId='"+ c.getString(idCol) +"'");
						cguestStars.moveToFirst();
						int guestStarCol = cguestStars.getColumnIndex("guestStar");
						if (cguestStars != null && cguestStars.isFirst()) {
							do {
								guestStars.add(cguestStars.getString(guestStarCol));
							} while (cguestStars.moveToNext());
						}
						cguestStars.close();
						List<String> writers = new ArrayList<String>();
						Cursor cwriters = Query("SELECT writer FROM writers WHERE serieId='"+ serieId
							+"' AND episodeId='"+ c.getString(idCol) +"'");
						cwriters.moveToFirst();
						int writersCol = cwriters.getColumnIndex("writer");
						if (cwriters != null && cwriters.isFirst()) {
							do {
								writers.add(cwriters.getString(writersCol));
							} while (cwriters.moveToNext());
						}
						cwriters.close();
						eTmp.setDirectors(directors);
						eTmp.setGuestStars(guestStars);
						eTmp.setWriters(writers);
						eTmp.setId(c.getString(idCol));
						eTmp.setCombinedEpisodeNumber(c.getString(combinedEpisodeNumberCol));
						eTmp.setCombinedSeason(c.getString(combinedSeasonCol));
						eTmp.setDvdChapter(c.getString(dvdChapterCol));
						eTmp.setDvdDiscId(c.getString(dvdDiscIdCol));
						eTmp.setDvdEpisodeNumber(c.getString(dvdEpisodeNumberCol));
						eTmp.setDvdSeason(c.getString(dvdSeasonCol));
						eTmp.setEpImgFlag(c.getString(epImgFlagCol));
						eTmp.setEpisodeName(c.getString(episodeNameCol));
						eTmp.setEpisodeNumber(c.getInt(episodeNumberCol));
						eTmp.setFirstAired(c.getString(firstAiredCol));
						eTmp.setImdbId(c.getString(imdbIdCol));
						eTmp.setLanguage(c.getString(languageCol));
						eTmp.setOverview(c.getString(overviewCol));
						eTmp.setProductionCode(c.getString(productionCodeCol));
						eTmp.setRating(c.getString(ratingCol));
						eTmp.setSeasonNumber(c.getInt(seasonNumberCol));
						eTmp.setAbsoluteNumber(c.getString(absoluteNumberCol));
						eTmp.setFilename(c.getString(filenameCol));
						eTmp.setLastUpdated(c.getString(lastUpdatedCol));
						eTmp.setSeriesId(c.getString(seriesIdCol));
						eTmp.setSeasonId(c.getString(seasonIdCol));
						if (c.getInt(seenCol) == 0) {
							eTmp.setSeen(false);
						} else {
							eTmp.setSeen(true);
						}
						episodes.add(eTmp);
					} while (c.moveToNext());
				}
			}
			c.close();
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
			c.close();
		}
		return episodes;
	}

	public List<String> getSeries(int showArchive) {
		List<String> series = new ArrayList<String>();
		String showArchiveString = "";
		if (showArchive < 2)
			showArchiveString = " WHERE passiveStatus="
					+(showArchive == 0 ? "0 OR passiveStatus IS NULL" : showArchive +"");
		Cursor cseries = Query("SELECT id FROM series"+ showArchiveString);
		try {
			cseries.moveToFirst();
			if (cseries != null && cseries.isFirst()) {
				do {
					series.add(cseries.getString(0));
				} while (cseries.moveToNext());
			}
			cseries.close();
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
			cseries.close();
		}
		return series;
	}

	public List<String> getEpisodes(String serieId, int seasonNumber) {
		List<String> episodes = new ArrayList<String>();
		Cursor c = Query("SELECT id FROM episodes WHERE serieId='"+ serieId +"' AND seasonNumber="
			+ seasonNumber +" ORDER BY episodeNumber ASC");
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				do {
					episodes.add(c.getString(0));
				} while (c.moveToNext());
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return episodes;
	}

	private List<EpisodeSeen> getSeen(String serieId, int max_season) {
		List<EpisodeSeen> episodesSeen = new ArrayList<EpisodeSeen>();
		Cursor c = Query("SELECT seasonNumber, episodeNumber, seen FROM episodes WHERE serieId='"+ serieId +"'"
			+ (max_season != -1 ? " AND (seasonNumber="+ max_season +" OR seasonNumber=0)": "")
			+" AND seen>0");
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				do {
					episodesSeen.add(new EpisodeSeen(c.getInt(0) +"x"+ c.getInt(1), c.getInt(2)));
				} while (c.moveToNext());
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return episodesSeen;
	}

	public String getSerieName(String serieId) {
		String sname = "";
		Cursor c = Query("SELECT serieName FROM series WHERE id='"+ serieId +"'");
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				sname = c.getString(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
			return null;
		}
		return sname;
	}

	public int getEPUnwatchedAired(String serieId) {
		int unwatchedAired = 0;
		String today = dateFormat.format(new Date());	// Get today's date
		Cursor c = Query("SELECT count(id) FROM episodes WHERE serieId='"+ serieId
			+"' AND seen=0 AND firstAired < '"+ today +"' AND firstAired <> ''"
			+ (DroidShows.includeSpecialsOption ? "" : " AND seasonNumber <> 0"));
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				unwatchedAired = c.getInt(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return unwatchedAired;
	}

	public int getSeasonEPUnwatchedAired(String serieId, int snumber) {
		int unwatched = -1;
		String today = dateFormat.format(new Date());	// Get today's date
		Cursor c = Query("SELECT count(id) FROM episodes WHERE serieId='"+ serieId +"' AND seasonNumber="+ snumber
				+" AND seen=0 AND firstAired < '"+ today +"' AND firstAired <> ''");
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				unwatched = c.getInt(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return unwatched;
	}

	public int getEPUnwatched(String serieId) {
		int unwatched = -1;
		Cursor c = Query("SELECT count(id) FROM episodes WHERE serieId='"+ serieId
			+"' AND seen=0 "+ (DroidShows.includeSpecialsOption ? "" : "AND seasonNumber <> 0"));
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				unwatched = c.getInt(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return unwatched;
	}

	public int getSeasonEPUnwatched(String serieId, int snumber) {
		int unwatched = 0;
		Cursor c = Query("SELECT count(id) FROM episodes WHERE serieId='"+ serieId
			+"' AND seasonNumber="+ snumber +" AND seen=0");
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				unwatched = c.getInt(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return unwatched;
	}

	public Date getNextAir(String serieId, int snumber) {
		Date na = null;
		Cursor c = null;
		try {
			if (snumber == -1) {
				c = Query("SELECT firstAired FROM episodes WHERE serieId='"+ serieId +"' AND seen=0"
						+ (DroidShows.includeSpecialsOption ? "" : " AND seasonNumber <> 0")
						+" ORDER BY seasonNumber, episodeNumber ASC LIMIT 1");
			} else {
				c = Query("SELECT firstAired FROM episodes WHERE serieId='"+ serieId +"' AND seen=0"
						+" AND seasonNumber="+ snumber 
						+" ORDER BY episodeNumber ASC LIMIT 1");
			}
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				String fa = c.getString(c.getColumnIndex("firstAired"));
				if (!fa.isEmpty() && !fa.equals("null")) {
					try {
						na = dateFormat.parse(fa);
					} catch (ParseException e) {
						Log.e(TAG, e.getMessage());
						return null;
					}
				}
			}
			c.close();
		} catch (SQLiteException e) {
			if (c != null) {
				c.close();
			}
			Log.e(TAG, e.getMessage());
			return null;
		}
		return na;
	}

	public String getNextEpisodeId(String serieId, int snumber, boolean noFutureEp) {
		int id = -1;
		Cursor c = null;
		try {
			String today = dateFormat.format(new Date());
			if (snumber == -1) {
				c = Query("SELECT id FROM episodes WHERE serieId='"+ serieId +"' AND seen=0"
						+ (DroidShows.includeSpecialsOption ? "" : " AND seasonNumber <> 0")
						+ (noFutureEp ? " AND firstAired < '"+ today +"' AND firstAired <> ''": "")
						+" ORDER BY seasonNumber, episodeNumber ASC LIMIT 1");
			} else {
				c = Query("SELECT id FROM episodes WHERE serieId='"+ serieId +"' AND seasonNumber="+ snumber
						+"AND seen=0 AND firstAired < '"+ today
						+"' AND firstAired <> '' ORDER BY episodeNumber ASC LIMIT 1");
			}
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				int index = c.getColumnIndex("id");
				id = c.getInt(index);
			}
			c.close();
		} catch (SQLiteException e) {
			if (c != null) {
				c.close();
			}
			Log.e(TAG, e.getMessage());
		}
		return ""+ id;
	}

	public String getNextEpisode(String serieId, int snumber) {
		String nextEpisode = "";
		Cursor c = null;
		try {
			if (snumber == -1) {
				c = Query("SELECT firstAired, episodeNumber, seasonNumber FROM episodes WHERE serieId='"+ serieId
					+"' AND seen=0"+ (DroidShows.includeSpecialsOption ? "" : " AND seasonNumber <> 0")
					+" ORDER BY seasonNumber, episodeNumber ASC LIMIT 1");
			} else {
				c = Query("SELECT firstAired, episodeNumber, seasonNumber FROM episodes WHERE serieId='"+ serieId
					+"' AND seasonNumber="+ snumber +" AND seen=0"
					+" ORDER BY episodeNumber ASC LIMIT 1");
			}
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				int faCol = c.getColumnIndex("firstAired");
				int enCol = c.getColumnIndex("episodeNumber");
				int snCol = c.getColumnIndex("seasonNumber");
				String epDataStr = "";
				String tmpEpDataStr = c.getString(faCol);
				if (!tmpEpDataStr.isEmpty() && !tmpEpDataStr.equals("null")) {
					try {
						Format formatter = SimpleDateFormat.getDateInstance();
						epDataStr += formatter.format(dateFormat.parse(c.getString(faCol)));
					} catch (ParseException e) {
						Log.e(TAG, e.getMessage());
					}
				}
				String enumber = "";
				if (c.getInt(enCol) < 10) {
					enumber = "0"+ c.getInt(enCol);
				} else {
					enumber = ""+ c.getInt(enCol);
				}
				if (!epDataStr.equals("")) {
					nextEpisode = c.getInt(snCol) +"x"+ enumber +" [on] "+ epDataStr;
				} else {
					nextEpisode = c.getInt(snCol) +"x"+ enumber;
				}
			}
			c.close();
		} catch (SQLiteException e) {
			if (c != null) {
				c.close();
			}
			Log.e(TAG, e.getMessage());
		}
		return nextEpisode;
	}

	public int getSeasonCount(String serieId) {
		int count = 0;
		Cursor c = Query("SELECT count(season) FROM serie_seasons WHERE serieId = '"+ serieId +"' AND season <> 0");
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				count = c.getInt(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return count;
	}

	public int getSeasonEpisodeCount(String serieId, int sNumber) {
		int count = -1;
		Cursor c = Query("SELECT count(id) FROM episodes WHERE serieId='"+ serieId +"' AND seasonNumber="+ sNumber);
		try {
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				count = c.getInt(0);
			}
			c.close();
		} catch (SQLiteException e) {
			c.close();
			Log.e(TAG, e.getMessage());
		}
		return count;
	}
	
	/* Update Methods */
	public void updateUnwatchedSeason(String serieId, int nseason) {
		try {
			String today = dateFormat.format(new Date());	// Get today's date
			Date date = new Date();
			int seen = 10000 * (1900 + date.getYear()) + 100 * (date.getMonth() + 1) + date.getDate();
			db.execSQL("UPDATE episodes SET seen="+ seen +" WHERE serieId='"+ serieId +"' AND seasonNumber="+ nseason
			+" AND firstAired < '"+ today +"' AND firstAired <> '' AND seen < 1");
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
		updateShowStats(serieId);
	}

	public void updateWatchedSeason(String serieId, int nseason) {
		try {
			db.execSQL("UPDATE episodes SET seen=0 WHERE serieId='"+ serieId +"' AND seasonNumber="
				+ nseason);
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
		updateShowStats(serieId);
	}

	public String updateUnwatchedEpisode(String serieId, String episodeId) {
		Cursor c = null;
		String episodeMarked = "";
		try {
			c = Query("SELECT seen, seasonNumber, episodeNumber FROM episodes WHERE serieId='"+ serieId+"' AND id='"+ episodeId +"'");
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				int seen = c.getInt(0);
				int season = c.getInt(1);
				int episode = c.getInt(2);
				episodeMarked =  season +"x"+ (episode < 10 ? "0" : "") + episode;
				c.close();
				if (seen > 0)
					seen = 0;
				else {
					Date date = new Date();
					seen = 10000 * (1900 + date.getYear()) + 100 * (date.getMonth() + 1) + date.getDate();
				}
				db.execSQL("UPDATE episodes SET seen="+ seen +" WHERE serieId='"+ serieId +"' AND id='"+ episodeId +"'");
			}
		} catch (SQLiteException e) {
			if (c != null) c.close();
			Log.e(TAG, e.getMessage());
		}
		updateShowStats(serieId);
		return episodeMarked;
	}

	public void updateSerieStatus(String serieId, int passiveStatus) {
		try {
			db.execSQL("UPDATE series SET passiveStatus="+ passiveStatus +" WHERE id='"+ serieId +"'");
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
	}

	public void updateSerie(Serie s, boolean last_season) {
		if (s == null) {
			Log.e(TAG, "Error: Serie is null");
			return;
		}
		try {
			String tmpSOverview = "";
			if (s.getOverview() != null) {
				if (!TextUtils.isEmpty(s.getOverview())) {
					tmpSOverview = s.getOverview();
				}
			}
			String tmpSName = "";
			if (!TextUtils.isEmpty(s.getSerieName())) {
				tmpSName = s.getSerieName();
			}
			Cursor cms = null;
			int max_season = -1;
			if (last_season) {
				try {
					cms = Query("SELECT season FROM serie_seasons WHERE serieID='"+ s.getId() +"'");
					cms.moveToFirst();
					if (cms != null && cms.isFirst()) {
						do {
							if (max_season < cms.getInt(0)) {
								max_season = cms.getInt(0);
							}
						} while (cms.moveToNext());
					}
					cms.close();
					Log.d(TAG, "Updating only last season "+ max_season +" and specials of "+ tmpSName);
				} catch (SQLiteException e) {
					if (cms != null) {
						cms.close();
					}
					Log.e(TAG, e.getMessage());
				}
			} else {
				Log.d(TAG, "Updating all seasons of "+ tmpSName);
			}
			// Log.d(TAG, "MAX SEASON: "+ max_season);
			db.beginTransaction();
			db.execSQL("UPDATE series SET language='"+ s.getLanguage() +"', serieName="+ DatabaseUtils.sqlEscapeString(tmpSName)
				+", overview="+ DatabaseUtils.sqlEscapeString(tmpSOverview) +", "+"firstAired='"+ s.getFirstAired()
				+"', imdbId='"+ s.getImdbId() +"', zap2ItId='"+ s.getZap2ItId() +"', "
				+"airsDayOfWeek='"+ s.getAirsDayOfWeek() +"', airsTime='"+ s.getAirsTime()
				+"', contentRating='"+ s.getContentRating() +"', "+"network='"+ s.getNetwork()
				+"db', rating='"+ s.getRating() +"', runtime='"+ s.getRuntime() +"', "+"status='"
				+ s.getStatus() +"', lastUpdated='"+ s.getLastUpdated() +"' WHERE id='"+ s.getId()
				+"'");
			
			// seasons
			db.execSQL("DELETE FROM serie_seasons WHERE serieId='"+ s.getId() +"'");
			for (int n = 0; n < s.getNSeasons().size(); n++) {
				execQuery("INSERT INTO serie_seasons (serieId, season) "+"VALUES ('"+ s.getId() +"', '"
					+ s.getNSeasons().get(n) +"');");
			}
			// actors
			db.execSQL("DELETE FROM actors WHERE serieId='"+ s.getId() +"'");
			for (int a = 0; a < s.getActors().size(); a++) {
				execQuery("INSERT INTO actors (serieId, actor) "+"VALUES ('"+ s.getId()
				+"',"+ DatabaseUtils.sqlEscapeString(s.getActors().get(a)) +");");
			}
			// genres
			db.execSQL("DELETE FROM genres WHERE serieId='"+ s.getId() +"'");
			for (int g = 0; g < s.getGenres().size(); g++) {
				execQuery("INSERT INTO genres (serieId, genre) "+"VALUES ('"+ s.getId()
				+"',"+ DatabaseUtils.sqlEscapeString(s.getGenres().get(g)) +");");
			}
			
			if (max_season != -1) {
				String episodes = "";
				try {
					cms = Query("SELECT id FROM episodes WHERE serieId='"+ s.getId() +"'"
					+" AND (seasonNumber="+ max_season +" OR seasonNumber=0)");
					cms.moveToFirst();
					if (cms != null && cms.isFirst()) {
						do {
							episodes += "'"+ cms.getString(0) +"', ";
						} while (cms.moveToNext());
					}
					cms.close();
					episodes = episodes.substring(0, episodes.length() - 2);
					// directors
					db.execSQL("DELETE FROM directors WHERE serieId='"+ s.getId() +"' AND episodeId IN ("+ episodes +")");
					// guestStars
					db.execSQL("DELETE FROM guestStars WHERE serieId='"+ s.getId() +"' AND episodeId IN ("+ episodes +")");
					// writers
					db.execSQL("DELETE FROM writers WHERE serieId='"+ s.getId() +"' AND episodeId IN ("+ episodes +")");
				} catch (SQLiteException e) {
					if (cms != null) {
						cms.close();
					}
					Log.e(TAG, e.getMessage());
				}
			} else {
				// directors
				db.execSQL("DELETE FROM directors WHERE serieId='"+ s.getId() +"'");
				// guestStars
				db.execSQL("DELETE FROM guestStars WHERE serieId='"+ s.getId() +"'");
				// writers
				db.execSQL("DELETE FROM writers WHERE serieId='"+ s.getId() +"'");
			}

			List<EpisodeSeen> seenEpisodes = getSeen(s.getId(), max_season);
			db.execSQL("DELETE FROM episodes WHERE serieId='"+ s.getId() +"'"
					+(max_season != -1 ? " AND (seasonNumber="+ max_season +" OR seasonNumber=0)" : ""));
			
			for (int e = 0; e < s.getEpisodes().size(); e++) {
				if (max_season != -1) {
					int season = s.getEpisodes().get(e).getSeasonNumber();
					if (season < max_season && season != 0) {	// include specials in update
						continue;
					}
				}
				// Guillaume: check for redundant rows for this episode and delete them
				for (int d = 0; d < s.getEpisodes().get(e).getDirectors().size(); d++) {
					execQuery("INSERT INTO directors (serieId, episodeId, director) "+"VALUES ('"
						+ s.getId() +"', '"+ s.getEpisodes().get(e).getId()
						+"',"+ DatabaseUtils.sqlEscapeString(s.getEpisodes().get(e).getDirectors().get(d)) +");");
				}
				for (int g = 0; g < s.getEpisodes().get(e).getGuestStars().size(); g++) {
					execQuery("INSERT INTO guestStars (serieId, episodeId, guestStar) "+"VALUES ('"
						+ s.getId() +"', '"+ s.getEpisodes().get(e).getId()
						+"',"+ DatabaseUtils.sqlEscapeString(s.getEpisodes().get(e).getGuestStars().get(g)) +");");
				}
				for (int w = 0; w < s.getEpisodes().get(e).getWriters().size(); w++) {
					execQuery("INSERT INTO writers (serieId, episodeId, writer) "+"VALUES ('"+ s.getId()
						+"', '"+ s.getEpisodes().get(e).getId()
						+"',"+ DatabaseUtils.sqlEscapeString(s.getEpisodes().get(e).getWriters().get(w)) +");");
				}
				String tmpOverview = "";
				if (s.getEpisodes().get(e).getOverview() != null) {
					if (!TextUtils.isEmpty(s.getEpisodes().get(e).getOverview())) {
						tmpOverview = s.getEpisodes().get(e).getOverview();
					}
				}
				String tmpName = "";
				if (s.getEpisodes().get(e).getEpisodeName() != null) {
					if (!TextUtils.isEmpty(s.getEpisodes().get(e).getEpisodeName())) {
						tmpName = s.getEpisodes().get(e).getEpisodeName();
					}
				}
				
				int iseen = 0;
				String epCode = s.getEpisodes().get(e).getSeasonNumber() +"x"+ s.getEpisodes().get(e).getEpisodeNumber();
				for (EpisodeSeen es : seenEpisodes) {
					if (epCode.equals(es.episode)) {
						iseen = es.seen;
						break;
					}
				}
								
				if (!tmpName.equals("")) {
					execQuery("INSERT INTO episodes (serieId, id, combinedEpisodeNumber, combinedSeason, "
						+"dvdChapter, dvdDiscId, dvdEpisodeNumber, dvdSeason, epImgFlag, episodeName, "
						+"episodeNumber, firstAired, imdbId, language, overview, productionCode, rating, seasonNumber, "
						+"absoluteNumber, filename, lastUpdated, seasonId, seen) VALUES ('"
						+ s.getId()
						+"', '"
						+ s.getEpisodes().get(e).getId()
						+"', '"
						+ s.getEpisodes().get(e).getCombinedEpisodeNumber()
						+"', '"
						+ s.getEpisodes().get(e).getCombinedSeason()
						+"', '"
						+ s.getEpisodes().get(e).getDvdChapter()
						+"', '"
						+ s.getEpisodes().get(e).getDvdDiscId()
						+"', '"
						+ s.getEpisodes().get(e).getEpisodeNumber()
						+"', '"
						+ s.getEpisodes().get(e).getDvdSeason()
						+"', '"
						+ s.getEpisodes().get(e).getEpImgFlag()
						+"',"
						+ DatabaseUtils.sqlEscapeString(tmpName)
						+", "
						+ s.getEpisodes().get(e).getEpisodeNumber()
						+", '"
						+ s.getEpisodes().get(e).getFirstAired()
						+"', '"
						+ s.getEpisodes().get(e).getImdbId()
						+"', '"
						+ s.getEpisodes().get(e).getLanguage()
						+"',"
						+ DatabaseUtils.sqlEscapeString(tmpOverview)
						+", '"
						+ s.getEpisodes().get(e).getProductionCode()
						+"', '"
						+ s.getEpisodes().get(e).getRating()
						+"', "
						+ s.getEpisodes().get(e).getSeasonNumber()
						+", '"
						+ s.getEpisodes().get(e).getAbsoluteNumber()
						+"', '"
						+ s.getEpisodes().get(e).getFilename()
						+"', '"
						+ s.getEpisodes().get(e).getLastUpdated()
						+"', '"
						+ s.getEpisodes().get(e).getSeasonId() +"', "+ iseen +");");
				}
			}
			db.setTransactionSuccessful();
			db.endTransaction();
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
		updateShowStats(s.getId());
	}

	/* Delete Methods */
	// DELETE FROM table_name WHERE some_column=some_value
	public void deleteSerie(String serieId) {
		try {
			db.execSQL("DELETE FROM directors WHERE serieId='"+ serieId +"'");
			db.execSQL("DELETE FROM guestStars WHERE serieId='"+ serieId +"'");
			db.execSQL("DELETE FROM writers WHERE serieId='"+ serieId +"'");
			db.execSQL("DELETE FROM episodes WHERE serieId='"+ serieId +"'");
			db.execSQL("DELETE FROM actors WHERE serieId='"+ serieId +"'");
			db.execSQL("DELETE FROM genres WHERE serieId='"+ serieId +"'");
			db.execSQL("DELETE FROM serie_seasons WHERE serieId='"+ serieId +"'");
			Cursor c = Query("SELECT posterThumb FROM series WHERE id='"+ serieId +"'");
			c.moveToFirst();
			if (c != null && c.isFirst()) {
				File thumbImage = new File(c.getString(0));
				thumbImage.delete();
			}
			c.close();
			db.execSQL("DELETE FROM series WHERE id='"+ serieId +"'");
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
	}
	
	public void deleteEpisode(String serieId, String episodeId) {
		try {
			db.execSQL("DELETE FROM directors WHERE serieId='"+ serieId +"' AND episodeId='"+ episodeId +"'");
			db.execSQL("DELETE FROM guestStars WHERE serieId='"+ serieId +"' AND episodeId='"+ episodeId +"'");
			db.execSQL("DELETE FROM writers WHERE serieId='"+ serieId +"' AND episodeId='"+ episodeId +"'");
			db.execSQL("DELETE FROM episodes WHERE serieId='"+ serieId +"' AND id='"+ episodeId +"'");			
		} catch (SQLiteException e) {
			Log.e(TAG, e.getMessage());
		}
		updateShowStats(serieId);
	}

	/* *********************************************************************************** */
	@Override
	public synchronized void close() {
		if (db != null) db.close();
		super.close();
	}

	@Override
	public void onCreate(SQLiteDatabase dbase) {
		// tabela dos directors
		dbase.execSQL("CREATE TABLE IF NOT EXISTS directors ("+"serieId VARCHAR, "
			+"episodeId VARCHAR, "+"director VARCHAR"+");");
		// tabela dos guestStars
		dbase.execSQL("CREATE TABLE IF NOT EXISTS guestStars ("+"serieId VARCHAR, "
			+"episodeId VARCHAR, "+"guestStar VARCHAR"+");");
		// tabela dos writers
		dbase.execSQL("CREATE TABLE IF NOT EXISTS writers ("+"serieId VARCHAR, "
			+"episodeId VARCHAR, "+"writer VARCHAR"+");");
		// tabela dos episodios
		dbase.execSQL("CREATE TABLE IF NOT EXISTS episodes ("+"serieId VARCHAR, "+"id VARCHAR, "
			+"combinedEpisodeNumber VARCHAR, "+"combinedSeason VARCHAR, "+"dvdChapter VARCHAR, "
			+"dvdDiscId VARCHAR, "+"dvdEpisodeNumber VARCHAR, "+"dvdSeason VARCHAR, "
			+"epImgFlag VARCHAR, "+"episodeName VARCHAR, "+"episodeNumber INT, "
			+"firstAired VARCHAR, "+"imdbId VARCHAR, "+"language VARCHAR, "+"overview TEXT, "
			+"productionCode VARCHAR, "+"rating VARCHAR, "+"seasonNumber INT, "
			+"absoluteNumber VARCHAR, "+"filename VARCHAR,"+"lastUpdated VARCHAR, "
			+"seasonId VARCHAR, "+"seen INT"+");");
		// tabela dos actores
		dbase.execSQL("CREATE TABLE IF NOT EXISTS actors ("+"serieId VARCHAR, "+"actor VARCHAR"
			+");");
		// tabela dos genres
		dbase.execSQL("CREATE TABLE IF NOT EXISTS genres ("+"serieId VARCHAR, "+"genre VARCHAR"
			+");");
		// tabela das seasons
		dbase.execSQL("CREATE TABLE IF NOT EXISTS serie_seasons ("+"serieId VARCHAR, "
			+"season VARCHAR"+");");
		// create tables
		dbase.execSQL("CREATE TABLE IF NOT EXISTS series ("+"id VARCHAR PRIMARY KEY, "
			+"serieId VARCHAR, "+"language VARCHAR, "+"serieName VARCHAR, "+"banner VARCHAR, "
			+"overview TEXT, "+"firstAired VARCHAR, "+"imdbId VARCHAR, "+"zap2ItId VARCHAR, "
			+"airsDayOfWeek VARCHAR, "+"airsTime VARCHAR, "+"contentRating VARCHAR, "
			+"network VARCHAR, "+"rating VARCHAR, "+"runtime VARCHAR, "+"status VARCHAR, "
			+"fanart VARCHAR, "+"lastUpdated VARCHAR, "+"passiveStatus INTEGER DEFAULT 0, "+"poster VARCHAR,"
			+"posterInCache VARCHAR, "+"posterThumb VARCHAR, "
			+"seasonCount INTEGER, "+"unwatchedAired INTEGER, "+"unwatched INTEGER, "+"nextEpisode VARCHAR, "+"nextAir VARCHAR" 
			+");");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub
	}
	
	public void updateShowStats() {
		List<String> series = getSeries(2);	// 2 = archive and current shows
		for (int i = 0; i < series.size(); i += 1) {
			updateShowStats(series.get(i));
		}
	}
	
	public void updateShowStats(String serieId) {
		int seasonCount = getSeasonCount(serieId);
		int unwatchedAired = getEPUnwatchedAired(serieId);
		int unwatched = getEPUnwatched(serieId);
		String nextEpisode = getNextEpisode(serieId, -1);
		String nextAir = "";
		Date tmpNextAir = getNextAir(serieId, -1);
		if (tmpNextAir != null) {
			nextAir = dateFormat.format(tmpNextAir);
		}
		execQuery("UPDATE series SET seasonCount="+ seasonCount +", unwatchedAired="+ unwatchedAired +", unwatched="+ unwatched +", nextEpisode='"+ nextEpisode +"', nextAir='"+ nextAir +"' WHERE id="+ serieId);
	}
	
	private class EpisodeSeen {
		public String episode;
		public int seen;
		
		public EpisodeSeen(String episodeValue, int seenValue) {
			episode = episodeValue;
			seen = seenValue;
		}
	}
}