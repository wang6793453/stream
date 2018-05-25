/*
 * Copyright (C) 2018 zhengjun, fanwe (http://www.fanwe.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.fanwe.lib.stream;

/**
 * 流接口
 * <p>
 * 当流接口代理对象的方法被调用的时候，会触发和代理对象相同tag的流对象的相同方法
 */
public interface FStream
{
    /**
     * 返回当前流对象的tag
     *
     * @return
     */
    Object getTag();
}
