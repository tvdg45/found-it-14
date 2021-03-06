/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package org.tensorflow.lite.nnapi;

import org.tensorflow.lite.Delegate;
import org.tensorflow.lite.TensorFlowLite;

@SuppressWarnings("MissingJavadocMethod")
public class NnApiDelegate implements Delegate, AutoCloseable {

    private static final long INVALID_DELEGATE_HANDLE = 0;

    private long delegateHandle;

    public static final class Options {
        public Options() {}

        public static final int EXECUTION_PREFERENCE_UNDEFINED = -1;

        public static final int EXECUTION_PREFERENCE_LOW_POWER = 0;

        public static final int EXECUTION_PREFERENCE_FAST_SINGLE_ANSWER = 1;

        public static final int EXECUTION_PREFERENCE_SUSTAINED_SPEED = 2;

        public Options setExecutionPreference(int preference) {
            this.executionPreference = preference;
            return this;
        }

        public Options setAcceleratorName(String name) {
            this.acceleratorName = name;
            return this;
        }

        public Options setCacheDir(String cacheDir) {
            this.cacheDir = cacheDir;
            return this;
        }

        public Options setModelToken(String modelToken) {
            this.modelToken = modelToken;
            return this;
        }

        public Options setMaxNumberOfDelegatedPartitions(int limit) {
            this.maxDelegatedPartitions = limit;
            return this;
        }

        public Options setUseNnapiCpu(boolean enable) {
            this.useNnapiCpu = enable;
            return this;
        }

        public Options setAllowFp16(boolean enable) {
            this.allowFp16 = enable;
            return this;
        }

        private int executionPreference = EXECUTION_PREFERENCE_UNDEFINED;
        private String acceleratorName = null;
        private String cacheDir = null;
        private String modelToken = null;
        private Integer maxDelegatedPartitions = null;
        private Boolean useNnapiCpu = null;
        private Boolean allowFp16 = null;
    }

    public NnApiDelegate(Options options) {
        // Ensure the native TensorFlow Lite libraries are available.
        TensorFlowLite.init();
        delegateHandle =
                createDelegate(
                        options.executionPreference,
                        options.acceleratorName,
                        options.cacheDir,
                        options.modelToken,
                        options.maxDelegatedPartitions != null
                                ? options.maxDelegatedPartitions
                                : -1,
                        /*overrideDisallowCpu=*/ options.useNnapiCpu != null,
                        /*disallowCpuValue=*/ options.useNnapiCpu != null
                                ? !options.useNnapiCpu.booleanValue()
                                : true,
                        options.allowFp16 != null ? options.allowFp16 : false);
    }

    public NnApiDelegate() {
        this(new Options());
    }

    @Override
    public long getNativeHandle() {
        return delegateHandle;
    }

    @Override
    public void close() {
        if (delegateHandle != INVALID_DELEGATE_HANDLE) {
            deleteDelegate(delegateHandle);
            delegateHandle = INVALID_DELEGATE_HANDLE;
        }
    }

    public int getNnapiErrno() {
        checkNotClosed();
        return getNnapiErrno(delegateHandle);
    }

    public boolean hasErrors() {
        return getNnapiErrno(delegateHandle) != 0 /*ANEURALNETWORKS_NO_ERROR*/;
    }

    private void checkNotClosed() {
        if (delegateHandle == INVALID_DELEGATE_HANDLE) {
            throw new IllegalStateException("Should not access delegate after it has been closed.");
        }
    }

    //
    private static native long createDelegate(
            int preference,
            String deviceName,
            String cacheDir,
            String modelToken,
            int maxDelegatedPartitions,
            boolean overrideDisallowCpu,
            boolean disallowCpuValue,
            boolean allowFp16);

    private static native void deleteDelegate(long delegateHandle);

    private static native int getNnapiErrno(long delegateHandle);
}
