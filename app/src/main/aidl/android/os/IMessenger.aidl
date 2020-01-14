package android.os;

oneway interface IMessenger {
    void send(in Message msg);
}