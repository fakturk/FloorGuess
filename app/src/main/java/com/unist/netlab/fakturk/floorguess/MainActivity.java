package com.unist.netlab.fakturk.floorguess;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import static android.hardware.SensorManager.GRAVITY_EARTH;

public class MainActivity extends AppCompatActivity
{
    ArrowView arrowView ;
    ArrowView accView;
    ArrowView accGraDiffView;
    ArrowView gyrView;
    LinearLayout linearLayoutHor1, linearLayoutHor2;

    TextView tv_gravity;
    Intent i;
    Button buttonStart, buttonGra, buttonAcc, buttonLinear, buttonGyr ;

    float[] acc, gyr, oldAcc, oldGyr, gravity, oldGravity, velocity, distance;
    float[][] rotation, resultOfDynamic;
    boolean start;

    Gravity g;
    Orientation orientation;

    int counter;
    float deltaT=0.01f;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        arrowView = (ArrowView) findViewById(R.id.arrowView);
        accView = (ArrowView) findViewById(R.id.accView);
        accGraDiffView = (ArrowView) findViewById(R.id.accGraDiffView);
        gyrView = (ArrowView) findViewById(R.id.gyrView);

        arrowView.setType("Gravity");
        accView.setType("Acc");
        accGraDiffView.setType("Lnr Acc");
        gyrView.setType("Gyr");

        linearLayoutHor1 = (LinearLayout) findViewById(R.id.linearLayHor1);
        linearLayoutHor2 = (LinearLayout) findViewById(R.id.linearLayHor2);


        tv_gravity = (TextView) findViewById(R.id.tv_gravity);
        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonGra = (Button) findViewById(R.id.buttonGra);
        buttonAcc = (Button) findViewById(R.id.buttonAcc);
        buttonLinear = (Button) findViewById(R.id.buttonAccGraDiff);
        buttonGyr = (Button) findViewById(R.id.buttonGyr);
        i = new Intent(this, SensorService.class);

        acc = new float[3];
        gyr = new float[3];
        oldAcc = null;
        oldGyr = null;
        gravity = new float[3];
        rotation = null;
        velocity = new float[]{0,0,0};
        distance = new float[]{0,0,0};


        g = new Gravity();
        orientation = new Orientation();

        counter=0;

        arrowView.setLine(0,100,0);
        accView.setLine(0,100,0);

