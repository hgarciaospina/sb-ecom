package com.ecommerce.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService {

    /**
     * Uploads an image file to the given path, assigning it a unique random name.
     *
     * @param path the destination folder path
     * @param file the image to upload
     * @return the new random file name
     * @throws IOException if an I/O error occurs
     */
    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        // Get original filename and its extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf('.'))
                : "";

        // Generate unique random file name
        String fileName = UUID.randomUUID().toString() + extension;

        // Create directory if not exists
        File dir = new File(path);
        if (!dir.exists()) dir.mkdirs();

        // Copy file to destination
        Path fullPath = Paths.get(path, fileName);
        Files.copy(file.getInputStream(), fullPath, StandardCopyOption.REPLACE_EXISTING);

        return fileName;
    }

    /**
     * Deletes an image file from the given path unless it's the default image.
     *
     * @param path the directory path where the image resides
     * @param imageName the image file name to delete
     * @throws IOException if deletion fails
     */
    @Override
    public void deleteImage(String path, String imageName) throws IOException {
        if (imageName == null || imageName.equals("default.png")) return;

        Path fullPath = Paths.get(path, imageName);
        if (Files.exists(fullPath)) {
            Files.delete(fullPath);
        }
    }
}