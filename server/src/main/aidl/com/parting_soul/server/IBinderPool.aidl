// IBinderPool.aidl
package com.parting_soul.server;

// Declare any non-default types here with import statements

interface IBinderPool {
    const int BINDER_CODE_NONE = 0;

    const int BINDER_CODE_COMPUTER = 1;

    const int BINDER_CODE_GET_MESSGE = 2;

    IBinder queryBinder(int code);
}
