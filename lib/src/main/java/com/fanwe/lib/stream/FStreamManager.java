package com.fanwe.lib.stream;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by zhengjun on 2018/2/9.
 */
public class FStreamManager
{
    private static FStreamManager sInstance;

    private final Map<Class, List<FStream>> MAP_STREAM = new HashMap<>();
    private final Map<Class, NotifySession> MAP_NOTIFY_SESSION = new HashMap<>();

    private boolean mIsDebug;

    private FStreamManager()
    {
    }

    public static FStreamManager getInstance()
    {
        if (sInstance == null)
        {
            synchronized (FStreamManager.class)
            {
                if (sInstance == null)
                {
                    sInstance = new FStreamManager();
                }
            }
        }
        return sInstance;
    }

    public void setDebug(boolean debug)
    {
        mIsDebug = debug;
    }

    private String getLogTag()
    {
        return FStreamManager.class.getSimpleName();
    }

    public <T extends FStream> T newProxy(Class<T> clazz)
    {
        return newProxy(clazz, null);
    }

    public <T extends FStream> T newProxy(Class<T> clazz, Object tag)
    {
        if (!clazz.isInterface())
        {
            throw new IllegalArgumentException("clazz must be an interface");
        }
        if (clazz == FStream.class)
        {
            throw new IllegalArgumentException("clazz must not be:" + FStream.class.getName());
        }

        T proxy = (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new ProxyInvocationHandler(clazz, tag));
        if (mIsDebug)
        {
            Log.i(getLogTag(), "proxy created:" + clazz.getName() + " tag(" + tag + ")");
        }
        return proxy;
    }

    synchronized void register(FStream stream)
    {
        if (stream == null)
        {
            return;
        }

        final Class clazz = getStreamClass(stream);
        List<FStream> holder = MAP_STREAM.get(clazz);
        if (holder == null)
        {
            holder = new CopyOnWriteArrayList<>();
            MAP_STREAM.put(clazz, holder);
        }

        if (holder.contains(stream))
        {
            return;
        }
        if (holder.add(stream))
        {
            if (mIsDebug)
            {
                Log.i(getLogTag(), "register:" + stream + " (" + clazz.getName() + ") tag(" + stream.getTag() + ") " + (holder.size()));
            }
        }
    }

    synchronized void unregister(FStream stream)
    {
        if (stream == null)
        {
            return;
        }

        final Class clazz = getStreamClass(stream);
        final List<FStream> holder = MAP_STREAM.get(clazz);
        if (holder == null)
        {
            return;
        }

        if (holder.remove(stream))
        {
            if (mIsDebug)
            {
                Log.e(getLogTag(), "unregister:" + stream + " (" + clazz.getName() + ") tag(" + stream.getTag() + ") " + (holder.size()));
            }
        }

        if (holder.isEmpty())
        {
            MAP_STREAM.remove(clazz);
            MAP_NOTIFY_SESSION.remove(clazz);
        }
    }

    synchronized NotifySession getNotifySession(Class clazz)
    {
        if (!MAP_STREAM.containsKey(clazz))
        {
            throw new RuntimeException("can not call getNotifySession() before stream is register");
        }

        NotifySession session = MAP_NOTIFY_SESSION.get(clazz);
        if (session == null)
        {
            session = new NotifySession();
            MAP_NOTIFY_SESSION.put(clazz, session);
        }
        return session;
    }

    Class getStreamClass(FStream stream)
    {
        final Class[] arrInterface = stream.getClass().getInterfaces();
        if (arrInterface.length != 1)
        {
            throw new IllegalArgumentException("stream can only implements one interface");
        } else
        {
            return arrInterface[0];
        }
    }

    private final class ProxyInvocationHandler implements InvocationHandler
    {
        private final Class nClass;
        private final Object nTag;

        public ProxyInvocationHandler(Class clazz, Object tag)
        {
            nClass = clazz;
            nTag = tag;
        }

        private boolean checkTag(FStream stream)
        {
            final Object tag = stream.getTag();
            return nTag == tag;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            synchronized (FStreamManager.this)
            {
                final String methodName = method.getName();
                if ("register".equals(methodName)
                        || "unregister".equals(methodName)
                        || "getNotifySession".equals(methodName)
                        || "getTag".equals(methodName))
                {
                    throw new RuntimeException(methodName + " method can not be called on proxy instance");
                }

                Object result = null;

                //---------- main logic start ----------
                final List<FStream> holder = MAP_STREAM.get(nClass);
                if (holder != null)
                {
                    if (mIsDebug)
                    {
                        Log.i(getLogTag(), "notify method -----> " + method + " " + (args == null ? "" : Arrays.toString(args)) + " tag(" + nTag + ")");
                    }
                    final NotifySession session = getNotifySession(nClass);
                    session.reset();

                    int notifyCount = 0;
                    Object tempResult = null;
                    for (Object item : holder)
                    {
                        final FStream stream = (FStream) item;
                        if (checkTag(stream))
                        {
                            tempResult = method.invoke(item, args);
                            session.saveResult(stream, tempResult);
                            notifyCount++;

                            if (mIsDebug)
                            {
                                Log.i(getLogTag(), "notify " + notifyCount + " " + stream);
                            }
                        }
                    }
                    if (mIsDebug)
                    {
                        Log.i(getLogTag(), "notifyCount:" + notifyCount + " totalCount:" + holder.size());
                    }

                    final FStream stream = session.getRequestAsResultStream();
                    if (stream != null)
                    {
                        if (mIsDebug)
                        {
                            Log.e(getLogTag(), stream + " request as result");
                        }
                        result = session.getResult(stream);
                    } else
                    {
                        result = tempResult;
                    }
                    session.reset();
                }
                //---------- main logic end ----------

                final Class returnType = method.getReturnType();
                final String returnTypeName = returnType.getName();
                if (returnType.isPrimitive() && !"void".equals(returnTypeName) && result == null)
                {
                    if (mIsDebug)
                    {
                        Log.e(getLogTag(), "return type:" + returnTypeName + " but result:" + result);
                    }
                    result = 0;
                }

                if (mIsDebug)
                {
                    Log.i(getLogTag(), "notify result " + result);
                }

                return result;
            }
        }
    }
}
