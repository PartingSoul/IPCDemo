// Book.aidl
package com.parting_soul.server;

import com.parting_soul.server.Book;

interface OnBookChangedCallback {
    void onNewBookArrived(in Book book);
}
