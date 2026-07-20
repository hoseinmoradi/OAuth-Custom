package com.master.oauth.captcha;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Locale;

@Service
public class CaptchaService {

    public static final String SESSION_CODE = "CAPTCHA_CODE";
    public static final String SESSION_EXPIRES = "CAPTCHA_EXPIRES";

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final SecureRandom random = new SecureRandom();

    @Value("${auth.captcha.enabled:true}")
    private boolean enabled;

    @Value("${auth.captcha.length:5}")
    private int length;

    @Value("${auth.captcha.ttl-seconds:300}")
    private int ttlSeconds;

    public boolean isEnabled() {
        return enabled;
    }

    public String createAndStore(HttpSession session) {
        String code = generateCode();
        session.setAttribute(SESSION_CODE, code.toUpperCase(Locale.ROOT));
        session.setAttribute(SESSION_EXPIRES, System.currentTimeMillis() + (ttlSeconds * 1000L));
        return code;
    }

    public boolean validate(HttpSession session, String userInput) {
        if (!enabled) {
            return true;
        }
        if (userInput == null || userInput.trim().isEmpty()) {
            return false;
        }
        Object stored = session.getAttribute(SESSION_CODE);
        Object expires = session.getAttribute(SESSION_EXPIRES);
        session.removeAttribute(SESSION_CODE);
        session.removeAttribute(SESSION_EXPIRES);

        if (stored == null || expires == null) {
            return false;
        }
        if (System.currentTimeMillis() > (Long) expires) {
            return false;
        }
        return stored.toString().equalsIgnoreCase(userInput.trim());
    }

    public byte[] renderImage(String code) throws IOException {
        int width = 160;
        int height = 52;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(232, 240, 245));
        g.fillRect(0, 0, width, height);

        for (int i = 0; i < 18; i++) {
            g.setColor(new Color(13, 115, 119, 40 + random.nextInt(50)));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }

        g.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics metrics = g.getFontMetrics();
        int x = 16;
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(15, 40, 55));
            double angle = (random.nextDouble() - 0.5) * 0.45;
            g.rotate(angle, x, 34);
            g.drawString(String.valueOf(code.charAt(i)), x, 34);
            g.rotate(-angle, x, 34);
            x += metrics.charWidth(code.charAt(i)) + 6;
        }

        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    private String generateCode() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
