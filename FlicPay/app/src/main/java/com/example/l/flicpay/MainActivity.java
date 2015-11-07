package com.example.l.flicpay;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
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
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import cz.msebera.android.httpclient.Header;
import io.flic.lib.FlicButton;
import io.flic.lib.FlicButtonCallback;
import io.flic.lib.FlicButtonCallbackFlags;
import io.flic.lib.FlicManager;
import io.flic.lib.FlicManagerInitializedCallback;


public class MainActivity extends Activity {

    private int area;
    private int area_submit;
    private int[] down_password = new int[10];
    private int[] up_password = new int[10];
    private int[] userPattern = new int[5];
    private FlicManager manager;
    private ImageView background;
    private View pass_nums;
    private Button btn_start;
    private Button btn_submit;
    private int counter;
    private TextSwitcher txt_counter;
    private List<FlicButton> buttons;
    private HashMap<String,FlicButton> buttons_map;
    private HashMap<String,Integer> buttons_score;
    private HashMap<String,Integer> buttons_rssi;
    private ImageView circle;
    private String binaryPassword;
    private String connectedButton; // mac address of the connected button
    private double price;
    private boolean recording;
    private boolean pairing;
    String TAG ="flicpay";
    private FlicButtonCallback buttonCallback = new FlicButtonCallback() {
        @Override
        public void onButtonUpOrDown(FlicButton button, boolean wasQueued, int timeDiff, boolean isUp, boolean isDown) {
            final String text = button + " was " + (isDown ? "pressed" : "released");
            Log.d(TAG, text);
            if(recording && button.getButtonId().equals(connectedButton)) {
                if(isDown)
                {
                    down_password[area]++;
                }
                if(isUp)
                {
                    up_password[area]++;
                }
            }
            Log.d(TAG,"pairing: "+pairing+" ,"+binaryPassword.charAt(area_submit));
            if(pairing && binaryPassword.charAt(area_submit)=='1')
            {
               //(Integer)buttons_score.get(button.getButtonId());
                Log.d(TAG,"plus:" + button.getButtonId());
                buttons_score.put(button.getButtonId(), buttons_score.get(button.getButtonId())+1);
            }

        }

        @Override
        public void onReadRemoteRSSI(FlicButton button, int rssi, int status) {
            super.onReadRemoteRSSI(button, rssi, status);

            buttons_rssi.put(button.getButtonId(),rssi);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recording = false;
        setContentView(R.layout.activity_pair);
        Random random = new Random();
        price = ((double)random.nextInt(9999))/100;
        ((TextView) findViewById(R.id.txt_price)).setText("Total: "+price+"\u20AC");
        /**/
        btn_submit = (Button)findViewById(R.id.button_submit);
        btn_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btn_submit.setVisibility(View.GONE);//hide
                startPairing();
            }
        });
        circle = (ImageView) findViewById(R.id.circle);
        circle.setVisibility(View.GONE);    //disappear

        FlicManager.setAppCredentials("[appId]", "[appSecret]", "FlicPay");

        FlicManager.getInstance(this, new FlicManagerInitializedCallback() {
            @Override
            public void onInitialized(FlicManager manager) {
                MainActivity.this.manager = manager;
                buttons = manager.getKnownButtons();
                buttons_map = new HashMap<String, FlicButton>();
                buttons_score = new HashMap<String, Integer>();
                buttons_rssi = new HashMap<String, Integer>();
                for (FlicButton button : buttons) {
                    buttons_map.put(button.getButtonId(), button);
                    buttons_score.put(button.getButtonId(), 0);
                    buttons_rssi.put(button.getButtonId(), 0);
                    setButtonCallback(button);
                }
//                manager.initiateGrabButton(MainActivity.this);// this refers to the current Activity.
            }
        });


