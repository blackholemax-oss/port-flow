package dev.blackholemax.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储抽象：当前为本地磁盘实现，后续可无缝替换为 OSS 直传实现。
 */
public interface FileStorage {

    /**
     * 保存上传文件并返回可访问的相对路径（如 /uploads/xxx.jpg），文件为空返回 null。
     */
    String save(MultipartFile file);
}