package net.mercnet.exifreader.controllers;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Controller
@Api(value="/uploadImage", description="Tool to dump the EXIF data from an image file")
public class UploadFile {
    private static final Logger LOG = LoggerFactory.getLogger(UploadFile.class);

    @Autowired
    public UploadFile() {

    }

    @GetMapping("/uploadImage")
    public String listUploadedFiles(Model model) {
        return "uploadForm";
    }

    @PostMapping("/processImage")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                         Model model) {

        final String filename = file.getOriginalFilename();

        LOG.info("Got file: {}", filename);

        if (filename.isEmpty()) {
            model.addAttribute("error", "Filename was empty");
            return "error";
        }

        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());

            final List<String> tagList = new ArrayList<>();
            final List<String> dirErrList = new ArrayList<>();

            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    final String tagStr = String.format("[ %s ] - %s = %s", directory.getName(), tag.getTagName(), tag.getDescription());
                    tagList.add(tagStr);
                    LOG.info("filename: {} {}", filename, tagStr);
                }
                if (directory.hasErrors()) {
                    for (String error : directory.getErrors()) {
                        final String errStr = String.format("[ %s ] - %s", directory.getName(), error);
                        LOG.info("filename: {} ERROR: {}", filename, errStr);
                        dirErrList.add(errStr);
                    }
                }
            }

            model.addAttribute("message", "Tags for file: " + filename);
            model.addAttribute("tags", tagList);
            if (!dirErrList.isEmpty()) {
                model.addAttribute("errors", dirErrList);
            }

        } catch (Exception e) {
            LOG.error("Got exception loading metadata for file: {}", filename, e);
            model.addAttribute("error", "Exception" + e.getLocalizedMessage());
            return "error";
        }
        return "results";
    }

}
