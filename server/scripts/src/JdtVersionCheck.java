/******************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *****************************************************************************/

///usr/bin/env jbang "$0" "$@" ; exit $?

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

// Checks that every place declaring org.eclipse.jdt:org.eclipse.jdt.core agrees with the version
// Spotless actually provisions for the eclipse('<N>') formatter step in build.gradle.
//
// Why this needs checking: spotless-lib-extra bundles a lockfile per JDT formatter version, pinning
// the exact jdt.core that eclipse('<N>') resolves. Nothing makes the other declarations follow it -
// buildSrc compiles the custom brace-fix steps against jdt.core, and the jbang path (format.sh,
// scripts/src/*.java) needs the same version because jbang-fmt otherwise hardcodes an older one.
// Different JDT versions format the same source differently, so drift here silently leaves the two
// formatter paths contradicting each other rather than failing outright.
//
// The declaration sites are discovered from the files themselves, so adding another jbang script
// with a jdt.core //DEPS line needs no change here.
//
// Run from the server root (see jdt-version-check.sh).
void main(String[] args) throws Exception {
    var eclipseVersion = findEclipseFormatterVersion();
    var libExtraVersion = findSpotlessLibExtraVersion();
    System.out.println("build.gradle formats with eclipse('" + eclipseVersion + "')");
    System.out.println("buildSrc uses spotless-lib-extra " + libExtraVersion);
    System.out.println();

    var sites = findDeclarationSites();
    if (sites.isEmpty()) {
        System.err.println("No org.eclipse.jdt.core declarations found; has the layout changed?");
        System.exit(2);
    }

    // The lockfile is authoritative. Without it (offline, say) the declarations can still be
    // checked against each other, which catches drift between them - just not a stale bump.
    var lockfiles = readLockfileJdtVersions(libExtraVersion);
    String expected;
    String source;
    if (lockfiles == null) {
        var fallback = sites.getFirst();
        expected = fallback.version();
        source = "the other declarations";
        System.out.println(
                "warning: could not read spotless-lib-extra " + libExtraVersion + "; comparing the "
                        + "declarations against each other only, using " + fallback.label() + " as the reference.");
        System.out.println();
    } else if (!lockfiles.containsKey(eclipseVersion)) {
        System.out.println(
                "NO LOCKFILE: spotless-lib-extra " + libExtraVersion + " does not bundle a lockfile for "
                        + "eclipse('" + eclipseVersion + "'), so Spotless falls back to provisioning it live from "
                        + "Eclipse's P2 repository - which fails outright when that repository is not populated.");
        System.out.println("             Bundled versions go up to " + lockfiles.lastKey() + ".");
        System.exit(1);
        return;
    } else {
        expected = lockfiles.get(eclipseVersion);
        source = "the version eclipse('" + eclipseVersion + "') provisions";
        System.out.println(
                "eclipse('" + eclipseVersion + "') provisions jdt.core " + expected
                        + " (per eclipse_jdt_formatter/v" + eclipseVersion + ".lockfile)");
        System.out.println();
    }

    var drifted = false;
    for (Site site : sites) {
        if (site.version().equals(expected)) {
            System.out.println("ok: " + site.label() + " = " + site.version());
        } else {
            System.out.println("MISMATCH: " + site.label() + " = " + site.version() + ", expected " + expected);
            drifted = true;
        }
    }

    if (drifted) {
        System.out.println();
        System.out.println(
                "One or more jdt.core declarations disagree with " + source + ". Set them all to " + expected
                        + ", or move the formatter to a version whose lockfile pins what you want.");
        System.exit(1);
    }

    System.out.println();
    System.out.println("All jdt.core declarations agree on " + expected + ".");
}

record Site(String label, String version) {}

String findEclipseFormatterVersion() throws IOException {
    return matchOne(Path.of("build.gradle"), Pattern.compile("eclipse\\('([\\w.\\-]+)'\\)"), "eclipse('<version>')");
}

String findSpotlessLibExtraVersion() throws IOException {
    return matchOne(
            Path.of("buildSrc/build.gradle"),
            Pattern.compile("com\\.diffplug\\.spotless:spotless-lib-extra:([\\w.\\-]+)"),
            "spotless-lib-extra coordinate");
}

/** Every file that names a concrete jdt.core version, in the shape that file declares it. */
List<Site> findDeclarationSites() throws IOException {
    var gav = "org\\.eclipse\\.jdt:org\\.eclipse\\.jdt\\.core:([\\w.\\-]+)";
    var sites = new ArrayList<Site>();
    addSite(sites, Path.of("buildSrc/build.gradle"), Pattern.compile("implementation\\s+'" + gav + "'"));
    addSite(sites, Path.of("scripts/format.sh"), Pattern.compile("^JDT_VERSION=([\\w.\\-]+)", Pattern.MULTILINE));

    // Discovered rather than listed, so a new jbang script is covered automatically.
    var depsPattern = Pattern.compile("^//DEPS\\s+" + gav, Pattern.MULTILINE);
    try (Stream<Path> scripts = Files.list(Path.of("scripts/src"))) {
        for (Path script : scripts.filter(p -> p.toString().endsWith(".java")).sorted().toList()) {
            addSite(sites, script, depsPattern);
        }
    }
    return sites;
}

