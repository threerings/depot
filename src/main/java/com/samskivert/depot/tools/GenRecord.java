//
// Depot library - a Java relational persistence library
// http://code.google.com/p/depot/source/browse/trunk/LICENSE

package com.samskivert.depot.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.GeneratedValue;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Transient;
import com.samskivert.depot.impl.DepotUtil;
import com.samskivert.io.StreamUtil;
import com.samskivert.util.ClassUtil;
import com.samskivert.util.GenUtil;
import com.samskivert.util.StringUtil;

public abstract class GenRecord
{
    public GenRecord (ClassLoader cloader) {
        _cloader = cloader;
        // resolve the PersistentRecord class using our classloader
        try {
            _prclass = _cloader.loadClass(PersistentRecord.class.getName());
        } catch (Exception e) {
            throw mkFail("Can't resolve PersistentRecord", e);
        }
    }

    /**
     * Processes a persistent record source file.
     */
    public void processRecord (File source)
    {
        // System.err.println("Processing " + source + "...");

        // load up the file and determine it's package and classname
        String name = null;
        try {
            name = readClassName(source);
        } catch (Exception e) {
            logWarn("Failed to parse " + source, e);
            return;
        }

        try {
            processRecord(source, _cloader.loadClass(name));
        } catch (ClassNotFoundException cnfe) {
            logWarn("Failed to load " + name + ".\nMissing class: " + cnfe.getMessage(), null);
        } catch (Exception e) {
            logWarn("Failed to process " + source, e);
        }
    }

    protected abstract void logInfo (String msg);
    protected abstract void logWarn (String msg, Exception e);
    protected abstract RuntimeException mkFail (String msg, Exception e);

