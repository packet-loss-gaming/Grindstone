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
        ObjectOutputStream oss = null;
        try {
            fos = new FileOutputStream(file);
            oss = new ObjectOutputStream(fos);
            oss.writeObject(object);
        } catch (FileNotFoundException e) {
            log.warning("Failed to find binary file: " + fileName + "!");
            log.warning(e.getMessage());
        } catch (IOException e) {
            log.warning("Failed to write binary file: " + fileName + "!");
            log.warning(e.getMessage());
        } finally {
            if (oss != null) {
                try {
                    oss.close();
                } catch (IOException e) {
                    log.warning("Could not close the Object Output Stream for binary file: " + fileName + "!");
                    log.warning(e.getMessage());
                }
            }
        }
    }

    public static Object readBinaryFile(File file) {

        Object object = null;
        FileInputStream fis;
        ObjectInputStream ois = null;
        try {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);

            object = ois.readObject();
        } catch (FileNotFoundException e) {
            log.warning("Failed to find a binary file!");
            log.warning(e.getMessage());
        } catch (ClassNotFoundException e) {
            log.warning("Couldn't find a compatible class for the binary file!");
            log.warning(e.getMessage());
        } catch (IOException e) {
            log.warning("Failed to read a binary file!");
            log.warning(e.getMessage());
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException e) {
                    log.warning("Could not close the Object Input Stream for the binary file!");
                    log.warning(e.getMessage());
                }
            }
        }

        return object;
    }
}
