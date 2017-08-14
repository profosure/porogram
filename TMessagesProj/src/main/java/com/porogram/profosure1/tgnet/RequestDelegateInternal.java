package com.porogram.profosure1.tgnet;

public interface RequestDelegateInternal {
    void run(int response, int errorCode, String errorText, int networkType);
}
