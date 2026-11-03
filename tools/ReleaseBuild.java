import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ReleaseBuild {

    static final String MOD = "pilumdoublespeed";
    static final String FOLDER = "Big.Pilum.Energy";
    static final String RELEASES = "https://github.com/SirHartley/Big-Pilum-Energy/releases";

    static final String VERSION_FILE = "BigPilumEnergy.version";
    static final String TOOL = "tools/ReleaseBuild.java";
    static final String ICON = "_best_missile.png";
    static final String VERSION = "[1-9][0-9]*\\.(?:0|[1-9][0-9]*)\\.[a-z]";
    static final Pattern HEADING = Pattern.compile(
            "^(" + VERSION + ")(?:[ \\t]*-[ \\t]*(UNRELEASED|CURRENT PUBLIC VERSION))?[ \\t]*$");

    static final Set<String> ROOT_FILES = Set.of("mod_info.json", VERSION_FILE, "Changelog", ICON,
            "LICENSE", "LICENSE.txt", "LICENSE.md", "NOTICE", "CREDITS", "CREDITS.txt", "CREDITS.md");
    static final List<String> REQUIRED = List.of("Changelog", "mod_info.json", VERSION_FILE, ICON,
            "data/weapons/weapon_data.csv", "data/config/LunaSettingsConfig.json",
            "data/config/version/version_files.csv");
    static final Set<String> NOTICES = Set.of();
    static final Map<String, Set<String>> ASSETS = Map.of(
            "data", Set.of("csv", "json", "shader", "ship", "wpn", "proj", "system", "variant", "skin"),
            "graphics", Set.of("png", "jpg", "jpeg"),
            "sounds", Set.of("ogg", "wav"));
    static final Set<String> SKIPPED_DIRS = Set.of("unused", "unedited", "tools", "docs");
    static final LocalDateTime ZIP_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    record Notes(String version, boolean unreleased, String text) {

    }

    public static void main(String[] args) throws Exception {
        Path root = Path.of("").toAbsolutePath();
        if ((args.length == 1 || args.length == 2) && args[0].equals("package")) {
            pack(root, args.length == 2 ? args[1] : null);
        } else {
            throw new IllegalArgumentException("From the mod root, with JDK 17:\n"
                    + "  java " + TOOL + " package [release-tag]");
        }
    }

    static void pack(Path root, String tag) throws Exception {
        Notes notes = notes(Files.readString(root.resolve("Changelog")), tag);
        Map<String, byte[]> entries = new TreeMap<>();
        for (String name : trackedFiles(root)) {
            if (releaseFile(name)) entries.put(FOLDER + "/" + name, Files.readAllBytes(root.resolve(name)));
        }
        for (String name : REQUIRED) {
            require(entries.containsKey(FOLDER + "/" + name), "Missing tracked release file: " + name);
        }
        checkAssets(entries);
        entries.put(FOLDER + "/mod_info.json", stampInfo(Files.readString(root.resolve("mod_info.json")),
                notes.version()).getBytes(StandardCharsets.UTF_8));
        String versionFile = stampVersion(Files.readString(root.resolve(VERSION_FILE)), notes.version());
        entries.put(FOLDER + "/" + VERSION_FILE, versionFile.getBytes(StandardCharsets.UTF_8));
        Path output = root.resolve("out/release");
        Files.createDirectories(output);
        Files.writeString(output.resolve(VERSION_FILE), versionFile);
        Path zip = output.resolve(FOLDER + "-" + notes.version() + ".zip");
        writeZip(zip, entries);
        // text block keeps the changelog's plain-text layout on github
        Files.writeString(output.resolve("release-notes.md"), "```text\n" + notes.text() + "\n```\n");
        Files.writeString(output.resolve("version.txt"), notes.version() + "\n");
        System.out.println("Packaged " + entries.size() + " files: " + zip + " (" + Files.size(zip) + " bytes)");
    }

    static Notes notes(String changelog, String tag) {
        if (tag != null) require(tag.matches(VERSION), "Expected a tag such as 1.0.a, without a v prefix.");
        List<String> lines = changelog.replace("\r\n", "\n").lines().toList();
        List<Integer> headings = new ArrayList<>();
        Set<String> versions = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            Matcher match = HEADING.matcher(lines.get(i));
            if (!match.matches()) continue;
            require(versions.add(match.group(1)), "Duplicate changelog version: " + match.group(1));
            headings.add(i);
        }
        require(!headings.isEmpty(), "Changelog has no version heading.");
        int start = headings.get(0);
        int end = headings.size() > 1 ? headings.get(1) : lines.size();
        Matcher heading = HEADING.matcher(lines.get(start));
        require(heading.matches(), "Invalid changelog heading.");
        String version = heading.group(1);
        boolean unreleased = "UNRELEASED".equals(heading.group(2));
        require(tag == null || tag.equals(version), "Tag must match the newest Changelog section: " + version);
        require(tag == null || !unreleased, "Remove UNRELEASED from " + version + " before tagging.");
        String body = String.join("\n", lines.subList(start + 1, end)).strip();
        require(Pattern.compile("(?m)^(Added|Adjusted|Fixed|Removed) - .+").matcher(body).find(),
                "Changelog section " + version + " has no changes.");
        require(!body.contains("```"), "Changelog must be plain text, without code fences.");
        return new Notes(version, unreleased, lines.get(start) + "\n" + body + "\n");
    }

    static void checkAssets(Map<String, byte[]> entries) {
        Pattern asset = Pattern.compile("(?:graphics|sounds|data)/" + MOD + "/[A-Za-z0-9_./-]+"
                + "\\.(?:png|jpg|jpeg|ogg|wav|shader)\\b");
        for (var file : entries.entrySet()) {
            if (!file.getKey().startsWith(FOLDER + "/data/")) continue;
            Matcher references = asset.matcher(new String(file.getValue(), StandardCharsets.UTF_8));
            while (references.find()) {
                require(entries.containsKey(FOLDER + "/" + references.group()),
                        file.getKey() + " references an unpackaged asset: " + references.group());
            }
        }
    }

    static String stampInfo(String text, String version) {
        require(version.matches(VERSION), "Invalid release version.");
        return replaceField(text, "version", "\"" + version + "\"");
    }

    static String stampVersion(String text, String version) {
        require(version.matches(VERSION), "Invalid release version.");
        String[] parts = version.split("\\.");
        text = replaceField(text, "major", parts[0]);
        text = replaceField(text, "minor", parts[1]);
        text = replaceField(text, "patch", "\"" + parts[2] + "\"");
        return replaceField(text, "directDownloadURL",
                "\"" + RELEASES + "/download/" + version + "/" + FOLDER + "-" + version + ".zip\"");
    }

    static String replaceField(String text, String key, String value) {
        Pattern field = Pattern.compile("(\"" + key + "\"\\s*:\\s*)(\"[^\"\\r\\n]*\"|[0-9]+)");
        Matcher matcher = field.matcher(text);
        require(matcher.find(), "Missing metadata field: " + key);
        require(!matcher.find(), "Duplicate metadata field: " + key);
        return field.matcher(text).replaceFirst("$1" + Matcher.quoteReplacement(value));
    }

    static boolean releaseFile(String name) {
        if (ROOT_FILES.contains(name) || NOTICES.contains(name)) return true;
        String[] parts = name.split("/");
        if (parts.length < 2 || !ASSETS.containsKey(parts[0])) return false;
        for (String part : parts) {
            if (part.startsWith(".") || SKIPPED_DIRS.contains(part)) return false;
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 && ASSETS.get(parts[0]).contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    static List<String> trackedFiles(Path root) throws Exception {
        Process git = new ProcessBuilder("git", "ls-files", "-z").directory(root.toFile())
                .redirectError(ProcessBuilder.Redirect.INHERIT).start();
        String output = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        require(git.waitFor() == 0, "git ls-files failed.");
        return output.isEmpty() ? List.of() : List.of(output.split("\u0000"));
    }

    static void writeZip(Path target, Map<String, byte[]> entries) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(target))) {
            zip.setLevel(9);
            for (var item : new TreeMap<>(entries).entrySet()) {
                ZipEntry entry = new ZipEntry(item.getKey());
                entry.setTimeLocal(ZIP_TIME);
                zip.putNextEntry(entry);
                zip.write(item.getValue());
                zip.closeEntry();
            }
        }
    }

    static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
