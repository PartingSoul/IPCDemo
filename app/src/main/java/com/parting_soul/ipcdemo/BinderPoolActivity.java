package com.parting_soul.ipcdemo;

import android.os.Bundle;
import android.os.IBinder;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.parting_soul.server.IBinderPool;
import com.parting_soul.server.IComputer;
import com.parting_soul.server.IModel;
import com.parting_soul.server.Log;

/**
 * @author parting_soul
 * @date 2020-01-14
 */
public class BinderPoolActivity extends AppCompatActivity {
    private BinderPool mBinderPool;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_binder_pool);
        mBinderPool = BinderPool.getInstance(this);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_add:
                add();
                break;
            case R.id.bt_get_msg:
                getMessage();
                break;
            default:
                break;
        }
    }

    private void getMessage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IBinder binder = mBinderPool.queryBinder(IBinderPool.BINDER_CODE_GET_MESSGE);
                    IModel model = IModel.Stub.asInterface(binder);
                    String result = model.getMessage();
                    Log.d("服务端返回信息： " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void add() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    IBinder binder = mBinderPool.queryBinder(IBinderPool.BINDER_CODE_COMPUTER);
                    IComputer computer = IComputer.Stub.asInterface(binder);
                    int result = computer.add(1, 2);
                    Log.d("计算结果: a + b = " + result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

}
