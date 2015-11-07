package com.example.l.flicpay;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.Transformation;
import android.widget.Button;

import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextSwitcher;

import java.util.HashMap;
import java.util.List;

import io.flic.lib.FlicButton;
import io.flic.lib.FlicButtonCallback;
import io.flic.lib.FlicButtonCallbackFlags;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;


public class MainActivity extends Activity {

    private int area;
    private int[] password = new int[10];
    private FlicManager manager;
    private ImageView background;
    private View pass_nums;
    private Button btn_start;
    private int counter;
    private TextSwitcher txt_counter;
    private List<FlicButton> buttons;
    private HashMap<String,FlicButton> buttons_map;

    private boolean recording;
    String TAG ="flicpay";
    private FlicButtonCallback buttonCallback = new FlicButtonCallback() {
        @Override
        public void onButtonUpOrDown(FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
            final String text = button + " was " + (isDown ? "pressed" : "released");
            Log.d(TAG, text);
            if(recording)
                password[area]++;
        }
    };
    private void stuffToBeExecuted(){

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recording = false;
        setContentView(R.layout.activity_main);

        btn_start = (Button)findViewById(R.id.button);
        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_start.setEnabled(false);
                startAnimation();
            }
        });

        FlicManager.setAppCredentials("[appId]", "[appSecret]", "FlicPay");

        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                MainActivity.this.manager = manager;
                buttons = manager.getKnownButtons();
                for (FlicButton button : buttons) {
                    buttons_map.put(button.getButtonId(), button);
                    setButtonCallback(button);
                }
//                manager.initiateGrabButton(MainActivity.this);// this refers to the current Activity.
            }
        });



        counter = 3;
        background = (ImageView) findViewById(R.id.img_background);
        pass_nums = findViewById(R.id.pass_nums);
        txt_counter = (TextSwitcher) findViewById(R.id.counter);
        txt_counter.setInAnimation(this, R.anim.fade_in);
        txt_counter.setOutAnimation(this, R.anim.fade_out);


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

    private void startAnimation()
    {

        final ResizeAnimation resizeAnimation = new ResizeAnimation(background, pass_nums.getWidth());
        resizeAnimation.setDuration(11000);
        resizeAnimation.setInterpolator(new LinearInterpolator());
        resizeAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                btn_start.setEnabled(true);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        final Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                txt_counter.setOutAnimation(getApplicationContext(), R.anim.fade_out);
                if(counter>0)
                {

                    txt_counter.setText(counter + "");
                    counter--;
                    handler.postDelayed(this,1000);
                }
                else
                {
                    txt_counter.setText("Start");
                    background.startAnimation(resizeAnimation);

                    //Record password
                    Log.d(TAG, "Click input pin button, start handler");

                    area = -1;
                    for(int i = 0; i<password.length; i++){
                        password[i]=0;
                    }

                    final Handler handler_pass = new Handler();

                    final Runnable r = new Runnable() {
                        public void run() {
                            area++;
                            if(area<10){
                                recording = true;
                                Log.d(TAG, "second:"+ area);
                                handler_pass.postDelayed(this, 1000);}
                            else{
                                recording = false;
                                Log.d(TAG,"password:"
                                        +"[0]"+ password[0]+","
                                        +"[1]"+ password[1]+","
                                        +"[2]"+ password[2]+","
                                        +"[3]"+ password[3]+","
                                        +"[4]"+ password[4]+","
                                        +"[5]"+ password[5]+","
                                        +"[6]"+ password[6]+","
                                        +"[7]"+ password[7]+","
                                        +"[8]"+ password[8]+","
                                        +"[9]"+ password[9]+".");
                            }

                        }
                    };

                    handler_pass.postDelayed(r, 1000);

                    counter--;
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            txt_counter.setOutAnimation(getApplicationContext(), R.anim.long_fade_out);
                            txt_counter.setText("");
                            counter = 3;
                        }
                    },800);
                }
            }
        };

        handler.post(runnable);

    }

    public class ResizeAnimation extends Animation {
        final int startWidth;
        final int targetWidth;
        View view;

        public ResizeAnimation(View view, int targetWidth) {
            this.view = view;
            this.targetWidth = targetWidth;
            startWidth = view.getWidth();
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            int newWidth = (int) (targetWidth * interpolatedTime);
            view.getLayoutParams().width = newWidth;
            view.requestLayout();
        }

        @Override
        public void initialize(int width, int height, int parentWidth, int parentHeight) {
            super.initialize(width, height, parentWidth, parentHeight);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }
}
