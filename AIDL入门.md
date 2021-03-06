[TOC]

### 一. 概述

Android Interface Define Language(接口定义语言)，可以利用该语言定义服务端与客户端通信的接口。

使用场景： 不同的进程间的通信并且服务端进程可以并发处理通信任务。

### 二. 用法

#### 2.1 AIDL语法

- 需在 **aidl** 文件中使用Java语法来定义接口，且该 **aidl** 文件应该位于**src/**目录下
- aidl文件有两种类型：接口类型以及声明类型
  - 接口类型用于定义行为，具体指的是进程间通信服务端和客户端两端通信的行为(方法)
  - 声明类型指的是通信时可能会用到一些自定义的序列化对象，用于声明这些自定义对象

接口类型

```java
// IBookManager.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements
import com.parting_soul.server.Book;


interface IBookManager {

   void insert(in  Book book);

   List<Book> getBookLists();

}
```

声明类型 (注意声明类型的aidl文件必须和它描述的序列化实体类在同一个包名下)

```java
// Book.aidl
package com.parting_soul.server;

parcelable Book;
```

支持的数据类型

- Java中的基本数据类型(byte、bool、char、short、int、long、float、double)
- String
- CharSequence
- 实现了Parcelable接口的自定义类，需要导入实体类所在的包名
- List：List中支持的类型必须是上述类型，接收的类型是ArrayList
- Map：Map中支持的类型必须是上述的类型，但不支持泛型，接收的类型始终是HashMap

aidl接口定义规则：

- 方法可以带零个或者多个参数，有返回值或者返回void
- 非基本类型参数都需要指明数据走向的方向标记(in、out、inout)，默认是in
- aidl中的代码注释都会保留到编译生成的IBinder类中
- 可在aidl文件中定义String 常量以及int 常量(测试时发现无法声明中文字符串常量)
- 使用@nullable注释可空参数或者返回值

一个aidl的简单例子

```java
package com.parting_soul.server;

// Declare any non-default types here with import statements
import com.parting_soul.server.Book;


interface IBookManager {

   const int VERSION = 1;

   const String AIDL_NAME = "Book Service";

   void insert(in  Book book);

   List<Book> getBookLists();

   Map getBookNum();

   void getFirstBook(out Book book);

   void updateBookMsg(inout Book book);
}
```

- in: 方法参数默认的数据流方向，客户端将参数传入aidl方法，在服务端修改传递过来的参数不会影响客户端的参数值，和值传递的效果类似
- out: 客户端将对象传入aidl的方法，服务端接收时，该对象没有之前的数据，修改该对象的值，会改变客户端原对象的值
- inout : 参数对象完整的从客户端传递到服务端，在服务端修改对象的值，会同步改变客户端对象的值，和引用传递的效果类似

注： 使用out参数修饰形参时，需要为形参对应的类创建一个无参的构造方法以及readFromParcel方法；返回值不能用上述关键字修饰；基本类型以及String或者CharSequence只能用in修饰。

#### 2.2 示例

##### 1. 创建服务端aidl文件

首先在服务端创建aidl文件，声明要暴露出来的接口方法

![aidl文件目录](https://i.postimg.cc/Xqt8cDt0/image.png)

与aidl相关的文件可以单独建立一个文件夹存放，可以方便的拷贝整个文件夹到客户端

在build.gradle中指明aidl以及Java类的路径

```groovy
android{
   sourceSets {
        main {
            java.srcDirs = ['src/main/java', 'src/main/aidl']
            resources.srcDirs = ['src/main/java', 'src/main/aidl']
            aidl.srcDirs = ['src/main/aidl']
            res.srcDirs = ['src/main/res']
            assets.srcDirs = ['src/main/assets']
        }
    }
}
```

首先是实体类

- 实体类若需要在aidl方法中作为参数传递，则需要实现Parcelable接口
- 若实体类作为aidl方法形参并用out修饰时，必须创建一个无参的构造方法以及readFromParcel方法
- 若实体类作为aidl方法形参并用inout修饰时，必须创建一个readFromParcel方法

```java
public class Book implements Parcelable {
    private String name;
    private String description;

    // 若aidl形参使用out修饰必须要有无参构造方法
    public Book() {
    }

    public Book(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // 若aidl形参使用out,inout修饰必须要有无参构造方法
    protected Book(Parcel in) {
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        name = in.readString();
        description = in.readString();
    }

    public static final Creator<Book> CREATOR = new Creator<Book>() {
        @Override
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        @Override
        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(description);
    }

    @Override
    public String toString() {
        return "Book{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
```

在aidl文件中引用该实体类，首选需要定义声明实体类的aidl文件

注：该aidl文件对应的包名需和实体类的包名相同

```java
// Book.aidl
package com.parting_soul.server;

parcelable Book;
```

创建服务端需要暴露给客户端的接口方法

```java
// IBookManager.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements
import com.parting_soul.server.Book;


interface IBookManager {

   const int VERSION = 1;

   const String AIDL_NAME = "Book Service";

   void insert(in  Book book);

   List<Book> getBookLists();

   Map getBookNum();

   void getFirstBook(out Book book);

   void updateBookMsg(inout Book book);

}
```

##### 2. 编译生成aidl对应的实现类

点击Build -> Rebuild Project后会为每个aidl文件生成通信的实现类

![AIDL实现类生成路径](https://i.postimg.cc/Dwhx0RT9/Xnip2020-01-03-15-44-21.jpg)

##### 3. 创建服务端Service

创建服务端Service，在onBind方法中返回一个IBinder对象，这个IBinder是生成的IBookManager的一个内部抽象类，实现了IBookManager接口和IBinder接口

```java
public class BookManagerService extends Service {

    private List<Book> mBookSources = new ArrayList<>();

    private IBinder mBinder = new IBookManager.Stub() {
        @Override
        public void insert(Book book) throws RemoteException {
            Log.d(" insert book " + book);
            mBookSources.add(book);
        }

        @Override
        public void getFirstBook(Book book) throws RemoteException {
            book.setName("第一本书");
            book.setDescription("描述");
        }

        @Override
        public void updateBookMsg(Book book) throws RemoteException {
            book.setDescription("更新信息");
        }

        @Override
        public List<Book> getBookLists() throws RemoteException {
            return mBookSources;
        }

        @Override
        public Map getBookNum() throws RemoteException {
            Map<String, Integer> bookNumMap = new HashMap<>();
            for (Book book : mBookSources) {
                Integer integer = bookNumMap.get(book.getName());
                int bookNum = 0;
                if (integer != null) {
                    bookNum = integer;
                }
                bookNum++;
                bookNumMap.put(book.getName(), bookNum);
            }
            return bookNumMap;
        }

    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d("bind service");
        return mBinder;
    }

}
```

##### 4. 客户端aidl文件

把服务端整个aidl文件夹拷贝到客户端，同样位于main目录下。

![客户端aidl文件存放位置](https://i.postimg.cc/5y7fy8HG/Xnip2020-01-03-15-53-26.jpg)

同样需要指定aidl的位置

```java
android{
   sourceSets {
        main {
            java.srcDirs = ['src/main/java', 'src/main/aidl']
            resources.srcDirs = ['src/main/java', 'src/main/aidl']
            aidl.srcDirs = ['src/main/aidl']
            res.srcDirs = ['src/main/res']
            assets.srcDirs = ['src/main/assets']
        }
    }
}
```

拷贝完成后重新build一下项目，此时会生成客户端aidl文件的实现类

##### 5. 编写客户端与与服务端通信代码

```java
public class MainActivity extends AppCompatActivity {

    private IBookManager mBookManager;
    private boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 绑定服务
        Intent intent = new Intent();
        intent.setClassName("com.parting_soul.server", "com.parting_soul.server.BookManagerService");
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
    }

    public void onClick(View view) throws RemoteException {
        switch (view.getId()) {
            case R.id.bt_insert:
                insertBook();
                break;
            case R.id.bt_getBookLists:
                getBookList();
                break;
            case R.id.bt_getBookNum:
                getBookNum();
                break;
            case R.id.bt_getBook_first:
                getFirstBook();
                break;
            case R.id.bt_update_book:
                updateBook();
                break;
            default:
                break;
        }
    }
    
    private void updateBook() throws RemoteException {
        Book book1 = new Book("Java编程思想", "Java 书籍");
        Log.d("更新前：" + book1);
        mBookManager.updateBookMsg(book1);
        Log.d("更新后：" + book1);
    }

    private void getFirstBook() throws RemoteException {
        Book book = new Book();
        mBookManager.getFirstBook(book);
        Log.d(book.toString());
    }

    private void getBookNum() throws RemoteException {
        Map<String, Integer> map = mBookManager.getBookNum();
        if (map != null) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                Log.d("书籍名： " + entry.getKey() + " " + entry.getValue());
            }
        }
    }

    private void getBookList() throws RemoteException {
        List<Book> lists = mBookManager.getBookLists();
        Log.d(lists.toString());
    }

    private void insertBook() throws RemoteException {
        mBookManager.insert(new Book("Android 开发艺术探索", "技术"));
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d("service connected");
            mBookManager = IBookManager.Stub.asInterface(service);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
}
```

#### 3. 其他

##### 3.1 oneway

aidl中定义的方法可以用oneway修饰，但是用oneway修饰后方法不能有返回值

oneway修饰的方法在不同场景调用的方式不同

- 当客户端和服务端不再同一个进程中，使用oneway修饰的方法为异步方法，客户端不用等待服务端方法执行完成，直接返回
- 如果客户端可服务端在同一个进程中，使用oneway修饰的方法为同步方法

oneway修饰方法

```java
// IBookManager.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements
import com.parting_soul.server.Book;


interface IBookManager {
 	....
    
  oneway void getData();
}
```



