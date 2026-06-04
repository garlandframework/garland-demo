package org.mtodemo.userservice.controller;

import org.mtodemo.userservice.dto.FileUploadResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @PostMapping
    public ResponseEntity<FileUploadResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename() : file.getName();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FileUploadResponse(filename, file.getSize()));
    }
}
