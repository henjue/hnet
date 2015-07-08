package org.henjue.hnet.demo;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import org.henjue.library.hnet.RequestIntercept;
import org.henjue.library.hnet.Callback;
import org.henjue.library.hnet.HNet;
import org.henjue.library.hnet.RequestFacade;
import org.henjue.library.hnet.Response;
import org.henjue.library.hnet.exception.HNetError;
import org.henjue.library.hnet.typed.TypedFile;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final HNet net = new HNet.Builder()
                .setEndpoint("http://test.maiquanshop.com/")
                .setIntercept(new MyIntercept())
//                .setConverter(new GsonConverter(new Gson()))
                .build();
        net.setLogLevel(HNet.LogLevel.BASIC);
        TestService service = net.create(TestService.class);

        Callback<Object> callback = new Callback<Object>() {
            @Override
            public void start() {

            }

            @Override
            public void success(Object o, Response response) {
                System.out.println(o);
            }

            @Override
            public void failure(HNetError error) {
                error.printStackTrace();
            }

            @Override
            public void end() {

            }
        };
//        service.download("http://www.163.com",callback);
//        service.getByPath("henjue", callback);
//        service.getByQuery("henjue", callback);
        service.postForm(189, 1, "4c6a32ac439d8a355215f9c956bdf72c", callback);
//        service.postMulti(1, "4c6a32ac439d8a355215f9c956bdf72c", callback);
//        TypedFile file = new TypedFile("image/jpg", new File(Environment.getExternalStorageDirectory(), "img111.jpg"));
//        service.upload("4c6a32ac439d8a355215f9c956bdf72c", file, callback);
//        service.upload2("4c6a32ac439d8a355215f9c956bdf72c", file, 66, callback);
//        service.upload3("4c6a32ac439d8a355215f9c956bdf72c", file, 66, "file", callback);
//
//        service.download(123, new Callback<Response>() {
//            @Override
//            public void start() {
//
//            }
//
//            @Override
//            public void success(Response response, Response response2) {
//                try {
//                    InputStream in = response.getBody().in();
//                    //do coding
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            @Override
//            public void failure(HNetError error) {
//
//            }
//
//            @Override
//            public void end() {
//
//            }
//        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class MyIntercept implements RequestIntercept {
        private HashMap<String, String> params = new HashMap<>();

        @Override
        public void onComplite(RequestFacade request) {
            String key = getKey(params);
            if (key != null) {
                params.put("key", key);
            }
            request.add("key", key);
        }

        private String getKey(Map<String, String> params) {
            if (params == null || params.isEmpty()) {
                return null;
            }
            Map<String, String> sortMap = new TreeMap<String, String>(new MapKeyComparator());
            sortMap.putAll(params);
            StringBuffer sb = new StringBuffer();

            for (String key : sortMap.keySet()) {
                sb.append(sortMap.get(key)).append("&");
            }
            String plaintext = sb.toString();
            plaintext = plaintext.endsWith("&") ? plaintext.substring(0, plaintext.length() - 1) : plaintext;
            return MD5Util.MD5(plaintext);
        }

        @Override
        public void onStart(RequestFacade request) {
            params.clear();
            /**
             * 添加一些任何任何接口都必须有的参数
             */
            request.add("versionName", "2.0.0");
            request.add("sign", "maimengkeji@4c6a32ac439d8a355215f9c956bdf72c");

        }

        @Override
        public void onAdd(String name, Object value) {
            if (value instanceof String || value instanceof Integer || value instanceof Boolean || value instanceof Long || value instanceof Float) {
                params.put(name, String.valueOf(value));
            }
        }

        public class MapKeyComparator implements Comparator<String> {
            public int compare(String str1, String str2) {
                return str1.compareTo(str2);
            }
        }
    }
}
