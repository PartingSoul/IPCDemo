package com.parting_soul.ipcdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.parting_soul.server.Book;
import com.parting_soul.server.IBookManager2;
import com.parting_soul.server.Log;
import com.parting_soul.server.OnBookChangedCallback;

import java.util.List;

/**
 * aidl进阶
 *
 * @author parting_soul
 * @date 2020-01-04
 */
public class AIDLAdvancedActivity extends AppCompatActivity {
    private boolean isBound;
    private IBookManager2 mBookManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_aidl_advanced);

        bindService();
    }

    private void bindService() {
        // 绑定服务
        Intent intent = new Intent();
        intent.setClassName("com.parting_soul.server", "com.parting_soul.server.BookManagerService2");
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public void onClick(View view) throws RemoteException {
        if (!isBound) {
            return;
        }
        switch (view.getId()) {
            case R.id.bt_insert:
                insertBook();
                break;
            case R.id.bt_getBookLists:
                getBookList();
                break;
            default:
                break;
        }
    }

    private void getBookList() throws RemoteException {
        List<Book> lists = mBookManager.getBookLists();
        Log.d(lists.toString());
    }

    //远程调用可能是耗时操作，实际上应该放在子线程中
    private void insertBook() throws RemoteException {
        mBookManager.insert(new Book("Android 开发艺术探索", "技术"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("bound = " + isBound + " " + mBookManager.asBinder().isBinderAlive());
        if (isBound && mBookManager != null && mBookManager.asBinder().isBinderAlive()) {
            try {
                Log.d(mOnBookChangedCallback.asBinder() + "");
                mBookManager.unregisterBookChangedCallback(mOnBookChangedCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // 无论远程服务有没有没杀死，都需要解绑，否则会造成内存泄漏
        unbindService(mServiceConnection);
        isBound = false;
        mServiceConnection = null;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("service connected " + service.getClass().getName());
            try {
                mBookManager = IBookManager2.Stub.asInterface(service);

                mBookManager.registerBookChangedCallback(mOnBookChangedCallback);
                mBookManager.asBinder().linkToDeath(mDeathRecipient, 0);
                isBound = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            Log.d("disconnected");

//            unbindService(mServiceConnection);
//            bindService();
        }
    };

    private OnBookChangedCallback mOnBookChangedCallback = new OnBookChangedCallback.Stub() {
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            //在binder线程中回调
            Log.d("新书到了: " + book.toString());

        }
    };

    private IBinder.DeathRecipient mDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            //当Binder死亡时，该方法会受收到回调
            Log.d("binderDied ");
            if (mBookManager == null) {
                return;
            }
            //移除绑定的死亡代理
            mBookManager.asBinder().unlinkToDeath(mDeathRecipient, 0);
            mBookManager = null;
            // 重新绑定服务
            bindService();
        }
    };

}
