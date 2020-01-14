package com.parting_soul.server;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * @author parting_soul
 * @date 2020-01-14
 */
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


    public static class ComputerImp extends IComputer.Stub {
        @Override
        public int add(int a, int b) throws RemoteException {
            return a + b;
        }
    }

    public static class ModelImp extends IModel.Stub {

        @Override
        public String getMessage() throws RemoteException {
            return "message from server";
        }

    }

}
