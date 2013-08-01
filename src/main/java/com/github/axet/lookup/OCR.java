package com.github.axet.lookup;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import com.github.axet.lookup.common.ClassResources;
import com.github.axet.lookup.common.FontFamily;
import com.github.axet.lookup.common.FontSymbol;
import com.github.axet.lookup.common.FontSymbolLookup;
import com.github.axet.lookup.common.ImageBinary;

public class OCR extends OCRCore {

    public OCR() {
        loadFontsDirectory(getClass(), new File("fonts"));
    }

    /**
     * Load fonts / symbols from a class directory or jar file
     * 
     * @param c
     *            class name, corresponded to the resources.
     *            com.example.MyApp.class
     * @param path
     *            path to the fonts folder. directory should only contain
     *            folders with fonts which to load
     * 
     */
    public void loadFontsDirectory(Class<?> c, File path) {
        ClassResources e = new ClassResources(c, path);

        List<String> str = e.names();

        for (String s : str)
            loadFont(c, new File(path, s));
    }

    /**
     * Load specified font family to load
     * 
     * @param c
     *            class name, corresponded to the resources.
     *            com.example.MyApp.class
     * @param path
     *            path to the fonts folder. directory should only contain
     *            folders with fonts which to load.
     * @param name
     *            name of the font to load
     * 
     */
    public void loadFont(Class<?> c, File path) {
        ClassResources e = new ClassResources(c, path);

        List<String> str = e.names();

        for (String s : str) {
            File f = new File(path, s);

            InputStream is = c.getResourceAsStream(f.getPath());

            String symbol = FilenameUtils.getBaseName(s);

            try {
                symbol = URLDecoder.decode(symbol, "UTF-8");
            } catch (UnsupportedEncodingException ee) {
                throw new RuntimeException(ee);
            }

            String name = path.getName();
            loadFontSymbol(name, symbol, is);
        }
    }

    public void loadFontSymbol(String fontName, String fontSymbol, InputStream is) {
        BufferedImage b = Capture.load(is);

        FontFamily ff = fontFamily.get(fontName);
        if (ff == null) {
            ff = new FontFamily(fontName);
            fontFamily.put(fontName, ff);
        }

        // b = prepareImageCrop(b);

        FontSymbol f = new FontSymbol(ff, fontSymbol, b);

        ff.add(f);
    }

    public String recognize(BufferedImage bi) {
        // bi = prepareImage(bi);
        ImageBinary i = new ImageBinary(bi);

        return recognize(i);
    }

    public String recognize(ImageBinary i) {
        List<FontSymbol> list = getSymbols();

        return recognize(i, 0, 0, i.getWidth() - 1, i.getHeight() - 1, list);
    }

    /**
     * 
     * @param fontSet
     *            use font in the specified folder only
     * @param bi
     * @return
     */
    public String recognize(String fontSet, BufferedImage bi) {
        ImageBinary i = new ImageBinary(bi);

        return recognize(fontSet, i);
    }

    public String recognize(String fontSet, ImageBinary i) {
        List<FontSymbol> list = getSymbols(fontSet);

        return recognize(i, 0, 0, i.getWidth() - 1, i.getHeight() - 1, list);
    }

    public String recognize(ImageBinary i, int x1, int y1, int x2, int y2, List<FontSymbol> list) {
        List<FontSymbolLookup> all = findAll(list, i, x1, y1, x2, y2);

        // bigger first.

        Collections.sort(all, new BiggerFirst());

        // big images eat small ones

        List<FontSymbolLookup> copy = new ArrayList<FontSymbolLookup>(all);

        for (int k = 0; k < copy.size(); k++) {
            FontSymbolLookup kk = copy.get(k);
            for (int j = k + 1; j < copy.size(); j++) {
                FontSymbolLookup jj = copy.get(j);
                if (kk.cross(jj))
                    all.remove(jj);
            }
        }

        // sort top/bottom/left/right

        Collections.sort(all, new Left2Right());

        // calculate rows

        String str = "";
        {
            int x = 0;
            int cx = 0;
            for (FontSymbolLookup s : all) {
                int maxCX = Math.max(cx, s.getWidth());

                // if distance betten end of previous symbol and begining of the
                // current is larger then a char size, then it is a space
                if (s.x - (x + cx) > maxCX)
                    str += " ";

                // if we drop back, then we have a end of line
                if (s.x < x)
                    str += "\n";
                x = s.x;
                cx = s.getWidth();
                str += s.fs.fontSymbol;
            }
        }

        return str;
    }

    static public void main(String[] args) {
        OCR l = new OCR();

        // will go to com/github/axet/lookup/fonts folder and load all font
        // familys (here is only font_1 family in this library)
        l.loadFontsDirectory(OCR.class, new File("fonts"));

        // example how to load only one family
        // "com/github/axet/lookup/fonts/font_1"
        l.loadFont(OCR.class, new File("fonts", "font_1"));

        String str = "";

        // recognize using all familys set
        str = l.recognize(Capture.load(new File("/Users/axet/Desktop/test3.png")));

        // recognize using only one family set
        str = l.recognize("font_1", Capture.load(new File("/Users/axet/Desktop/test3.png")));

        // str = recognized string
        System.out.println(str);
    }
}