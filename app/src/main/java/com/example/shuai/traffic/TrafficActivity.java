package com.example.shuai.traffic;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.shuai.traffic.models.Car;
import com.example.shuai.traffic.models.Light;

import java.util.Queue;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class TrafficActivity extends AppCompatActivity {

    EditText etSumTime;     // 模拟时长
    EditText etCarNumber;   // 模拟汽车数量
    EditText etGreenTime;   // 模拟绿灯时长

    TextView tv_north_car;      // 4个方向的汽车
    TextView tv_south_car;
    TextView tv_west_car;
    TextView tv_east_car;

    TextView tv_north_light;    // 4个方向的交通灯
    TextView tv_south_light;
    TextView tv_west_light;
    TextView tv_east_light;

    Animation tv_north_anim;    // 4个动画
    Animation tv_south_anim;
    Animation tv_west_anim;
    Animation tv_east_anim;

    int timeRange = 0, carNumber = 0, lightGreenSeconds = 0; // 初始化3个要输入的模拟值

    // 方向和颜色常量
    final int North = 0;    // 4个方向
    final int South = 1;
    final int West = 2;
    final int East = 3;

    final int Red = 10;     // 2种颜色
    final int Green = 11;

    Button btnTime;     // 左侧计时器
    Button btnStart;    // 中间开始按钮
    Button btnResult;   // 右侧跳转结果

    String result = ""; // 存储结果

    private void initView() {
        // 初始化3个输入框
        etSumTime = (EditText) findViewById(R.id.edit_total_time);
        etCarNumber = (EditText) findViewById(R.id.edit_car_number);
        etGreenTime = (EditText) findViewById(R.id.edit_green_time);
        // 初始化4个方向的汽车
        tv_north_car = (TextView) findViewById(R.id.tv_north_car);
        tv_south_car = (TextView) findViewById(R.id.tv_south_car);
        tv_west_car = (TextView) findViewById(R.id.tv_west_car);
        tv_east_car = (TextView) findViewById(R.id.tv_east_car);
        // 初始化4个方向的交通灯
        tv_north_light = (TextView) findViewById(R.id.tv_north_light);
        tv_south_light = (TextView) findViewById(R.id.tv_south_light);
        tv_west_light = (TextView) findViewById(R.id.tv_west_light);
        tv_east_light = (TextView) findViewById(R.id.tv_east_light);

        btnTime = (Button) findViewById(R.id.btn_time);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traffic);

        // 初始化view控件
        initView();

        // 初始化Start按钮
        btnStart = (Button) findViewById(R.id.btn_start);
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取 模拟时长，汽车数量，绿灯时长
                if (!etSumTime.getText().toString().equals("")) {
                    timeRange = Integer.parseInt(etSumTime.getText().toString());
                }
                if (!etCarNumber.getText().toString().equals("")) {
                    carNumber = Integer.parseInt(etCarNumber.getText().toString());
                }
                if (!etGreenTime.getText().toString().equals("")) {
                    lightGreenSeconds = Integer.parseInt(etGreenTime.getText().toString());
                }
                trafficStart();
            }
        });

        btnResult = (Button) findViewById(R.id.btn_result);
        btnResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(TrafficActivity.this, ResultActivity.class);
                intent.putExtra("result", result);
                startActivity(intent);
            }
        });

    }

    private void sortCarsByShowTime(Car cars[]) {
        // copy车辆信息
        Car[] tmpCars = new Car[carNumber];
        System.arraycopy(cars, 0, tmpCars, 0, cars.length);

        // 对车辆信息按照出现时间排序
        int index = 0; // 遍历cars数组
        for (int i = 1; i <= timeRange; i++) { // showTime递增
            for (Car c : tmpCars) { // 遍历tmpCars
                if (c.getShowTime() == i) { // 找到showTime=i的车辆
                    cars[index] = c;
                    index++;
                }
            }
        }
    }

    // 调度函数
    private void trafficStart() {

        // 南北向和东西向各设置一个交通灯就可以
        Light northLight = new Light(North, Green, 0); // 方向，颜色，时间
        Light eastLight = new Light(East, Red, 0); // 时间不用指定，因为后面有timeCounter

        // 随机生成车辆信息
        Car[] cars = new Car[carNumber];
        Random random = new Random();
        for (int i = 0; i < carNumber; i++) { // foreach不行，因为是数组初始化
            cars[i] = new Car(random.nextInt(4), random.nextInt(timeRange) + 1);
            // new Car(int direction, int showTime) 随机生成车辆朝向和出现时间，时间范围[1, timeRange]
            // 在Car构造函数内部随机生成了车辆ID，即车牌号
        }

        // cars数组按车辆出现时间排序的
        sortCarsByShowTime(cars);

        // 车辆信息进队列
        Queue<Car> northCarsQueue = new LinkedBlockingQueue<>();
        Queue<Car> southCarsQueue = new LinkedBlockingQueue<>();
        Queue<Car> westCarsQueue = new LinkedBlockingQueue<>();
        Queue<Car> eastCarsQueue = new LinkedBlockingQueue<>();
        for (Car c : cars) {
            switch (c.getDirection()) {
                case North:
                    northCarsQueue.add(c);
                    break;
                case South:
                    southCarsQueue.add(c);
                    break;
                case West:
                    westCarsQueue.add(c);
                    break;
                case East:
                    eastCarsQueue.add(c);
                    break;
                default:
                    break;
            }
        }

        int lightGreen; // 绿灯持续时间
        int timeCounter = 0; // 计时器

        // 一条路双向车道，设置2个信互斥信号量集
        int[] northMutex = new int[100];
        int[] southMutex = new int[100];
        int[] westMutex = new int[100];
        int[] eastMutex = new int[100];

        // 存储4个方向的通过信息
        String northPassInfo = "";
        String southPassInfo = "";
        String westPassInfo = "";
        String eastPassInfo = "";

        while (!northCarsQueue.isEmpty() || !southCarsQueue.isEmpty() ||
                !westCarsQueue.isEmpty() || !eastCarsQueue.isEmpty()) {
            // 调度车辆
            if (northLight.getColor() == Green) {       // 南北向车辆通过十字路口
                System.out.println("南北向绿灯亮");
                lightGreen = lightGreenSeconds;

                result += (timeCounter + 1) + "----" + (timeCounter + lightGreenSeconds) + "s\t" + "南北向绿灯亮\n";

                while (lightGreen-- > 0) { // 每经过一辆车花费时间为1，每花费1的时间最多通过2辆车，双向车道
                    timeCounter++;
                    btnTime.setText(String.valueOf(timeCounter));
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    northPassInfo += getPassOneCarInfo(northCarsQueue, northMutex, timeCounter);
                    southPassInfo += getPassOneCarInfo(southCarsQueue, southMutex, timeCounter);
                }

                result += northPassInfo + "\n";
                result += southPassInfo + "\n";

                northPassInfo = "";
                southPassInfo = "";

                // 交通灯颜色转换
                northLight.setColor(Red);
                eastLight.setColor(Green);
                tv_north_light.setBackgroundColor(Color.RED);
                tv_south_light.setBackgroundColor(Color.RED);
                tv_west_light.setBackgroundColor(Color.GREEN);
                tv_east_light.setBackgroundColor(Color.GREEN);

            } else {                                    // 东西向车辆通过十字路口
                System.out.println("东西向绿灯亮");
                lightGreen = lightGreenSeconds;

                result += (timeCounter + 1) + "----" + (timeCounter + lightGreenSeconds) + "s\t" + "东西向绿灯亮\n";

                while (lightGreen-- > 0) { // 每经过一辆车花费时间为1
                    timeCounter++;
                    btnTime.setText(String.valueOf(timeCounter));
//                    try {
//                        Thread.sleep(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
                    westPassInfo += getPassOneCarInfo(westCarsQueue, westMutex, timeCounter);
                    eastPassInfo += getPassOneCarInfo(eastCarsQueue, eastMutex, timeCounter);
                }

                result += westPassInfo + "\n";
                result += eastPassInfo + "\n";

                westPassInfo = "";
                eastPassInfo = "";

                // 交通灯颜色转换
                eastLight.setColor(Red);
                northLight.setColor(Green);
                tv_west_light.setBackgroundColor(Color.RED);
                tv_east_light.setBackgroundColor(Color.RED);
                tv_north_light.setBackgroundColor(Color.GREEN);
                tv_south_light.setBackgroundColor(Color.GREEN);
            }
        }
    }

    // 经过一辆车
    private String getPassOneCarInfo(Queue<Car> carsQueue, int[] roadMutex, int timeCounter) {
        if (!carsQueue.isEmpty()) {
            Car car = carsQueue.peek();
            String carInfo = "";
            if (car.getShowTime() <= timeCounter) { // 假定提前到了，在这一次绿灯亮一定可以穿行
                int actualPassTime = getActualPassTime(roadMutex, car.getShowTime(), timeCounter);
                carInfo = car.toString() + "----" + actualPassTime + "\n";
                System.out.print(carInfo);
                switch (car.getDirection()) {
                    case North:
//                        System.out.println("north");
                        tv_north_car.setText(car.getShowTime() + "," + actualPassTime);
                        // 执行动画
                        tv_north_anim = AnimationUtils.loadAnimation(TrafficActivity.this, R.anim.tv_north_anim);
                        tv_north_car.setAnimation(tv_north_anim);
                        tv_north_car.startAnimation(tv_north_anim);
                        tv_north_car.invalidate();
                        break;
                    case South:
//                        System.out.println("south");
                        tv_south_car.setText(car.getShowTime() + "," + actualPassTime);
                        tv_south_anim = AnimationUtils.loadAnimation(TrafficActivity.this, R.anim.tv_south_anim);
                        tv_south_car.setAnimation(tv_south_anim);
                        tv_south_car.startAnimation(tv_south_anim);
                        tv_south_car.invalidate();
                        break;
                    case West:
//                        System.out.println("west");
                        tv_west_car.setText(car.getShowTime() + "," + actualPassTime);
                        tv_west_anim = AnimationUtils.loadAnimation(TrafficActivity.this, R.anim.tv_west_anim);
                        tv_west_car.setAnimation(tv_west_anim);
                        tv_west_car.startAnimation(tv_west_anim);
                        tv_west_car.invalidate();
                        break;
                    case East:
//                        System.out.println("east");
                        tv_east_car.setText(car.getShowTime() + "," + actualPassTime);
                        tv_east_anim = AnimationUtils.loadAnimation(TrafficActivity.this, R.anim.tv_east_anim);
                        tv_east_car.setAnimation(tv_east_anim);
                        tv_east_car.startAnimation(tv_east_anim);
                        tv_east_car.invalidate();
                        break;
                    default:
                        break;
                }
            }
            carsQueue.remove();
            return carInfo;
        }
        return "";
    }

    /**
     * 获取车辆实际经过十字路口的时间
     *
     * @param roadMutex   道路互斥信号量集
     * @param showTime    汽车出现时间
     * @param timeCounter 计时器
     * @return actualPassTime 车辆实际通过路口的时间
     */
    private int getActualPassTime(int[] roadMutex, int showTime, int timeCounter) {
        // timeCounter-1 确保timeLower落在正确范围内，取商运算
        int timeLower = (timeCounter - 1) / lightGreenSeconds * lightGreenSeconds + 1; // 时间下界

        // 汽车出现时间 < timeLower 重置出现时间，说明汽车等待到下一个绿灯
        if (showTime <= timeLower) showTime = timeLower;

        if (roadMutex[showTime] == 0) { // 该时刻的道路资源未被占用，可通过，直接返回showTime，并将roadMutex[showTime]置1
            roadMutex[showTime] = 1;
            return showTime;
        } else {                        // 这一时刻的道路资源已被占用了，不可通过
            int sum = 0;
            for (int i = showTime; i <= timeCounter; i++) { // 查询roadMutex数组，看自己排在队列的第几位
                if (roadMutex[i] == 1) { // =1 表示showTime之后的时刻的道路资源被占用
                    sum++;
                }
            }
            roadMutex[showTime + sum] = 1; // 表示该车占用该一时刻的道路资源
            return showTime + sum; // 返回实际通过时间
        }
    }
}
