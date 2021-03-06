package com.sd.lib.stream;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 流接口
 */
public interface FStream
{
    /**
     * 返回当前流对象的tag
     * <p>
     * 代理对象方法被触发的时候，会调用流对象的这个方法返回一个tag用于和代理对象的tag比较，tag相等的流对象才会被通知
     *
     * @param clazz 对应哪个接口的方法被触发
     * @return
     */
    default Object getTagForClass(Class<?> clazz)
    {
        return null;
    }

    class ProxyBuilder
    {
        Class mClass;
        Object mTag;
        DispatchCallback mDispatchCallback;

        /**
         * 设置代理对象的tag
         *
         * @param tag
         * @return
         */
        public ProxyBuilder setTag(Object tag)
        {
            mTag = tag;
            return this;
        }

        /**
         * 设置流对象方法分发回调
         *
         * @param callback
         * @return
         */
        public ProxyBuilder setDispatchCallback(DispatchCallback callback)
        {
            mDispatchCallback = callback;
            return this;
        }

        /**
         * 创建代理对象
         *
         * @param clazz
         * @param <T>
         * @return
         */
        public <T extends FStream> T build(Class<T> clazz)
        {
            if (clazz == null)
                throw new NullPointerException("clazz is null");
            if (!clazz.isInterface())
                throw new IllegalArgumentException("clazz must be an interface");
            if (clazz == FStream.class)
                throw new IllegalArgumentException("clazz must not be:" + FStream.class.getName());

            mClass = clazz;
            return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, new FStreamManager.ProxyInvocationHandler(this, FStreamManager.getInstance()));
        }
    }

    interface DispatchCallback
    {
        /**
         * 流对象的方法被通知的时候回调
         *
         * @param method       方法
         * @param methodParams 方法参数
         * @param methodResult 方法返回值
         * @param observer     流对象
         * @return true-停止分发，false-继续分发
         */
        boolean onDispatch(Method method, Object[] methodParams, Object methodResult, Object observer);
    }
}
