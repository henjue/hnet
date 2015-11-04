package org.henjue.hnet.demo;

import android.os.Build;

import org.henjue.library.hnet.HNet;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import java.util.concurrent.Executor;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = Build.VERSION_CODES.JELLY_BEAN)
public abstract class AbstractTest implements Executor {
    protected HNet client;

    @Before
    public void setup() {
        client = new HNet.Builder()
                .setEndpoint("http://192.168.199.224:8001")
                .setLog(new HNet.Log() {
                    @Override
                    public void log(String message) {
                        System.out.println(message);
                    }
                })
                .setHttpExecutor(this)
//                .setConverter(new GsonConverter(new Gson()))
                .build();
        client.setLogLevel(HNet.LogLevel.FULL);
        before();
    }

    protected abstract void before();

    @Override
    public void execute(Runnable command) {
        Robolectric.getBackgroundThreadScheduler().post(command);
    }
}
