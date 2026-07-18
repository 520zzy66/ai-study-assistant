package com.study.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XfyunPropertiesTest {

    @Test
    void serviceCredentialsOverrideLegacySharedCredentials() {
        XfyunProperties properties = new XfyunProperties();
        properties.setAppId("shared-app");
        properties.setApiKey("shared-key");
        properties.setApiSecret("shared-secret");
        properties.getTts().setEnabled(true);
        properties.getTts().setAppId("tts-app");
        properties.getTts().setApiKey("tts-key");
        properties.getTts().setApiSecret("tts-secret");
        properties.getImage().setEnabled(true);
        properties.getImage().setAppId("image-app");
        properties.getImage().setApiKey("image-key");
        properties.getImage().setApiSecret("image-secret");

        assertTrue(properties.isTtsConfigured());
        assertTrue(properties.isImageConfigured());
        assertEquals("tts-app", properties.getTtsAppId());
        assertEquals("tts-key", properties.getTtsApiKey());
        assertEquals("tts-secret", properties.getTtsApiSecret());
        assertEquals("image-app", properties.getImageAppId());
        assertEquals("image-key", properties.getImageApiKey());
        assertEquals("image-secret", properties.getImageApiSecret());
    }

    @Test
    void legacySharedCredentialsRemainAsFallback() {
        XfyunProperties properties = new XfyunProperties();
        properties.setAppId("shared-app");
        properties.setApiKey("shared-key");
        properties.setApiSecret("shared-secret");
        properties.getTts().setEnabled(true);
        properties.getImage().setEnabled(true);

        assertTrue(properties.isTtsConfigured());
        assertTrue(properties.isImageConfigured());
        assertEquals("shared-app", properties.getTtsAppId());
        assertEquals("shared-key", properties.getImageApiKey());
    }

    @Test
    void serviceIsDisabledWhenItsOwnCredentialsAreIncompleteAndNoFallbackExists() {
        XfyunProperties properties = new XfyunProperties();
        properties.getTts().setEnabled(true);
        properties.getTts().setAppId("tts-app");
        properties.getTts().setApiKey("tts-key");
        properties.getImage().setEnabled(true);
        properties.getImage().setApiSecret("image-secret");

        assertFalse(properties.isTtsConfigured());
        assertFalse(properties.isImageConfigured());
    }
}
