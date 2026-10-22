package android.media; public class AudioAttributes { public static final int USAGE_ALARM = 4, CONTENT_TYPE_SONIFICATION = 4;
  public static class Builder { public Builder setUsage(int u) { return this; } public Builder setContentType(int u) { return this; } public AudioAttributes build() { return new AudioAttributes(); } } }
