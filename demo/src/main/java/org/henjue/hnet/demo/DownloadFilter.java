package org.henjue.hnet.demo;

import android.util.Log;

import org.henjue.library.hnet.RequestFacade;
import org.henjue.library.hnet.RequestFilter;

/**
 * Created by android on 15-7-1.
 */
public class DownloadFilter implements RequestFilter {
    @Override
    public void onComplite(RequestFacade request) {
        Log.d("DownloadFilter","onComplite");
    }

    @Override
    public void onStart(RequestFacade request) {
        Log.d("DownloadFilter","onStart");
    }

    @Override
    public void onAdd(String name, Object value) {
        Log.d("DownloadFilter",String.format("onAdd,name:%s,value:%s",name,value.toString()));
    }
}
