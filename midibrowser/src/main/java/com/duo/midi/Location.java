//package com.duo.midi;
//
//import android.app.Application;
//
////import com.baidu.location.LocationClient;
////import com.baidu.location.LocationClientOption;
//
//public class Location extends Application {
//	public LocationClient mLocationClient;
//	@Override
//	public void onCreate() {
//		init();
//		super.onCreate();
//
//	}
//	public void init(){
//		mLocationClient = new LocationClient(this.getApplicationContext()); // ����LocationClient��
//		setLocationOptions();
//	}
//	private void setLocationOptions() {
//		LocationClientOption option = new LocationClientOption();
//		option.setOpenGps(true);
//		option.setAddrType("all");
//		option.setCoorType("bd09ll");
//		option.setScanSpan(10000);
//		option.setPriority( LocationClientOption.NetWorkFirst);
//		this.mLocationClient.setLocOption(option);
//	}
//}
