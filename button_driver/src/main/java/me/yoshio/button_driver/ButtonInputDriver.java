package me.yoshio.button_driver;

import com.google.android.things.userdriver.UserDriverManager;
import com.google.android.things.userdriver.input.InputDriver;
import com.google.android.things.userdriver.input.InputDriverEvent;

import java.io.IOException;

public class ButtonInputDriver implements AutoCloseable {
    private static String DRIVER_NAME = "button";
    private Button mDevice;
    private int mKeycode;
    private InputDriver mDriver;

    public ButtonInputDriver(String pin, Button.LogicState logicLevel, int keycode)
            throws IOException{
        mDevice = new Button(pin, logicLevel);
        mKeycode = keycode;
    }

    public void setDebounceDelay(long delay) {
        mDevice.setDebounceDelay(delay);
    }

    @Override
    public void close() throws IOException {
        unregister();
        if(mDevice != null){
            try{
                mDevice.close();
            } finally {
                mDevice = null;
            }
        }
    }

    public void register() {
        if(mDevice == null) {
            throw new IllegalStateException("cannot register closed driver");
        }
        if(mDriver == null) {
            mDriver = build(mDevice, mKeycode);
            UserDriverManager.getInstance().registerInputDriver(mDriver);
        }
    }

    public void unregister(){
        if(mDriver != null){
            UserDriverManager.getInstance().unregisterInputDriver(mDriver);
            mDriver = null;
        }
    }

    static InputDriver build(Button button, final int keyCode){
        final InputDriver inputDriver = new InputDriver.Builder()
                .setName(DRIVER_NAME)
                .setSupportedKeys(new int[]{keyCode})
                .build();
        final InputDriverEvent inputEvent = new InputDriverEvent();
        button.setOnButtonEventListener(new Button.OnButtonEventListener() {
            @Override
            public void onButtonEvent(Button button, boolean pressed) {
                inputEvent.clear();
                inputEvent.setKeyPressed(keyCode, pressed);
                inputDriver.emit(inputEvent);
            }
        });
        return inputDriver;
    }
}
