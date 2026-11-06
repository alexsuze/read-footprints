package com.readfootprints.service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * 從書封算書脊底色（規劃書第 3 節）：縮圖後依色相分桶取最多的一桶，
 * 略過近白、近黑與灰，再把飽和度與明度壓進和風範圍，免得跟書房配色打架。
 * ponytail: ImageIO 只認 JPEG / PNG / GIF / BMP，WebP 回 null 由前端用書名雜湊；真的有很多 WebP 再加解碼器。
 */
public final class SpineColor {

    private static final int SAMPLE = 32;
    private static final int BUCKETS = 24;
    /** 整張黑白灰的書封 */
    static final String NEUTRAL = "#5A5750";

    private SpineColor() {}

    /** 書封 data URL 或純 Base64；沒有書封或解不開回 null，書照樣存。 */
    public static String of(String cover) {
        if (cover == null || cover.isBlank()) return null;
        try {
            byte[] bytes = Base64.getMimeDecoder().decode(cover.substring(cover.indexOf(',') + 1));
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
            return image == null ? null : dominant(image);
        } catch (IOException | RuntimeException e) { // 壞圖的例外種類很雜
            return null;
        }
    }

    private static String dominant(BufferedImage source) {
        BufferedImage image = new BufferedImage(SAMPLE, SAMPLE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.drawImage(source, 0, 0, SAMPLE, SAMPLE, Color.WHITE, null);
        g.dispose();

        long[][] buckets = new long[BUCKETS][4]; // 數量、R、G、B 加總
        float[] hsb = new float[3];
        for (int y = 0; y < SAMPLE; y++) {
            for (int x = 0; x < SAMPLE; x++) {
                Color c = new Color(image.getRGB(x, y));
                Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), hsb);
                if (hsb[1] < .12f || hsb[2] < .15f) continue;
                long[] b = buckets[Math.min((int) (hsb[0] * BUCKETS), BUCKETS - 1)];
                b[0]++;
                b[1] += c.getRed();
                b[2] += c.getGreen();
                b[3] += c.getBlue();
            }
        }

        long[] best = buckets[0];
        for (long[] b : buckets) if (b[0] > best[0]) best = b;
        if (best[0] == 0) return NEUTRAL;

        Color.RGBtoHSB((int) (best[1] / best[0]), (int) (best[2] / best[0]), (int) (best[3] / best[0]), hsb);
        int rgb = Color.HSBtoRGB(hsb[0], clamp(hsb[1], .28f, .6f), clamp(hsb[2], .38f, .62f));
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
