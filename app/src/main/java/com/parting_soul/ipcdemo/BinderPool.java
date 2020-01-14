package com.parting_soul.ipcdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.parting_soul.server.IBinderPool;
import com.parting_soul.server.Log;

/**
 * @author parting_soul
 * @date 2020-01-14
 */
public class BinderPool {
    private static volatile BinderPool sBinderPool;
    private Context mContext;
    private boolean isBind;
    private IBinderPool mIBinderPool;

    private BinderPool(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static BinderPool getInstance(Context context) {
        if (sBinderPool == null) {
            synchronized (BinderPool.class) {
                if (sBinderPool == null) {
                    sBinderPool = new BinderPool(context);
                }
            }
        }
        return sBinderPool;
    }

    /**
     * 绑定服务端的Service
     */
    public void connect() {
        Intent intent = new Intent();
        intent.setClassName("com.parting_soul.server", "com.parting_soul.server.BinderPoolService");
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 取消绑定
     */
    public void disconnect() {
        if (isBind && mIBinderPool != null && mIBinderPool.asBinder().isBinderAlive()) {
            mIBinderPool.asBinder().unlinkToDeath(mDeathRecipient, 0);
        }
        mContext.unbindService(mServiceConnection);
        isBind = false;
    }


    /**
     * 根据业务code 获取指定的IBinder
     *
     * @param code
     * @return
     */
    public IBinder queryBinder(int code) {
        IBinder binder = null;
        try {
            binder = mIBinderPool.queryBinder(code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return binder;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mIBinderPool = IBinderPool.Stub.asInterface(service);
                mIBinderPool.asBinder().linkToDeath(mDeathRecipient, 0);
                isBind = true;
                Log.d("服务连接成功");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBind = false;
        }
    };


    /**
     * 死亡代理
     */
    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            disconnect();
            connect();
        }
    };


}
