/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ballerinalang.bre.bvm;

import org.ballerinalang.model.values.BValue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * {@code ChannelRegistryManager} is the registry for channel entries.
 *
 * @since 0.981
 */
public class ChannelRegistry {
    private static ChannelRegistry channelRegistry = null;
    private Map<String, Map<BValue, LinkedList<PendingContext>>> channelList;

    private ChannelRegistry() {
        channelList = new HashMap<>();
    }

    public static ChannelRegistry getInstance() {
        if (channelRegistry == null) {
            synchronized (ChannelRegistry.class) {
                if (channelRegistry == null) {
                    channelRegistry = new ChannelRegistry();
                }
            }
        }
        return channelRegistry;
    }

    /**
     * Add a new channel if not exist.
     * @param name Channel identifier
     */
    public void addChannel(String name) {
        channelList.computeIfAbsent(name, chn -> new HashMap<>());
    }

    /**
     * Add a worker context to the channels map that is waiting for a message.
     * @param channel Channel the worker is waiting for
     * @param key key of the message
     * @param ctx requested context
     * @param regIndex variable index to assign the channel message
     */
    public void addWaitingContext(String channel, BValue key, WorkerExecutionContext ctx, int regIndex) {
        //add channel if absent
        addChannel(channel);
        Map<BValue, LinkedList<PendingContext>> channelEntries = channelList.get(channel);
        LinkedList<PendingContext> ctxList = channelEntries.computeIfAbsent(key, bValue -> new LinkedList<>());

        PendingContext pContext = new PendingContext();
        pContext.regIndex = regIndex;
        pContext.context = ctx;
        ctxList.add(pContext);
        channelEntries.put(key, ctxList);
    }

    /**
     * return a {@code PendingContext} that is waiting on a message from the given channel.
     * @param channel Channel to check the waiting context
     * @param key message key
     * @return Worker context or null
     */
    public PendingContext pollOnChannel(String channel, BValue key) {
        //add channel if absent
        addChannel(channel);
        LinkedList<PendingContext> pendingCtxs = channelList.get(channel).get(key);
        if (pendingCtxs != null) {
            return pendingCtxs.poll();
        }
        return null;
    }

    /**
     * Represents a worker waiting for a data from a Channel.
     */
    public static class PendingContext {
        int regIndex;
        WorkerExecutionContext context;
    }
}
