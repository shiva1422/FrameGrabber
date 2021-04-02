package com.geekyengineer.framegrabber_screenrecorder;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

public class Transport implements Serializable {
  MediaProjection mediaProjection;
}
