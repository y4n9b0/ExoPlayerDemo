package com.y4n9b0.exoplayer;

import static com.google.android.exoplayer2.util.Util.castNonNull;

import android.os.Parcel;

import com.google.android.exoplayer2.metadata.Metadata;

public class SeiUserDataUnregistered implements Metadata.Entry {
    public final String payload;

    public SeiUserDataUnregistered(String payload) {
        this.payload = payload;
    }

    /* package */ SeiUserDataUnregistered(Parcel in) {
        this.payload = castNonNull(in.readString());
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SeiUserDataUnregistered{");
        sb.append("payload='").append(payload).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SeiUserDataUnregistered that = (SeiUserDataUnregistered) o;
        return payload.equals(that.payload);
    }

    @Override
    public int hashCode() {
        return payload.hashCode();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }

    public static final Creator<SeiUserDataUnregistered> CREATOR =
            new Creator<SeiUserDataUnregistered>() {

                @Override
                public SeiUserDataUnregistered createFromParcel(Parcel in) {
                    return new SeiUserDataUnregistered(in);
                }

                @Override
                public SeiUserDataUnregistered[] newArray(int size) {
                    return new SeiUserDataUnregistered[size];
                }
            };
}
