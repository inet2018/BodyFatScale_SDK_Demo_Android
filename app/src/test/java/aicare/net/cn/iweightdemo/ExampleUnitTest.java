package aicare.net.cn.iweightdemo;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void isEmpty() throws Exception {
        Map<String, String> map = new HashMap<>();
        assertTrue(map.isEmpty());
    }

    @Test
    public void isTrue() throws Exception {
        byte[] b1 = new byte[]{(byte) 0xAC, 0x02, (byte) 0xFF, 0x00, 0x02, 0x21, 0x11, 0x02, 0x0E, 0x12, 0x07, 0x02, 0x03, 0x02, 0x01, 0x0A, 0x01, 0x28, 0x00, 0x00};
        byte[] b2 = new byte[]{0x01, 0x00, 0x0B, 0x01, (byte) 0x81, 0x06, (byte) 0xF5, 0x00, 0x1B, 0x02, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x1C, (byte) 0xAA, 0x02, 0x2C};

        /*System.out.println(AicareBleConfig.getDatas(b2));
        System.out.println(AicareBleConfig.getDatas(b1));
        System.out.println(AicareBleConfig.getDatas(b2));
        System.out.println(AicareBleConfig.getDatas(b1));
        System.out.println(AicareBleConfig.getDatas(b1));
        System.out.println(AicareBleConfig.getDatas(b2));*/

        //System.out.println(ParseData.getDataTime(140422003543L));
        byte[] temp = new byte[] {(byte) 0xF1, 0x0C};
    }
}