    /** Processes a resolved persistent record class instance. */
    protected void processRecord (File source, Class<?> rclass)
    {
        // make sure we extend persistent record
        if (!_prclass.isAssignableFrom(rclass)) {
            logInfo("Skipping non-record '" + rclass.getName() + "'");
            return;
        }

        // determine our primary key fields for getKey() generation (if we're not an abstract)
        List<Field> kflist = Lists.newArrayList();
        if (!Modifier.isAbstract(rclass.getModifiers())) {
            // determine which fields make up our primary key; we'd just use Class.getFields() but
            // that returns things in a random order whereas ClassUtil returns fields in
            // declaration order starting from the top-most class and going down the line
            for (Field field : ClassUtil.getFields(rclass)) {
                if (hasAnnotation(field, Id.class)) kflist.add(field);
                else if (hasAnnotation(field, GeneratedValue.class)) {
                    logWarn("Skipping " + rclass.getName() + ".  Field '" + field.getName() +
                            "' has @GeneratedValue, which may only used on primary keys with @Id.",
                            null);
                    return;
                }
            }
        }

        // determine which fields we need to generate constants for
        List<Field> flist = Lists.newArrayList();
        for (Field field : rclass.getFields()) {
            if (isPersistentField(field)) {
                flist.add(field);
            }
        }
        Set<Field> declared = Sets.newHashSet();
        for (Field field : rclass.getDeclaredFields()) {
            if (isPersistentField(field)) {
                declared.add(field);
            }
        }

        // read the source file and break it into lines
        Charset charset = Charset.defaultCharset(); // TODO?
        String sourceText;
        String[] lines = null;
        try {
            sourceText = Files.toString(source, charset);
            lines = Files.readLines(source, charset).toArray(new String[0]);
        } catch (IOException ioe) {
            logWarn("Error reading '" + source + "'", ioe);
            return;
        }

        // now determine where to insert our static field declarations
        int bstart = -1, bend = -1;
        int nstart = -1, nend = -1;
        int mstart = -1, mend = -1;
        for (int ii = 0; ii < lines.length; ii++) {
            String line = lines[ii].trim();

            // look for the start of the class body
            if (NAME_PATTERN.matcher(line).find()) {
                if (line.endsWith("{")) {
                    bstart = ii+1;
                } else {
                    // search down a few lines for the open brace
                    for (int oo = 1; oo < 10; oo++) {
                        if (get(lines, ii+oo).trim().endsWith("{")) {
                            bstart = ii+oo+1;
                            break;
                        }
                    }
                }

            // track the last } on a line by itself and we'll call that the end of the class body
            } else if (line.equals("}")) {
                bend = ii;

            // look for our field and method markers
            } else if (line.equals(FIELDS_START)) {
                nstart = ii;
            } else if (line.equals(FIELDS_END)) {
                nend = ii+1;
            } else if (line.equals(METHODS_START)) {
                mstart = ii;
            } else if (line.equals(METHODS_END)) {
                mend = ii+1;
            }
        }

        // sanity check the markers
        if (check(source, "fields start", nstart, "fields end", nend) ||
            check(source, "fields end", nend, "fields start", nstart) ||
            check(source, "methods start", mstart, "methods end", mend) ||
            check(source, "methods end", mend, "methods start", mstart)) {
            return;
        }

        // we have no previous markers then stuff the fields at the top of the class body and the
        // methods at the bottom
        if (nstart == -1) {
            nstart = bstart;
            nend = bstart;
        }
        if (mstart == -1) {
            mstart = bend;
            mend = bend;
        }

        // get the unqualified class name
        String rname = DepotUtil.justClassName(rclass);

        // generate our fields section
        StringBuilder fsection = new StringBuilder();

        // add our prototype declaration
        Map<String, String> subs = Maps.newHashMap();
        // We could add the @Generated annotation, but
        // - we'd need to add the import for javax.annotation.Generated
        // - it adds a lot of boilerplate to the source file
        // - we can't annotate the static initializer that registers the key fields
        // So it was decided to omit it and the few benefits:
        //   - marked as Generated in javadoc
        //   - timestamp of last generation
        //   - a reference back to this class
//        subs.put("generated", "@Generated(value={\"" + GenRecordTask.class.getName() + "\"}, " +
//            "date=\"" + // ISO 8601 date
//            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()) + "\")");
        subs.put("record", rname);
        fsection.append(mergeTemplate(PROTO_TMPL, subs));

        // add our ColumnExp constants
        for (int ii = 0; ii < flist.size(); ii++) {
            Field f = flist.get(ii);
            String fname = f.getName();

            // create our substitution mappings
            Map<String, String> fsubs = Maps.newHashMap(subs);
            fsubs.put("type", getTypeName(f.getGenericType()));
            fsubs.put("field", fname);
            fsubs.put("capfield", StringUtil.unStudlyName(fname).toUpperCase());

            // now generate our bits
            fsection.append(mergeTemplate(COL_TMPL, fsubs));
        }

        // generate our methods section
        StringBuilder msection = new StringBuilder();

        // add a getKey() method, if applicable
        if (kflist.size() > 0) {
            StringBuilder argList = new StringBuilder();
            StringBuilder argNameList = new StringBuilder();
            StringBuilder fieldNameList = new StringBuilder();
            for (Field keyField : kflist) {
                if (argList.length() > 0) {
                    argList.append(", ");
                    argNameList.append(", ");
                    fieldNameList.append(", ");
                }
                String name = keyField.getName();
                argList.append(GenUtil.simpleName(keyField)).append(" ").append(name);
                argNameList.append(name);
                fieldNameList.append(StringUtil.unStudlyName(name));
            }

            subs.put("argList", argList.toString());
            subs.put("argNameList", argNameList.toString());
            subs.put("fieldNameList", fieldNameList.toString());

            // generate our bits and append them as appropriate to the string buffers
            msection.append(mergeTemplate(KEY_TMPL, subs));
        }

        // now bolt everything back together into a class declaration
        StringWriter out = new StringWriter();
        PrintWriter pout = new PrintWriter(out);
        for (int ii = 0; ii < nstart; ii++) {
            pout.println(lines[ii]);
        }

        if (fsection.length() > 0) {
            String prev = get(lines, nstart-1);
            if (!StringUtil.isBlank(prev) && !prev.equals("{")) {
                pout.println();
            }
            pout.println("    " + FIELDS_START);
            pout.write(fsection.toString());
            pout.println("    " + FIELDS_END);
            if (!StringUtil.isBlank(get(lines, nend))) {
                pout.println();
            }
        }
        for (int ii = nend; ii < mstart; ii++) {
            pout.println(lines[ii]);
        }

        if (msection.length() > 0) {
            if (!StringUtil.isBlank(get(lines, mstart-1))) {
                pout.println();
            }
            pout.println("    " + METHODS_START);
            pout.write(msection.toString());
            pout.println("    " + METHODS_END);
            String next = get(lines, mend);
            if (!StringUtil.isBlank(next) && !next.equals("}")) {
                pout.println();
            }
        }
        for (int ii = mend; ii < lines.length; ii++) {
            pout.println(lines[ii]);
        }

        String newSourceText = out.toString();
        if (!sourceText.equals(newSourceText)) {
            try {
                Files.write(newSourceText, source, charset);
                logInfo("Regenerated '" + source + "'");
            } catch (IOException ioe) {
                logWarn("Error writing to '" + source + "'", ioe);
            }
        }
    }

    /**
     * Returns true if the supplied field is part of a persistent record (is a public, non-static,
     * non-@Transient field).
     */
    protected boolean isPersistentField (Field field)
    {
        int mods = field.getModifiers();
        return Modifier.isPublic(mods) && !Modifier.isStatic(mods) &&
            !hasAnnotation(field, Transient.class);
    }

    /**
     * Safely gets the <code>index</code>th line, returning the empty string if we exceed the
     * length of the array.
     */
    protected String get (String[] lines, int index)
    {
        return (index < lines.length) ? lines[index] : "";
    }

    /** Helper function for sanity checking marker existence. */
    protected boolean check (File source, String mname, int mline, String fname, int fline)
    {
        if (mline == -1 && fline != -1) {
            logWarn("Found " + fname + " marker (at line " + (fline+1) + ") but no " +
                    mname + " marker in '" + source + "'.", null);
            return true;
        }
        return false;
    }

