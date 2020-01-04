package com.parting_soul.ipcdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.parting_soul.server.Book;
import com.parting_soul.server.Log;

/**
 * @author parting_soul
 * @date 2020-01-03
 */
public class MessengerActivity extends AppCompatActivity {
    private boolean isBound;
    private Messenger mServiceMessenger;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_messenger);

        // 绑定服务
        Intent intent = new Intent();
        intent.setClassName("com.parting_soul.server", "com.parting_soul.server.MessengerService");
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isBound) {
            unbindService(mServiceConnection);
            isBound = false;
        }
    }

    public void onClick(View view) {
        if (!isBound) {
            return;
        }

        switch (view.getId()) {
            case R.id.bt_send_message:
                sendMessage();
                break;
            case R.id.bt_send_and_receive_message:
                sendAndReceiveMessage();
                break;
            default:
                break;
        }
    }

    private void sendAndReceiveMessage() {
        try {
            Message message = Message.obtain();
            message.what = 2;
            //设置自己的Messenger
            message.replyTo = mClientMessenger;

            //传递序列化对象
            Bundle bundle = new Bundle();
            bundle.putParcelable("data", new Book("Android 书籍", "技术"));
            message.setData(bundle);

            mServiceMessenger.send(message);
            Log.d("fff");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        try {
            Message message = Message.obtain();
            message.what = 1;
            mServiceMessenger.send(message);
            Log.d("service = ");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("service connected");
            isBound = true;
            mServiceMessenger = new Messenger(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };


    private Messenger mClientMessenger = new Messenger(new ClientHandler());

    static class ClientHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d("接收到服务端的消息");
            switch (msg.what) {
                case 200:
                    Bundle bundle = msg.getData();
                    String result = bundle.getString("data");
                    Log.d(result);
                    break;
                default:
            }
        }
    }

}
