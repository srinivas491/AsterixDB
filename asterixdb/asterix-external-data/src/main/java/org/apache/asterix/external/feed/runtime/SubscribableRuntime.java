/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.asterix.external.feed.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.asterix.external.feed.api.ISubscribableRuntime;
import org.apache.asterix.external.feed.api.ISubscriberRuntime;
import org.apache.asterix.external.feed.dataflow.DistributeFeedFrameWriter;
import org.apache.asterix.external.feed.management.FeedId;

public abstract class SubscribableRuntime extends FeedRuntime implements ISubscribableRuntime {

    protected static final Logger LOGGER = Logger.getLogger(SubscribableRuntime.class.getName());
    protected final FeedId feedId;
    protected final List<ISubscriberRuntime> subscribers;
    protected final DistributeFeedFrameWriter dWriter;

    public SubscribableRuntime(FeedId feedId, FeedRuntimeId runtimeId, DistributeFeedFrameWriter dWriter) {
        super(runtimeId);
        this.feedId = feedId;
        this.dWriter = dWriter;
        this.subscribers = new ArrayList<ISubscriberRuntime>();
    }

    public FeedId getFeedId() {
        return feedId;
    }

    @Override
    public String toString() {
        return "SubscribableRuntime" + " [" + feedId + "]" + "(" + runtimeId + ")";
    }

    public FeedRuntimeType getFeedRuntimeType() {
        return runtimeId.getFeedRuntimeType();
    }
}
