package com.parting_soul.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemClock;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author parting_soul
 * @date 2020-01-04
 */
public class BookManagerService2 extends Service {
    private volatile boolean isDestroy;

    /**
     * 书本集合
     */
    CopyOnWriteArrayList<Book> mBookLists = new CopyOnWriteArrayList<>();

    /**
     * 回调集合
     */
    CopyOnWriteArrayList<OnBookChangedCallback> mBookChangedCallbacks = new CopyOnWriteArrayList<>();


    RemoteCallbackList<OnBookChangedCallback> mRemoteCallbackList = new RemoteCallbackList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        new Thread(new BookTask()).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IBookManager2.Stub() {
            @Override
            public void insert(Book book) throws RemoteException {
                mBookLists.add(book);
                notifyBookChanged(book);
            }

            @Override
            public List<Book> getBookLists() throws RemoteException {
                return mBookLists;
            }

            @Override
            public void registerBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
//                mBookChangedCallbacks.add(callback);
                mRemoteCallbackList.register(callback);
                Log.d("register IBinder " + callback.asBinder() + "");
                Log.d("register callback " + callback);
            }

            @Override
            public void unregisterBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
//                if (!mBookChangedCallbacks.contains(callback)) {
//                    Log.d("不存在该连接");
//                }
//                mBookChangedCallbacks.remove(callback);

                mRemoteCallbackList.unregister(callback);
                Log.d("当前剩余连接数： " + mBookChangedCallbacks.size());
                Log.d("unregister IBinder " + callback.asBinder() + "");
                Log.d("unregister callback " + callback);
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroy = true;
    }

    private void notifyBookChanged(Book book) {
//        for (OnBookChangedCallback callback : mBookChangedCallbacks) {
//            try {
//                callback.onNewBookArrived(book);
//            } catch (RemoteException e) {
//            }
//        }
        int n = mRemoteCallbackList.beginBroadcast();
        for (int i = 0; i < n; i++) {
            OnBookChangedCallback callback = mRemoteCallbackList.getBroadcastItem(i);
            try {
                callback.onNewBookArrived(book);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        mRemoteCallbackList.finishBroadcast();
    }


    //一个线程，每隔5s添加一本新书
    class BookTask implements Runnable {
        @Override
        public void run() {
            for (int i = 0; ; i++) {
                if (isDestroy) break;
                Book book = new Book("Android 书籍 " + i, "描述");
                mBookLists.add(book);
                notifyBookChanged(book);
                SystemClock.sleep(5 * 1000);
            }
        }
    }

}
