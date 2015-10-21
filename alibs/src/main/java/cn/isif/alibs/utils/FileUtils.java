package cn.isif.alibs.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.LinkedList;


public class FileUtils {
    private static final char SYSTEM_SEPARATOR = File.separatorChar;

    /**
     * retain ths source file creation date,copy file
     *
     * @param srcFile
     * @param destFile
     * @throws IOException
     */
    public static void copyFile(File srcFile, File destFile)
            throws IOException {
        copyFile(srcFile, destFile, true);
    }

    /**
     * Optional retain the source file creation date，copy file
     *
     * @param srcFile
     * @param destFile
     * @param preserveFileDate
     * @throws IOException
     */
    public static void copyFile(File srcFile, File destFile, boolean preserveFileDate)
            throws IOException {
        if (srcFile == null) {
            throw new NullPointerException("Source must not be null");
        }
        if (destFile == null) {
            throw new NullPointerException("Destination must not be null");
        }
        if (!srcFile.exists()) {
            throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
        }
        if (srcFile.isDirectory()) {
            throw new IOException("Source '" + srcFile + "' exists but is a directory");
        }
        if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
            throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
        }
        if ((destFile.getParentFile() != null) && (!destFile.getParentFile().exists()) &&
                (!destFile.getParentFile().mkdirs())) {
            throw new IOException("Destination '" + destFile + "' directory cannot be created");
        }

