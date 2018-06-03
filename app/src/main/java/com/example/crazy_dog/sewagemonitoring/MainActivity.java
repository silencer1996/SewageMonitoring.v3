package com.example.crazy_dog.sewagemonitoring;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.view.WindowManager;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;


import java.util.ArrayList;
import java.util.List;

import static org.opencv.imgproc.Imgproc.equalizeHist;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,CameraBridgeViewBase.CvCameraViewListener2 {

    /*声明ViewPager相关变量*/
    private View view1,view2,view3;
    private ViewPager viewPager;
    private List<View> viewList;
    /*声明TextView*/
    private TextView data;
    /*数据库相关*/
    private MyDatabaseHelper dbHelper;
    /*视频播放相关*/
    private JavaCameraView javaCameraView,javaCameraView2;
    private Mat mRgba,mRgba2;
    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    javaCameraView.enableView();
                }
                break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
                /*ViewPager相关变量初始化*/
        viewPager=(ViewPager)findViewById(R.id.viewpager);
        LayoutInflater inflater=getLayoutInflater();
        view1=inflater.inflate(R.layout.vplayout1,null);
        view2=inflater.inflate(R.layout.vplayout2,null);
        view3=inflater.inflate(R.layout.vplayout3,null);
        //数组赋值
        viewList=new ArrayList<View>();
        viewList.add(view1);
        viewList.add(view2);
        viewList.add(view3);
        //保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        /*Pageradapter重写*/
        PagerAdapter pagerAdapter=new PagerAdapter() {
            @Override
            public int getCount() {
                return viewList.size();
            }

            @Override
            public boolean isViewFromObject(View arg0, Object arg1) {
                return arg0==arg1;
            }

            @Override
            public void destroyItem(ViewGroup container,int position,Object object){
                container.removeView(viewList.get(position));
            }
            @Override
            public Object instantiateItem(ViewGroup container,int position){
                container.addView(viewList.get(position));
                return viewList.get(position);
            }
        };
        viewPager.setAdapter(pagerAdapter);

        //获取子布局资源，步骤2
        final TextView data=view1.findViewById(R.id.data);
        data.append("信息如下");
        //创建数据库存储信息
        dbHelper=new MyDatabaseHelper(this,"Data.db",null,1);
        Button createDatabase=(Button) view1.findViewById(R.id.create_database);
        createDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dbHelper.getWritableDatabase();
            }
        });
        //本地修改数据库信息
        Button addData=(Button) view1.findViewById(R.id.add_data);
        addData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db=dbHelper.getWritableDatabase();
                ContentValues values=new ContentValues();
                //开始组装第一条数据
                values.put("time","5月4日");
                values.put("flow","0.30");
                values.put("temperature",20);
                values.put("PH",7.0);
                values.put("BOD",7.0);
                values.put("COD",7.0);
                //插入第一条数据
                db.insert("Water",null,values);
                values.clear();
                //开始组装第二条数据
                values.put("time","5月5日");
                values.put("flow","0.45");
                values.put("temperature",15);
                values.put("PH",7.2);
                values.put("BOD",7.0);
                values.put("COD",7.0);
                //插入第二条数据
                db.insert("Water",null,values);
                values.clear();
                data.append("数据库修改"+"\n");
            }
        });
        //使用数据库显示信息
        Button queryButton=(Button) view1.findViewById(R.id.query_data);
        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db=dbHelper.getWritableDatabase();
                //查询Water表中所有数据
                Cursor cursor=db.query("Water",null,null,null,null,null,null);
                if(cursor.moveToFirst()){
                    do{
                        //遍历Cursor对象，取出数据并打印
                        String time=cursor.getString(cursor.getColumnIndex("time"));
                        String flow=cursor.getString(cursor.getColumnIndex("flow"));
                        double temperature=cursor.getDouble(cursor.getColumnIndex("temperature"));
                        double PH =cursor.getDouble(cursor.getColumnIndex("PH"));
                        data.append("时间"+time+"水位"+flow+"温度"+temperature+"PH"+PH+"\n");
                    } while(cursor.moveToNext());
                }
                cursor.close();
            }
        });
        //删除数据
        Button deleteData=(Button) view1.findViewById(R.id.delete_data);
        deleteData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SQLiteDatabase db=dbHelper.getWritableDatabase();
                db.delete("Water",null,null);
                data.setText("信息如下");
            }
        });
    //播放视频功能
        javaCameraView = (JavaCameraView) view2.findViewById(R.id.javaCameraView);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
        javaCameraView.enableFpsMeter();

    //View3网址访问
        WebView webView= (WebView) view3.findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //返回值是true的时候是控制网页在WebView中去打开，如果为false调用系统浏览器或第三方浏览器打开
                view.loadUrl(url);
                return true;
            }
            //WebViewClient帮助WebView去处理一些页面控制和请求通知
        });
        webView.loadUrl("http://localhost:60350/");


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.equipment1) {
            // Handle the camera action
        } else if (id == R.id.equipment2) {

        } else if (id == R.id.equipment3) {

        } else if (id == R.id.equipment4) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;

    }
    //重写视频播放相关功能
    @Override
    public void onPause() {
        super.onPause();
        if (javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, baseLoaderCallback);
        } else {
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }
    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba=inputFrame.rgba();
        //图片处理内容
    /*                cvtColor(mRgba,mRgba,COLOR_RGBA2GRAY);
                    equalizeHist(mRgba,mRgba);*/
            List<Mat> imageRGB=new java.util.ArrayList<Mat>(3);
            Core.split(mRgba,imageRGB);
            for(int i=0;i<3;i++) {
                equalizeHist(imageRGB.get(i), imageRGB.get(i));
            }
            Core.merge(imageRGB,mRgba);
            for(int i=0;i<3;i++) {
                imageRGB.get(i).release();
            }
            return mRgba;
    }

}
