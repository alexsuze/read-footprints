package com.readfootprints.service;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpineColorTest {

    /** 白底，左半塊 main、右下一小塊 accent */
    private static String cover(Color main, Color accent) throws IOException {
        BufferedImage img = new BufferedImage(100, 140, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 140);
        g.setColor(main);
        g.fillRect(0, 0, 50, 140);
        g.setColor(accent);
        g.fillRect(80, 120, 20, 20);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static float[] hsb(String hex) {
        Color c = Color.decode(hex);
        return Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
    }

    @Test
    void picksLargestHueIgnoringWhiteAndKeepsItMuted() throws IOException {
        float[] c = hsb(SpineColor.of(cover(new Color(20, 60, 230), new Color(230, 30, 30))));
        assertTrue(c[0] > .55f && c[0] < .72f, "應為藍色系，實際色相 " + c[0]);
        assertTrue(c[1] <= .61f && c[2] <= .63f, "飽和度與明度應被壓低");
    }

    @Test
    void blackAndWhiteCoverIsNeutral() throws IOException {
        assertEquals(SpineColor.NEUTRAL, SpineColor.of(cover(Color.BLACK, Color.GRAY)));
    }

    @Test
    void missingOrUndecodableCoverIsNull() {
        assertNull(SpineColor.of(null));
        assertNull(SpineColor.of(""));
        assertNull(SpineColor.of("data:image/webp;base64,AAAA"));
        assertNull(SpineColor.of("https://example.com/a.jpg"));
    }
}