        if ((destFile.exists()) && (!destFile.canWrite())) {
            throw new IOException("Destination '" + destFile + "' exists but is read-only");
        }
        doCopyFile(srcFile, destFile, preserveFileDate);
    }

    /**
     * really method copy file
     *
     * @param srcFile
     * @param destFile
     * @param preserveFileDate
     * @throws IOException
     */
    private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate)
            throws IOException {
        if ((destFile.exists()) && (destFile.isDirectory())) {
            throw new IOException("Destination '" + destFile + "' exists but is a directory");
        }

        FileInputStream fis = null;
        FileOutputStream fos = null;
        FileChannel input = null;
        FileChannel output = null;
        try {
            fis = new FileInputStream(srcFile);
            fos = new FileOutputStream(destFile);
            input = fis.getChannel();
            output = fos.getChannel();
            long size = input.size();
            long pos = 0L;
            long count = 0L;
            while (pos < size) {
                count = size - pos > 52428800L ? 52428800L : size - pos;
                pos += output.transferFrom(input, pos, count);
            }
        } finally {
            IOUtils.closeQuietly(output);
            IOUtils.closeQuietly(fos);
            IOUtils.closeQuietly(input);
            IOUtils.closeQuietly(fis);
        }

        if (srcFile.length() != destFile.length()) {
            throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
        }

        if (preserveFileDate)
            destFile.setLastModified(srcFile.lastModified());
    }

    /**
     * delete file by quietly
     *
     * @param file
     * @return
     */
    public static boolean deleteQuietly(File file) {
        if (file == null)
            return false;
        try {
            if (file.isDirectory())
                cleanDirectory(file);
        } catch (Exception ignored) {
        }
        try {
            return file.delete();
        } catch (Exception ignored) {
        }
        return false;
    }

    /**
     * clean directory
     *
     * @param directory
     * @throws IOException
     */
    public static void cleanDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            String message = directory + " does not exist";
            throw new IllegalArgumentException(message);
        }

        if (!directory.isDirectory()) {
            String message = directory + " is not a directory";
            throw new IllegalArgumentException(message);
        }

        File[] files = directory.listFiles();
        if (files == null) {
            throw new IOException("Failed to list contents of " + directory);
        }

        IOException exception = null;
        for (File file : files) {
            try {
                forceDelete(file);
            } catch (IOException ioe) {
                exception = ioe;
            }
        }

        if (null != exception)
            throw exception;
    }

    /**
     * force Delete
     *
     * @param file
     * @throws IOException
     */
    public static void forceDelete(File file) throws IOException {
        if (file.isDirectory()) {
            deleteDirectory(file);
        } else {
            boolean filePresent = file.exists();
            if (!file.delete()) {
                if (!filePresent) {
                    throw new FileNotFoundException("File does not exist: " + file);
                }
                String message = "Unable to delete file: " + file;

                throw new IOException(message);
            }
        }
    }

    /**
     * delete directory
     *
     * @param directory
     * @throws IOException
     */
    public static void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        if (!isSymlink(directory)) {
            cleanDirectory(directory);
        }

        if (!directory.delete()) {
            String message = "Unable to delete directory " + directory + ".";

            throw new IOException(message);
        }
    }

    /**
     * is system file
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static boolean isSymlink(File file) throws IOException {
        if (file == null) {
            throw new NullPointerException("File must not be null");
        }
        if (isSystemWindows()) {
            return false;
        }
        File fileInCanonicalDir = null;
        if (file.getParent() == null) {
            fileInCanonicalDir = file;
        } else {
            File canonicalDir = file.getParentFile().getCanonicalFile();
            fileInCanonicalDir = new File(canonicalDir, file.getName());
        }

        return !fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile());
    }

    /**
     * check system with windows
     *
     * @return
     */
    static boolean isSystemWindows() {
        return SYSTEM_SEPARATOR == '\\';
    }

    /**
     * force mkdir
     *
     * @param directory
     * @throws IOException
     */
    public static void forceMkdir(File directory) throws IOException {
        if (directory.exists()) {
            if (!directory.isDirectory()) {
                String message = "File " + directory + " exists and is " + "not a directory. Unable to create directory.";

                throw new IOException(message);
            }
        } else if (!directory.mkdirs()) {
            if (!directory.isDirectory()) {
                String message = "Unable to create directory " + directory;

                throw new IOException(message);
            }
        }
    }

    /**
     * open file to outputStream
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static FileOutputStream openOutputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canWrite())
                throw new IOException("File '" + file + "' cannot be written to");
        } else {
            File parent = file.getParentFile();
            if ((parent != null) && (!parent.exists()) &&
                    (!parent.mkdirs())) {
                throw new IOException("File '" + file + "' could not be created");
            }
        }
        return new FileOutputStream(file);
    }

    /**
     * open file to inputStream
     *
     * @param file
     * @return
     * @throws IOException
     */
    public static FileInputStream openInputStream(File file) throws IOException {
        if (file.exists()) {
            if (file.isDirectory()) {
                throw new IOException("File '" + file + "' exists but is a directory");
            }
            if (!file.canRead())
                throw new IOException("File '" + file + "' cannot be read");
        } else {
            throw new FileNotFoundException("File '" + file + "' does not exist");
        }
        return new FileInputStream(file);
    }

    /**
     * copy inputStream to file
     *
     * @param source
     * @param destination
     * @throws IOException
     */
    public static void copyInputStreamToFile(InputStream source, File destination) throws IOException {
        try {
            FileOutputStream output = openOutputStream(destination);
            try {
                IOUtils.copy(source, output);
            } finally {
                IOUtils.closeQuietly(output);
            }
        } finally {
            IOUtils.closeQuietly(source);
        }
    }

    /**
     * Check whether a file is included in the file list
     *
     * @param files
     * @param directory
     * @param filter
     */
    private static void innerListFiles(Collection<File> files, File directory, FileFilter filter) {
        File[] found = directory.listFiles(filter);
        if (found != null)
            for (File file : found)
                if (file.isDirectory())
                    innerListFiles(files, file, filter);
                else
                    files.add(file);
    }


    public static Collection<File> listFiles(File directory, String suffix) {
        final String _suffix = "." + suffix;
        FileFilter filter = new FileFilter() {
            public boolean accept(File file) {
                if (file.isDirectory())
                    return true;
                String name = file.getName();
                int endLen = _suffix.length();
                if (name.regionMatches(true, name.length() - endLen, _suffix, 0, endLen)) {
                    return true;
                }
                return false;
            }
        };
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Parameter 'directory' is not a directory");
        }

        if (filter == null) {
            throw new NullPointerException("Parameter 'fileFilter' is null");
        }
        Collection<File> files = new LinkedList<File>();
        innerListFiles(files, directory, filter);
        return files;
    }

}