    /** Helper function for generating our boilerplate code. */
    protected String mergeTemplate (String tmpl, Map<String, String> subs)
    {
        try {
            String text = StreamUtil.toString(
                getClass().getClassLoader().getResourceAsStream(tmpl), "UTF-8");
            text = text.replace("\n", System.getProperty("line.separator"));
            for (Map.Entry<String, String> entry : subs.entrySet()) {
                text = text.replaceAll("@"+entry.getKey()+"@", entry.getValue());
            }
            return text;
        } catch (Exception e) {
            throw mkFail("Failed processing template [tmpl=" + tmpl + "]", e);
        }
    }

    protected static boolean hasAnnotation (Field field, Class<?> annotation)
    {
        // iterate because getAnnotation() fails if we're dealing with multiple classloaders
        for (Annotation a : field.getAnnotations()) {
            if (annotation.getName().equals(a.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads in the supplied source file and locates the package and class or interface name and
     * returns a fully qualified class name.
     */
    protected static String readClassName (File source)
        throws IOException
    {
        // load up the file and determine it's package and classname
        String pkgname = null, name = null;
        BufferedReader bin = new BufferedReader(new FileReader(source));
        String line;
        while ((line = bin.readLine()) != null) {
            Matcher pm = PACKAGE_PATTERN.matcher(line);
            if (pm.find()) {
                pkgname = pm.group(1);
            }
            Matcher nm = NAME_PATTERN.matcher(line);
            if (nm.find()) {
                name = nm.group(2);
                break;
            }
        }
        bin.close();

        // make sure we found something
        if (name == null) {
            throw new IOException("Unable to locate class or interface name in " + source + ".");
        }

        // prepend the package name to get a name we can Class.forName()
        if (pkgname != null) {
            name = pkgname + "." + name;
        }

        return name;
    }

    protected static String getTypeName (Type type)
    {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>)type;
            if (clazz.isArray()) {
                Class<?> cclass = clazz.getComponentType();
                return (cclass.isPrimitive() ? cclass.getSimpleName() : getTypeName(cclass)) + "[]";
            } else {
                Class<?> nclass = clazz.isPrimitive() ? BOXES.get(clazz) : clazz;
                Class<?> eclass = nclass.getEnclosingClass();
                return (eclass == null) ? nclass.getSimpleName() :
                    getTypeName(eclass) + "." + nclass.getSimpleName();
            }

        } else if (type instanceof ParameterizedType) {
            ParameterizedType paramType = (ParameterizedType)type;
            Type[] typeArgs = paramType.getActualTypeArguments();
            String tparams = (typeArgs == null || typeArgs.length == 0) ? "" :
                "<" + Joiner.on(", ").join(
                    Lists.transform(Lists.newArrayList(typeArgs), new Function<Type, String>() {
                        @Override public String apply (Type input) {
                            return getTypeName(input);
                        }
                    })) + ">";
            return getTypeName(paramType.getRawType()) + tparams;

        } else if (type instanceof GenericArrayType) {
            return getTypeName(((GenericArrayType)type).getGenericComponentType()) + "[]";

        } else {
            throw new IllegalArgumentException("Unknown kind of type '" + type + "'");
        }
    }

    /** Used to do our own classpath business. */
    protected final ClassLoader _cloader;

    /** {@link PersistentRecord} resolved with the proper classloader so that we can compare it to
     * loaded derived classes. */
    protected final Class<?> _prclass;

    /** Specifies the path to the name code template. */
    protected static final String PROTO_TMPL = "com/samskivert/depot/tools/record_proto.tmpl";

    /** Specifies the path to the column code template. */
    protected static final String COL_TMPL = "com/samskivert/depot/tools/record_column.tmpl";

    /** Specifies the path to the key code template. */
    protected static final String KEY_TMPL = "com/samskivert/depot/tools/record_key.tmpl";

    // markers
    protected static final String MARKER = "// AUTO-GENERATED: ";
    protected static final String FIELDS_START = MARKER + "FIELDS START";
    protected static final String FIELDS_END = MARKER + "FIELDS END";
    protected static final String METHODS_START = MARKER + "METHODS START";
    protected static final String METHODS_END = MARKER + "METHODS END";

    /** A regular expression for matching the package declaration. */
    protected static final Pattern PACKAGE_PATTERN = Pattern.compile("^\\s*package\\s+(\\S+)\\W");

    /** A regular expression for matching the class or interface declaration. */
    protected static final Pattern NAME_PATTERN = Pattern.compile(
        "^\\s*public\\s+(?:abstract\\s+)?(interface|class)\\s+(\\w+)(\\W|$)");

    protected static final Map<Class<?>, Class<?>> BOXES =
        ImmutableMap.<Class<?>,Class<?>>builder().
        put(Boolean.TYPE, Boolean.class).
        put(Byte.TYPE, Byte.class).
        put(Short.TYPE, Short.class).
        put(Character.TYPE, Character.class).
        put(Integer.TYPE, Integer.class).
        put(Long.TYPE, Long.class).
        put(Float.TYPE, Float.class).
        put(Double.TYPE, Double.class).build();
}
