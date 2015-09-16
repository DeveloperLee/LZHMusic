package com.lzh.service;

import java.util.ArrayList;
import java.util.List;

import com.lzh.activity.MainActivity;
import com.lzh.activity.MusicActivity;
import com.lzh.constants.PlayStyleConstant;
import com.lzh.constants.StateConstant;
import com.lzh.lzhmusic.R;
import com.lzh.model.LyricsItem;
import com.lzh.model.MediaFile;
import com.lzh.model.User;
import com.lzh.util.MusicIntent;
import com.lzh.util.MusicUtils;
import com.lzh.util.RandomGenerator;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.animation.AnimationUtils;
import android.widget.RemoteViews;

public class MusicService extends Service {
	
	
	private static MediaPlayer player;
	private static boolean isPaused = true;      //�жϵ�ǰ�����Ƿ�����ͣ״̬
	private static boolean continue_play = false;  //�ж��Ƿ�Ҫ����ͣ����������
	private static boolean isPrepared = false;
	private static String path;
	private static int current_position = -1;
	private static int play_mode = PlayStyleConstant.NORMAL_SEQUENCE;   //Ĭ��Ϊ˳�򲥷�
	private static int currentTime;
	private static int duration;
	
	public static List<MediaFile> mediaFiles;       //��ǰ���ݼ���
	public static List<MediaFile> downloadedFiles;
	public static List<MediaFile> allFiles;
	public static User current_user;
	
	private static ServiceBroadCastReceiver receiver;
	
	private LrcProcess mLrcProcess;	//��ʴ���
	private List<LyricsItem> lrcList = new ArrayList<LyricsItem>(); //��Ÿ���б����
	private int index = 0;			//��ʼ���ֵ
	
