package gg.packetloss.grindstone.util.persistence;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sk89q.commandbook.CommandBook;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

public class SingleFileFilesystemStateHelper<T> {
    private final Gson gson = new Gson();

    private final Path stateFile;
    private final Type objectType;

    public SingleFileFilesystemStateHelper(String fileName, TypeToken<T> typeToken) throws IOException {
        this.stateFile = getStateFile(fileName);
        this.objectType = typeToken.getType();
    }

    private Path getStateFile(String fileName) throws IOException {
        Path baseDir = Path.of(CommandBook.inst().getDataFolder().getPath(), "state");
        Path statesDir = Files.createDirectories(baseDir.resolve("states"));
        return statesDir.resolve(fileName);
    }

    public Optional<T> load() throws IOException {
        if (!Files.exists(stateFile)) {
            return Optional.empty();
        }

        try (BufferedReader reader = Files.newBufferedReader(stateFile)) {
            return Optional.of(gson.fromJson(reader, objectType));
        }
    }

    public void save(T object) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(stateFile, CREATE, TRUNCATE_EXISTING)) {
            writer.write(gson.toJson(object, objectType));
        }
    }
}
