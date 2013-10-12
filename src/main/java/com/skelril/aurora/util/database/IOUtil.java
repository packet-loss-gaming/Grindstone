package com.skelril.aurora.util.database;

import com.sk89q.commandbook.CommandBook;

import java.io.*;
import java.util.logging.Logger;

/**
 * User: Wyatt Childers
 * Date: 10/12/13
 */
public class IOUtil {

    private static Logger log = CommandBook.logger();

    public static void toBinaryFile(File workingDir, String fileName, Object object) {

        fileName += ".dat";

        File file = new File(workingDir.getPath() + "/" + fileName);

        if (file.exists()) {
            File oldFile = new File(workingDir.getPath() + "/old-" + fileName);
            if (!oldFile.exists() || oldFile.delete()) {
                if (!file.renameTo(oldFile)) {
                    log.warning("Failed to rename binary file: " + fileName + "!");
                    return;
                }
            }
        }

        try {
            if (!workingDir.exists() && !workingDir.mkdirs()) {
                log.warning("Failed to create binary file: " + fileName + "!");
                return;
            }

            file.createNewFile();
        } catch (IOException e) {
            log.warning("Failed to create binary file: " + fileName + "!");
            log.warning(e.getMessage());
            return;
        }

        FileOutputStream fos;
        try {
            fos = new FileOutputStream(file);
            ObjectOutputStream oss = new ObjectOutputStream(fos);
            oss.writeObject(object);
            oss.close();
        } catch (FileNotFoundException e) {
            log.warning("Failed to find binary file: " + fileName + "!");
            log.warning(e.getMessage());
        } catch (IOException e) {
            log.warning("Failed to write binary file: " + fileName + "!");
            log.warning(e.getMessage());
        }
    }

    public static Object readBinaryFile(File file) {

        Object object = null;
        FileInputStream fis;
        try {
            fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            object = ois.readObject();

            ois.close();
        } catch (FileNotFoundException e) {
            log.warning("Failed to find a binary file!");
            log.warning(e.getMessage());
        } catch (ClassNotFoundException e) {
            log.warning("Couldn't find a compatible class for the binary file!");
            log.warning(e.getMessage());
        } catch (IOException e) {
            log.warning("Failed to read a binary file!");
            log.warning(e.getMessage());
        }

        return object;
    }
}
