package com.fanwe.stream;

import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;

import com.fanwe.lib.stream.FStream;
import com.fanwe.lib.stream.FStreamManager;

/**
 * Created by Administrator on 2018/2/9.
 */
public class TestTextView extends AppCompatTextView
{
    /**
     * 回调代理对象
     */
    private Callback mCallback = FStreamManager.getInstance().newProxyBuilder()
            .tag(null) // 默认tag为null
            .build(Callback.class);

    public TestTextView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                /**
                 * 调用回调代理对象的方法，从注册的流对象中得到一个返回值
                 *
                 * 注意：只有tag和当前代理对象tag相等的流对象才会被通知到，tag比较相等的规则为 “==” 或者 “equals”
                 */
                final int result = mCallback.getTextViewContent();
                setText(String.valueOf(result));
            }
        });
    }

    /**
     * 回调接口继承流接口
     */
    public interface Callback extends FStream
    {
        int getTextViewContent();
    }
}
