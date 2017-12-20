package com.example.jennydegtiar.backgroundimageservice.encryptor;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.ImageWriteException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by yonatan.katz on 12/20/2017.
 */
public class EncryptorFacade {

    public static void addPhoneToMetadata(String imagePath, String phoneNumber) throws IOException, ImageReadException, ImageWriteException {
        File sourceFile = new File(imagePath);
        String userComment = ImageMetadataMgr.getUserComment(sourceFile);
        if (userComment.contains(phoneNumber)) {
            return;
        }
        userComment = userComment + ";" + phoneNumber;
        File tempFile = File.createTempFile("tmp", "tmp");
        tempFile.deleteOnExit();
        if (userComment.length() > 256) {
            userComment = removeFirstNumbers(userComment);
        }
        ImageMetadataMgr.changeExifMetadata(sourceFile, tempFile, userComment);
        copy(tempFile, sourceFile);
        try {
            tempFile.delete();
        } catch ( Exception e) {
            e.printStackTrace();
        }
    }

    public static void copy(File src, File dst) throws IOException {
        try (InputStream in = new FileInputStream(src)) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    private static String removeFirstNumbers(String userComment) {
        String[] split = userComment.split(";");
        if (split.length < 3) {
            return userComment;
        }
        LinkedList<String> allPhones = new LinkedList<>();
        for (int i = 2; i < split.length; ++i) {
            allPhones.add(split[i]);
        }
        return allPhones.stream().collect( Collectors.joining( ";" ) );
    }

    public static List<String> getPhonesFromMetadata(String imagePath) throws IOException, ImageReadException {
        String userComment = ImageMetadataMgr.getUserComment(new File(imagePath));
        if (userComment == null || userComment.isEmpty()) {
            return new LinkedList<>();
        }
        String[] split = userComment.split(";");
        List<String> phones = new LinkedList<>();
        for (String phone : split) {
            if (phone != null && !phone.isEmpty()){
                phones.add(phone);
            }
        }
        return phones;
    }

    public static boolean isImageEncrypted(String imagePath) throws IOException, ImageReadException {
        String userComment = ImageMetadataMgr.getUserComment(new File(imagePath));
        return userComment != null && !userComment.isEmpty();
    }

    public static void encryptImage(String imagePath, String password, String phone) throws IOException, ImageWriteException, ImageReadException {
        JPEGEncryptor encryptor = new JPEGEncryptor(phone + password);
        encryptor.encryptFile(imagePath, imagePath);
        addPhoneToMetadata(imagePath, phone);
    }

    public static void decryptImage(String imagePath, String password, String sourcePhone, String myPhone) throws IOException, ImageWriteException, ImageReadException {
        JPEGEncryptor encryptor = new JPEGEncryptor(sourcePhone + password);
        encryptor.decryptFile(imagePath, imagePath);
        addPhoneToMetadata(imagePath, myPhone);
    }

}
