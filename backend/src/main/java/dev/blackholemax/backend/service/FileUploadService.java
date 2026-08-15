package dev.blackholemax.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileUploadService implements FileStorage {

    private static final String UPLOAD_DIR = "uploads";

    /**
     * 保存上传文件到项目根目录 uploads/ 下，返回可访问的相对路径（如 /uploads/xxx.jpg）。
     * 文件名为 UUID + 原文件扩展名，目录不存在时自动创建。
     */
    @Override
    public String save(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            Path uploadDir = Paths.get(UPLOAD_DIR).toAbsolutePath();
            if (Files.notExists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            // 用 InputStream + Files.copy 写入，绕过 Tomcat 临时目录解析问题
            Path dest = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), dest);
            return "/uploads/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("文件上传失败", e);
        }
    }
}