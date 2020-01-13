package com.parting_soul.ipcdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.parting_soul.server.Book;
import com.parting_soul.server.IBookManager;
import com.parting_soul.server.Log;

import java.util.List;
import java.util.Map;

/**
 * @author parting_soul
 * @date 2020-01-03
 */
public class AIDLActivity extends AppCompatActivity {

    private IBookManager mBookManager;
    private boolean isBound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_aidl);

        // 绑定服务
        Intent intent = new Intent();
        intent.setClassName("com.parting_soul.server", "com.parting_soul.server.BookManagerService");
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        isBound = false;
        mServiceConnection = null;
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
            case R.id.bt_getBookNum:
                getBookNum();
                break;
            case R.id.bt_getBook_first:
                getFirstBook();
                break;
            case R.id.bt_update_book:
                updateBook();
                break;
            case R.id.bt_oneway:
                oneWay();
                break;
            default:
                break;
        }
    }

    private void oneWay() throws RemoteException {
        mBookManager.getData();
        Log.d("oneWay");
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
