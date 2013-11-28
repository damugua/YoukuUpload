package com.example.videosend;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import android.os.Handler;
import android.os.Message;

public class Request {
	public void onSuccess(String resposeBody){}
    public void onFailure(String exceptionMsg){}
    String strResult ="";
    public void execute(final String uriAPI){
    	new Thread(){
    		public void run(){
    			 try {
//    	            URL url = new URL(path);
//    	            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
//    	            conn.setConnectTimeout(3*1000);
//    	            conn.setReadTimeout(2000);
//    	            int responseCode =  conn.getResponseCode();
//    	            onResponse(responseCode);
//    	        	String uriAPI = "";
    	        	HttpPost httpRequest = new HttpPost(uriAPI);
    	        	List<NameValuePair> params = new ArrayList<NameValuePair>();
    	        	params.add(new BasicNameValuePair("client_id","dcafd5c299d3a448"));
    	        	params.add(new BasicNameValuePair("client_secret","b685e0fde716293b138635fdffbada0e"));
    	        	params.add(new BasicNameValuePair("grant_type","bjust0501"));
    	        	params.add(new BasicNameValuePair("username","bianjixing@uvicsoft.com"));
    	        	params.add(new BasicNameValuePair("password","bjust0501"));
    	        	httpRequest.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
    	        	HttpResponse httpResponse = new DefaultHttpClient().execute(httpRequest);
    	        	if(httpResponse.getStatusLine().getStatusCode() == 200){
    		        	strResult = EntityUtils.toString(httpResponse.getEntity());
    	        	}
//    	        	onResponse(httpResponse.getStatusLine().getStatusCode());
    	        	mHandler.sendEmptyMessage(httpResponse.getStatusLine().getStatusCode());
    	        } catch (MalformedURLException ex) {
    	            onFailure(ex.getMessage()); 
    	        } catch (IOException ex) {
    	           onFailure(ex.getMessage()); 
    	        } 
    		}
    	}.start();
       
    }
    
    public void onResponse(int code){ 
        if(code == 200){
            onSuccess(strResult);
        }else{
            onFailure("发送请求失败！请求代码："+code);
        }
    }
    public Handler mHandler = new  Handler(){
    	public void handleMessage(Message msg) {
    		switch(msg.what){
	    		case 200:
	    			onSuccess(strResult);
	    			break;
	    		default:
	    			onFailure("发送请求失败！请求代码："+msg.what);
	    			break;
    		}
    	};
    };
}
