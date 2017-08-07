package com.example.shuai.traffic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class ResultActivity extends AppCompatActivity {

    TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        tvResult = (TextView) findViewById(R.id.tv_result);
        tvResult.setMovementMethod(ScrollingMovementMethod.getInstance());

        Intent intent = this.getIntent();
        if (!intent.getStringExtra("result").equals("")) {
            String tvContent = intent.getStringExtra("result");
            tvResult.setText(tvContent);
        }
    }
}
