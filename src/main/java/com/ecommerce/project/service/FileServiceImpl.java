package com.ecommerce.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class FileServiceImpl implements FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        logger.info("Received file upload request. Original filename: {}", originalFileName);

        String randomId = UUID.randomUUID().toString();
        String fileExtension = originalFileName.substring(originalFileName.lastIndexOf('.'));
        String fileName = randomId + fileExtension;
        String filePath = path + File.separator + fileName;

        File folder = new File(path);
        if (!folder.exists()) {
            boolean dirCreated = folder.mkdirs();
            if (dirCreated) {
                logger.info("Directory '{}' did not exist and was successfully created.", path);
            } else {
                logger.warn("Directory '{}' did not exist and could NOT be created. Check permissions.", path);
            }
        } else {
            logger.debug("Upload directory '{}' already exists.", path);
        }

        logger.info("Saving file to: {}", filePath);
        Files.copy(file.getInputStream(), Paths.get(filePath));
        logger.info("File saved successfully as: {}", fileName);

        return fileName;
    }
}