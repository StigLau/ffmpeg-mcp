package no.lau.mcp.file;

import no.lau.mcp.ffmpeg.FileHasher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages file operations such as listing files, creating new files with auto-generated names,
 * and generating temporary files.
 * This class is designed for Java 23 applications.
 */
public class FileManagerImpl implements FileManager {

    public final Path sourceFolder;
    public final Path destinationFolder;
    private static final String DEFAULT_GENERATED_FILE_EXTENSION = ".mp4";
    private final Map<String, Path> videoReferences = new HashMap<>();

    private static final Logger log = LoggerFactory.getLogger(FileManagerImpl.class);

    /**
     * Constructs a FileManager instance.
     *
     * @param sourceFolderPath      The path to the source folder for listing files.
     * @param destinationFolderPath The path to the destination folder for creating new files.
     * @throws IOException if an I/O error occurs when creating directories.
     * @throws IllegalArgumentException if either path is not a directory after attempting creation.
     */
    public FileManagerImpl(String sourceFolderPath, String destinationFolderPath) {
        if (sourceFolderPath == null || sourceFolderPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Source folder path cannot be null or empty.");
        }
        if (destinationFolderPath == null || destinationFolderPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Destination folder path cannot be null or empty.");
        }

        // Store paths as absolute and normalized to ensure consistency
        this.sourceFolder = Paths.get(sourceFolderPath).toAbsolutePath().normalize();
        this.destinationFolder = Paths.get(destinationFolderPath).toAbsolutePath().normalize();

        // Ensure source folder exists, create if not
        if (Files.notExists(this.sourceFolder)) {
            try {
                Files.createDirectories(this.sourceFolder);
                //log.info("Created source folder: " + this.sourceFolder);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not create directory: " + sourceFolderPath, e);
            }
        } else if (!Files.isDirectory(this.sourceFolder)) {
            throw new IllegalArgumentException("Source path exists but is not a directory: " + sourceFolderPath);
        }

        // Ensure destination folder exists, create if not
        if (Files.notExists(this.destinationFolder)) {
            try {
                Files.createDirectories(this.destinationFolder);
                //log.info("Created destination folder: " + this.destinationFolder);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not create directory: " + destinationFolderPath, e);
            }
        } else if (!Files.isDirectory(this.destinationFolder)) {
            throw new IllegalArgumentException("Destination path exists but is not a directory: " + destinationFolderPath);
        }
        videoReferences.putAll(listFilesWithGeneratedKeys(sourceFolder));
    }

