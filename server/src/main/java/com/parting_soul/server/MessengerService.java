package com.parting_soul.server;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

import androidx.annotation.NonNull;

/**
 * @author parting_soul
 * @date 2020-01-04
 */
public class MessengerService extends Service {
    private Messenger mMessenger = new Messenger(new H());

    @Override
    public IBinder onBind(Intent intent) {
        return mMessenger.getBinder();
    }

    static class H extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            String str = "what = " + msg.what + " 接收到消息";
            switch (msg.what) {
                case 1:
                    break;
                case 2:
                    Bundle bundle = msg.getData();
                    bundle.setClassLoader(Book.class.getClassLoader());
                    Book book = bundle.getParcelable("data");
                    str += " " + book;

                    Messenger clientMessenger = msg.replyTo;
                    replyToClient(clientMessenger);

                    break;
                default:
            }
            Log.d(str);
        }

        private void replyToClient(Messenger clientMessenger) {
            try {
                Bundle bundle = new Bundle();
                bundle.putString("data", "接收成功");
                Message newMsg = Message.obtain();
                newMsg.what = 200;
                newMsg.setData(bundle);
                clientMessenger.send(newMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

}