/*

*/

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
        button.setFlicButtonCallbackFlags(FlicButtonCallbackFlags.UP_OR_DOWN);;
    }


    @Override
    protected void onDestroy() {
        FlicManager.destroyInstance();
        super.onDestroy();
    }
    private void startPairing(){

        pairing = true;
        //generate random binary code, 5 bits, 31 combinations
        Random r = new Random();
        binaryPassword = Integer.toBinaryString(r.nextInt(32)+64); //char c = s.charAt(0);

        circle.setVisibility(View.VISIBLE);
        final Drawable green_circle = getResources().getDrawable(R.drawable.green_circle);
        final Drawable red_circle = getResources().getDrawable(R.drawable.red_circle);
        circle.setImageDrawable(red_circle);

        final Handler handler_circle = new Handler();
        area_submit=-1;
        for(int i = 0; i<userPattern.length; i++){
            userPattern[i]=0;
        }

        //clean the hashmap score
        Map map = buttons_score;
        Iterator iter = map.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();
            entry.setValue( 0 );
        }

        final Runnable c = new Runnable() {
            public void run() {
                area_submit++;
                if(area_submit<5){
                    if( binaryPassword.charAt(area_submit+1) == '0')
                        circle.setImageDrawable(red_circle);
                    else
                        circle.setImageDrawable(green_circle);

                    handler_circle.postDelayed(this, 1000);}
                else{
                    //finnish pairing, begin to check
                    circle.setVisibility(View.GONE);

                    //choose the highest score
                    int highestScore=1;
                    ArrayList<String> highestList = new ArrayList<String>();

                    Map<String, FlicButton> map = buttons_map;
                    Iterator<Map.Entry<String, FlicButton>> entries = map.entrySet().iterator();
                    while (entries.hasNext()) {
                        Map.Entry<String, FlicButton> entry = entries.next();
                        if(highestScore == buttons_score.get(entry.getKey()) ){
                            highestList.add(entry.getKey());
                        }else if(highestScore < buttons_score.get(entry.getKey()) ){
                            highestList.clear();
                            highestList.add(entry.getKey());
                        }
                    }

                    if (highestList.size()<=0){
                        btn_submit.setVisibility(View.VISIBLE);

                    }else {
                        if(highestList.size()==1){
                            Log.d(TAG,"choosed button mac:"+highestList.get(0));
                            connectedButton=highestList.get(0);
                        }else{// more than 1, choose the best bluetooth signal one
                            double highestSignal=0;
                            for (int i=0;i< highestList.size();i++){
                                if(buttons_rssi.get(highestList.get(i)) > highestSignal ){
                                    connectedButton=highestList.get(i);
                                }
                            }
                        }
                        //reset the layout
                        setContentView(R.layout.activity_main);
                        ((TextView) findViewById(R.id.txt_price)).setText("Total: " + price + "\u20AC");
                        btn_start = (Button)findViewById(R.id.button);
                        btn_start.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                btn_start.setEnabled(false);
                                startAnimation();
                            }
                        });

                        counter = 3;
                        background = (ImageView) findViewById(R.id.img_background);
                        pass_nums = findViewById(R.id.pass_nums);
                        txt_counter = (TextSwitcher) findViewById(R.id.counter);
                        txt_counter.setInAnimation(MainActivity.this, R.anim.fade_in);
                        txt_counter.setOutAnimation(MainActivity.this, R.anim.fade_out);
                    }
                    pairing = false;
                }

            }
        };

        handler_circle.postDelayed(c, 1000);

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
                    for(int i = 0; i<up_password.length; i++){
                        up_password[i]=0;
                        down_password[i]=0;
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
//                                Log.d(TAG, "password:"
//                                        + "[0]" + password[0] + ","
//                                        + "[1]" + password[1] + ","
//                                        + "[2]" + password[2] + ","
//                                        + "[3]" + password[3] + ","
//                                        + "[4]" + password[4] + ","
//                                        + "[5]" + password[5] + ","
//                                        + "[6]" + password[6] + ","
//                                        + "[7]" + password[7] + ","
//                                        + "[8]" + password[8] + ","
//                                        + "[9]" + password[9] + ".");
//                                String complete_password = password[0]+""+password[1]+""+password[2]+""+
//                                        password[3]+""+password[4]+""+password[5]+""+
//                                        password[6]+""+password[7]+""+password[8]+""+
//                                        password[9];

                                verify(calculatePassword());

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

    private String calculatePassword()
    {
        String thisPassword = "";

        for(int i = 0; i< 10; i++)
        {
            thisPassword = thisPassword + down_password[i]+up_password[i]*10;
            thisPassword+= "-";
        }
        return thisPassword.substring(0, thisPassword.length()-1);
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

    private void verify(String complete_password)
    {
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("flic_id", connectedButton);
        params.put("password", complete_password);
        params.put("amount", price);
        client.get("http://pan0166.panoulu.net/flic_pay/pay.php", params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(getApplicationContext(), "Transaction succeeded",Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d("values",headers.toString());
                Log.d("values",new String(responseBody));
                Toast.makeText(getApplicationContext(), "Transaction failed",Toast.LENGTH_SHORT).show();
            }
        });
    }
}