    /**
     * Lists all regular files in the source folder and returns them in a map.
     * The keys of the map are randomly generated UUIDs, and the values are the Paths to the files.
     * The 'mapName' parameter is a conceptual identifier for the map being generated,
     * potentially for logging or external reference.
     *
     * @return A Map where keys are generated String IDs and values are Path objects of the files.
     * @throws IOException if an I/O error occurs when reading the directory.
     */
    static Map<String, Path> listFilesWithGeneratedKeys(Path sourceFolder) {
        Map<String, Path> fileMap = new HashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceFolder)) {
            for (Path entry : stream) {
                if (Files.isRegularFile(entry)) {
                    String hashBrown = FileHasher.getMd5Hash(entry);
                    fileMap.put(hashBrown, entry.toAbsolutePath());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("Found " + fileMap.size() + " files in " + sourceFolder);
        return fileMap;
    }

    /**
     * Creates a new, empty file in the destination folder with an automatically generated unique name.
     * The generated filename will consist of a UUID and a default extension (e.g., ".dat").
     * This method is secure against path traversal attacks originating from filename input,
     * as the filename is internally generated.
     *
     * @return The Path to the newly created file (absolute and normalized).
     * @throws IOException if an I/O error occurs.
     * @throws SecurityException if, despite internal generation, the path resolution leads outside the destination (highly unlikely but checked).
     */
    public Path createNewFileWithAutoGeneratedNameInSecondFolder() throws IOException {
        // Step 1: Generate a unique filename.
        String generatedFileName = UUID.randomUUID() + DEFAULT_GENERATED_FILE_EXTENSION;

        // Step 2: Resolve the generated filename against the trusted destinationFolder.
        Path newFilePathAttempt = this.destinationFolder.resolve(generatedFileName);

        // Step 3: Normalize the resolved path. This is good practice, though with a UUID-based
        // filename, ".." or "." segments are not expected to be part of generatedFileName.
        Path normalizedNewFilePath = newFilePathAttempt.normalize();

        // Step 4: Convert to absolute path for consistent checking.
        Path absoluteNormalizedNewFilePath = normalizedNewFilePath.toAbsolutePath();

        // Step 5: SECURITY CHECK - This is a defense-in-depth measure.
        // Verify that the absolute, normalized path of the file to be created
        // *still* starts with the absolute, normalized path of the designated destination folder.
        // Given that the filename is generated, this check should virtually always pass
        // unless there's an issue with the destinationFolder path itself or an
        // unexpected interaction with the file system.
        if (!absoluteNormalizedNewFilePath.startsWith(this.destinationFolder)) {
            // This case is highly improbable with generated filenames but is kept for utmost safety.
            System.err.println("SECURITY ALERT: Path resolution unexpectedly led outside the destination folder with a generated filename.");
            System.err.println("Destination Folder: " + this.destinationFolder);
            System.err.println("Generated File Name: " + generatedFileName);
            System.err.println("Attempted Normalized Path: " + absoluteNormalizedNewFilePath);
            throw new SecurityException("Attempted to create file outside of the designated destination folder using an auto-generated name. This indicates a potential internal issue.");
        }

        //log.info("Securely creating new file with auto-generated name at: " + absoluteNormalizedNewFilePath);
        System.err.println("Securely creating new file with auto-generated name at: " + absoluteNormalizedNewFilePath);
        // Files.createFile will throw FileAlreadyExistsException if the file exists,
        // though with UUID-based names, collisions are extremely rare.
        return Files.createFile(absoluteNormalizedNewFilePath);
    }
     /**
     * Overloaded version of createNewFileWithAutoGeneratedNameInSecondFolder that allows specifying an extension.
     * Creates a new, empty file in the destination folder with an automatically generated unique name
     * and a user-specified extension.
     *
     */
    public Path createNewFileWithAutoGeneratedNameInSecondFolder(String fileref) throws IOException {
        Path absoluteNormalizedNewFilePath = createTemporaryFile(fileref, ".mp4").toAbsolutePath();
        videoReferences.putIfAbsent(fileref, absoluteNormalizedNewFilePath);

        if (!absoluteNormalizedNewFilePath.startsWith(this.destinationFolder)) {
            System.err.println("SECURITY ALERT: Path resolution unexpectedly led outside the destination folder.");
            System.err.println("Destination Folder: " + this.destinationFolder);
            System.err.println("Attempted Normalized Path: " + absoluteNormalizedNewFilePath);
            throw new SecurityException("Attempted to create file outside of the designated destination folder.");
        }
        return absoluteNormalizedNewFilePath;
    }

    @Override
    public Map<String, Path> videoReferences() {
        return videoReferences;
    }


    /**
     * Generates a temporary file with the given prefix and suffix in the system's default temporary-file directory.
     * The file is registered to be automatically deleted when the Java Virtual Machine terminates.
     *
     * @param prefix The prefix string to be used in generating the file's name; may be null.
     * @param suffix The suffix string to be used in generating the file's name; may be null (e.g., ".tmp").
     * @return The Path to the newly created temporary file.
     * @throws IOException if an I/O error occurs or if a file could not be created.
     */
    public Path createTemporaryFile(String prefix, String suffix) throws IOException {
        // Creates a temporary file in the system's default temporary-file directory.
        Path tempFile = Files.createTempFile(destinationFolder, prefix, suffix);
        log.info("Created temporary file: " + tempFile.toAbsolutePath());

        // Register the file for deletion on JVM exit.
        // Note: Deletion on exit is not guaranteed in all circumstances (e.g., JVM crash).
        tempFile.toFile().deleteOnExit();

        return tempFile;
    }

    public String replaceVideoReferences(String command)  {
        return FileManagerUtils.replaceVideoReferences(command, videoReferences);
    }
    

    @Override
    public void addTargetVideoReference(String id, Path path) {
        videoReferences.put(id, path);
    }
}

