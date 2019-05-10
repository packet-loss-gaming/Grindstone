/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package gg.packetloss.grindstone.util.database;

import com.sk89q.commandbook.CommandBook;

import java.io.*;
import java.util.logging.Logger;

public class IOUtil {

    private static Logger log = CommandBook.logger();

    public static File getBinaryFile(File workingDir, String fileName) {

        return new File(workingDir.getPath() + "/" + fileName + ".dat");
    }

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


        try (
                FileOutputStream fos = new FileOutputStream(file);
                ObjectOutputStream oss = new ObjectOutputStream(fos)
        ) {
            oss.writeObject(object);
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
        try (
                FileInputStream fis = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(fis)
        ) {
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
        }

        return object;
    }
}
