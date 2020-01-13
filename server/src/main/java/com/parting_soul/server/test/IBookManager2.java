/*
 * This file is auto-generated.  DO NOT MODIFY.
 */
package com.parting_soul.server.test;

/**
 * 需要在Binder中进行传输的接口都要实现IInterface接口
 */
public interface IBookManager2 extends android.os.IInterface {


    // IBookManager2的默认实现类
    public static class Default implements IBookManager2 {
        @Override
        public void insert(com.parting_soul.server.Book book) throws android.os.RemoteException {
        }

        @Override
        public java.util.List<com.parting_soul.server.Book> getBookLists() throws android.os.RemoteException {
            return null;
        }
        // 注册书本变化回调

        @Override
        public void registerBookChangedCallback(com.parting_soul.server.OnBookChangedCallback callback) throws android.os.RemoteException {
        }

        @Override
        public void unregisterBookChangedCallback(com.parting_soul.server.OnBookChangedCallback callback) throws android.os.RemoteException {
        }

        @Override
        public android.os.IBinder asBinder() {
            return null;
        }
    }


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
                    data.enforceInterface(descriptor);
                    java.util.List<com.parting_soul.server.Book> _result = this.getBookLists();
                    reply.writeNoException();
                    reply.writeTypedList(_result);
                    return true;
                }
                case TRANSACTION_registerBookChangedCallback: {
                    data.enforceInterface(descriptor);
                    com.parting_soul.server.OnBookChangedCallback _arg0;
                    _arg0 = com.parting_soul.server.OnBookChangedCallback.Stub.asInterface(data.readStrongBinder());
                    this.registerBookChangedCallback(_arg0);
                    reply.writeNoException();
                    return true;
                }
                case TRANSACTION_unregisterBookChangedCallback: {
                    data.enforceInterface(descriptor);
                    com.parting_soul.server.OnBookChangedCallback _arg0;
                    _arg0 = com.parting_soul.server.OnBookChangedCallback.Stub.asInterface(data.readStrongBinder());
                    this.unregisterBookChangedCallback(_arg0);
                    reply.writeNoException();
                    return true;
                }
                default: {
                    return super.onTransact(code, data, reply, flags);
                }
            }
        }


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

            @Override
            public java.util.List<com.parting_soul.server.Book> getBookLists() throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                java.util.List<com.parting_soul.server.Book> _result;
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    boolean _status = mRemote.transact(Stub.TRANSACTION_getBookLists, _data, _reply, 0);
                    if (!_status && getDefaultImpl() != null) {
                        return getDefaultImpl().getBookLists();
                    }
                    _reply.readException();
                    _result = _reply.createTypedArrayList(com.parting_soul.server.Book.CREATOR);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
                return _result;
            }
            // 注册书本变化回调

            @Override
            public void registerBookChangedCallback(com.parting_soul.server.OnBookChangedCallback callback) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
                    boolean _status = mRemote.transact(Stub.TRANSACTION_registerBookChangedCallback, _data, _reply, 0);
                    if (!_status && getDefaultImpl() != null) {
                        getDefaultImpl().registerBookChangedCallback(callback);
                        return;
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterBookChangedCallback(com.parting_soul.server.OnBookChangedCallback callback) throws android.os.RemoteException {
                android.os.Parcel _data = android.os.Parcel.obtain();
                android.os.Parcel _reply = android.os.Parcel.obtain();
                try {
                    _data.writeInterfaceToken(DESCRIPTOR);
                    _data.writeStrongBinder((((callback != null)) ? (callback.asBinder()) : (null)));
                    boolean _status = mRemote.transact(Stub.TRANSACTION_unregisterBookChangedCallback, _data, _reply, 0);
                    if (!_status && getDefaultImpl() != null) {
                        getDefaultImpl().unregisterBookChangedCallback(callback);
                        return;
                    }
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public static IBookManager2 sDefaultImpl;
        }

        // 方法标识符
        static final int TRANSACTION_insert = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
        static final int TRANSACTION_getBookLists = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
        static final int TRANSACTION_registerBookChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
        static final int TRANSACTION_unregisterBookChangedCallback = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);

        public static boolean setDefaultImpl(IBookManager2 impl) {
            if (Stub.Proxy.sDefaultImpl == null && impl != null) {
                Stub.Proxy.sDefaultImpl = impl;
                return true;
            }
            return false;
        }

        public static IBookManager2 getDefaultImpl() {
            return Stub.Proxy.sDefaultImpl;
        }
    }

    public void insert(com.parting_soul.server.Book book) throws android.os.RemoteException;

    public java.util.List<com.parting_soul.server.Book> getBookLists() throws android.os.RemoteException;
    // 注册书本变化回调

    public void registerBookChangedCallback(com.parting_soul.server.OnBookChangedCallback callback) throws android.os.RemoteException;

    public void unregisterBookChangedCallback(com.parting_soul.server.OnBookChangedCallback callback) throws android.os.RemoteException;
}
