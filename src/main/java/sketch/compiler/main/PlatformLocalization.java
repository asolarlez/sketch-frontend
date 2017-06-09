package sketch.compiler.main;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.CodeSource;
import java.util.Properties;
import java.util.Vector;
import java.util.regex.Pattern;

import static sketch.util.DebugOut.assertFalse;
import static sketch.util.DebugOut.printDebug;
import static sketch.util.DebugOut.printError;
import static sketch.util.DebugOut.printWarning;

import sketch.compiler.main.cmdline.SketchOptions;
import sketch.util.exceptions.SketchSolverException;

/**
 * get any variables related to this specific compile, e.g. version number, and resolve
 * the path to the cegis binary.
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class PlatformLocalization {
    protected static PlatformLocalization singleton;
    public String version = "1.7.4";
    public String osname = System.getProperty("os.name");
    public String osarch = System.getProperty("os.arch");
    public Properties localization;
    public File usersketchdir;
    protected static String CEGIS_NAME = "cegis";
    /** only extract to a secure (not shared) location */
    public File tmpdir;
    /**
     * if this is false, fake values will be filled in for version, etc. so other code
     * doesn't fail.
     */
    public boolean isSet = false;

    public static PlatformLocalization getLocalization() {
        if (singleton == null) {
            singleton = new PlatformLocalization();
        }
        return singleton;
    }

    public PlatformLocalization() {
        // try to read the localization.properties file...
        localization = new Properties();
        URL resource = getCompilerRc("localization.properties");
        try {
            if (resource != null) {
                localization.load(resource.openStream());
                if (!localization.getProperty("version").contains("$")) {
                    version = localization.getProperty("version");
                    osname = System.getProperty("os.name");// localization.getProperty("osname");
                    osarch = System.getProperty("os.arch");// localization.getProperty("osarch");
                    isSet = true;
                } else {
                    printWarning("couldn't read SKETCH info from localization file");
                }
            } else {
                printWarning("SKETCH localization file doesn't exist! "
                        + "You may have to specify the CEGIS path (try '-h')");
            }
        } catch (IOException e) {
            printError("error retriving localization properties");
        }
    }

    public void setTempDirs() {
        String argtmpdir = SketchOptions.getSingleton().feOpts.tempdir;
        if (argtmpdir != null) {
            usersketchdir = md(path(argtmpdir));
            if (usersketchdir == null) {
                System.out.println("You asked to use temp directory " + argtmpdir +
                        " but I can't write to it.");
            }
        } else {
            String homedir = System.getProperty("user.home");
            usersketchdir = md(path(homedir, ".sketch"));
            if (homedir == null || usersketchdir == null) {
                System.out.println("Java claims your home directory is: " +
                        homedir +
                        "\n but I can't write to it. This may be related to this bug in java: http://bugs.sun.com/view_bug.do?bug_id=4787931");
            }
        }
        tmpdir = md(path(usersketchdir, "tmp"));
        if(tmpdir == null){
            System.out.println("Can't create your temp file in " + usersketchdir);
        }
    }

    public File get_jarpath() {
        File jarpath = null;
        CodeSource codesource = getClass().getProtectionDomain().getCodeSource();
        if (codesource != null) {
            URL location = codesource.getLocation();
            if (location != null) {
                jarpath = path(location.getFile());
                if (jarpath != null && jarpath.isFile()) {
                    jarpath = jarpath.getParentFile();
                }
            }
        }
        return jarpath;
    }

    public File load_from_jar(String cegisName) {
        try {
            URL rc_url = getCompilerRc(cegisName);
            if (rc_url != null) {
                InputStream fileIn = getCompilerRc(cegisName).openStream();
                if (fileIn.available() > 0) {
                    return loadTempFile(fileIn, cegisName);
                }
            }
        } catch (IOException e) {}
        return null;
    }

    public String getCegisPath() {
        SketchOptions options = SketchOptions.getSingleton();
        if (options.feOpts.cegisPath != null) {
            return options.feOpts.cegisPath;
        } else {
            return getCegisPathInner(CEGIS_NAME);
        }
    }

    public abstract class ResolvePath {
        public final String name;
        protected final File[] otherPaths;

        public ResolvePath(String name, File[] otherPaths) {
            this.name = name;
            this.otherPaths = otherPaths;
        }

        public File[] searchJarfile() {
            File[] rv = {};
            if (platformMatchesJava() && tmpdir != null) {
                // try to get it from the jar
                File[] r = { load_from_jar(this.name) };
                rv = r;
            } else if (isSet && tmpdir != null) {
                System.err.println("Your system doesn't match the " +
                        "localization strings of the SKETCH jar: " + osname + ", " +
                        osarch);
                System.err.println("os.arch=" + System.getProperty("os.arch") + "os=" +
                        System.getProperty("os.name"));
            }
            return rv;
        }

        public File[] searchJarpath() {
            File jarpath = get_jarpath();
            File[] r =
                    { path(jarpath, "cegis", "src", "SketchSolver", this.name),
                            path(jarpath, this.name) };
            return r;
        }

        public File[] searchEnvVar(String... envVars) {
            Vector<File> all_files = new Vector<File>();
            for (String envVar : envVars) {
                String envString = System.getenv(envVar);
                if (envString == null) {
                    continue;
                }
                for (String pathDir : envString.split(File.pathSeparator)) {
                    addEnvVarDirToVec(all_files, pathDir);
                }
            }
            return all_files.toArray(new File[0]);
        }

        /** override me */
        public void addEnvVarDirToVec(Vector<File> vec, String pathDir) {
            vec.add(path(pathDir, this.name));
        }

        public abstract File[] searchEnvVars();

        public File[] fromDefaultPaths(String... paths) {
            Vector<File> all_files = new Vector<File>();
            for (String path : paths) {
                String[] v = path.split(Pattern.quote(File.separator));
                String[] v2 = new String[v.length];
                for (int a = 1; a < v.length; a++) {
                    v2[a - 1] = v[a];
                }
                v2[v.length - 1] = this.name;
                all_files.add(path(v[0], v2));
            }
            all_files.add(path(usersketchdir, this.name + "-" + version));
            all_files.add(path(usersketchdir, this.name));
            return all_files.toArray(new File[0]);
        }

        public abstract File[] searchDefaultPaths();

        public String resolve() {
            File[][] paths =
                    { otherPaths, searchJarfile(), searchJarpath(), searchDefaultPaths(),
                            searchEnvVars() };
            return doResolve(paths);
        }

        public String doResolve(File[][] paths) {
            SketchOptions options = SketchOptions.getSingleton();
            for (File[] lst : paths) {
                for (File file : lst) {
                    if (options.debugOpts.verbosity > 5) {
                        System.out.println("searching for file " + file);
                    }
                    if (file != null && file.isFile()) {
                        try {
                            if (options.debugOpts.verbosity > 2) {
                                System.out.println("resolved " + this.name + " to path " +
                                        file.getCanonicalPath());
                            }
                            return file.getCanonicalPath();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            return this.name;
        }
    }

    public class ResolveFromPATH extends ResolvePath {
        public ResolveFromPATH(String name, File... otherPaths) {
            super(name, otherPaths);
        }

        @Override
        public void addEnvVarDirToVec(Vector<File> vec, String pathDir) {
            vec.add(path(pathDir, this.name));
            vec.add(path(pathDir, "src", "SketchSolver", this.name));
            vec.add(path(pathDir, "..", "sketch-backend", "src", "SketchSolver",
                    this.name));
            vec.add(path(pathDir, "..", "sketch-backend", "bindings", this.name));
        }

        @Override
        public File[] searchEnvVars() {
            return searchEnvVar("PATH", "SKETCH_HOME");
        }

        @Override
        public File[] searchDefaultPaths() {
            return fromDefaultPaths("cegis/src/SketchSolver",
                    "../sketch-backend/src/SketchSolver", "../sketch-backend/bindings");
        }
    }
    
    static File[] convertFiles(String name, File[] files) {
        Vector<File> dirs = new Vector<File>(1);
        for (File f : files) {
            if (f.isDirectory()) {
                dirs.add(new File(f, name));
            } else if (f.isFile()) {
                dirs.add(new File(f.getParentFile(), name));
            }
        }
        return dirs.toArray(new File[0]);
    }

    public class ResolveFromFileAndPATH extends ResolveFromPATH {
        public ResolveFromFileAndPATH(String name, File... files) {
            super(name, convertFiles(name, files));
        }
    }

    public class ResolveRuntime extends ResolvePath {
        public ResolveRuntime(String name, File... otherPaths) {
            super(name, otherPaths);
        }

        @Override
        public File[] searchEnvVars() {
            return searchEnvVar("SKETCH_HOME");
        }

        @Override
        public void addEnvVarDirToVec(Vector<File> vec, String pathDir) {
            vec.add(path(pathDir, this.name));
            vec.add(path(pathDir, "src", "runtime", "include", this.name));
        }

        @Override
        public File[] searchDefaultPaths() {
            return fromDefaultPaths("src/runtime/include");
        }
    }

    public String getCegisPathInner(String name) {
        if (isWin()) {
            name = name + ".exe";
        }
        return (new ResolveFromPATH(name)).resolve();
    }

    /** make directories if they don't already exist, return $dirname$ or null */
    public static File md(File dirname) {
        if (dirname == null) {
            return null;
        } else if (dirname.isDirectory()) {
            return dirname;
        } else if (dirname.mkdirs()) {
            return dirname;
        } else {
            return null;
        }
    }

    public static File path(File base, String... subpaths) {
        if (base == null) {
            return null;
        } else if (subpaths.length == 0) {
            return base;
        } else {
            String[] next = new String[subpaths.length - 1];
            System.arraycopy(subpaths, 1, next, 0, next.length);
            return path(new File(base, subpaths[0]), next);
        }
    }

    public static File path(String base, String... subpaths) {
        if (base == null) {
            return null;
        } else {
            return path(new File(base), subpaths);
        }
    }

    protected File loadTempFile(InputStream fileIn, String cegisName) {
        // try to extract it to the operating system temporary directory
        File path = new File(tmpdir, cegisName);
        String canonicalName = null;
        try {
            canonicalName = path.getCanonicalPath();
            if (path.exists() && (path.length() == fileIn.available())) {
                return path;
            } else {
                FileOutputStream fileOut = new FileOutputStream(path);
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fileIn.read(buffer)) > 0) {
                    fileOut.write(buffer, 0, len);
                }
                fileOut.flush();
                fileOut.close();
                assert (fileIn.available() == 0) : "didn't read all of file";
                if (!isWin()) {
                    Process proc =
                            (new ProcessBuilder("chmod", "755", canonicalName)).start();
                    assert (proc.waitFor() == 0) : "couldn't make cegis executable";
                }
                return path;
            }
        } catch (Exception e) {
            System.err.println("couldn't extract cegis binary; " + e);
        }
        return null;
    }

    // misc functions
    public URL getCompilerRc(String name) {
        ClassLoader cl = getClass().getClassLoader();
        URL defPath = cl.getResource("sketch/compiler/" + name);
        if (defPath == null || !path(defPath.getPath()).exists()) {
            return cl.getResource("sketch/compiler/resources/" + name);
        } else {
            return defPath;
        }
    }

    public boolean platformMatchesJava() {
        return isSet && osname.equals(System.getProperty("os.name")) &&
                osarch.equals(System.getProperty("os.arch"));
    }

    public boolean isWin() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public String getDefaultTempFile() {
        try {
            if (tmpdir == null) {
                return null;
            } else {
                return (new File(tmpdir, "sketch.sk.tmp")).getCanonicalPath();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static int trygetenv(int defValue, String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null) {
                try {
                    return Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    continue;
                }
            }
        }
        return defValue;
    }

    public File getTempPath(String... subpaths) {
        if (tmpdir == null) {
            System.out.println("null tmpdir. This should never happen!!!");
            throw new SketchSolverException("null tmpdir. This should never happen!!!");
        }
        return path(tmpdir, subpaths);
    }

    public String getTempPathString(String... subpaths) {
        try {
            return path(tmpdir, subpaths).getCanonicalPath();
        } catch (IOException e) {
            assertFalse("canonicalPath failed for subpaths", subpaths);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param name
     *            e.g. "detailed stack trace"
     */
    public void writeDebugMsg(String name, String contents, String... subpaths) {
        final File filePath = path(tmpdir, subpaths);
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.write(contents);
            writer.close();
            printDebug("[SKETCH] " + name + " written to file: " + filePath.getPath());
        } catch (IOException e) {
            System.err.println("[ERROR] [SKETCH] couldn't write output file " + filePath);
        }
    }
}
