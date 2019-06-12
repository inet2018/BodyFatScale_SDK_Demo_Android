package aicare.net.cn.iweightdemo;

import android.app.Application;
import android.test.ApplicationTestCase;

import aicare.net.cn.iweightlibrary.utils.L;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {
    public ApplicationTest() {
        super(Application.class);
    }

    public void testGetDeviceType() {
        byte[] b = new byte[]{(byte) 0xA8, 01, 01, 01, 0x1B, (byte) 0xEF, 43, (byte) 0xB0, (byte) 0xEC, (byte) 0xB3, 00};
        byte deviceType = AicareBleConfigBak.getDeviceType(b);
        L.e(deviceType+"");
    }
}