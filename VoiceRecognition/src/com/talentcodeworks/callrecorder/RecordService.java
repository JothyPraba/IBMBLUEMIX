package com.talentcodeworks.callrecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.Exception;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Date;
import java.util.Scanner;
import java.text.SimpleDateFormat;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.widget.Toast;
import android.util.Log;

//import java.security.KeyPairGenerator;
//import java.security.KeyPair;
//import java.security.Key;

import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

import com.example.voicerecognition.FLACRecorder;
import com.example.voicerecognition.R;
import com.example.voicerecognition.Recorder;

public class RecordService 
    extends Service
    implements MediaRecorder.OnInfoListener, MediaRecorder.OnErrorListener
{
    private static final String TAG = "CallRecorder";

    public static final String DEFAULT_STORAGE_LOCATION = "/sdcard/callrecorder";
    private static final int RECORDING_NOTIFICATION_ID = 1;
    String fileName = Environment.getExternalStorageDirectory()	+ "/recording.flac";
    private static final long MIN = 10000000;
	private static final long MAX = 900000009999999L;
	long PAIR;
	String api_key = "AIzaSyBgnC5fljMTmCFeilkgLsOKBvvnx6CBS0M";

	String language = "en_us";
    private MediaRecorder recorder = null;
    private boolean isRecording = false;
    private File recording = null;;
	private Recorder mRecorder;
	int sampleRate;
	String root = "https://www.google.com/speech-api/full-duplex/v1/";
	String dwn = "down?maxresults=1&pair=";
	String API_DOWN_URL = root + dwn;
	String up_p1 = "up?lang=" + language
			+ "&lm=dictation&client=chromium&pair=";
	String up_p2 = "&key=";
    /*
    private static void test() throws java.security.NoSuchAlgorithmException
    {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.genKeyPair();
        Key publicKey = kp.getPublic();
        Key privateKey = kp.getPrivate();
    }
    */

    private File makeOutputFile (SharedPreferences prefs)
    {
        File dir = new File(DEFAULT_STORAGE_LOCATION);

        // test dir for existence and writeability
        if (!dir.exists()) {
            try {
                dir.mkdirs();
            } catch (Exception e) {
                Log.e("CallRecorder", "RecordService::makeOutputFile unable to create directory " + dir + ": " + e);
                Toast t = Toast.makeText(getApplicationContext(), "CallRecorder was unable to create the directory " + dir + " to store recordings: " + e, Toast.LENGTH_LONG);
                t.show();
                return null;
            }
        } else {
            if (!dir.canWrite()) {
                Log.e(TAG, "RecordService::makeOutputFile does not have write permission for directory: " + dir);
                Toast t = Toast.makeText(getApplicationContext(), "CallRecorder does not have write permission for the directory directory " + dir + " to store recordings", Toast.LENGTH_LONG);
                t.show();
                return null;
            }
        }

        // test size

        // create filename based on call data
        String prefix = "call";
        //SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd_HH:MM:SS");
        //prefix = sdf.format(new Date()) + "-callrecording";

        // add info to file name about what audio channel we were recording
        int audiosource = Integer.parseInt(prefs.getString(Preferences.PREF_AUDIO_SOURCE, "1"));
        prefix += "-channel" + audiosource + "-";

        // create suffix based on format
        String suffix = "";
        int audioformat = Integer.parseInt(prefs.getString(Preferences.PREF_AUDIO_FORMAT, "1"));
        switch (audioformat) {
        case MediaRecorder.OutputFormat.THREE_GPP:
            suffix = ".3gpp";
            break;
        case MediaRecorder.OutputFormat.MPEG_4:
            suffix = ".mpg";
            break;
        case MediaRecorder.OutputFormat.RAW_AMR:
            suffix = ".amr";
            break;
        }

        try {
            return File.createTempFile(prefix, suffix, dir);
        } catch (IOException e) {
            Log.e("CallRecorder", "RecordService::makeOutputFile unable to create temp file in " + dir + ": " + e);
            Toast t = Toast.makeText(getApplicationContext(), "CallRecorder was unable to create temp file in " + dir + ": " + e, Toast.LENGTH_LONG);
            t.show();
            return null;
        }
    }

    public void onCreate()
    {
        super.onCreate();
        mRecorder = new Recorder(this, mRecordingHandler);
      //  recorder = new MediaRecorder();
        Log.i("CallRecorder", "onCreate created MediaRecorder object");
    }

    public void onStart(Intent intent, int startId) {
       System.out.println("Start Recording....");
   	mRecorder.start(fileName);



	//stopButton.setEnabled(true);
	Toast.makeText(getApplicationContext(), "Recording...",
			Toast.LENGTH_LONG).show();

       
       
       
       
        return; //return 0; //return START_STICKY;
    }
 // Handler used for sending request to Google API
 	Handler handler = new Handler();

 	// Recording callbacks
 	private Handler mRecordingHandler = new Handler(new Handler.Callback() {
 		public boolean handleMessage(Message m) {
 			switch (m.what) {
 			case FLACRecorder.MSG_AMPLITUDES:
 				FLACRecorder.Amplitudes amp = (FLACRecorder.Amplitudes) m.obj;

 				break;

 			case FLACRecorder.MSG_OK:
 				// Ignore
 				break;

 			case Recorder.MSG_END_OF_RECORDING:

 				break;

 			default:
 				mRecorder.stop();
// 				mErrorCode = m.what;
// 				showDialog(DIALOG_RECORDING_ERROR);
 				break;
 			}

 			return true;
 		}
 	});
 	Handler messageHandler = new Handler() {

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1: // GET DOWNSTREAM json id="@+id/comment"
				String mtxt = msg.getData().getString("text");
				if (mtxt.length() > 20) {
					final String f_msg = mtxt;
					handler.post(new Runnable() { // This thread runs in the UI
						// TREATMENT FOR GOOGLE RESPONSE
						@Override
						public void run() {
							System.out.println(f_msg);
							//txtView.setText(f_msg);
						}
					});
				}
				break;
			case 2:
				break;
			}
		}
		
	}; // doDOWNSTRM Handler end
	// UPSTREAM channel. its servicing a thread and should have its own handler
	Handler messageHandler2 = new Handler() {

		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case 1: // GET DOWNSTREAM json
				Log.d("ParseStarter", msg.getData().getString("post"));
				break;
			case 2:
				Log.d("ParseStarter", msg.getData().getString("post"));
				break;
			}

		}
	}; // UPstream handler end


    public void onDestroy()
    {
        super.onDestroy();

        if (null != mRecorder) {
            Log.i("CallRecorder", "RecordService::onDestroy calling recorder.release()");
            isRecording = false;

    		Toast.makeText(getApplicationContext(), "Loading...", Toast.LENGTH_LONG)
    				.show();
    	

    		sampleRate = mRecorder.mFLACRecorder.getSampleRate();
    		getTranscription(sampleRate);
            mRecorder.stop();
            Toast t = Toast.makeText(getApplicationContext(), "CallRecorder finished recording call to " + recording, Toast.LENGTH_LONG);
            t.show();

            /*
            // encrypt the recording
            String keyfile = "/sdcard/keyring";
            try {
                //PGPPublicKey k = readPublicKey(new FileInputStream(keyfile));
                test();
            } catch (java.security.NoSuchAlgorithmException e) {
                Log.e("CallRecorder", "RecordService::onDestroy crypto test failed: ", e);
            }
            //encrypt(recording);
            */
        }

        updateNotification(false);
    }
	public void getTranscription(int sampleRate) {

		File myfil = new File(fileName);
		if (!myfil.canRead())
			Log.d("ParseStarter", "FATAL no read access");

		// first is a GET for the speech-api DOWNSTREAM
		// then a future exec for the UPSTREAM / chunked encoding used so as not
		// to limit
		// the POST body sz

		PAIR = MIN + (long) (Math.random() * ((MAX - MIN) + 1L));
		// DOWN URL just like in curl full-duplex example plus the handler
		downChannel(API_DOWN_URL + PAIR, messageHandler);

		// UP chan, process the audio byteStream for interface to UrlConnection
		// using 'chunked-encoding'
		FileInputStream fis;
		try {
			fis = new FileInputStream(myfil);
			FileChannel fc = fis.getChannel(); // Get the file's size and then
												// map it into memory
			int sz = (int) fc.size();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, sz);
			byte[] data2 = new byte[bb.remaining()];
			Log.d("ParseStarter", "mapfil " + sz + " " + bb.remaining());
			bb.get(data2);
			// conform to the interface from the curl examples on full-duplex
			// calls
			// see curl examples full-duplex for more on 'PAIR'. Just a globally
			// uniq value typ=long->String.
			// API KEY value is part of value in UP_URL_p2
			upChannel(root + up_p1 + PAIR + up_p2 + api_key, messageHandler2,
					data2);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

    // methods to handle binding the service

    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public boolean onUnbind(Intent intent)
    {
        return false;
    }

    public void onRebind(Intent intent)
    {
    }


    private void updateNotification(Boolean status)
    {
        Context c = getApplicationContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);

        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

        if (status) {
            int icon = R.drawable.rec;
            CharSequence tickerText = "Recording call from channel " + prefs.getString(Preferences.PREF_AUDIO_SOURCE, "1");
            long when = System.currentTimeMillis();
            
            Notification notification = new Notification(icon, tickerText, when);
            
            Context context = getApplicationContext();
            CharSequence contentTitle = "CallRecorder Status";
            CharSequence contentText = "Recording call from channel...";
            Intent notificationIntent = new Intent(this, RecordService.class);
            PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
            
            notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
            mNotificationManager.notify(RECORDING_NOTIFICATION_ID, notification);
        } else {
            mNotificationManager.cancel(RECORDING_NOTIFICATION_ID);
        }
    }

    // MediaRecorder.OnInfoListener
    public void onInfo(MediaRecorder mr, int what, int extra)
    {
        Log.i("CallRecorder", "RecordService got MediaRecorder onInfo callback with what: " + what + " extra: " + extra);
        isRecording = false;
    }

    // MediaRecorder.OnErrorListener
    public void onError(MediaRecorder mr, int what, int extra) 
    {
        Log.e("CallRecorder", "RecordService got MediaRecorder onError callback with what: " + what + " extra: " + extra);
        isRecording = false;
        mr.release();
    }
    
    
    private void downChannel(String urlStr, final Handler messageHandler) {

		final String url = urlStr;

		new Thread() {
			Bundle b;

			public void run() {
				String response = "NAO FOI";
				Message msg = Message.obtain();
				msg.what = 1;
				// handler for DOWN channel http response stream - httpsUrlConn
				// response handler should manage the connection.... ??
				// assign a TIMEOUT Value that exceeds by a safe factor
				// the amount of time that it will take to write the bytes
				// to the UPChannel in a fashion that mimics a liveStream
				// of the audio at the applicable Bitrate. BR=sampleRate * bits
				// per sample
				// Note that the TLS session uses
				// "* SSLv3, TLS alert, Client hello (1): "
				// to wake up the listener when there are additional bytes.
				// The mechanics of the TLS session should be transparent. Just
				// use
				// httpsUrlConn and allow it enough time to do its work.
				Scanner inStream = openHttpsConnection(url);
				// process the stream and store it in StringBuilder
				while (inStream.hasNextLine()) {
					b = new Bundle();
					b.putString("text", inStream.nextLine());
					msg.setData(b);
					messageHandler.dispatchMessage(msg);
				}

			}
		}.start();
	}

	private void upChannel(String urlStr, final Handler messageHandler,
			byte[] arg3) {

		final String murl = urlStr;
		final byte[] mdata = arg3;
		Log.d("ParseStarter", "upChan " + mdata.length);
		new Thread() {
			public void run() {
				String response = "NAO FOI";
				Message msg = Message.obtain();
				msg.what = 2;
				Scanner inStream = openHttpsPostConnection(murl, mdata);
				inStream.hasNext();
				// process the stream and store it in StringBuilder
				while (inStream.hasNextLine()) {
					response += (inStream.nextLine());
					Log.d("ParseStarter", "POST resp " + response.length());
				}
				Bundle b = new Bundle();
				b.putString("post", response);
				msg.setData(b);
				// in.close(); // mind the resources
				messageHandler.sendMessage(msg);

			}
		}.start();

	}
	// GET for DOWNSTREAM
		private Scanner openHttpsConnection(String urlStr) {
			InputStream in = null;
			int resCode = -1;
			Log.d("ParseStarter", "dwnURL " + urlStr);

			try {
				URL url = new URL(urlStr);
				URLConnection urlConn = url.openConnection();

				if (!(urlConn instanceof HttpsURLConnection)) {
					throw new IOException("URL is not an Https URL");
				}

				HttpsURLConnection httpConn = (HttpsURLConnection) urlConn;
				httpConn.setAllowUserInteraction(false);
				// TIMEOUT is required
				httpConn.setInstanceFollowRedirects(true);
				httpConn.setRequestMethod("GET");

				httpConn.connect();

				resCode = httpConn.getResponseCode();
				if (resCode == HttpsURLConnection.HTTP_OK) {
					return new Scanner(httpConn.getInputStream());
				}

			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		// GET for UPSTREAM
		private Scanner openHttpsPostConnection(String urlStr, byte[] data) {
			InputStream in = null;
			byte[] mextrad = data;
			int resCode = -1;
			OutputStream out = null;
			// int http_status;
			try {
				URL url = new URL(urlStr);
				URLConnection urlConn = url.openConnection();

				if (!(urlConn instanceof HttpsURLConnection)) {
					throw new IOException("URL is not an Https URL");
				}

				HttpsURLConnection httpConn = (HttpsURLConnection) urlConn;
				httpConn.setAllowUserInteraction(false);
				httpConn.setInstanceFollowRedirects(true);
				httpConn.setRequestMethod("POST");
				httpConn.setDoOutput(true);
				httpConn.setChunkedStreamingMode(0);
				httpConn.setRequestProperty("Content-Type", "audio/x-flac; rate="
						+ sampleRate);
				httpConn.connect();

				try {
					// this opens a connection, then sends POST & headers.
					out = httpConn.getOutputStream();
					// Note : if the audio is more than 15 seconds
					// dont write it to UrlConnInputStream all in one block as this
					// sample does.
					// Rather, segment the byteArray and on intermittently, sleeping
					// thread
					// supply bytes to the urlConn Stream at a rate that approaches
					// the bitrate ( =30K per sec. in this instance ).
					Log.d("ParseStarter", "IO beg on data");
					out.write(mextrad); // one big block supplied instantly to the
										// underlying chunker wont work for duration
										// > 15 s.
					Log.d("ParseStarter", "IO fin on data");
					// do you need the trailer?
					// NOW you can look at the status.
					resCode = httpConn.getResponseCode();

					Log.d("ParseStarter", "POST OK resp "
							+ httpConn.getResponseMessage().getBytes().toString());

					if (resCode / 100 != 2) {
						Log.d("ParseStarter", "POST bad io ");
					}

				} catch (IOException e) {
					Log.d("ParseStarter", "FATAL " + e);

				}

				if (resCode == HttpsURLConnection.HTTP_OK) {
					Log.d("ParseStarter", "OK RESP to POST return scanner ");
					return new Scanner(httpConn.getInputStream());
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
}
