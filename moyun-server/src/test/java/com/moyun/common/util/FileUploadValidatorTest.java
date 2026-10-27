package com.moyun.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FileUploadValidatorTest {

    @Test
    void imagePasses() {
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        assertNull(FileUploadValidator.validate(f, 1024));
    }

    @Test
    void exeRejected() {
        MockMultipartFile f = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1});
        assertNotNull(FileUploadValidator.validate(f, 1024));
    }

    @Test
    void oversizedRejected() {
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/png", new byte[2048]);
        assertNotNull(FileUploadValidator.validate(f, 1024));
    }

    @Test
    void emptyRejected() {
        MockMultipartFile f = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);
        assertNotNull(FileUploadValidator.validate(f, 1024));
    }
}
