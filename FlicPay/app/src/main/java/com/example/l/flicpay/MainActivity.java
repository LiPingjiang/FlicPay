package com.example.l.flicpay;

import android.content.Intent;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import android.os.Handler;

import java.lang.reflect.Array;

import io.flic.lib.FlicButton;
import io.flic.lib.FlicButtonCallback;
import io.flic.lib.FlicButtonCallbackFlags;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;


public class MainActivity extends ActionBarActivity {

    private int area;
    private int[] passward= new int[10];
    private Button Button_Input;
    private FlicManager manager;
    String TAG ="flicpay";
    private FlicButtonCallback buttonCallback = new FlicButtonCallback() {
        @Override
        public void onButtonUpOrDown(FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
            final String text = button + " was " + (isDown ? "pressed" : "released");
            Log.d(TAG, text);
            passward[area]++;
        }
    };
    private void stuffToBeExecuted(){

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button_Input = (Button)findViewById(R.id.button);
        Button_Input.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Click input pin button, start handler");

                area = -1;
                for( int i:passward){
                    i=0;
                }

                final Handler handler = new Handler();

                final Runnable r = new Runnable() {
                    public void run() {
                        area++;
                        if(area<10){
                            Log.d(TAG, "second:"+ area);
                            handler.postDelayed(this, 1000);}
                        else{
                            Log.d(TAG,"password:"
                                    +"[0]"+passward[0]+","
                                    +"[1]"+passward[1]+","
                                    +"[2]"+passward[2]+","
                                    +"[3]"+passward[3]+","
                                    +"[4]"+passward[4]+","
                                    +"[5]"+passward[5]+","
                                    +"[6]"+passward[6]+","
                                    +"[7]"+passward[7]+","
                                    +"[8]"+passward[8]+","
                                    +"[9]"+passward[9]+".");
                        }

                    }
                };

                handler.postDelayed(r, 0);
            }
        });

        FlicManager.setAppCredentials("[appId]", "[appSecret]", "FlicPay");

        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                MainActivity.this.manager = manager;
                manager.initiateGrabButton(MainActivity.this);// this refers to the current Activity.
            }
        });


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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        FlicButton button = manager.completeGrabButton(requestCode, resultCode, data);

        if (button != null) {
            Log.d(TAG, "Got a button: " + button);
            setButtonCallback(button);
        }
    }

    private void setButtonCallback(FlicButton button) {
        button.removeAllFlicButtonCallbacks();
        button.addFlicButtonCallback(buttonCallback);
        button.setFlicButtonCallbackFlags(FlicButtonCallbackFlags.UP_OR_DOWN);
    }


    @Override
    protected void onDestroy() {
        FlicManager.destroyInstance();
        super.onDestroy();
    }
}