/** Appends one Site per match, so a file declaring the version twice is reported twice. */
void addSite(List<Site> sites, Path file, Pattern pattern) throws IOException {
    if (!Files.exists(file)) {
        return;
    }
    var matcher = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
    while (matcher.find()) {
        sites.add(new Site(file.toString(), matcher.group(1)));
    }
}

String matchOne(Path file, Pattern pattern, String what) throws IOException {
    var matcher = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
    if (!matcher.find()) {
        System.err.println("Could not find " + what + " in " + file + "; has the layout changed?");
        System.exit(2);
    }
    return matcher.group(1);
}

/** Eclipse formatter version -> the jdt.core its lockfile pins, or null if the jar is unavailable.
 *  Sorted by version so the highest bundled one can be reported. */
TreeMap<String, String> readLockfileJdtVersions(String libExtraVersion) {
    Path jar;
    try {
        jar = locateSpotlessLibExtra(libExtraVersion);
    } catch (Exception e) {
        System.err.println("warning: " + e);
        return null;
    }

    var prefix = "com/diffplug/spotless/extra/eclipse_jdt_formatter/v";
    var suffix = ".lockfile";
    var pinPattern = Pattern.compile("^org\\.eclipse\\.jdt:org\\.eclipse\\.jdt\\.core:(.+)$", Pattern.MULTILINE);
    var versions = new TreeMap<String, String>(versionOrder());
    try (var zip = new ZipFile(jar.toFile())) {
        for (var entries = zip.entries(); entries.hasMoreElements();) {
            var entry = entries.nextElement();
            var name = entry.getName();
            if (!name.startsWith(prefix) || !name.endsWith(suffix)) {
                continue;
            }
            var content = new String(zip.getInputStream(entry).readAllBytes(), StandardCharsets.UTF_8);
            var pin = pinPattern.matcher(content);
            if (pin.find()) {
                versions.put(name.substring(prefix.length(), name.length() - suffix.length()), pin.group(1).strip());
            }
        }
    } catch (IOException e) {
        System.err.println("warning: could not read " + jar + ": " + e);
        return null;
    }
    return versions.isEmpty() ? null : versions;
}

/** Prefers an already-downloaded copy so the check works offline, then falls back to Maven Central. */
Path locateSpotlessLibExtra(String version) throws Exception {
    var jarName = "spotless-lib-extra-" + version + ".jar";
    var home = Path.of(System.getProperty("user.home"));
    var m2 = home.resolve(".m2/repository/com/diffplug/spotless/spotless-lib-extra").resolve(version).resolve(jarName);
    if (Files.isRegularFile(m2)) {
        return m2;
    }
    // The Gradle cache interposes a hash directory between the version and the jar.
    var gradleCache = home.resolve(
            ".gradle/caches/modules-2/files-2.1/com.diffplug.spotless/spotless-lib-extra").resolve(version);
    if (Files.isDirectory(gradleCache)) {
        try (Stream<Path> found = Files.walk(gradleCache, 2)) {
            var hit = found.filter(p -> p.getFileName().toString().equals(jarName)).findFirst();
            if (hit.isPresent()) {
                return hit.get();
            }
        }
    }

    var url = "https://repo1.maven.org/maven2/com/diffplug/spotless/spotless-lib-extra/" + version + "/" + jarName;
    var target = Files.createTempFile("spotless-lib-extra", ".jar");
    target.toFile().deleteOnExit();
    try (var client = HttpClient.newHttpClient()) {
        var response = client.send(
                HttpRequest.newBuilder(URI.create(url)).build(),
                HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != 200) {
            throw new IOException("GET " + url + " returned HTTP " + response.statusCode());
        }
    }
    return target;
}

/** Numeric per segment, so 4.9 sorts below 4.10 rather than above it. */
Comparator<String> versionOrder() {
    return (left, right) -> {
        var leftParts = left.split("\\.");
        var rightParts = right.split("\\.");
        for (int i = 0; i < Math.max(leftParts.length, rightParts.length); i++) {
            var cmp = Integer.compare(segment(leftParts, i), segment(rightParts, i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    };
}

/** Missing and non-numeric segments count as 0 - enough to order the bundled formatter versions,
 *  which are all plain <major>.<minor>. */
int segment(String[] parts, int index) {
    if (index >= parts.length) {
        return 0;
    }
    try {
        return Integer.parseInt(parts[index]);
    } catch (NumberFormatException e) {
        return 0;
    }
}
