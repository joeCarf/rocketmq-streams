/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.streams.examples.window;

import org.apache.rocketmq.common.MixAll;
import org.apache.rocketmq.streams.core.RocketMQStream;
import org.apache.rocketmq.streams.core.rstream.StreamBuilder;
import org.apache.rocketmq.streams.core.topology.TopologyBuilder;
import org.apache.rocketmq.streams.core.util.Pair;
import org.apache.rocketmq.streams.core.window.Time;
import org.apache.rocketmq.streams.core.window.WindowBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * 1、启动RocketMQ
 * 2、创建topic
 * 3、启动本例子运行
 * 4、向topic中写入数据
 * 5、观察输出结果
 */
public class WindowAvg {
    public static void main(String[] args) {
        StreamBuilder builder = new StreamBuilder("WindowAvg");
        builder.source("avgSource", source -> {
            String value = new String(source, StandardCharsets.UTF_8);
            Integer num = Integer.parseInt(value);
            return new Pair<>(null, num);
        }).foreach(value -> System.out.println(String.format("time:%s, input:%d", LocalTime.now(), value)))
        .filter(value -> value > 0)
        .keyBy(value -> "key")
        .window(WindowBuilder.tumblingWindow(Time.seconds(5)))
        .avg()
        .toRStream()
        .print();

        TopologyBuilder topologyBuilder = builder.build();

        Properties properties = new Properties();
        properties.putIfAbsent(MixAll.NAMESRV_ADDR_PROPERTY, "127.0.0.1:9876");

        RocketMQStream rocketMQStream = new RocketMQStream(topologyBuilder, properties);
        Runtime.getRuntime().addShutdownHook(new Thread("WindowAvg-shutdown-hook") {
            @Override
            public void run() {
                rocketMQStream.stop();
            }
        });

        rocketMQStream.start();
    }
}
