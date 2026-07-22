package com.jadhavr.erp.storage;

public interface ObjectStorageService {

    void put(String key, byte[] content, String contentType);

    StoredObject get(String key);

    void delete(String key);

    record StoredObject(byte[] content, String contentType) {}
}
