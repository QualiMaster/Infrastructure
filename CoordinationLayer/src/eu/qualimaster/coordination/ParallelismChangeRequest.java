/*
 * Copyright 2009-2015 University of Hildesheim, Software Systems Engineering
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.qualimaster.coordination;

import java.io.Serializable;

import eu.qualimaster.common.QMInternal;
import eu.qualimaster.common.QMSupport;

/**
 * Specifies a change request in parallelism for a certain pipeline node.
 * 
 * @author Holger Eichelberger
 */
@QMSupport
public class ParallelismChangeRequest implements Serializable {

    public static final String ANY_HOST = "*";
    public static final int DELETE = Integer.MIN_VALUE + 1;
    @QMInternal
    public static final int FULFILLED = Integer.MIN_VALUE;
    private static final long serialVersionUID = -3340936266234669093L;
    private Boolean otherHostThenAssignment;
    private int executorDiff;
    private String host;

    /**
     * Creates a parallelism change request in terms of the desired new number of parallel executors on the same 
     * host.
     * 
     * @param executorDiff a positive number increases the number of parallel executors, a negative 
     *     number decreases, may be {@link #DELETE} to delete / kill all
     *     related executors.
     */
    public ParallelismChangeRequest(int executorDiff) {
        this(executorDiff, null);
    }
    
    /**
     * Creates a parallelism change request in terms of the desired new number of parallel executors on the given 
     * supervisor host.
     * 
     * @param executorDiff a positive number increases the number of parallel executors, a negative 
     *     number decreases, 0 indicates a migration of executors to a different host, may be {@link #DELETE} to 
     *     delete / kill all related executors.
     * @param host the host name as also known to Storm, may be <b>null</b> if the same host shall be used, may
     *     be {@link #ANY_HOST} for let infrastructure decide
     */
    public ParallelismChangeRequest(int executorDiff, String host) {
        this(executorDiff, host, null);
    }
    
    /**
     * Creates a parallelism change request in terms of the desired new number of parallel executors on the given 
     * supervisor host. [testing]
     * 
     * @param executorDiff a positive number increases the number of parallel executors, a negative 
     *     number decreases, 0 indicates a migration of executors to a different host (or a different 
     *     supervisor of <code>otherHostThanAssignment</code> is given), may be {@link #DELETE} to delete / kill all
     *     related executors.
     * @param host the host name as also known to Storm, may be <b>null</b> if the same host shall be used
     * @param otherHostThenAssignment select another host than given in the respective assignment if possible,
     *     intended for testing, <code>false</code> for the same, ignored for <b>null</b>), may
     *     be {@link #ANY_HOST} for let infrastructure decide
     */
    @QMInternal
    public ParallelismChangeRequest(int executorDiff, String host, Boolean otherHostThenAssignment) {
        this.executorDiff = executorDiff;
        this.host = null == host ? null : host.trim();
        this.otherHostThenAssignment = otherHostThenAssignment;
    }

    /**
     * Copies a parallelism change request.
     * 
     * @param source the source request
     */
    @QMInternal
    public ParallelismChangeRequest(ParallelismChangeRequest source) {
        this(source.executorDiff, source.host, source.otherHostThenAssignment);
    }

    /**
     * Returns the supervisor host.
     * 
     * @return the supervisor host (desired source or target for the modification), <b>null</b> if the same shall 
     *     be used
     */
    public String getHost() {
        return host;
    }
    
    /**
     * Returns whether the supervisor host is the pseudo name {@link #ANY_HOST} to be replaced by the infrastructure
     * with a free/adequate host if possible.
     * 
     * @return <code>true</code> if the infrastructure shall decide, <code>false</code> if the host name is fixed or 
     *     <b>null</b>
     */
    public boolean isAnyHost() {
        return ANY_HOST.equals(host);
    }

    /**
     * Returns the supervisor host selector in case of multiple supervisors on {@link #getHost()}.
     * 
     * @return the selector, ignored if negative
     */
    @QMInternal
    public Boolean otherHostThenAssignment() {
        return otherHostThenAssignment;
    }

    /**
     * Returns the executor difference to be realized.
     * 
     * @return a reduction in executors if negative, an increase if possible
     */
    public int getExecutorDiff() {
        return executorDiff;
    }
    
    /**
     * Changes the number of executors to indicate the remaining unfulfilled change in executors.
     * 
     * @param executorDiff the new difference
     */
    void setRemainingExecutorDiff(int executorDiff) {
        this.executorDiff = executorDiff;
    }
    
    @QMInternal
    @Override
    public boolean equals(Object obj) {
        boolean result = false;
        if (obj instanceof ParallelismChangeRequest) {
            ParallelismChangeRequest other = (ParallelismChangeRequest) obj;
            result = other.getExecutorDiff() == getExecutorDiff();
            if (null == other.getHost()) {
                result &= null == getHost();
            } else {
                result &= other.getHost().equals(getHost());
            }
            result &= other.otherHostThenAssignment() == otherHostThenAssignment();
        } else {
            result = false;
        }
        return result;
    }
    
    @QMInternal
    @Override
    public int hashCode() {
        return getExecutorDiff() + (null == getHost() ? 0 : getHost().hashCode()) 
            + hashCode(otherHostThenAssignment());
    }
    
    /**
     * Turns a boolean into a hash code considering <b>null</b>.
     * 
     * @param bool the boolean value (may be <b>null</b>)
     * @return the hash code
     */
    private static int hashCode(Boolean bool) {
        return null == bool ? 0 : bool.hashCode();
    }
    
    @QMInternal
    @Override
    public String toString() {
        return "[diff " + executorDiff + " host " + host + " " + otherHostThenAssignment + "]";
    }
    
}