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
