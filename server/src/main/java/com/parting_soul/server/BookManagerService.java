package com.parting_soul.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author parting_soul
 * @date 2019-12-27
 */
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
