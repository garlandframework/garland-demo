package dev.garlandframework.demo.tests.support.common.factory;

import dev.garlandframework.http.model.HttpCallRequest;
import dev.garlandframework.http.model.MultipartBody;
import dev.garlandframework.demo.tests.support.base.Connections;

import java.io.IOException;
import java.nio.file.Path;

public final class TestFileRequests {

    private static final String URL = Connections.USER_SERVICE_URL + "/api/files";

    private TestFileRequests() {}

    public static HttpCallRequest<MultipartBody> uploadFromDisk(String description, Path file, String contentType) throws IOException {
        return HttpCallRequest.post(URL,
                new MultipartBody()
                        .field("description", description)
                        .file("file", file, contentType));
    }

    public static HttpCallRequest<MultipartBody> uploadFromBytes(String description, byte[] data, String filename, String contentType) {
        return HttpCallRequest.post(URL,
                new MultipartBody()
                        .field("description", description)
                        .file("file", data, filename, contentType));
    }
}