	private static final String LRC_PATH = Environment.getExternalStorageDirectory()+"/LzhMusic/Lyrics/";
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		player = new MediaPlayer();
		setUpMusicListener();
		downloadedFiles = MusicUtils.getAllDownloadMusics(MusicUtils.queryAllMusic(getContentResolver()));
		allFiles = MusicUtils.getMediaFileList(MusicUtils.queryAllMusic(getContentResolver())); 
		mediaFiles = allFiles;
		registReceiver();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(player != null){
			player.stop();
			player.release();
			player = null;
		}
		isPrepared = false;
		unregisterReceiver(receiver);
	}
   
	
	/**
	 * ��OnCreate�Ѿ����ù��Ժ�Service������ʱ�����ٵ���OnCreate����
	 * ����ֱ�ӵ���OnStart
	 * MediaPlayer ���ڣ� reset() --> setDataSource(String) --> prepare() --> start()
	 */
	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		int mode = intent.getIntExtra("mode", 0);
		if(intent.getAction().equals(StateConstant.PLAY)){
			    if(!continue_play){ //��������ֹͣ���ٵ�����Ż����if���
			      if(mode!=1){
				    initLrc();
			      }
				 reprepareService();
				 player.start();
				}else{     //����������ͣ���ٵ�����Ż����else��䣬ֱ��start
				 player.start();
				}
				isPaused = false;  
				continue_play = true;
				setUpNotificationController(isPaused,"���ڲ��ţ�",
						MusicService.getMusicName(), MusicService.getArtistName());
		}else if(intent.getAction().equals(StateConstant.PAUSE)){
			player.pause();
			continue_play = true;
			isPaused = true;
			setUpNotificationController(isPaused,"��ͣ����:",
					MusicService.getMusicName(), MusicService.getArtistName());
		}else if(intent.getAction().equals(StateConstant.STOP)){  //�û������ֹͣ���Ժ����˴�
			player.stop();
			continue_play = false;
			isPaused = true;
		}else if(intent.getAction().equals(StateConstant.PREPARE)){ 
			//�˴������κδ���ֻ����Service
		}else if(intent.getAction().equals("stop_service")){
			stopSelf();
		}
	}
	
	/**
	 * �û��ֶ���קseekbar�󴥷����¼� ע�� ����int �����0 Ҫ��ת��Ϊfloat
	 * @param progress
	 */
	public static void setCurrentProgress(int progress){
		   int seek_position;
	       if(!continue_play){    
	       reprepareService();
	       seek_position = (int) (progress*1.0 / 1000.0 * player.getDuration()); 
		   player.start();
		   player.seekTo(seek_position);
	       }else{
	    	seek_position = (int) (progress*1.0 / 1000.0 * player.getDuration());
			player.seekTo(seek_position);
	       }
	       continue_play = true;
	       isPaused = false;
	}
	
	@Override
	public boolean onUnbind(Intent intent) {
		player.stop();
		return super.onUnbind(intent);
	}


	
	public static int getCurrentPosition(){
		
		return continue_play ? player.getCurrentPosition() : 0;
		
	}
	public static void setPath(String p){
		path = p;
	}
	
	public static int getCurrSongDuration(){
		Log.i("tag", String.valueOf(player.getDuration()));
		return path ==null ? -1:player.getDuration();
	}
	
	private static void reprepareService(){
		resetStatuse();
		player.reset();
		try {
			player.setDataSource(mediaFiles.get(current_position).getPath());
			player.prepare();
			isPrepared = true;
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	/**
	 * ����Service����״̬
	 */
	public static void resetStatuse() {
            isPaused = true;
            continue_play = false;
            isPrepared = false;
	}
	
	public static void setPosition(int position){
		current_position = position;
	}
	/**
	 * ��õ�ǰService�в��Ÿ�����·����û���򷵻�""
	 * @return
	 */
	public static String getCurrentPath(){
		return path;
	}
    
	private void prepareSongWithPath(String path){
		resetStatuse();
		player.reset();
		try {
			player.setDataSource(path);
			player.prepare();
			isPrepared = true;
			this.setUpNotificationController(false,"���ڲ��ţ�",
					MusicService.getMusicName(), MusicService.getArtistName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	//֮���Դ�����һ�׸���߼�Ҫ����Service���У�����Ϊ�������Դ�����Activity��Ϊ��̨����ʱ
	//��̨���ŵ�����
	private void nextSong(){
		 if(mediaFiles != null){
		 if(play_mode == PlayStyleConstant.NORMAL_SEQUENCE){
		 current_position++;       //�����̨������ɣ���ôpostion++,ģ�����ȱ�֤��Խ��
		 current_position = current_position % mediaFiles.size();
		 }else if (play_mode == PlayStyleConstant.SHUFFELL_ALL){
			 current_position = RandomGenerator.generateNextShuffelPos(current_position, mediaFiles.size());
		 }
		 path = mediaFiles.get(current_position).getPath();
		 prepareSongWithPath(path);
		 player.start();
		 Intent intent = new Intent(StateConstant.AUTO_NEXT_SONG);    
		 intent.putExtra("position", current_position);
		 sendBroadcast(intent);                                                //֪ͨActivity����UI
		 }
	}
	//���������Ƿ񲥷���ɣ�������͹㲥��MusicActivity��HomeActivity
	private void setUpMusicListener(){
		player.setOnCompletionListener(new OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				 nextSong();
				 Intent intent = new Intent(StateConstant.SEND_POSITION_FROM_SERVICE);
				 intent.putExtra("position",current_position);
				 sendBroadcast(intent);
				 Bundle b = new Bundle();
				 Intent to_homeIntent = new Intent(StateConstant.SEND_MUSIC_INFO_FROM_SERVICE);
				 b.putString("music_name",  mediaFiles.get(current_position).getMusic_name());
				 b.putString("artist_name", mediaFiles.get(current_position).getArtist_name());
				 b.putBoolean("isDownload", mediaFiles.get(current_position).isDownload());
				 to_homeIntent.putExtras(b);
				 sendBroadcast(to_homeIntent);
			}
		});
	}
	
	//�Ӳ����б��ȡ���Ĺ㲥��Ϣ������˵ڼ���������Ŀ��
	public class ServiceBroadCastReceiver extends BroadcastReceiver{

		@Override
		public void onReceive(Context context, Intent intent) {
			       if(intent.getAction().equals(StateConstant.SEND_POSTION_FROM_ACTIVITY)){
			          current_position = intent.getIntExtra("position", 0);    //��ȡ���ڲ����б��е�λ��
			       }else if(intent.getAction().equals(StateConstant.SEND_MODE_FROM_ACTIVITY)){
			    	   play_mode = intent.getIntExtra("play_mode", 0);
			       }else if(intent.getAction().equals(StateConstant.SEND_LIST_POSITION)){
			    	   current_position = intent.getIntExtra("position", 0);
			       }else if(intent.getAction().equals(StateConstant.send_POSITION_FROM_ARTIST)){
			    	   int id = intent.getIntExtra("song_id", 0);
			    	   for(int i =0;i<mediaFiles.size();i++){
			    		     if(id == mediaFiles.get(i).getMusic_id()){
			    		    	 current_position = i;
			    		    	 break;
			    		     }
			    	   }
			       }else if(intent.getAction().equals(StateConstant.CHANGE_SONG_SET)){
			    	   current_position = intent.getIntExtra("position", 0);
			    	   path = mediaFiles.get(current_position).getPath();
			       }
		}
	}
	
	private void registReceiver(){
		receiver = new ServiceBroadCastReceiver();
		IntentFilter filter = new IntentFilter();
		filter.addAction(StateConstant.SEND_LIST_POSITION);
		filter.addAction(StateConstant.SEND_POSTION_FROM_ACTIVITY);
		filter.addAction(StateConstant.SEND_MODE_FROM_ACTIVITY);
		filter.addAction(StateConstant.send_POSITION_FROM_ARTIST);
		registerReceiver(receiver, filter);
	}
	
	//ʵ�ڲ�֪����ʲô����ʵ�֣�������д���Ժ���������ٸ�
	public static int getPosition(){
		return current_position;
		 
	}
	
	//ֱ�ӷ����Ƿ��и����ڲ���
	public static boolean isPaused(){
		return  isPaused;
	}
	
	public static boolean isPlaying(){
		return player.isPlaying();
	}
	
	public static boolean isPrepared(){
		return isPrepared;
	}
	
	//���ز���ģʽ
	public static int getPlayMode(){
		return play_mode;
	}
	
	public static String getMusicName(){
		if(current_position != -1){
			return mediaFiles.get(current_position).getMusic_name();
		}else{
			return "";
		}
	}
	
	public static String getArtistName(){
		if(current_position != -1){
			return mediaFiles.get(current_position).getArtist_name();
		}else{
			return "";
		}
	}
	
	
	/**
	 * ��ʼ���������
	 */
	public void initLrc(){
		mLrcProcess = new LrcProcess();
		//��ȡ����ļ�
		mLrcProcess.readLRC(LRC_PATH+mediaFiles.get(current_position).getMusic_name()+".lrc");
		//���ش����ĸ���ļ�
		lrcList = mLrcProcess.getLrcList();
		MusicActivity.lrcView.setmLrcList(lrcList);
		//�л���������ʾ���
		MusicActivity.lrcView.setAnimation(AnimationUtils.loadAnimation(MusicService.this,com.lzh.lzhmusic.R.anim.alpha_z));
		handler.post(mRunnable);
	}
	Runnable mRunnable = new Runnable() {
		
		@Override
		public void run() {
			MusicActivity.lrcView.setIndex(lrcIndex());
			MusicActivity.lrcView.invalidate();
			handler.postDelayed(mRunnable, 100);
		}
	};

	/**
	 * ����ʱ���ȡ�����ʾ������ֵ
	 * @return
	 */
	public int lrcIndex() {
		if(player.isPlaying()) {
			currentTime = player.getCurrentPosition();
			duration = player.getDuration();
		}
		if(currentTime < duration) {
			for (int i = 0; i < lrcList.size(); i++) {
				if (i < lrcList.size() - 1) {
					if (currentTime < lrcList.get(i).getTime() && i == 0) {
						index = i;
					}
					if (currentTime > lrcList.get(i).getTime()
							&& currentTime < lrcList.get(i + 1).getTime()) {
						index = i;
					}
				}
				if (i == lrcList.size() - 1
						&& currentTime > lrcList.get(i).getTime()) {
					index = i;
				}
			}
		}
		return index;
	}
	
	/*
	 * ���ֲ���ʱ��֪ͨ������ʾ��Ϣ
	 */
	public void setUpNotificationController(boolean isPaused,String status,String music_name,String artist_name){
		long when = System.currentTimeMillis();
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		String contentTitle = status+music_name;
		Intent notificationIntent = new Intent(this, MainActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent,
				PendingIntent.FLAG_UPDATE_CURRENT);
        RemoteViews rv = new RemoteViews(getPackageName(),R.layout.notification_remote_view);
        MusicIntent mi = new MusicIntent(this);
        rv.setTextViewText(R.id.notify_music_name, music_name);
		rv.setTextViewText(R.id.notify_artist_name,artist_name);
		if(isPaused){
			rv.setImageViewResource(R.id.notify_play_status, R.drawable.playing_play_normal);
			mi.setAction(StateConstant.PLAY);
		}else{
			rv.setImageViewResource(R.id.notify_play_status, R.drawable.playing_pause_normal);
			mi.setAction(StateConstant.PAUSE);
		}
		PendingIntent pService = PendingIntent.getService(this, 0, mi, 0);
		rv.setOnClickPendingIntent(R.id.notify_play_status, pService);
		Notification notification = new Notification(R.drawable.ic_launcher, contentTitle, when);
		notification.contentView = rv;
		notification.contentIntent = contentIntent;
		notification.flags = Notification.FLAG_FOREGROUND_SERVICE;  //��̨Service��������
		mNotificationManager.notify(1, notification);
	}

	/**
	 * handler����������Ϣ�������͹㲥���²���ʱ��
	 */
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			if (msg.what == 1) {
				if(player != null) {
					currentTime = player.getCurrentPosition(); // ��ȡ��ǰ���ֲ��ŵ�λ��
					handler.sendEmptyMessageDelayed(1, 1000);
				}
			}
		};
	};
	
	
}
