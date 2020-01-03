package com.parting_soul.server;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * @author parting_soul
 * @date 2019-12-27
 */
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
