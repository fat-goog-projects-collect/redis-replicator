/*
 * Copyright 2016-2018 Leon Chen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.moilioncircle.redis.replicator.cmd.impl;

import com.moilioncircle.redis.replicator.cmd.CommandSpec;

/**
 * @author Leon Chen
 * @since 3.5.0
 */
@CommandSpec(command = "XGROUP", subCommand = "CREATECONSUMER")
public class XGroupCreateConsumerCommand extends XGroupCommand {
    
    private static final long serialVersionUID = 1L;
    
    private byte[] group;
    private byte[] consumer;
    
    public XGroupCreateConsumerCommand() {
    }
    
    public XGroupCreateConsumerCommand(byte[] key, byte[] group, byte[] consumer) {
        super(key);
        this.group = group;
        this.consumer = consumer;
    }
    
    public byte[] getGroup() {
        return group;
    }
    
    public void setGroup(byte[] group) {
        this.group = group;
    }
    
    public byte[] getConsumer() {
        return consumer;
    }
    
    public void setConsumer(byte[] consumer) {
        this.consumer = consumer;
    }
}
