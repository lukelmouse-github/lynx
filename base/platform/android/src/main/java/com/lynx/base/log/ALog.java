package com.example.common.log;

import android.annotation.SuppressLint;
import android.app.Application;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * ALog - 增强的日志工具类
 * 功能：
 * 1. 包装Android系统Log的w,d,e,i方法
 * 2. 日志超长自动分行输出
 * 3. 自动通过反射获取调用者的文件名、函数名和行号
 */
public class ALog {
    private static final String TAG = "ALog";
    // 日志最大长度，超过此长度将自动分行
    private static final int MAX_LOG_LENGTH = 4000;
    private static final Object logLock = new Object();

    public static void dd(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.DEBUG, msg, info, false);
    }

    public static void dd(String tag, String msg) {
        String[] info = getCallerInfo();
        printLog(Log.DEBUG, tag, msg, info, false);
    }

    /**
     * 输出DEBUG级别日志
     * @param msg 日志内容
     */
    public static void d(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.DEBUG, msg, info, true);
    }
    /**
     * 输出DEBUG级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     */
    public static void d(String tag, String msg) {
        String[] info = getCallerInfo();
        printLog(Log.DEBUG, tag, msg, info, true);
    }
    /**
     * 输出DEBUG级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 异常
     */
    public static void d(String tag, String msg, Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.DEBUG, tag, msg + "\n" + Log.getStackTraceString(tr), info, true);
    }

    /**
     * 输出DEBUG级别日志，支持多个对象参数
     * @param objects 要打印的对象数组
     */
    public static void d(Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.DEBUG, msg.toString().trim(), info, true);
    }

    public static void d(String tag, Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.DEBUG, tag, msg.toString().trim(), info, true);
    }

    public static void ii(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.INFO, msg, info, false);
    }

    /**
     * 输出INFO级别日志
     * @param msg 日志内容
     */
    public static void i(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.INFO, msg, info, true);
    }
    /**
     * 输出INFO级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     */
    public static void i(String tag, String msg) {
        String[] info = getCallerInfo();
        printLog(Log.INFO, tag, msg, info, true);
    }

    /**
     * 输出INFO级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 异常
     */
    public static void i(String tag, String msg, Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.INFO, tag, msg + "\n" + Log.getStackTraceString(tr), info, true);
    }

    /**
     * 输出DEBUG级别日志，支持多个对象参数
     * @param objects 要打印的对象数组
     */
    public static void i(Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.INFO, msg.toString().trim(), info, true);
    }

    public static void i(String tag, Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.INFO, tag, msg.toString().trim(), info, true);
    }

    public static void ww(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.WARN, msg, info, false);
    }

    /**
     * 输出WARN级别日志
     * @param msg 日志内容
     */
    public static void w(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.WARN, msg, info, true);
    }
    /**
     * 输出WARN级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     */
    public static void w(String tag, String msg) {
        String[] info = getCallerInfo();
        printLog(Log.WARN, tag, msg, info, true);
    }
    /**
     * 输出WARN级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 异常
     */
    public static void w(String tag, String msg, Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.WARN, tag, msg + "\n" + Log.getStackTraceString(tr), info, true);
    }
    /**
     * 输出DEBUG级别日志，支持多个对象参数
     * @param objects 要打印的对象数组
     */
    public static void w(Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.WARN, msg.toString().trim(), info, true);
    }

    public static void w(String tag, Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.WARN, tag, msg.toString().trim(), info, true);
    }

    /**
     * 输出ERROR级别日志
     * @param msg 日志内容
     */
    public static void e(String msg) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, msg, info, true);
    }


    public static void ee(String tag, String msg) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, tag, msg, info, false);
    }

    /**
     * 输出ERROR级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     */
    public static void e(String tag, String msg) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, tag, msg, info, true);
    }
    /**
     * 输出ERROR级别日志
     * @param tag 日志标签
     * @param msg 日志内容
     * @param tr 异常
     */
    public static void e(String tag, String msg, Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, tag, msg + "\n" + Log.getStackTraceString(tr), info, true);
    }
    /**
     * 输出异常日志
     * @param tr 异常
     */
    public static void e(Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, Log.getStackTraceString(tr), info, true);
    }
    /**
     * 输出异常日志
     * @param tag 日志标签
     * @param tr 异常
     */
    public static void e(String tag, Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, tag, Log.getStackTraceString(tr), info, true);
    }

    /**
     * 输出DEBUG级别日志，支持多个对象参数
     * @param objects 要打印的对象数组
     */
    public static void e(Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.ERROR, msg.toString().trim(), info, true);
    }

    public static void e(String tag, Object... objects) {
        String[] info = getCallerInfo();
        StringBuilder msg = new StringBuilder();
        if (objects != null) {
            for (Object obj : objects) {
                msg.append(toString(obj)).append(" ");
            }
        }
        printLog(Log.ERROR, tag, msg.toString().trim(), info, true);
    }
    /**
     * 记录错误日志并抛出异常
     * @param msg 错误信息
     * @param tr 异常
     * @throws RuntimeException 总是抛出异常
     */
    public static void throwException(String msg, Throwable tr) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, msg + "\n" + Log.getStackTraceString(tr), info, true);

        // 抛出异常
        if (tr instanceof RuntimeException) {
            throw (RuntimeException) tr;
        } else {
            throw new RuntimeException(msg, tr);
        }
    }

    /**
     * 获取调用者信息
     * @return 包含文件名、方法名和行号的数组
     */
    private static String[] getCallerInfo() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // 查找调用ALog的栈帧
        int index = 0;
        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement e = stackTrace[i];
            String className = e.getClassName();
            if (className.equals(ALog.class.getName())) {
                index = i;
            } else if (index > 0 && !className.equals(ALog.class.getName())) {
                // 找到调用ALog的类
                StackTraceElement caller = stackTrace[i];
                String fileName = caller.getFileName();
                String methodName = caller.getMethodName();
                int lineNumber = caller.getLineNumber();
                return new String[]{fileName, methodName, String.valueOf(lineNumber)};
            }
        }
        return new String[]{"Unknown", "Unknown", "0"};
    }

    private static void printLog(int priority, String msg, String[] info, boolean addCallInfo) {
        printLog(priority, "", msg, info, addCallInfo);
    }

    public static String getProcessName() {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/self/cmdline"))) {
            String processName = reader.readLine().trim();
            // 处理可能的乱码或特殊字符（如末尾的\0）
            return processName.replaceAll("\\x00", "");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 打印日志，超长自动分行
     * @param priority 日志级别
     * @param tag 日志标签
     * @param msg 日志内容
     */
    @SuppressLint("DefaultLocale")
    private static void printLog(int priority, String tag, String msg, String[] info, boolean addCallInfo) {
        String firstLineTag = "(" + info[0] + ":" + info[2] + ")#" + info[1];
        if (!TextUtils.isEmpty(tag)) {
            firstLineTag = "[" + tag + "]" + firstLineTag;
        }
        // 获取线程信息
        Thread currentThread = Thread.currentThread();
        String threadName = currentThread.getName();
        long threadId = currentThread.getId();
        String processName = getProcessName();
        String shortProcessName = "M"; // 默认主进程
        int index = processName.lastIndexOf(":");
        if (index >= 0) {
            shortProcessName = processName.substring(index + 1);
        }

        /**
         * 组装日志
         * MAIN("M"),           // 主进程
                 UI相关操作
                 生命周期管理
                 页面路由
                 用户交互

         单个小程序默认用STANDARD进程
         多个小程序同时运行时,会分配到TASK_1、TASK_2等进程
         需要UI交互的部分在MAIN进程
         根据内存占用和性能需求动态分配
         */
        if (addCallInfo) {
            msg = firstLineTag + String.format("-[%s][Thread:%s-%d]", shortProcessName, threadName, threadId) +"\n" + msg;
        } else if (!TextUtils.isEmpty(tag)) {
            msg = "[" + tag + "]" + " " + msg;
        }

        // 检查消息长度，如果超过最大长度则分行输出
        int length = msg.length();
        if (length <= MAX_LOG_LENGTH) {
            Log.println(priority, TAG, msg);
            return;
        }

        synchronized (logLock) {
            // 分行输出 , 这里多线程的情况下，会破坏这个分段, 所以要加锁.
            int chunkCount = length / MAX_LOG_LENGTH;
            if (length % MAX_LOG_LENGTH > 0) {
                chunkCount++;
            }
            for (int i = 0; i < chunkCount; i++) {
                int start = i * MAX_LOG_LENGTH;
                int end = Math.min((i + 1) * MAX_LOG_LENGTH, length);
                String part = msg.substring(start, end);
                Log.println(priority, TAG, "[" + (i + 1) + "/" + chunkCount + "] " + part);
            }
        }
    }
    /**
     * 格式化对象为字符串
     * @param obj 对象
     * @return 格式化后的字符串
     */
    public static String toString(Object obj) {
        try {
            return (obj == null) ? "null" : obj.toString();
        } catch (Throwable e) {
            String message = "Failed to convert to log string, error: " + e.getMessage();
            throwException(message, e);
            return "{ " + message + " }";
        }
    }

    /**
     * 输出当前调用栈信息（ERROR级别，红色显示）
     */
    public static void trace() {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, getStackTraceString(), info, true);
    }

    /**
     * 输出当前调用栈信息（ERROR级别，红色显示）
     */
    public static void trace(String message) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, message + "\n" + getStackTraceString(), info, true);
    }

    /**
     * 输出当前调用栈信息（ERROR级别，红色显示）
     * @param tag 日志标签
     * @param message 附加消息
     */
    public static void trace(String tag, String message) {
        String[] info = getCallerInfo();
        printLog(Log.ERROR, tag, message + "\n" + getStackTraceString(), info, true);
    }

    /**
     * 获取当前线程的调用栈字符串
     * @return 格式化的调用栈字符串
     */
    private static String getStackTraceString() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder("Current stack trace:\n");

        // 跳过前几个元素（通常是Thread.getStackTrace, getStackTraceString, trace方法自身）
        boolean foundALog = false;
        boolean skipComplete = false;

        for (int i = 0; i < stackTrace.length; i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();

            // 跳过ALog类内部的调用
            if (className.equals(ALog.class.getName())) {
                foundALog = true;
                continue;
            }

            // 找到第一个非ALog的调用者后开始记录
            if (foundALog && !skipComplete) {
                skipComplete = true;
            }

            if (skipComplete) {
                sb.append("\tat ").append(element.toString()).append("\n");
            }
        }

        return sb.toString();
    }
}