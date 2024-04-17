/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.statsdb;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;

public interface RemoteStatsDB {
    @ToString
    class Sync implements Serializable {
        @Serial
        private static final long serialVersionUID = 6835215675536753051L;

        public final ArrayList<NodeIdNode> data;
        public final String id;

        @JsonCreator
        public Sync( ArrayList<NodeIdNode> data, String id ) {
            this.data = data;
            this.id = id;
        }

        @JsonIgnore
        public final boolean isEmpty() {
            return data.isEmpty();
        }

        @ToString
        public static class NodeIdNode implements Serializable {
            @Serial
            private static final long serialVersionUID = 1612321099236706698L;

            public final NodeId nodeId;
            public final Node node;

            @JsonCreator
            public NodeIdNode( NodeId nodeId, Node node ) {
                this.nodeId = nodeId;
                this.node = node;
            }
        }
    }
}
