# From backport-util-concurrent-3.1.jar
-dontwarn sun.misc.Perf

# From commons-logging-1.2.jar - we use the android supplied version
-dontwarn org.apache.commons.logging.**

# ical4j uses groovy internally
-dontwarn net.fortuna.ical4j.model.ContentBuilder
-dontwarn net.fortuna.ical4j.model.**Factory

# We use our own CalendarContract with extra backwards compatibility support
-dontnote android.provider.CalendarContract**

# Dont rename stuff, we want stack traces to make sense in bug reports
-keepnames interface ** { *; }
-keepnames class ** { *; }
-keepnames enum ** { *; }
