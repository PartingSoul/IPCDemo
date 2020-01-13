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

//   void call(in Book a,in Book  b,in Book book);

}
