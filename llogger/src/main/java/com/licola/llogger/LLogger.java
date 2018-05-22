package com.licola.llogger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Log日志工具类
 * 1：支持打印类名，行号，方法等信息，且能够点击跳转到具体代码
 * 2：支持变长参数打印，json格式化打印
 * 3：支持log保存到本地文件，且使用时间间隔防止日志文件过长
 *
 * 基于：https://github.com/ZhaoKaiQiang/KLog项目改造
 */

public final class LLogger {

  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  private static final String DEFAULT_MESSAGE = "execute";
  private static final String ARGUMENTS = "argument";
  private static final String NULL = "null";

  private static final String SUFFIX_JAVA = ".java";
  static final String DEFAULT_TAG = "LLogger";
  private static final String TRACE_CLASS_END = "at com.licola.llogger.LLogger";

  private static final String DEFAULT_FILE_PREFIX = "LLogger_";
  private static final String FILE_FORMAT = ".log";

  public static final int V = 0x1;
  public static final int D = 0x2;
  public static final int I = 0x3;
  public static final int W = 0x4;
  public static final int E = 0x5;
  public static final int A = 0x6;

  private static int STACK_TRACE_INDEX_WRAP = 4;//线程的栈层级
  private static final int JSON_INDENT = 4;

  private static boolean mShowLog = true;
  private static String TAG = DEFAULT_TAG;
  private static File mLogFileDir = null;
  private static boolean mSaveLog = false;
  private static String FILE_PREFIX;

  private static Logger logger;

  static {
    boolean androidAvailable = AndroidLogger.isAndroidLogAvailable();
    logger = androidAvailable ? new AndroidLogger() : new JavaLogger();
    if (androidAvailable) {
      //android 环境的 StackTraceElement 多一层dalvik.system.VMStack
      STACK_TRACE_INDEX_WRAP++;
    }
  }

  /**
   * 配置方法
   *
   * @param showLog true：打印log false：不打印
   */
  public static void init(boolean showLog) {
    init(showLog, DEFAULT_TAG);
  }

  /**
   * 配置方法
   *
   * @param showLog showLog true：打印log false：不打印
   * @param tag log的tag显示
   */
  public static void init(boolean showLog, String tag) {
    mShowLog = showLog;
    TAG = tag;
  }

  /**
   * 配置方法
   *
   * @param showLog showLog true：打印log false：不打印
   * @param tag log的tag显示
   * @param logFileDir log
   */
  public static void init(boolean showLog, String tag, File logFileDir) {
    init(showLog, tag, logFileDir, DEFAULT_FILE_PREFIX);
  }

  public static void init(boolean showLog, String tag, File logFileDir, String logFilePrefix) {
    mShowLog = showLog;
    TAG = tag;
    checkLogDirFile(logFileDir);
    mSaveLog = true;
    mLogFileDir = logFileDir;
    FILE_PREFIX = logFilePrefix;
  }

  private static void checkLogDirFile(File logFileDir) {
    if (logFileDir == null) {
      throw new NullPointerException("logFileDir == null");
    }

    if (!logFileDir.exists()) {
      throw new IllegalArgumentException("logFileDir file not exists");
    }

    if (!logFileDir.isDirectory()) {
      throw new IllegalArgumentException("logFileDir must be dir try to re-create");
    }
  }

  public static void v() {
    printLog(V, DEFAULT_MESSAGE);
  }

  public static void v(Object msg) {
    printLog(V, msg);
  }

  public static void v(Object... objects) {
    printLog(V, objects);
  }

  public static void d() {
    printLog(D, DEFAULT_MESSAGE);
  }

  public static void d(Object msg) {
    printLog(D, msg);
  }

  public static void d(Object... objects) {
    printLog(D, objects);
  }

  public static void i() {
    printLog(I, DEFAULT_MESSAGE);
  }

  public static void i(Object msg) {
    printLog(I, msg);
  }

  public static void i(Object... objects) {
    printLog(I, objects);
  }

  public static void w() {
    printLog(W, DEFAULT_MESSAGE);
  }

  public static void w(Object msg) {
    printLog(W, msg);
  }

  public static void w(Object... objects) {
    printLog(W, objects);
  }

  public static void e() {
    printLog(E, DEFAULT_MESSAGE);
  }

  public static void e(Object msg) {
    printLog(E, msg);
  }

  public static void e(Object... objects) {
    printLog(E, objects);
  }

  public static void a() {
    printLog(A, DEFAULT_MESSAGE);
  }

  public static void a(Object msg) {
    printLog(A, msg);
  }

  public static void a(Object... objects) {
    printLog(A, objects);
  }

  public static void trace() {
    printStackTrace();
  }

  public static void json(JSONObject jsonObject) {
    printJson(jsonObject);
  }

  public static void json(JSONArray jsonArray) {
    printJson(jsonArray);
  }

