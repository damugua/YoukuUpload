package com.example.videosend;

public class Ranking {
	private  String path;
    
    public Ranking(String path){
        this.path = path;
    } 
    
    public  void list(final ICallBack call) { 
        
        Request request = new Request() {
            @Override
            public void onSuccess(String resposeBody) {
                if (null != call) {
                    call.onSuccess(resposeBody);
                } else {
                    onFailure("未初始化回调函数");
                }
            }

            @Override
            public void onFailure(String exceptionMsg) {
                if (null != call) {
                    call.onFailure(exceptionMsg);
                }
            }
        };

        request.execute(path);
    }
}
