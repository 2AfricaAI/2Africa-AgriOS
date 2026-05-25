package ai.toafrica.agrios.system.controller;

import ai.toafrica.agrios.common.R;
import ai.toafrica.agrios.system.service.FileService;
import ai.toafrica.agrios.system.vo.FileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "02 · 文件", description = "文件上传 / 下载 / 删除")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @Operation(summary = "上传文件 (后端代传)")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<FileVO> upload(
            @Parameter(description = "文件", required = true)
            @RequestPart("file") MultipartFile file,
            @Parameter(description = "业务分类, 例如 avatar / crop_image / activity_photo")
            @RequestParam(value = "bizType", required = false)
            @Schema(example = "avatar") String bizType) {
        return R.ok(fileService.upload(file, bizType));
    }

    @Operation(summary = "取文件元数据 (含预签名下载 URL)")
    @GetMapping("/{id}")
    public R<FileVO> get(@PathVariable Long id) {
        return R.ok(fileService.get(id));
    }

    @Operation(summary = "重新生成预签名下载 URL (仅返回 URL 字符串)")
    @GetMapping("/{id}/download-url")
    public R<String> downloadUrl(@PathVariable Long id) {
        return R.ok(fileService.presignedDownloadUrl(id));
    }

    @Operation(summary = "删除文件 (软删元数据 + 真删对象)")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        fileService.delete(id);
        return R.ok();
    }
}
