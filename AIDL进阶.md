[TOC]

### 一. AIDL方法传递AIDL接口

​	前面介绍了[AIDL的基本用法]([https://partingsoul.github.io/2020/01/03/AIDL%E5%85%A5%E9%97%A8/](https://partingsoul.github.io/2020/01/03/AIDL入门/))，其中提到，AIDL的方法形参可以是基本类型、String、Charquence、Parcelable、List以及Map，其实还可以是aidl接口类型。

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

![服务端剩余连接数](https://i.postimg.cc/vH6Vmsn6/binder.jpg)

客户端在退出时，会调用unregisterBookChangedCallback取消订阅，但发现服务端订阅数并没有变化。

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

![执行结果](https://i.postimg.cc/xjXRLBH9/binder-2.jpg)

我们发现客户端传递到unregisterBookChangedCallback的回调接口不在我们保存的客户端接口列表中，其实进程间的数据传递本质是对象的序列化与反序列化的过程，毕竟两个进程不是位于同一个虚拟机中，拥有不同的内存空间，所以Binder会将客户端的传递过来的对象进行序列化与反序列化，生成一个新的对象副本。

由此引出RemoteCallbackList，RemoteCallbackList是一个专门用于删除跨进程接口的类。

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

![IBinder](https://i.postimg.cc/9QhY5Mkf/aidl.jpg)

可以看到运行结果和我们的猜想一致，同一个aidl对象传递时，服务端中该aidl对象的IBinder是一样的。

### 二. 权限验证

有时候我们需要限制其他应用来绑定我们的服务，调用服务端的API，此时就需要用到权限验证。

#### 2.1 声明绑定Service必须要有的权限

在Service端声明一个权限

```xml
 <permission android:name="com.parting_soul.permission_BookManagerService" />
```

在Service组件添加启动或者绑定该服务所需的权限

```xml
<service
         android:name=".BookManagerService2"
         android:exported="true"
         android:permission="com.parting_soul.permission_BookManagerService" />
```

#### 2.2 在onTransact中做权限验证

onTransact 方法会在客户端远程调用服务端方法前被调用，可在该方法做包名校验。

```java
@Override
public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
  String packageName = null;
  String[] packages = getPackageManager().getPackagesForUid(getCallingUid());
  if (packages != null && packages.length > 0) {
    packageName = packages[0];
  }
  if (packageName == null ||
      !packageName.startsWith("com.parting_soul")) {
    Log.d("权限验证失败");
    return false;
  }

  return super.onTransact(code, data, reply, flags);
}
```

### 三. 异常情况重新连接服务

常会出现这种状况，客户端还需要使用远程的服务，但是远程服务因为一些原因突然死亡了，这时为了客户端的功能不受影响，需要重新连接远程的服务。

#### 3.1 onServiceDisconnected

该方法与服务的连接丢失时调用，通常是托管服务的进程被杀死了，可以在该方法中重新绑定服务，该方法在主线程中被回调。

```java
@Override
public void onServiceDisconnected(ComponentName name) {
  isBound = false;
  Log.d("disconnected");

  unbindService(mServiceConnection);
  bindService();
}
```

#### 3.2 死亡代理                                     

声明一个死亡代理对象，当服务端Binder死亡时binderDied方法会在客户端Binder线程中回调，此时可以移除之前的死亡代理，重新绑定服务。

```java
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
```

在服务绑定成功时，需要给服务端的Binder绑定死亡代理

```java
 @Override
public void onServiceConnected(ComponentName name, IBinder service) {
  Log.d("service connected");
  try {
    mBookManager = IBookManager2.Stub.asInterface(service);

    mBookManager.registerBookChangedCallback(mOnBookChangedCallback);
    mBookManager.asBinder().linkToDeath(mDeathRecipient, 0);
    isBound = true;
  } catch (RemoteException e) {
    e.printStackTrace();
  }
}
```

### 四. Binder连接池

​	在之前的做法中，一个使用AIDL的业务服务端需要创建一个Service，若多个业务都要使用AIDL，那么每一个使用AIDL的业务服务端都需要创建一个自己的Service，这样多个Service会使得应用看起开十分臃肿，而且不好管理，此时就可以用到Binder连接池，服务端只需要存在一个Service，客户端通过业务码去从服务端获取对应业务的IBinder。

![Binder连接池](https://i.postimg.cc/SRTvPG8n/binder.png)

#### 4.1 服务端

- 创建各种aidl文件以及具体业务的实现

计算服务

```java
// IComputer.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements

interface IComputer {

    int add(int a,int b);
}
```

具体的业务实现

```java
public static class ComputerImp extends IComputer.Stub {
  @Override
  public int add(int a, int b) throws RemoteException {
    return a + b;
  }
}
```

获取消息服务

```java
// IModel.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements

interface IModel {
    String getMessage();
}
```

具体的业务实现

```java
public static class ComputerImp extends IComputer.Stub {
  @Override
  public int add(int a, int b) throws RemoteException {
    return a + b;
  }
}
```

创建一个用于提供上述业务的IBinder的AIDL文件

```java
// IBinderPool.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements

interface IBinderPool {
    const int BINDER_CODE_NONE = 0;

    const int BINDER_CODE_COMPUTER = 1;

    const int BINDER_CODE_GET_MESSGE = 2;

    IBinder queryBinder(int code);
}
```

创建服务端的Service，根据客户端的业务code返回对应的Binder

```java
public class BinderPoolService extends Service {

    @Override
    public IBinder onBind(Intent intent) {
        return new IBinderPool.Stub() {
            @Override
            public IBinder queryBinder(int code) throws RemoteException {
                IBinder binder = null;
                switch (code) {
                    case IBinderPool.BINDER_CODE_COMPUTER:
                        binder = new ComputerImp();
                        break;
                    case IBinderPool.BINDER_CODE_GET_MESSGE:
                        binder = new ModelImp();
                        break;
                    default:
                        break;
                }
                return binder;
            }
        };
    }
}
```

#### 4.2 客户端

Binder连接池：主要用于与服务端建立连接以及提供获取对应业务Binder的API

```java
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
```

客户端调用(服务端的方法可能是耗时操作，因此放在子线程中)

```java
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
```

### 五. AIDL Java层源码分析

当客户端调用bindService时，服务端会返回一个包含服务端业务的Binder对象，客户端就可以通过该Binder对象调用服务端提供的服务。我们在Java层来看看它的实现方式。

在上述例子中，我们创建了一个IBookManager2 aidl接口，用于做进程间的通信。

我们声明了IBookManager2这个aidl文件后，编译项目，项目会在指定的文件夹中创建一个IBookManager2类。

![aidl类生成路径](https://i.postimg.cc/kGNDxnvj/aidl.jpg)

这个生成的IBookManager2类的UML类图如下：

![aidl生成类UML](https://i.postimg.cc/8C3bYHQ2/aidl.png)

可以看到有几个核心类：

- IBookManager2： 是一个接口，继承了IInterface，该类定义服务端开放给外部的接口，也就是aidl文件中定义的方法
- Stub： 是一个抽象类，实现了IBookManager2接口以及继承了Binder类，一般服务端Service的onBind方法返回该抽象类的子类实例，在子类示例中实现了具体服务端提供的服务。在客户端调用bindService时，服务端会将该Binder返回至客户端，使得客户端可以调用服务端提供的方法。
- Proxy: 是一个代理类，实现了IBookManager2接口，这个类作用在客户端，客户端通过该类去调用服务端的方法(客户端和服务端不在同一个进程中)

#### 5.1 服务端代码

首先看Stub类，服务端Service要给客户端提供服务，需要在onBind方法中返回一个IBinder对象，Stub是一个继承了Binder类，实现了IBookManager2的抽象类，在Service的onBind方法中需要返回一个该类的实现类，在实现类中书写具体的服务逻辑。

```java
/**
 * 继承了Binder类，实现了IBookManager2接口
 */
public static abstract class Stub extends android.os.Binder implements IBookManager2 {

  /**
   * Binder的唯一标识符
   */
  private static final String DESCRIPTOR = "com.parting_soul.server.IBookManager2";

  public Stub() {
    //给Binder添加文件描述符
    this.attachInterface(this, DESCRIPTOR);
  }

  /**
   * 将Binder对象转化为IBookManager2接口
   */
  public static IBookManager2 asInterface(android.os.IBinder obj) {
    if ((obj == null)) {
      return null;
    }
    //判断当前服务端与客户端是否在一个进程，若在一进程，会通过描述符找到Binder
    android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
    if (((iin != null) && (iin instanceof IBookManager2))) {
      // IInterface不为空表示服务端和客户端在同一个进程中
      return ((IBookManager2) iin);
    }
    //服务端和客户端不在同一个进程，返回一个用于和服务端通信的代理类
    return new Stub.Proxy(obj);
  }

  @Override
  public android.os.IBinder asBinder() {
    return this;
  }


  /**
         * 该方法在服务端被调用
         *
         * @param code  客户端调用方法的标识符
         * @param data  用于获取方法参数的Parcel
         * @param reply 用于写方法返回值的Parcel
         * @param flags flag为0标识正常的RPC
         * @return 返回false, 客户端的请求会失败
         * @throws android.os.RemoteException
         */
  @Override
  public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
    String descriptor = DESCRIPTOR;
    switch (code) {
      case INTERFACE_TRANSACTION: {
        reply.writeString(descriptor);
        return true;
      }
      case TRANSACTION_insert: {
        data.enforceInterface(descriptor);
        com.parting_soul.server.Book _arg0;
        if ((0 != data.readInt())) {
          // book 形参不为空，则从Parcel中反序列化出Book对象
          _arg0 = com.parting_soul.server.Book.CREATOR.createFromParcel(data);
        } else {
          _arg0 = null;
        }
        // 调用插入方法
        this.insert(_arg0);
        reply.writeNoException();
        return true;
      }
      case TRANSACTION_getBookLists: {
        ...
          return true;
      }
      case TRANSACTION_registerBookChangedCallback: {
        ...
          return true;
      }
      case TRANSACTION_unregisterBookChangedCallback: {
        ...
          return true;
      }
      default: {
        return super.onTransact(code, data, reply, flags);
      }
    }
  }

}

```

该类中有两个方法需要注意asInterface方法与onTransact方法

我们知道，要实现IPC，需要同时在服务端和客户端放入aidl文件，并且两者编译生成的aidl类代码是相同的，因此生成的代码中一部分服务端的API，一部分是客户端的API。

**asInterface方法：** 该方法主要是用于将IBinder转化为IBookManager2对象。若服务端和客户端不在同一个进程中，服务端和客户端调用会出现不同的情况，因此可以通过Binder的唯一标识符判断服务端和客户端是否在同一个进程中，若在同一个进程中，直接将Binder对象强转为IBookManager2；若不是，那么客户端需要通过Binder与服务端进行通信，则创建一个代理的IBookManager2对象，用于发起RPC操作。

```java
/**
   * 将Binder对象转化为IBookManager2接口
   */
public static IBookManager2 asInterface(android.os.IBinder obj) {
  if ((obj == null)) {
    return null;
  }
  //判断当前服务端与客户端是否在一个进程，若在一进程，会通过描述符找到Binder
  android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
  if (((iin != null) && (iin instanceof IBookManager2))) {
    // IInterface不为空表示服务端和客户端在同一个进程中
    return ((IBookManager2) iin);
  }
  //服务端和客户端不在同一个进程，返回一个用于和服务端通信的代理类
  return new Stub.Proxy(obj);
}
```

上述描述中出现了一个问题，为什么当客户端与服务端不再同一个进程中，客户端就无法通过Binder的唯一标识符获取Binder对象？其实客户端使用的IBinder是一个代理的IBinder对象。

```java
public final class BinderProxy implements IBinder {
    
    // 一个指针，指向native中Ibinder对象以及DeathRecipientList的内存空间
    private final long mNativeData;

    private BinderProxy(long nativeData) {
        mNativeData = nativeData;
    }

    // 发起RPC
    public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
    }


    // 根据Binder唯一标识符返回IInterface，这里始终返回null, 也就是为什么当客户端和服务端不在一个进程时，返回结果不同的原因
    public IInterface queryLocalInterface(String descriptor) {
        return null;
    }

    ...
}
```

客户端的IBinder对象其实BinderProxy对象，在该对象中，我们可以看到queryLocalInterface方法返回的是null，这也就是服务端和客户端不在同一个进程时调用queryLocalInterface有不同返回值的原因。

**onTransact方法：** 当客户端发起远程调用时，该方法会在服务端的Binder线程中被调用，该方法的返回值若为true，标识IPC成功，反之则调用失败。

这里以客户端调用insertBook方法为例

- 从写入形参的Parcel读出形参
- 调用具体的实现方法
- 将返回结果写入Parcel

```java
@Override
public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
    String descriptor = DESCRIPTOR;
    // code用于标识客户端调用的方法
    switch (code) {
        case INTERFACE_TRANSACTION: {
            reply.writeString(descriptor);
            return true;
        }
        case TRANSACTION_insert: {
            // 插入书籍的方法
            data.enforceInterface(descriptor);
            com.parting_soul.server.Book _arg0;
            if ((0 != data.readInt())) {
                // book 形参不为空，则从Parcel中反序列化出Book对象
                _arg0 = com.parting_soul.server.Book.CREATOR.createFromParcel(data);
            } else {
                _arg0 = null;
            }
            // 调用插入方法
            this.insert(_arg0);
            reply.writeNoException();
            return true;
        }
        ... 
    }
}
```

#### 5.2 客户端代码

现在来看客户端，客户端在绑定Service成功后，会获取到一个IBinder对象，该对象类型为BindProxy，是服务端返回Binder的代理类。由于客户端需要调用服务端提供的API，所以需要将该BindProxy对象转化为具体的接口类型。

```java
public static IBookManager2 asInterface(android.os.IBinder obj) {
  if ((obj == null)) {
    return null;
  }
  //判断当前服务端与客户端是否在一个进程，若在一进程，会通过描述符找到Binder
  android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
  if (((iin != null) && (iin instanceof IBookManager2))) {
    // IInterface不为空表示服务端和客户端在同一个进程中
    return ((IBookManager2) iin);
  }
  //服务端和客户端不在同一个进程，返回一个用于和服务端通信的代理类
  return new Stub.Proxy(obj);
}
```

从上述代码可以知道，当客户端与服务端不再同一个进程中，客户端得到的是一个Stub.Proxy对象，该对象实现了IBookManager2并且有一个为类型为BindProxy的成员属性。

```java
/**
 * 客户端与服务端通信的代理类，具体用于的通信对象是成员属性IBinder
 */
private static class Proxy implements IBookManager2 {
    // 用于通信的具体对象，该对象类型为BinderProxy
    private android.os.IBinder mRemote;

    Proxy(android.os.IBinder remote) {
        mRemote = remote;
    }

    @Override
    public android.os.IBinder asBinder() {
        return mRemote;
    }

    public String getInterfaceDescriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void insert(com.parting_soul.server.Book book) throws android.os.RemoteException {
        // 创建一个用于写入方法参数和返回值的包裹对象，该包裹对象可通过Binder发送
        android.os.Parcel _data = android.os.Parcel.obtain();
        android.os.Parcel _reply = android.os.Parcel.obtain();
        try {
            // 写入Binder唯一标识
            _data.writeInterfaceToken(DESCRIPTOR);
            if ((book != null)) {
                //参数不为空，则写入参数
                _data.writeInt(1);
                book.writeToParcel(_data, 0);
            } else {
                // 对象参数为空，用0标识
                _data.writeInt(0);
            }
            // 发起远程调用，同时当前线程被挂起
            boolean _status = mRemote.transact(Stub.TRANSACTION_insert, _data, _reply, 0);
            if (!_status && getDefaultImpl() != null) {
                // 若远程调用失败，使用默认的方式调用方法
                getDefaultImpl().insert(book);
                return;
            }
            // 读取返回值中的异常情况
            _reply.readException();
        } finally {
            _reply.recycle();
            _data.recycle();
        }
    }

 		...

    public static IBookManager2 sDefaultImpl;
}
```

客户端可以调服务端的方法，实际上是调用了Proxy类中的方法，例如insert方法，在该方法中创建一个用于写方法形参的Parcel以及一个用于写入返回值的Parcel对象，然后通过IBinder对象发起RPC，同时当前线程会被挂起(这也是客户端调用服务端方法时，尽可能的放在子线程中，防止主线程被阻塞发生ANR)。

#### 5.3 总结

![AIDL Java层流程](https://i.postimg.cc/0N8H9rhn/aidl-Java.png)

1. 客户端绑定服务端的Service，绑定成功后服务端返回一个代理的IBinder对象

2. 将服务端返回的IBinder对象转化成服务端API接口对象。若客户端和服务端在同一个进程中，直接将IBinder对象强转为接口对象；若不在一个进程中，返回一个让客户端调用服务端API的Proxy类
3. 客户端调用服务端API实际上是通过调用本地的Proxy类，将方法调用所需的形参写入Parcel，调用方法时，通过Binder发送该Parcel，通过客户端线程被挂起
4. 服务端的onTransact方法被调用，将方法形参从Parcel中读出，调用对应的方法，若有返回值，将返回值写入Parcel，完成服务端的调用
5. 客户端挂起的线程被唤醒，从Parcel中读出返回值