  public static void json(String jsonFormat) {
    printJson(jsonFormat);
  }

  private static void printJson(Object object) {

    if (!mShowLog) {
      return;
    }

    String headString = wrapperContent(STACK_TRACE_INDEX_WRAP);

    String message = null;

    try {
      if (object instanceof JSONObject) {
        message = ((JSONObject) object).toString(JSON_INDENT);
      } else if (object instanceof JSONArray) {
        message = ((JSONArray) object).toString(JSON_INDENT);
      } else {
        message = object.toString();
      }
    } catch (JSONException e) {
      logger.log(E, TAG, getStackTraceString(e));
    }

    if (message != null) {
      message = headString + LLogger.LINE_SEPARATOR + message;
      logger.log(D, TAG, message);
    }

  }

  private static void printLog(int type, Object... objects) {
    if (!mShowLog) {
      return;
    }

    String headString = wrapperContent(STACK_TRACE_INDEX_WRAP);
    String msg = (objects == null) ? NULL : getObjectsString(objects);

    if (mSaveLog) {
      printFile(headString, msg);
    }

    logger.log(type, TAG, headString + msg);

  }

  private static void printFile(String headString, String msg) {

    long timeMillis = System.currentTimeMillis();
    SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.CHINA);
    String timeFormat = dateFormat.format(timeMillis);

    File logFile = makeLogFileWithTime(mLogFileDir, timeMillis);

    try {
      FileLog.printFile(logFile, timeFormat, headString, msg);
    } catch (IOException e) {
      logger.log(E, TAG, "log printFile failed :" + LINE_SEPARATOR + getStackTraceString(e));
    }
  }

  private static File makeLogFileWithTime(File LogFileDir, long timeMillis) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH", Locale.CHINA);
    String timeFormat = dateFormat.format(timeMillis);
    File file = new File(LogFileDir, FILE_PREFIX + timeFormat + FILE_FORMAT);
    if (!file.exists()) {
      try {
        file.createNewFile();
        logger.log(I, TAG, "create log file local:" + file.getAbsolutePath());
        return file;
      } catch (IOException e) {
        e.printStackTrace();
        logger.log(E, TAG, "log create file failed :" + LINE_SEPARATOR + getStackTraceString(e));
      }
    }
    return file;
  }

  public static String getStackTraceString(Throwable tr) {
    if (tr == null) {
      return "";
    }

    Throwable t = tr;
    while (t != null) {
      if (t instanceof UnknownHostException) {
        return "";
      }
      t = t.getCause();
    }

    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    tr.printStackTrace(pw);
    pw.flush();
    return sw.toString();
  }

  private static void printStackTrace() {
    if (!mShowLog) {
      return;
    }

    Throwable throwable = new Throwable();
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    throwable.printStackTrace(printWriter);
    printWriter.flush();
    String message = stringWriter.toString();

    String[] traces = message.split("\\n\\t");
    StringBuilder builder = new StringBuilder();
    builder.append("\n");
    for (String trace : traces) {
      if (trace.contains(TRACE_CLASS_END)) {
        continue;
      }
      builder.append(trace).append("\n");
    }

    String msg = builder.toString();
    String headString = wrapperContent(STACK_TRACE_INDEX_WRAP);
    logger.log(D, TAG, headString + msg);
  }


  private static String wrapperContent(int stackTraceIndex) {

    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

    StackTraceElement targetElement = stackTraceElements[stackTraceIndex];
    String classFileName = targetElement.getFileName();
    String className = targetElement.getClassName();

    String[] classNameInfo = className.split("\\.");
    if (classNameInfo.length > 0) {
      className = classNameInfo[classNameInfo.length - 1] + SUFFIX_JAVA;
    }

    String innerClassName = null;
    if (!classFileName.equals(className) && className.contains("$")) {
      //内部类
      int index = className.indexOf("$");
      innerClassName = className.substring(index);
    }

    String methodName = targetElement.getMethodName();
    int lineNumber = targetElement.getLineNumber();

    if (lineNumber < 0) {
      lineNumber = 0;
    }

    return "[ ("
        + classFileName
        + ':'
        + lineNumber
        + ')'
        + (innerClassName == null ? "#" : innerClassName + "#")
        + methodName
        + " ] ";
  }

  private static String getObjectsString(Object[] objects) {

    if (objects.length > 1) {
      StringBuilder builder = new StringBuilder();
      builder.append("\n");
      for (int i = 0, length = objects.length; i < length; i++) {
        Object object = objects[i];
        builder.append("\t").append(ARGUMENTS).append("[").append(i).append("]").append("=");
        if (object == null) {
          builder.append(NULL);
        } else {
          builder.append(object.toString());
        }
        if (i != length - 1) {
          builder.append("\n");
        }
      }
      return builder.toString();
    } else {
      Object object = objects[0];
      return object == null ? NULL : object.toString();
    }

  }

}
