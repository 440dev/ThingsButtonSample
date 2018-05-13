package me.yoshio.sampleapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.util.Log;

import java.io.IOException;

import me.yoshio.button_driver.Button;
import me.yoshio.button_driver.ButtonInputDriver;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class HomeActivity extends Activity {
    private static final String TAG = "ButtonActivity";
    private static final String BUTTON_PIN_NAME = "BCM21";
    private ButtonInputDriver mButtonInputDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        try{
            mButtonInputDriver = new ButtonInputDriver(BUTTON_PIN_NAME, Button.LogicState.PRESSED_WHEN_LOW, KeyEvent.KEYCODE_SPACE);
        } catch(IOException e) {
            Log.e(TAG, "Error configuraing GPIO Pin", e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mButtonInputDriver.register();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mButtonInputDriver.unregister();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if(keyCode == KeyEvent.KEYCODE_SPACE) {
            Log.i(TAG, "Button KeyDown");
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_SPACE){
            Log.i(TAG, "Button KeyUp");
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if(mButtonInputDriver != null) {
            try{
                mButtonInputDriver.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing Button driver", e);
            }
        }
    }
}
