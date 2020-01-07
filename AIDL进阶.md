[TOC]

### 一. AIDL方法传递AIDL接口

​	前面介绍了[AIDL的基本用法](./AIDL入门)，其中提到，AIDL的方法形参可以是基本类型、String、Charquence、Parcelable、List以及Map。其实还可以是aidl接口类型。

​	还是之前书本的例子，若现在需要增加一个需求服务端有新书增加时，需要通知订阅的客户端有新书到来。若两者在同一个进程中，我们会使用观察者模式，声明一个通知书本增加的接口，客户端去注册这个接口，服务端保存所有订阅的客户端信息，然后服务端有书本更新时，遍历客户端信息，通知每一个客户端有新书更新。现在两者不再一个进程中，其实也是一样，只需要将用Java声明的接口改为aidl接口即可。

```java
// OnBookChangedCallback.aidl
package com.parting_soul.server;

import com.parting_soul.server.Book;

interface OnBookChangedCallback {
    void onNewBookArrived(in Book book);
}
```

声明注册接口的方法以及解注册的方法并使用上述aidl接口作为形参

```java
// IBookManager2.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements
import com.parting_soul.server.Book;
import com.parting_soul.server.OnBookChangedCallback;

interface IBookManager2 {
   void insert(in  Book book);

   List<Book> getBookLists();

   // 注册书本变化回调
   void registerBookChangedCallback(OnBookChangedCallback callback);

   void unregisterBookChangedCallback(OnBookChangedCallback callback);
}
```

服务端代码

- 服务端开启一个线程，每隔5s钟添加一本新书，并通知订阅了书籍变化的客户端
- 在客户端取消订阅时，移除客户端的接口回调
- 在客户端退出时，及时取消订阅并且解绑服务

```java
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
                mBookChangedCallbacks.add(callback);
            }

            @Override
            public void unregisterBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
                mBookChangedCallbacks.remove(callback);
                Log.d("当前剩余连接数： " + mBookChangedCallbacks.size());
            }
        };
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroy = true;
    }

    private void notifyBookChanged(Book book) {
        for (OnBookChangedCallback callback : mBookChangedCallbacks) {
            try {
                callback.onNewBookArrived(book);
            } catch (RemoteException e) {
            }
        }
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
```

客户端代码

- 客户端绑定服务端的服务
- 当服务绑定成功后，客户端订阅服务端的书籍变化

```java
public class AIDLAdvancedActivity extends AppCompatActivity {
    private boolean isBound;
    private IBookManager2 mBookManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_aidl_advanced);

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

        if (mBookManager != null && mBookManager.asBinder().isBinderAlive()) {
            try {
                mBookManager.unregisterBookChangedCallback(mOnBookChangedCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        if (isBound) {
            unbindService(mServiceConnection);
            isBound = false;
            mServiceConnection = null;
        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("service connected");
            try {
                mBookManager = IBookManager2.Stub.asInterface(service);

                mBookManager.registerBookChangedCallback(mOnBookChangedCallback);
                isBound = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    private OnBookChangedCallback mOnBookChangedCallback = new OnBookChangedCallback.Stub() {
        @Override
        public void onNewBookArrived(Book book) throws RemoteException {
            //在binder线程中回调
            Log.d("新书到了: " + book.toString());
        }
    };


}
```

在服务端执行指定客户端的取消订阅请求后打印了当前服务端持有的客户端订阅数，若当前客户端退出，可以看一下服务端的订阅数打印结果：

![服务端剩余连接数](https://i.postimg.cc/7h9tyDt3/Xnip2020-01-06-16-50-56.jpg)

客户端在退出时，回调用unregisterBookChangedCallback取消订阅，但发现服务端订阅数并没有变化。

```java
@Override
public void unregisterBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
  mBookChangedCallbacks.remove(callback);
  Log.d("当前剩余连接数： " + mBookChangedCallbacks.size());
}
```

稍微修改一下代码

```java
@Override
public void unregisterBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
  if (!mBookChangedCallbacks.contains(callback)) {
    Log.d("不存在该连接");
  }
  mBookChangedCallbacks.remove(callback);
  Log.d("当前剩余连接数： " + mBookChangedCallbacks.size());
}
```

再次进行同样的操作，可以看到执行结果

![执行结果](https://i.postimg.cc/V6VJ6ZG3/Xnip2020-01-06-17-39-05.jpg)

我们发现客户端传递到unregisterBookChangedCallback的回调接口不在我们保存的客户端接口列表中，其实进程间的数据传递本质是对象的序列化与反序列化的过程，毕竟两个进程不是位于同一个虚拟机中，拥有不同的内存空间，所以Binder会将客户端的传递过来的对象进行序列化与反序列化，生成一个新的对象副本。

#### 1. RemoteCallbackList

RemoteCallbackList是一个专门用于删除跨进程接口的类。

修改服务端代码，使用RemoteCallbackList注册和解注册aidl接口参数

```java
@Override
public void registerBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
  mRemoteCallbackList.register(callback);
}

@Override
public void unregisterBookChangedCallback(OnBookChangedCallback callback) throws RemoteException {
  mRemoteCallbackList.unregister(callback);
  Log.d("当前剩余连接数： " + mBookChangedCallbacks.size());
}
```

这边就有一个疑问了，RemoteCallbackList是怎么解决上述问题的，看下它的实现。

可以看到RemoteCallbackList内部维护了一个Map，Key为aidl接口对应的IBinder，键值为回调对象的包装类。

```java
public class RemoteCallbackList<E extends IInterface> {
    private static final String TAG = "RemoteCallbackList";

    //一个map,IBinder为键，回调的包装类为key
    @UnsupportedAppUsage
    /*package*/ ArrayMap<IBinder, Callback> mCallbacks
            = new ArrayMap<IBinder, Callback>();

    public boolean register(E callback, Object cookie) {
        synchronized (mCallbacks) {
            if (mKilled) {
                return false;
            }
            logExcessiveCallbacks();
            //获取aidl接口的IBinder对象
            IBinder binder = callback.asBinder();
            try {
                Callback cb = new Callback(callback, cookie);
                binder.linkToDeath(cb, 0);
                mCallbacks.put(binder, cb);
                return true;
            } catch (RemoteException e) {
                return false;
            }
        }
    }

    public boolean unregister(E callback) {
        synchronized (mCallbacks) {
            //移除key为IBinder对应的value
            Callback cb = mCallbacks.remove(callback.asBinder());
            if (cb != null) {
                cb.mCallback.asBinder().unlinkToDeath(cb, 0);
                return true;
            }
            return false;
        }
    }

}
```

难道同一个aidl对象传递的时候，虽然该对象会在服务端创建反序列化创建新的副本，但它的IBinder对象是一样的？

在注册和解注册时，我们打印下两个对象以及它对应的IBinder对象。

运行结果：

![IBinder](https://i.postimg.cc/4ycmCH7h/Xnip2020-01-07-16-57-40.jpg)

可以看到运行结果和我们的猜想一致，同一个aidl对象传递时，服务端中该aidl对象的IBinder是一样的。

