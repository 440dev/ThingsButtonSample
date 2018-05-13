package me.yoshio.button_driver;

import android.os.Handler;
import android.util.Log;
import android.view.ViewConfiguration;

import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class Button implements AutoCloseable{
    private static final String TAG = Button.class.getSimpleName();

    private Gpio mButtonGpio;
    private OnButtonEventListener mListener;
    private Handler mDebounceHandler;
    private CheckDebounce mPendingCheckDebounce;
    private long mDebounceDelay = ViewConfiguration.getTapTimeout();


    public enum LogicState {
        PRESSED_WHEN_HIGH,
        PRESSED_WHEN_LOW
    }

    public interface OnButtonEventListener {
        void onButtonEvent(Button button, boolean pressed);
    }

    public Button(String pin, LogicState logicLevel) throws IOException{
        PeripheralManager pinService = PeripheralManager.getInstance();
        Gpio buttonGpio = pinService.openGpio(pin);
        try {
            connect(buttonGpio, logicLevel);
        }catch(IOException|RuntimeException e) {
            close();
            throw e;
        }
    }

    private void connect(Gpio buttonGpio, LogicState logicLevel) throws IOException {
        mButtonGpio = buttonGpio;
        mButtonGpio.setDirection(Gpio.DIRECTION_IN);
        mButtonGpio.setEdgeTriggerType(Gpio.EDGE_BOTH);
        mButtonGpio.setActiveType(logicLevel == LogicState.PRESSED_WHEN_LOW ?
                Gpio.ACTIVE_LOW : Gpio.ACTIVE_HIGH);
        mButtonGpio.registerGpioCallback(mInterruptCallback);

        mDebounceHandler = new Handler();
    }

    private GpioCallback mInterruptCallback = new InterruptCallback();

    class InterruptCallback implements GpioCallback {
        @Override
        public boolean onGpioEdge(Gpio gpio){
            try {
                boolean currentState = gpio.getValue();

                if(mDebounceDelay == 0){
                    performButtonEvent(currentState);
                } else{
                    boolean trigger = (mPendingCheckDebounce == null) ?
                            currentState : mPendingCheckDebounce.getTriggerState();
                    removeDebounceCallback();
                    mPendingCheckDebounce = new CheckDebounce(trigger);
                    mDebounceHandler.postDelayed(mPendingCheckDebounce, mDebounceDelay);
                }
            } catch(IOException e){
                Log.e(TAG, "Error reading button state", e);
            }
            return true;
        }
    }

    public void setOnButtonEventListener(OnButtonEventListener listener){
        mListener = listener;
    }

    public void setDebounceDelay(long delay) {
        if(delay < 0){
            throw new IllegalArgumentException("Debounce delay can not be negative value ");
        }
        removeDebounceCallback();
        mDebounceDelay = delay;
    }

    @Override
    public void close() throws IOException{
        removeDebounceCallback();
        mListener = null;
        if(mButtonGpio != null) {
            mButtonGpio.unregisterGpioCallback(mInterruptCallback);
            try{
                mButtonGpio.close();
            } finally {
                mButtonGpio = null;
            }
        }
    }

    void performButtonEvent(boolean state){
        if(mListener != null) {
            mListener.onButtonEvent(this, state);
        }
    }

    private void removeDebounceCallback(){
        if(mPendingCheckDebounce != null) {
            mDebounceHandler.removeCallbacks(mPendingCheckDebounce);
            mPendingCheckDebounce = null;
        }
    }


    private final class CheckDebounce implements Runnable{
        private boolean mTriggerState;

        public CheckDebounce(boolean triggerState) {
            mTriggerState = triggerState;
        }

        public boolean getTriggerState(){
            return mTriggerState;
        }

        @Override
        public void run() {
            if(mButtonGpio != null) {
                try{
                    if(mButtonGpio.getValue() == mTriggerState) {
                        performButtonEvent(mTriggerState);
                    }
                    removeDebounceCallback();
                }catch (IOException e){
                    Log.e(TAG, "Unable to read button value", e);
                }
            }
        }
    }
}
