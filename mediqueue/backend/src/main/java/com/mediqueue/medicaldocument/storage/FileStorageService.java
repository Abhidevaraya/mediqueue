package com.mediqueue.medicaldocument.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface FileStorageService {

    void store(InputStream inputStream, String storageKey, String contentType, long size);

    Resource loadAsResource(String storageKey);

    void delete(String storageKey);
}