        LocalBroadcastManager.getInstance(this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                acc = (intent.getFloatArrayExtra("ACC_DATA"));
                gyr = intent.getFloatArrayExtra("GYR_DATA");

                if (acc!=null && oldAcc==null)
                {
                    oldAcc = new float[3];
                    System.arraycopy(acc, 0, oldAcc, 0,acc.length);

                }
                if (gyr!=null && oldGyr==null)
                {
                    oldGyr = new float[3];
                    System.arraycopy(gyr,0,oldGyr,0,gyr.length);

                }
                if (acc==null && oldAcc!=null)
                {
                    acc = new float[3];
                    System.arraycopy(oldAcc, 0, acc, 0,oldAcc.length);

                }
                if (gyr==null && oldGyr!=null)
                {
                    gyr = new float[]{0,0,0};

                }


                if (acc!=null && gyr!=null && start!=true)
                {
                    start = true;
                    float accNorm = (float) Math.sqrt(Math.pow(acc[0],2)+Math.pow(acc[1],2)+Math.pow(acc[2],2));
                    for (int j = 0; j < 3; j++)
                    {
                        gravity[j] = acc[j]*(GRAVITY_EARTH/accNorm);
                    }
                    rotation = orientation.rotationFromGravity(gravity);


                }
                if (start)
                {

                    float thresholdAcc = 0; //0.15
                    float thresholdGyr = 0.15f; //0.2
                    float accNorm = (float) Math.sqrt(Math.pow(acc[0],2)+Math.pow(acc[1],2)+Math.pow(acc[2],2));

                    //if phone stable gravity = acc
                    if ((Math.abs(gyr[0])+Math.abs(gyr[1])+Math.abs(gyr[2]))<thresholdGyr )
                    {

                        System.out.print("stable, ");
                        if (counter>10) //if phone is stable over a second
                        {
                            for (int j = 0; j < 3; j++)
                            {
                                gravity[j] = acc[j]*(GRAVITY_EARTH/accNorm);

                            }
//
                            counter=0;
                        }
                        else
                        {
                            counter++;
                        }


                    }
//

                    else // not stable
                    {
                        System.out.print("not stable, ");
                        counter=0;
//
                        rotation = orientation.rotationFromGravity(gravity);
                        rotation = orientation.updateRotationMatrix(rotation, orientation.rotatedGyr(gyr,rotation),deltaT);
                        gravity = g.gravityAfterRotation(rotation);
//
                    }


                    //store acc values
                    System.arraycopy(acc, 0, oldAcc, 0,acc.length);
                    for (int j = 0; j < 3; j++)
                    {
                        velocity[j]+=acc[j]-gravity[j];
                    }
                    for (int j = 0; j < 3; j++)
                    {
                        distance[j]+=velocity[j];
                    }

                    //set views
                    int lS = 20; // size coefficient of the line
                    arrowView.setLine((-1)*gravity[0]*lS,gravity[1]*lS,gravity[2]*lS);
                    accView.setLine((-1)*acc[0]*lS,acc[1]*lS, acc[2]*lS);
                    accGraDiffView.setLine((-1)*acc[0]*lS - (-1)*gravity[0]*lS,acc[1]*lS - gravity[1]*lS, acc[2]*lS - gravity[2]*lS);
                    gyrView.setLine(gyr[1]*lS,gyr[0]*lS, -1*gyr[2]*lS);

//                    System.out.println(acc[0]+", "+acc[1]+", "+acc[2]+", "+gyr[0]+", "+gyr[1]+", "+gyr[2]+", "+gravity[0]+", "+gravity[1]+", "+gravity[2]+", ");

                    tv_gravity.setText(velocity[0]+", "+velocity[1]+", "+velocity[2]+"\n"+distance[0]+", "+distance[1]+", "+distance[2]);



                }
            }
        }, new IntentFilter(SensorService.ACTION_SENSOR_BROADCAST));

        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (buttonStart.getText().equals("Start")) {
                    buttonStart.setText("Stop");
                    startService(new Intent(MainActivity.this, SensorService.class));

                } else {
                    buttonStart.setText("Start");
                    stopService(new Intent(MainActivity.this, SensorService.class));


                }
            }
        });
        buttonGra.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (buttonGra.getText().equals("GRA")) {
//
                    if (buttonAcc.getText().equals("acc"))
                    {
                        makeSmaller("ACC");
                    }
                    else if (buttonLinear.getText().equals("linear"))
                    {
                        makeSmaller("LINEAR");
                    }
                    else if (buttonGyr.getText().equals("gyr"))
                    {
                        makeSmaller("GYR");
                    }
                    else
                    {
                        makeBigger("GRA");
                    }




                } else {
//
                    makeSmaller("GRA");




                }
            }
        });

        buttonAcc.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (buttonAcc.getText().equals("ACC")) {

//                    System.out.println("ACC");
                    if (buttonGra.getText().equals("gra"))
                    {
                        makeSmaller("GRA");
                    }
                    else if (buttonLinear.getText().equals("linear"))
                    {
                        makeSmaller("LINEAR");
                    }
                    else if (buttonGyr.getText().equals("gyr"))
                    {
                        makeSmaller("GYR");
                    }
                    else
                    {
                        makeBigger("ACC");
                    }



                } else {

//                    System.out.println("acc");
                    makeSmaller("ACC");




                }
            }
        });

        buttonLinear.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (buttonLinear.getText().equals("LINEAR")) {
//
                    if (buttonAcc.getText().equals("acc"))
                    {
                        makeSmaller("ACC");
                    }
                    else if (buttonGra.getText().equals("gra"))
                    {
                        makeSmaller("GRA");
                    }
                    else if (buttonGyr.getText().equals("gyr"))
                    {
                        makeSmaller("GYR");
                    }
                    else
                    {
                        makeBigger("LINEAR");
                    }




                } else {
//
                    makeSmaller("LINEAR");




                }
            }
        });

        buttonGyr.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (buttonGyr.getText().equals("GYR")) {
//
                    if (buttonAcc.getText().equals("acc"))
                    {
                        makeSmaller("ACC");
                    }
                    else if (buttonLinear.getText().equals("linear"))
                    {
                        makeSmaller("LINEAR");
                    }
                    else  if (buttonGra.getText().equals("gra"))
                    {
                        makeSmaller("GRA");
                    }
                    else
                    {
                        makeBigger("GYR");
                    }



                } else {
//
                    makeSmaller("GYR");



                }
            }
        });
    }

    void makeBigger(String button)
    {


        if (button=="GRA")
        {
            buttonGra.setText("gra");
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*3/2));
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*1/2));

            linearLayoutHor1.removeView(accView);
            linearLayoutHor2.addView(accView);
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*3/2,arrowView.getHeight()*3/2));
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*1/2,accView.getHeight()*1/2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*1/2,accGraDiffView.getHeight()*1/2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*1/2,gyrView.getHeight()*1/2));

            arrowView.setCoefficient(arrowView.getCoefficient()*3/2);
            accView.setCoefficient(accView.getCoefficient()*1/2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*1/2);
            gyrView.setCoefficient(gyrView.getCoefficient()*1/2);
        }
        if (button=="ACC")
        {
            buttonAcc.setText("acc");
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*3/2));
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*1/2));

            linearLayoutHor1.removeView(arrowView);
            linearLayoutHor2.addView(arrowView);
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*3/2,accView.getHeight()*3/2));
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*1/2,arrowView.getHeight()*1/2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*1/2,accGraDiffView.getHeight()*1/2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*1/2,gyrView.getHeight()*1/2));

            arrowView.setCoefficient(arrowView.getCoefficient()*1/2);
            accView.setCoefficient(accView.getCoefficient()*3/2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*1/2);
            gyrView.setCoefficient(gyrView.getCoefficient()*1/2);

        }
        if (button=="LINEAR")
        {
            buttonLinear.setText("linear");
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*3/2));
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*1/2));

            linearLayoutHor2.removeView(gyrView);
            linearLayoutHor1.addView(gyrView);
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*1/2,arrowView.getHeight()*1/2));
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*1/2,accView.getHeight()*1/2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*3/2,accGraDiffView.getHeight()*3/2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*1/2,gyrView.getHeight()*1/2));

            arrowView.setCoefficient(arrowView.getCoefficient()*1/2);
            accView.setCoefficient(accView.getCoefficient()*1/2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*3/2);
            gyrView.setCoefficient(gyrView.getCoefficient()*1/2);
        }
        if (button=="GYR")
        {
            buttonGyr.setText("gyr");
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*3/2));
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*1/2));

            linearLayoutHor2.removeView(accGraDiffView);
            linearLayoutHor1.addView(accGraDiffView);
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*1/2,arrowView.getHeight()*1/2));
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*1/2,accView.getHeight()*1/2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*1/2,accGraDiffView.getHeight()*1/2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*3/2,gyrView.getHeight()*3/2));

            arrowView.setCoefficient(arrowView.getCoefficient()*1/2);
            accView.setCoefficient(accView.getCoefficient()*1/2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*1/2);
            gyrView.setCoefficient(gyrView.getCoefficient()*3/2);

        }

    }

    void makeSmaller(String button)
    {
//        System.out.println("make smaller "+button);
        if (button=="GRA")
        {
            buttonGra.setText("GRA");
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*2/3));
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*2));

            linearLayoutHor2.removeView(accView);
            linearLayoutHor1.addView(accView);

            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*2/3,arrowView.getHeight()*2/3));
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*2,accView.getHeight()*2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*2,accGraDiffView.getHeight()*2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*2,gyrView.getHeight()*2));

            arrowView.setCoefficient(arrowView.getCoefficient()*2/3);
            accView.setCoefficient(accView.getCoefficient()*2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*2);
            gyrView.setCoefficient(gyrView.getCoefficient()*2);

        }
        if (button=="ACC")
        {
            buttonAcc.setText("ACC");
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*2/3));
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*2));

            linearLayoutHor2.removeView(arrowView);
            linearLayoutHor1.addView(arrowView);
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*2/3,accView.getHeight()*2/3));
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*2,arrowView.getHeight()*2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*2,accGraDiffView.getHeight()*2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*2,gyrView.getHeight()*2));

            arrowView.setCoefficient(arrowView.getCoefficient()*2);
            accView.setCoefficient(accView.getCoefficient()*2/3);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*2);
            gyrView.setCoefficient(gyrView.getCoefficient()*2);

        }
        if (button=="LINEAR")
        {
            buttonLinear.setText("LINEAR");
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*2/3));
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*2));

            linearLayoutHor1.removeView(gyrView);
            linearLayoutHor2.addView(gyrView);
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*2,arrowView.getHeight()*2));
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*2,accView.getHeight()*2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*2/3,accGraDiffView.getHeight()*2/3));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*2,gyrView.getHeight()*2));

            arrowView.setCoefficient(arrowView.getCoefficient()*2);
            accView.setCoefficient(accView.getCoefficient()*2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*2/3);
            gyrView.setCoefficient(gyrView.getCoefficient()*2);

        }
        if (button=="GYR")
        {
            buttonGyr.setText("GYR");
            linearLayoutHor2.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor2.getWidth(),linearLayoutHor2.getHeight()*2/3));
            linearLayoutHor1.setLayoutParams(new LinearLayout.LayoutParams(linearLayoutHor1.getWidth(),linearLayoutHor1.getHeight()*2));

            linearLayoutHor1.removeView(accGraDiffView);
            linearLayoutHor2.addView(accGraDiffView);
            arrowView.setLayoutParams(new LinearLayout.LayoutParams(arrowView.getWidth()*2,arrowView.getHeight()*2));
            accView.setLayoutParams(new LinearLayout.LayoutParams(accView.getWidth()*2,accView.getHeight()*2));
            accGraDiffView.setLayoutParams(new LinearLayout.LayoutParams(accGraDiffView.getWidth()*2,accGraDiffView.getHeight()*2));
            gyrView.setLayoutParams(new LinearLayout.LayoutParams(gyrView.getWidth()*2/3,gyrView.getHeight()*2/3));

            arrowView.setCoefficient(arrowView.getCoefficient()*2);
            accView.setCoefficient(accView.getCoefficient()*2);
            accGraDiffView.setCoefficient(accGraDiffView.getCoefficient()*2);
            gyrView.setCoefficient(gyrView.getCoefficient()*2/3);


        }

    }



    @Override
    protected void onPause() {

        super.onPause();


    }


    @Override
    protected void onResume() {

        super.onResume();
//        startService(new Intent(this, SensorService.class));
    }


    @Override
    protected void onDestroy() {


        super.onDestroy();
    }


    @Override
    public void onStart() {

        super.onStart();


    }

    @Override
    public void onStop() {

        super.onStop();


    }
}